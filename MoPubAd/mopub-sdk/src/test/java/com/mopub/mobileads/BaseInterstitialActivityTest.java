package com.mopub.mobileads;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import static com.mopub.mobileads.AdFetcher.AD_CONFIGURATION_KEY;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.stub;

@RunWith(SdkTestRunner.class)
public class BaseInterstitialActivityTest {
    private BaseInterstitialActivity subject;
    @Mock private AdConfiguration adConfiguration;

    // Make a concrete version of the abstract class for testing purposes.
    private static class TestInterstitialActivity extends BaseInterstitialActivity {
        View view;

        @Override
        public View getAdView() {
            if (view == null) {
                view = new View(this);
            }
            return view;
        }
    }

    @Before
    public void setup() {

    }

    @Test
    public void onCreate_shouldCreateView() throws Exception {
        subject = Robolectric.buildActivity(TestInterstitialActivity.class).create().get();
        View adView = getContentView(subject).getChildAt(0);

        assertThat(adView).isNotNull();
    }

    @Test
    public void onDestroy_shouldCleanUpContentView() throws Exception {
        subject = Robolectric.buildActivity(
                TestInterstitialActivity.class).create().destroy().get();

        assertThat(getContentView(subject).getChildCount()).isEqualTo(0);
    }

    @Test
    public void getAdConfiguration_shouldReturnAdConfigurationFromIntent() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        Intent intent = new Intent(context, TestInterstitialActivity.class);
        intent.putExtra(AD_CONFIGURATION_KEY, adConfiguration);

        subject = Robolectric.buildActivity(TestInterstitialActivity.class)
                .withIntent(intent)
                .create().get();
        assertThat(subject.getAdConfiguration()).isNotNull();
    }

    @Test
    public void getAdConfiguration_withMissingOrWrongAdConfiguration_shouldReturnNull() throws Exception {
        Context context = Robolectric.buildActivity(Activity.class).create().get();
        Intent intent = new Intent(context, TestInterstitialActivity.class);
        // This intent is missing an AdConfiguration extra.

        subject = Robolectric.buildActivity(TestInterstitialActivity.class)
                .withIntent(intent)
                .create().get();

        assertThat(subject.getAdConfiguration()).isNull();
    }

    protected FrameLayout getContentView(BaseInterstitialActivity subject) {
        return (FrameLayout) ((ViewGroup) subject.findViewById(android.R.id.content)).getChildAt(0);
    }

    protected void resetMockedView(View view) {
        reset(view);
        stub(view.getLayoutParams()).toReturn(
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
    }
}
