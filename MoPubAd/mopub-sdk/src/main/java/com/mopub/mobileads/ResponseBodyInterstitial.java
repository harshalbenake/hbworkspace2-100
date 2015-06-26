package com.mopub.mobileads;

import android.content.Context;

import java.util.Map;

import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.MoPubErrorCode.NETWORK_INVALID_STATE;

public abstract class ResponseBodyInterstitial extends CustomEventInterstitial {
    private EventForwardingBroadcastReceiver mBroadcastReceiver;
    protected Context mContext;
    protected AdConfiguration mAdConfiguration;
    long mBroadcastIdentifier;

    abstract protected void extractExtras(Map<String, String> serverExtras);
    abstract protected void preRenderHtml(CustomEventInterstitialListener customEventInterstitialListener);
    public abstract void showInterstitial();

    @Override
    public void loadInterstitial(
            Context context,
            CustomEventInterstitialListener customEventInterstitialListener,
            Map<String, Object> localExtras,
            Map<String, String> serverExtras) {

        mContext = context;

        if (extrasAreValid(serverExtras)) {
            extractExtras(serverExtras);
        } else {
            customEventInterstitialListener.onInterstitialFailed(NETWORK_INVALID_STATE);
            return;
        }

        mAdConfiguration = AdConfiguration.extractFromMap(localExtras);
        if (mAdConfiguration != null) {
            mBroadcastIdentifier = mAdConfiguration.getBroadcastIdentifier();
        }

        mBroadcastReceiver = new EventForwardingBroadcastReceiver(customEventInterstitialListener, mBroadcastIdentifier);
        mBroadcastReceiver.register(context);

        preRenderHtml(customEventInterstitialListener);
    }

    @Override
    public void onInvalidate() {
        if (mBroadcastReceiver != null) {
            mBroadcastReceiver.unregister();
        }
    }

    private boolean extrasAreValid(Map<String,String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }
}
