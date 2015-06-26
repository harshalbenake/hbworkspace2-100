package com.mopub.nativeads;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.DownloadResponse;
import com.mopub.common.DownloadTask;
import com.mopub.common.DownloadTask.DownloadTaskListener;
import com.mopub.common.HttpClient;
import com.mopub.common.HttpResponses;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.event.Event;
import com.mopub.common.event.MoPubEvents;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.nativeads.MoPubNativeAdPositioning.MoPubClientPositioning;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Requests positioning information from the MoPub ad server.
 *
 * The expected JSON format contains a set of rules for fixed and repeating positions. For example:
 * {
 *   fixed: [{
 *     position: 7
 *   }, {
 *     section : 1
 *     position: 6
 *   }],
 *   repeating:  {
 *     interval: 12
 *   }
 * }
 *
 * Both fixed and repeating rules are optional. If they exist they must follow the following
 * guidelines:
 *
 * fixed - contains a set of positioning objects, each with an optional section and a required
 * position. Section is used for iOS clients only, and non-zero sections are ignored on Android.
 *
 * repeating - contains a required interval, which must be 2 or greater.
 *
 * The JSON parsing logic treats any violations to the above spec as invalid,
 * rather than trying to continue with a partially valid response.
 */
class ServerPositioningSource implements PositioningSource {

    private static final double DEFAULT_RETRY_TIME_MILLISECONDS = 1000; // 1 second
    private static final double EXPONENTIAL_BACKOFF_FACTOR = 2;

    @VisibleForTesting
    static int MAXIMUM_RETRY_TIME_MILLISECONDS = 5 * 60 * 1000; // 5 minutes.

    private static final String FIXED_KEY = "fixed";
    private static final String SECTION_KEY = "section";
    private static final String POSITION_KEY = "position";
    private static final String REPEATING_KEY = "repeating";
    private static final String INTERVAL_KEY = "interval";

    // Max value to avoid bad integer math calculations. This is 2 ^ 16.
    private static final int MAX_VALUE = 1 << 16;

    @NonNull private final Context mContext;

    @NonNull private final DownloadTaskProvider mDownloadTaskProvider;

    // Handler and runnable for retrying after a failed response.
    @NonNull private final Handler mRetryHandler;
    @NonNull private final Runnable mRetryRunnable;

    // Only exists while a request is in flight.
    @Nullable private DownloadTask mDownloadTask;

    @Nullable private PositioningListener mListener;
    private int mRetryCount;
    @Nullable private String mRetryUrl;

    ServerPositioningSource(@NonNull final Context context) {
        this(context, new DownloadTaskProvider());
    }

    @VisibleForTesting
    ServerPositioningSource(@NonNull final Context context,
            @NonNull final DownloadTaskProvider downloadTaskProvider) {
        mContext = context.getApplicationContext();
        mDownloadTaskProvider = downloadTaskProvider;
        mRetryHandler = new Handler();
        mRetryRunnable = new Runnable() {
            @Override
            public void run() {
                requestPositioningInternal();
            }
        };
    }

    @Override
    public void loadPositions(@NonNull String adUnitId, @NonNull PositioningListener listener) {
        // If a request is in flight, remove it.
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }

        // If a retry is pending remove it.
        if (mRetryCount > 0) {
            mRetryHandler.removeCallbacks(mRetryRunnable);
            mRetryCount = 0;
        }

        mListener = listener;
        mRetryUrl = new PositioningUrlGenerator(mContext)
                .withAdUnitId(adUnitId)
                .generateUrlString(Constants.HOST);
        requestPositioningInternal();
    }

    private void requestPositioningInternal() {
        MoPubLog.d("Loading positioning from: " + mRetryUrl);
        mDownloadTask = mDownloadTaskProvider.get(mTaskListener);
        final HttpGet httpGet = HttpClient.initializeHttpGet(mRetryUrl, mContext);
        AsyncTasks.safeExecuteOnExecutor(mDownloadTask, httpGet);
    }

    @NonNull
    @VisibleForTesting
    DownloadTaskListener mTaskListener = new DownloadTask.DownloadTaskListener() {
        @Override
        public void onComplete(@Nullable final String url,
                @Nullable final DownloadResponse downloadResponse) {
            mDownloadTask = null;
            if (downloadResponse == null || downloadResponse.getStatusCode() != HttpStatus.SC_OK) {
                MoPubLog.e("Invalid positioning download response");
                handleFailure();
                return;
            }

            String responseText = HttpResponses.asResponseString(downloadResponse);
            MoPubClientPositioning positioning;
            try {
                positioning = parseJsonResponse(responseText);
            } catch (JSONException exception) {
                MoPubLog.e("Error parsing JSON: ", exception);
                MoPubEvents.log(new Event.Builder("", "").build());
                handleFailure();
                return;
            }

            handleSuccess(positioning);
        }
    };

    @VisibleForTesting
    static class DownloadTaskProvider {
        @NonNull
        DownloadTask get(DownloadTaskListener listener) {
            return new DownloadTask(listener, MoPubEvents.Type.POSITIONING_REQUEST);
        }
    }

    private void handleSuccess(@NonNull MoPubClientPositioning positioning) {
        if (mListener != null) {
            mListener.onLoad(positioning);
        }
        mListener = null;
        mRetryCount = 0;
    }

    private void handleFailure() {
        double multiplier = Math.pow(EXPONENTIAL_BACKOFF_FACTOR, mRetryCount + 1);
        int delay = (int) (DEFAULT_RETRY_TIME_MILLISECONDS * multiplier);
        if (delay >= MAXIMUM_RETRY_TIME_MILLISECONDS) {
            MoPubLog.d("Error downloading positioning information");
            if (mListener != null) {
                mListener.onFailed();
            }
            mListener = null;
            return;
        }

        mRetryCount++;
        mRetryHandler.postDelayed(mRetryRunnable, delay);
    }

    @NonNull
    @VisibleForTesting
    MoPubClientPositioning parseJsonResponse(@Nullable String json) throws JSONException {
        if (json == null || json.equals("")) {
            throw new JSONException("Empty response");
        }

        // If the server returns an error explicitly, throw an error with the message.
        JSONObject jsonObject = new JSONObject(json);
        String error = jsonObject.optString("error", null);
        if (error != null) {
            throw new JSONException(error);
        }

        // Parse fixed and repeating rules.
        JSONArray fixed = jsonObject.optJSONArray(FIXED_KEY);
        JSONObject repeating = jsonObject.optJSONObject(REPEATING_KEY);
        MoPubClientPositioning positioning = new MoPubClientPositioning();
        if (fixed == null && repeating == null) {
            throw new JSONException("Must contain fixed or repeating positions");
        }
        if (fixed != null) {
            parseFixedJson(fixed, positioning);
        }
        if (repeating != null) {
            parseRepeatingJson(repeating, positioning);
        }
        return positioning;
    }

    private void parseFixedJson(@NonNull final JSONArray fixed,
            @NonNull final MoPubClientPositioning positioning) throws JSONException {
        for (int i = 0; i < fixed.length(); ++i) {
            JSONObject positionObject = fixed.getJSONObject(i);
            int section = positionObject.optInt(SECTION_KEY, 0);
            if (section < 0) {
                throw new JSONException("Invalid section " + section + " in JSON response");
            }
            if (section > 0) {
                // Ignore sections > 0.
                continue;
            }
            int position = positionObject.getInt(POSITION_KEY);
            if (position < 0 || position > MAX_VALUE) {
                throw new JSONException("Invalid position " + position + " in JSON response");
            }
            positioning.addFixedPosition(position);
        }
    }

    private void parseRepeatingJson(@NonNull final JSONObject repeatingObject,
            @NonNull final MoPubClientPositioning positioning) throws JSONException {
        int interval = repeatingObject.getInt(INTERVAL_KEY);
        if (interval < 2 || interval > MAX_VALUE) {
            throw new JSONException("Invalid interval " + interval + " in JSON response");
        }
        positioning.enableRepeatingPositions(interval);
    }
}
