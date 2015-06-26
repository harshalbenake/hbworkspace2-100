package com.mopub.nativeads;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.Constants;
import com.mopub.common.DownloadResponse;
import com.mopub.common.DownloadTask;
import com.mopub.common.GpsHelper;
import com.mopub.common.HttpClient;
import com.mopub.common.Preconditions;
import com.mopub.common.VisibleForTesting;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.event.MoPubEvents;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.DeviceUtils;
import com.mopub.common.util.ManifestUtils;
import com.mopub.common.util.ResponseHeader;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.mopub.common.GpsHelper.GpsHelperListener;
import static com.mopub.common.GpsHelper.fetchAdvertisingInfoAsync;
import static com.mopub.nativeads.CustomEventNative.CustomEventNativeListener;
import static com.mopub.nativeads.NativeErrorCode.CONNECTION_ERROR;
import static com.mopub.nativeads.NativeErrorCode.EMPTY_AD_RESPONSE;
import static com.mopub.nativeads.NativeErrorCode.INVALID_REQUEST_URL;
import static com.mopub.nativeads.NativeErrorCode.SERVER_ERROR_RESPONSE_CODE;
import static com.mopub.nativeads.NativeErrorCode.UNEXPECTED_RESPONSE_CODE;
import static com.mopub.nativeads.NativeErrorCode.UNSPECIFIED;

public class MoPubNative {
    public interface MoPubNativeNetworkListener {
        public void onNativeLoad(final NativeResponse nativeResponse);
        public void onNativeFail(final NativeErrorCode errorCode);
    }

    static final MoPubNativeNetworkListener EMPTY_NETWORK_LISTENER = new
            MoPubNativeNetworkListener() {
        @Override
        public void onNativeLoad(@NonNull final NativeResponse nativeResponse) {
            // If this listener is invoked, it means that MoPubNative instance has been destroyed
            // so destroy any leftover incoming NativeResponses
            nativeResponse.destroy();
        }
        @Override
        public void onNativeFail(final NativeErrorCode errorCode) {
        }
    };

    static final MoPubNativeEventListener EMPTY_EVENT_LISTENER = new
            MoPubNativeEventListener() {
        @Override
        public void onNativeImpression(@Nullable final View view) {
        }
        @Override
        public void onNativeClick(@Nullable final View view) {
        }
    };

    public interface MoPubNativeEventListener {
        public void onNativeImpression(final View view);
        public void onNativeClick(final View view);
    }

    /**
     * @deprecated As of release 2.4, use {@link MoPubNativeEventListener} and
     * {@link MoPubNativeNetworkListener} instead.
     */
    @Deprecated
    public interface MoPubNativeListener extends MoPubNativeNetworkListener, MoPubNativeEventListener {
    }

    // must be an activity context since 3rd party networks need it
    @NonNull private final WeakReference<Context> mContext;
    @NonNull private final String mAdUnitId;
    @NonNull private MoPubNativeNetworkListener mMoPubNativeNetworkListener;
    @NonNull private MoPubNativeEventListener mMoPubNativeEventListener;
    @NonNull private Map<String, Object> mLocalExtras = Collections.emptyMap();

    /**
     * @deprecated As of release 2.4, use {@link MoPubNative(Context, String,
     * MoPubNativeNetworkListener)} and {@link #setNativeEventListener(MoPubNativeEventListener)}
     * instead.
     */
    @Deprecated
    public MoPubNative(@NonNull final Context context,
            @NonNull final String adUnitId,
            @NonNull final MoPubNativeListener moPubNativeListener) {
        this(context, adUnitId, (MoPubNativeNetworkListener)moPubNativeListener);
        setNativeEventListener(moPubNativeListener);
    }

    public MoPubNative(@NonNull final Context context,
                @NonNull final String adUnitId,
                @NonNull final MoPubNativeNetworkListener moPubNativeNetworkListener) {
        Preconditions.checkNotNull(context, "Context may not be null.");
        Preconditions.checkNotNull(adUnitId, "AdUnitId may not be null.");
        Preconditions.checkNotNull(moPubNativeNetworkListener, "MoPubNativeNetworkListener may not be null.");

        ManifestUtils.checkNativeActivitiesDeclared(context);

        mContext = new WeakReference<Context>(context);
        mAdUnitId = adUnitId;
        mMoPubNativeNetworkListener = moPubNativeNetworkListener;
        mMoPubNativeEventListener = EMPTY_EVENT_LISTENER;

        // warm up cache for google play services info
        fetchAdvertisingInfoAsync(context, null);
    }

    public void setNativeEventListener(@Nullable final MoPubNativeEventListener nativeEventListener) {
        mMoPubNativeEventListener = (nativeEventListener == null)
                ? EMPTY_EVENT_LISTENER : nativeEventListener;
    }

    public void destroy() {
        mContext.clear();
        mMoPubNativeNetworkListener = EMPTY_NETWORK_LISTENER;
        mMoPubNativeEventListener = EMPTY_EVENT_LISTENER;
    }

    public void setLocalExtras(@Nullable final Map<String, Object> localExtras) {
        if (localExtras == null) {
            mLocalExtras = Collections.emptyMap();
        } else {
            mLocalExtras = new HashMap<String, Object>(localExtras);
        }
    }

    public void makeRequest() {
        makeRequest((RequestParameters)null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters) {
        makeRequest(requestParameters, null);
    }

    public void makeRequest(@Nullable final RequestParameters requestParameters,
            @Nullable Integer sequenceNumber) {
        makeRequest(new NativeGpsHelperListener(requestParameters, sequenceNumber));
    }

    void makeRequest(@NonNull final NativeGpsHelperListener nativeGpsHelperListener) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (!DeviceUtils.isNetworkAvailable(context)) {
            mMoPubNativeNetworkListener.onNativeFail(CONNECTION_ERROR);
            return;
        }

        // If we have access to Google Play Services (GPS) but the advertising info
        // is not cached then guarantee we get it before building the ad request url
        // in the callback, this is a requirement from Google
        GpsHelper.fetchAdvertisingInfoAsync(
                context,
                nativeGpsHelperListener
        );
    }

    private void loadNativeAd(
            @Nullable final RequestParameters requestParameters,
            @Nullable final Integer sequenceNumber) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        final NativeUrlGenerator generator = new NativeUrlGenerator(context)
                .withAdUnitId(mAdUnitId)
                .withRequest(requestParameters);

        if (sequenceNumber != null) {
            generator.withSequenceNumber(sequenceNumber);
        }

        final String endpointUrl = generator.generateUrlString(Constants.HOST);

        if (endpointUrl != null) {
            MoPubLog.d("Loading ad from: " + endpointUrl);
        }

        requestNativeAd(endpointUrl);
    }

    void loadNativeAd(final RequestParameters requestParameters) {
        loadNativeAd(requestParameters, null);
    }

    void requestNativeAd(@Nullable final String endpointUrl) {
        final Context context = getContextOrDestroy();
        if (context == null) {
            return;
        }

        if (endpointUrl == null) {
            mMoPubNativeNetworkListener.onNativeFail(INVALID_REQUEST_URL);
            return;
        }

        final HttpGet httpGet;
        try {
            httpGet = HttpClient.initializeHttpGet(endpointUrl, context);
        } catch (IllegalArgumentException e) {
            mMoPubNativeNetworkListener.onNativeFail(INVALID_REQUEST_URL);
            return;
        }

        downloadJson(httpGet);
    }

    private void downloadJson(final HttpUriRequest httpUriRequest) {
        final DownloadTask jsonDownloadTask = new DownloadTask(
                new AdRequestListener(),
                MoPubEvents.Type.AD_REQUEST
        );

        try {
            AsyncTasks.safeExecuteOnExecutor(jsonDownloadTask, httpUriRequest);
        } catch (Exception e) {
            MoPubLog.d("Failed to download json", e);
            mMoPubNativeNetworkListener.onNativeFail(UNSPECIFIED);
        }

    }

    Context getContextOrDestroy() {
        final Context context = mContext.get();
        if (context == null) {
            destroy();
            MoPubLog.d("Weak reference to Activity Context in MoPubNative became null. This instance" +
                    " of MoPubNative is destroyed and No more requests will be processed.");
        }
        return context;
    }

    // Do not store this class as a member of MoPubNative; will result in circular reference
    class NativeGpsHelperListener implements GpsHelperListener {
        private final RequestParameters mRequestParameters;
        private final Integer mSequenceNumber;

        NativeGpsHelperListener(RequestParameters requestParameters, Integer sequenceNumber) {
            mRequestParameters = requestParameters;
            mSequenceNumber = sequenceNumber;
        }

        NativeGpsHelperListener(RequestParameters requestParameters) {
            this(requestParameters, null);
        }

        @Override
        public void onFetchAdInfoCompleted() {

            loadNativeAd(mRequestParameters, mSequenceNumber);
        }
    }

    private class AdRequestListener implements DownloadTask.DownloadTaskListener {
        @Override
        public void onComplete(@Nullable final String url,
                @Nullable final DownloadResponse downloadResponse) {
            if (downloadResponse == null) {
                mMoPubNativeNetworkListener.onNativeFail(UNSPECIFIED);
            } else if (downloadResponse.getStatusCode() >= 500 &&
                    downloadResponse.getStatusCode() < 600) {
                mMoPubNativeNetworkListener.onNativeFail(SERVER_ERROR_RESPONSE_CODE);
            } else if (downloadResponse.getStatusCode() != HttpStatus.SC_OK) {
                mMoPubNativeNetworkListener.onNativeFail(UNEXPECTED_RESPONSE_CODE);
            } else if (downloadResponse.getContentLength() == 0) {
                mMoPubNativeNetworkListener.onNativeFail(EMPTY_AD_RESPONSE);
            } else {
                final CustomEventNativeListener customEventNativeListener =
                        new CustomEventNativeListener() {
                            @Override
                            public void onNativeAdLoaded(@NonNull final NativeAdInterface nativeAd) {
                                final Context context = getContextOrDestroy();
                                if (context == null) {
                                    return;
                                }
                                mMoPubNativeNetworkListener.onNativeLoad(new NativeResponse(context,
                                        downloadResponse,
                                        mAdUnitId,
                                        nativeAd,
                                        mMoPubNativeEventListener));
                            }

                            @Override
                            public void onNativeAdFailed(final NativeErrorCode errorCode) {
                                requestNativeAd(downloadResponse.getFirstHeader(ResponseHeader.FAIL_URL));
                            }
                        };

                final Context context = getContextOrDestroy();
                if (context == null) {
                    return;
                }

                CustomEventNativeAdapter.loadNativeAd(
                        context,
                        mLocalExtras,
                        downloadResponse,
                        customEventNativeListener
                );
            }
        }
    }

    @NonNull
    @VisibleForTesting
    @Deprecated
    MoPubNativeNetworkListener getMoPubNativeNetworkListener() {
        return mMoPubNativeNetworkListener;
    }

    @NonNull
    @VisibleForTesting
    @Deprecated
    MoPubNativeEventListener getMoPubNativeEventListener() {
        return mMoPubNativeEventListener;
    }
}
