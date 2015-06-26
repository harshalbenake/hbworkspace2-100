package com.mopub.mraid;


import android.net.Uri;
import android.support.annotation.NonNull;

import com.mopub.mobileads.MraidActivity;
import com.mopub.mobileads.ResponseBodyInterstitial;

import java.util.Map;

import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;

class MraidInterstitial extends ResponseBodyInterstitial {
    private String mHtmlData;

    @Override
    protected void extractExtras(@NonNull Map<String, String> serverExtras) {
        mHtmlData = Uri.decode(serverExtras.get(HTML_RESPONSE_BODY_KEY));
    }

    @Override
    protected void preRenderHtml(@NonNull CustomEventInterstitialListener
            customEventInterstitialListener) {
        MraidActivity.preRenderHtml(mContext, customEventInterstitialListener, mHtmlData);
    }

    @Override
    public void showInterstitial() {
        MraidActivity.start(mContext, mHtmlData, mAdConfiguration);
    }
}
