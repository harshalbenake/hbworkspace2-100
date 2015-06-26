package com.mopub.mobileads;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout.LayoutParams;

import com.mopub.common.CloseableLayout;
import com.mopub.common.CloseableLayout.OnCloseListener;

import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;

abstract class BaseInterstitialActivity extends Activity {
    enum JavaScriptWebViewCallbacks {
        // The ad server appends these functions to the MRAID javascript to help with third party
        // impression tracking.
        WEB_VIEW_DID_APPEAR("webviewDidAppear();"),
        WEB_VIEW_DID_CLOSE("webviewDidClose();");

        private String mJavascript;
        private JavaScriptWebViewCallbacks(String javascript) {
            mJavascript = javascript;
        }

        protected String getJavascript() {
            return mJavascript;
        }

        protected String getUrl() {
            return "javascript:" + mJavascript;
        }
    }

    private CloseableLayout mCloseableLayout;
    private long mBroadcastIdentifier;

    public abstract View getAdView();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        View adView = getAdView();

        mCloseableLayout = new CloseableLayout(this);
        mCloseableLayout.setOnCloseListener(new OnCloseListener() {
            @Override
            public void onClose() {
                finish();
            }
        });
        mCloseableLayout.addView(adView,
                new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setContentView(mCloseableLayout);

        final AdConfiguration adConfiguration = getAdConfiguration();
        if (adConfiguration != null) {
            mBroadcastIdentifier = adConfiguration.getBroadcastIdentifier();
        }
    }

    @Override
    protected void onDestroy() {
        mCloseableLayout.removeAllViews();
        super.onDestroy();
    }

    long getBroadcastIdentifier() {
        return mBroadcastIdentifier;
    }

    protected void showInterstitialCloseButton() {
        mCloseableLayout.setCloseVisible(true);
    }

    protected void hideInterstitialCloseButton() {
        mCloseableLayout.setCloseVisible(false);
    }

    protected AdConfiguration getAdConfiguration() {
        AdConfiguration adConfiguration;
        try {
            adConfiguration = (AdConfiguration) getIntent().getSerializableExtra(AD_CONFIGURATION_KEY);
        } catch (ClassCastException e) {
            adConfiguration = null;
        }
        return adConfiguration;
    }
}
