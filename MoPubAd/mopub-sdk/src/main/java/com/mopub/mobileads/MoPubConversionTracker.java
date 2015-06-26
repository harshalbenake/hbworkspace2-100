package com.mopub.mobileads;

import android.content.Context;
import android.content.SharedPreferences;

import com.mopub.common.BaseUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.GpsHelper;
import com.mopub.common.SharedPreferencesHelper;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.factories.HttpClientFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import static com.mopub.common.GpsHelper.GpsHelperListener;

public class MoPubConversionTracker {
    private Context mContext;
    private String mIsTrackedKey;
    private SharedPreferences mSharedPreferences;
    private String mPackageName;

    public void reportAppOpen(Context context) {
        if (context == null) {
            return;
        }

        mContext = context;
        mPackageName = mContext.getPackageName();
        mIsTrackedKey = mPackageName + " tracked";
        mSharedPreferences = SharedPreferencesHelper.getSharedPreferences(mContext);

        if (!isAlreadyTracked()) {
            GpsHelper.fetchAdvertisingInfoAsync(mContext, new GpsHelperListener() {
                @Override
                public void onFetchAdInfoCompleted() {
                    new Thread(new TrackOpen()).start();
                }
            });
        } else {
            MoPubLog.d("Conversion already tracked");
        }
    }

    private boolean isAlreadyTracked() {
        return mSharedPreferences.getBoolean(mIsTrackedKey, false);
    }

    private class ConversionUrlGenerator extends BaseUrlGenerator {
        @Override
        public String generateUrlString(String serverHostname) {
            initUrlString(serverHostname, Constants.CONVERSION_TRACKING_HANDLER);

            setApiVersion("6");
            setPackageId(mPackageName);
            ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);
            setUdid(clientMetadata.getAdvertisingId());
            setDoNotTrack(clientMetadata.isDoNotTrackSet());
            setAppVersion(clientMetadata.getAppVersion());
            return getFinalUrlString();
        }

        private void setPackageId(String packageName) {
            addParam("id", packageName);
        }
    }

    private class TrackOpen implements Runnable {
        public void run() {
            String url = new ConversionUrlGenerator().generateUrlString(Constants.HOST);
            MoPubLog.d("Conversion track: " + url);

            DefaultHttpClient httpClient = HttpClientFactory.create();
            HttpResponse response;
            try {
                HttpGet httpget = new HttpGet(url);
                response = httpClient.execute(httpget);
            } catch (Exception e) {
                MoPubLog.d("Conversion track failed [" + e.getClass().getSimpleName() + "]: " + url);
                return;
            }

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                MoPubLog.d("Conversion track failed: Status code != 200.");
                return;
            }

            HttpEntity entity = response.getEntity();
            if (entity == null || entity.getContentLength() == 0) {
                MoPubLog.d("Conversion track failed: Response was empty.");
                return;
            }

            // If we made it here, the request has been tracked
            MoPubLog.d("Conversion track successful.");
            mSharedPreferences
                    .edit()
                    .putBoolean(mIsTrackedKey, true)
                    .commit();
        }
    }
}
