package com.mopub.mraid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.WebViewClient;

import com.mopub.common.test.support.SdkTestRunner;
import com.mopub.mobileads.AdConfiguration;
import com.mopub.mraid.MraidBridge.MraidBridgeListener;
import com.mopub.mraid.MraidBridge.MraidWebView;
import com.mopub.mraid.MraidNativeCommandHandler.MraidCommandFailureListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.robolectric.Robolectric;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SdkTestRunner.class)
public class MraidBridgeTest {
    @Mock AdConfiguration mockAdConfiguration;
    @Mock MraidNativeCommandHandler mockNativeCommandHandler;
    @Mock MraidBridgeListener mockBridgeListener;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MraidWebView mockBannerWebView;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) MraidWebView mockInterstitialWebView;
    @Captor ArgumentCaptor<WebViewClient> bannerWebViewClientCaptor;

    private Activity activity;
    private MraidBridge subjectBanner;
    private MraidBridge subjectInterstitial;

    @Before
    public void setUp() {
        activity = Robolectric.buildActivity(Activity.class).create().get();

        subjectBanner = new MraidBridge(
                mockAdConfiguration, PlacementType.INLINE, mockNativeCommandHandler);
        subjectBanner.setMraidBridgeListener(mockBridgeListener);
        subjectBanner.attachView(mockBannerWebView);

        subjectInterstitial = new MraidBridge(
                mockAdConfiguration, PlacementType.INTERSTITIAL, mockNativeCommandHandler);
        subjectInterstitial.setMraidBridgeListener(mockBridgeListener);
        subjectInterstitial.attachView(mockInterstitialWebView);

        verify(mockBannerWebView).setWebViewClient(bannerWebViewClientCaptor.capture());
        reset(mockBannerWebView);
    }

    @Test
    public void attachView_thenDetach_shouldSetMRaidWebView_thenShouldClear() {
        assertThat(subjectBanner.getMraidWebView()).isEqualTo(mockBannerWebView);

        subjectBanner.detach();
        assertThat(subjectBanner.getMraidWebView()).isNull();
    }

    @Test
    public void attachView_thenOnPageFinished_shouldFireReady() {
        bannerWebViewClientCaptor.getValue().onPageFinished(mockBannerWebView, "fake://url");

        verify(mockBridgeListener).onPageLoaded();
    }

    @Test
    public void attachView_thenOnPageFinished_twice_shouldNotFireReadySecondTime() {
        bannerWebViewClientCaptor.getValue().onPageFinished(mockBannerWebView, "fake://url");
        bannerWebViewClientCaptor.getValue().onPageFinished(mockBannerWebView, "fake://url2");

        verify(mockBridgeListener, times(1)).onPageLoaded();
    }

    @Test
    public void attachView_thenSetContentHtml_shouldCallLoadDataWithBaseURL() {
        subjectBanner.setContentHtml("test-html");

        verify(mockBannerWebView).loadDataWithBaseURL(
                null, "test-html", "text/html", "UTF-8", null);
    }

    @Test
    public void handleShouldOverrideUrl_invalidUrl_shouldFireErrorEvent() {
        boolean result = subjectBanner.handleShouldOverrideUrl("bad bad bad");

        verify(mockBannerWebView).loadUrl(startsWith(
                "javascript:window.mraidbridge.notifyErrorEvent"));
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_mopubUrl_shouldNeverLoadUrl_shouldReturnTrue() {
        boolean result = subjectBanner.handleShouldOverrideUrl("mopub://special-mopub-command");

        verify(mockBannerWebView, never()).loadUrl(anyString());
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_mraidUrl_invalid_shouldFireErrorEvent_shouldReturnTrue() {
        boolean result = subjectBanner.handleShouldOverrideUrl("mraid://bad-command");

        verify(mockBannerWebView).loadUrl(startsWith(
                "javascript:window.mraidbridge.notifyErrorEvent"));
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_smsUrl_notClicked_shouldReturnFalse() {
        boolean result = subjectBanner.handleShouldOverrideUrl("sms://123456789");

        assertThat(result).isFalse();
    }

    @Test
    public void handleShouldOverrideUrl_smsUrl_clicked_shouldStartActivity() {
        subjectBanner.setClicked(true);
        reset(mockBannerWebView);
        when(mockBannerWebView.getContext()).thenReturn(activity);

        boolean result = subjectBanner.handleShouldOverrideUrl("sms://123456789");

        Intent startedIntent = Robolectric.getShadowApplication().getNextStartedActivity();
        assertThat(startedIntent).isNotNull();
        assertThat(startedIntent.getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK).isNotEqualTo(0);
        assertThat(startedIntent.getComponent()).isNull();
        assertThat(result).isTrue();
    }

    @Test
    public void handleShouldOverrideUrl_normalUrl_shouldReturnFalse() {
        boolean result = subjectBanner.handleShouldOverrideUrl("http://www.mopub.com");

        assertThat(result).isFalse();
    }

    @Test(expected = MraidCommandException.class)
    public void runCommand_requiresClick_notClicked_shouldThrowException()
            throws MraidCommandException {
        subjectBanner = new MraidBridge(mockAdConfiguration, PlacementType.INLINE);
        subjectBanner.attachView(mockBannerWebView);
        subjectBanner.setClicked(false);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "http://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);
    }

    public void runCommand_requiresClick_clicked_shouldNotThrowException()
            throws MraidCommandException {
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "http://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);
    }

    @Test(expected = MraidCommandException.class)
    public void runCommand_interstitial_requiresClick_notClicked_shouldThrowException()
            throws MraidCommandException {
        subjectInterstitial.setClicked(false);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "http://valid-url");

        subjectInterstitial.runCommand(MraidJavascriptCommand.OPEN, params);
    }

    @Test
    public void runCommand_interstitial_requiresClick_clicked_shouldNotThrowException()
            throws MraidCommandException {
        subjectInterstitial.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("url", "http://valid-url");

        subjectInterstitial.runCommand(MraidJavascriptCommand.OPEN, params);
    }

    @Test
    public void runCommand_close_shouldCallListener()
            throws MraidCommandException {
        Map<String, String> params = new HashMap<String, String>();

        subjectBanner.runCommand(MraidJavascriptCommand.CLOSE, params);

        verify(mockBridgeListener).onClose();
    }

    @Test
    public void runCommand_expand_shouldCallListener()
            throws MraidCommandException {
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        verify(mockBridgeListener).onExpand(null, true);
    }

    @Test
    public void runCommand_expand_withUrl_shouldCallListener()
            throws MraidCommandException {
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("url", "http://valid-url");
        params.put("shouldUseCustomClose", "true");

        subjectBanner.runCommand(MraidJavascriptCommand.EXPAND, params);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockBridgeListener).onExpand(
                uriCaptor.capture(), eq(true));
        assertThat(uriCaptor.getValue().toString()).isEqualTo("http://valid-url");
    }

    @Test
    public void runCommand_playVideo_shouldCallListener()
            throws MraidCommandException {
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "http://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.PLAY_VIDEO, params);

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(mockBridgeListener).onPlayVideo(uriCaptor.capture());
        assertThat(uriCaptor.getValue().toString()).isEqualTo("http://valid-url");
    }

    @Test
    public void runCommand_storePicture_shouldCallListener()
            throws MraidCommandException {
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("uri", "http://valid-url");

        subjectBanner.runCommand(MraidJavascriptCommand.STORE_PICTURE, params);

        verify(mockNativeCommandHandler).storePicture(any(Context.class), eq("http://valid-url"),
                any(MraidCommandFailureListener.class));
    }

    @Test
    public void runCommand_createCalendarEvent_shouldCallListener()
            throws MraidCommandException {
        subjectBanner.setClicked(true);
        Map<String, String> params = new HashMap<String, String>();
        params.put("eventName", "Dinner at my house");

        subjectBanner.runCommand(MraidJavascriptCommand.CREATE_CALENDAR_EVENT, params);

        verify(mockNativeCommandHandler).createCalendarEvent(any(Context.class),
                anyMapOf(String.class, String.class));
    }
}
