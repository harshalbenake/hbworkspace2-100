package com.mopub.mobileads;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.IntentUtils;
import com.mopub.mraid.MraidVideoViewController;

import static com.mopub.mobileads.EventForwardingBroadcastReceiver.ACTION_INTERSTITIAL_FAIL;
import static com.mopub.mobileads.EventForwardingBroadcastReceiver.broadcastAction;

public class MraidVideoPlayerActivity extends BaseVideoPlayerActivity implements BaseVideoViewController.BaseVideoViewControllerListener {
    @Nullable private BaseVideoViewController mBaseVideoController;
    private long mBroadcastIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        final AdConfiguration adConfiguration = getAdConfiguration();
        if (adConfiguration != null) {
            mBroadcastIdentifier = adConfiguration.getBroadcastIdentifier();
        } else {
            MoPubLog.d("Unable to obtain broadcast identifier. Video interactions cannot be tracked.");
        }

        try {
            mBaseVideoController = createVideoViewController();
        } catch (IllegalStateException e) {
            // This can happen if the activity was started without valid intent extras. We leave
            // mBaseVideoController set to null, and finish the activity immediately.
            
            broadcastAction(this, mBroadcastIdentifier, ACTION_INTERSTITIAL_FAIL);
            finish();
            return;
        }

        mBaseVideoController.onCreate();
    }

    @Override
    protected void onPause() {
        if (mBaseVideoController != null) {
            mBaseVideoController.onPause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBaseVideoController != null) {
            mBaseVideoController.onResume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mBaseVideoController != null) {
            mBaseVideoController.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (mBaseVideoController != null && mBaseVideoController.backButtonEnabled()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (mBaseVideoController != null) {
            mBaseVideoController.onActivityResult(requestCode, resultCode, data);
        }
    }

    private AdConfiguration getAdConfiguration() {
        AdConfiguration adConfiguration;
        try {
            adConfiguration = (AdConfiguration) getIntent().getSerializableExtra(AdFetcher.AD_CONFIGURATION_KEY);
        } catch (ClassCastException e) {
            adConfiguration = null;
        }
        return adConfiguration;
    }

    private BaseVideoViewController createVideoViewController() throws IllegalStateException {
        String clazz = getIntent().getStringExtra(VIDEO_CLASS_EXTRAS_KEY);

        if ("vast".equals(clazz)) {
            return new VastVideoViewController(this, getIntent().getExtras(), mBroadcastIdentifier, this);
        } else if ("mraid".equals(clazz)) {
            return new MraidVideoViewController(this, getIntent().getExtras(), mBroadcastIdentifier, this);
        } else {
            throw new IllegalStateException("Unsupported video type: " + clazz);
        }
    }

    /**
     * Implementation of BaseVideoViewControllerListener
     */

    @Override
    public void onSetContentView(final View view) {
        setContentView(view);
    }

    @Override
    public void onSetRequestedOrientation(final int requestedOrientation) {
        setRequestedOrientation(requestedOrientation);
    }

    @Override
    public void onFinish() {
        finish();
    }

    @Override
    public void onStartActivityForResult(final Class<? extends Activity> clazz,
            final int requestCode,
            final Bundle extras) {
        if (clazz == null) {
            return;
        }

        final Intent intent = IntentUtils.getStartActivityIntent(this, clazz, extras);

        try {
            startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            MoPubLog.d("Activity " + clazz.getName() + " not found. Did you declare it in your AndroidManifest.xml?");
        }
    }

    @Deprecated // for testing
    BaseVideoViewController getBaseVideoViewController() {
        return mBaseVideoController;
    }

    @Deprecated // for testing
    void setBaseVideoViewController(final BaseVideoViewController baseVideoViewController) {
        mBaseVideoController = baseVideoViewController;
    }
}
