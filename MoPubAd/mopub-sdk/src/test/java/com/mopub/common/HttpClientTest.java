package com.mopub.common;

import android.app.Activity;
import android.webkit.WebView;

import com.mopub.common.util.ResponseHeader;
import com.mopub.common.util.test.support.CommonUtils;

import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.tester.org.apache.http.RequestMatcher;
import org.robolectric.tester.org.apache.http.TestHttpResponse;

import static com.mopub.common.HttpClient.getWebViewUserAgent;
import static com.mopub.common.HttpClient.initializeHttpGet;
import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class HttpClientTest {
    static final String url = "http://www.mopub.com";
    private Activity context;
    private String userAgent;

    @Before
    public void setup() {
        context = new Activity();
        userAgent = new WebView(context).getSettings().getUserAgentString();

        Robolectric.addHttpResponseRule(new RequestMatcher() {
            @Override
            public boolean matches(HttpRequest request) {
                return true;
            }
        }, new TestHttpResponse(200, "body"));

        HttpClient.setWebViewUserAgent(null);
        Robolectric.getBackgroundScheduler().pause();
        Robolectric.clearPendingHttpResponses();
    }

    @After
    public void tearDown() throws Exception {
        HttpClient.setWebViewUserAgent(null);
        Robolectric.getBackgroundScheduler().reset();
        Robolectric.clearPendingHttpResponses();
    }

    @Test
    public void initializeHttpGet_shouldReturnHttpGetWithWebViewUserAgent() throws Exception {
        HttpGet httpGet = initializeHttpGet(url, context);

        assertThat(httpGet.getURI().toURL().toString()).isEqualTo(url);
        assertThat(httpGet.getFirstHeader(ResponseHeader.USER_AGENT.getKey()).getValue()).isEqualTo(userAgent);
    }

    @Test
    public void initializeHttpGet_shouldPopulateStaticWebViewUserAgent() throws Exception {
        assertThat(HttpClient.getWebViewUserAgent()).isNull();

        HttpGet httpGet = initializeHttpGet(url, context);

        assertThat(HttpClient.getWebViewUserAgent()).isEqualTo(userAgent);
    }

    @Test
    public void getWebViewUserAgent_whenUserAgentNotSet_shouldReturnDefault() {
        assertThat(getWebViewUserAgent("test")).isEqualTo("test");
    }

    @Test(expected = NullPointerException.class)
    public void initializeHttpGet_withNullUrl_shouldThrowNullPointerException() throws Exception {
        HttpGet httpGet = initializeHttpGet(null, context);
    }

    @Test
    public void initializeHttpGet_withNullContext_shouldNotPopulateUserAgentHeader() throws Exception {
        HttpGet httpGet = initializeHttpGet(url, null);

        assertThat(httpGet.getURI().toURL().toString()).isEqualTo(url);
        assertThat(httpGet.getFirstHeader(ResponseHeader.USER_AGENT.getKey())).isNull();
    }

    @Test
    public void makeTrackingHttpRequest_shouldMakeTrackingHttpRequestWithWebViewUserAgent() throws Exception {
        HttpClient.makeTrackingHttpRequest(url, context);

        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(500);

        CommonUtils.assertHttpRequestsMade(userAgent, url);
    }

    @Test
    public void makeTrackingHttpRequest_withNullUrl_shouldNotMakeTrackingHttpRequest() throws Exception {
        HttpClient.makeTrackingHttpRequest((String) null, context);

        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(500);

        CommonUtils.assertHttpRequestsMade(null);
    }

    @Test
    public void makeTrackingHttpRequest_withNullContext_shouldNotMakeTrackingHttpRequest() throws Exception {
        HttpClient.makeTrackingHttpRequest(url, null);

        Robolectric.getBackgroundScheduler().unPause();
        Thread.sleep(500);

        CommonUtils.assertHttpRequestsMade(null);
    }
}
