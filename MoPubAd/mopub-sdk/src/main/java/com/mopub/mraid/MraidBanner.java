package com.mopub.mraid;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.AdViewController;
import com.mopub.mobileads.CustomEventBanner;
import com.mopub.mobileads.factories.MraidControllerFactory;
import com.mopub.mraid.MraidController.MraidListener;

import java.util.Map;

import static com.mopub.mobileads.AdFetcher.HTML_RESPONSE_BODY_KEY;
import static com.mopub.mobileads.MoPubErrorCode.MRAID_LOAD_ERROR;

class MraidBanner extends CustomEventBanner {

    @Nullable private MraidController mMraidController;
    @Nullable private CustomEventBannerListener mBannerListener;
    @Nullable private MraidWebViewDebugListener mDebugListener;

    @Override
    protected void loadBanner(@NonNull Context context,
                    @NonNull CustomEventBannerListener customEventBannerListener,
                    @NonNull Map<String, Object> localExtras,
                    @NonNull Map<String, String> serverExtras) {
        mBannerListener = customEventBannerListener;

        String htmlData;
        if (extrasAreValid(serverExtras)) {
            htmlData = Uri.decode(serverExtras.get(HTML_RESPONSE_BODY_KEY));
        } else {
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        AdConfiguration adConfiguration = AdConfiguration.extractFromMap(localExtras);
        if (htmlData == null || adConfiguration == null) {
            mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            return;
        }

        mMraidController = MraidControllerFactory.create(
                    context, adConfiguration, PlacementType.INLINE);

        mMraidController.setDebugListener(mDebugListener);
        mMraidController.setMraidListener(new MraidListener() {
            @Override
            public void onLoaded(View view) {
                // Honoring the server dimensions forces the WebView to be the size of the banner
                AdViewController.setShouldHonorServerDimensions(view);
                mBannerListener.onBannerLoaded(view);
            }

            @Override
            public void onFailedToLoad() {
                mBannerListener.onBannerFailed(MRAID_LOAD_ERROR);
            }

            @Override
            public void onExpand() {
                mBannerListener.onBannerExpanded();
                mBannerListener.onBannerClicked();
            }

            @Override
            public void onOpen() {
                mBannerListener.onBannerClicked();
            }

            @Override
            public void onClose() {
                mBannerListener.onBannerCollapsed();
            }
        });
        mMraidController.loadContent(htmlData);
    }

    @Override
    protected void onInvalidate() {
        if (mMraidController != null) {
            mMraidController.setMraidListener(null);
            mMraidController.destroy();
        }
    }

    private boolean extrasAreValid(Map<String, String> serverExtras) {
        return serverExtras.containsKey(HTML_RESPONSE_BODY_KEY);
    }

    @VisibleForTesting
    public void setDebugListener(@Nullable MraidWebViewDebugListener debugListener) {
        mDebugListener = debugListener;
        if (mMraidController != null) {
            mMraidController.setDebugListener(debugListener);
        }
    }
}
