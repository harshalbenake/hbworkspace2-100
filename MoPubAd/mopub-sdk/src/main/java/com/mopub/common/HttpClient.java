package com.mopub.common;

import android.content.Context;
import android.net.http.AndroidHttpClient;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.webkit.WebView;

import com.mopub.common.event.MoPubEvents;
import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.AsyncTasks;
import com.mopub.common.util.DeviceUtils;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.util.Arrays;

import static com.mopub.common.util.ResponseHeader.USER_AGENT;

public class HttpClient {
    private static final int CONNECTION_TIMEOUT = 10000;
    private static final int SOCKET_TIMEOUT = 10000;
    private static String sWebViewUserAgent;

    public static AndroidHttpClient getHttpClient() {

        String defaultUserAgent = DeviceUtils.getUserAgent();
        String userAgent = getWebViewUserAgent(defaultUserAgent);

        AndroidHttpClient httpClient = AndroidHttpClient.newInstance(userAgent);

        HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        HttpClientParams.setRedirecting(params, true);

        return httpClient;
    }

    public static HttpGet initializeHttpGet(final String url, final Context context) {
        final HttpGet httpGet = new HttpGet(url);

        if (getWebViewUserAgent() == null && context != null) {
            // Memoize the user agent since creating WebViews is expensive
            setWebViewUserAgent(new WebView(context).getSettings().getUserAgentString());
        }


        final String webViewUserAgent = getWebViewUserAgent();
        if (webViewUserAgent != null) {
            httpGet.addHeader(USER_AGENT.getKey(), webViewUserAgent);
        }

        return httpGet;
    }

    public static void makeTrackingHttpRequest(final Iterable<String> urls, final Context context) {
        makeTrackingHttpRequest(urls, context, null);
    }

    public static void makeTrackingHttpRequest(final Iterable<String> urls,
            final Context context,
            final MoPubEvents.Type type) {
        if (urls == null || context == null) {
            return;
        }

        final DownloadTask.DownloadTaskListener downloadTaskListener = new DownloadTask.DownloadTaskListener() {
            @Override
            public void onComplete(final String url, final DownloadResponse downloadResponse) {
                if (downloadResponse == null || downloadResponse.getStatusCode() != HttpStatus.SC_OK) {
                    MoPubLog.d("Failed to hit tracking endpoint: " + url);
                    return;
                }

                final String result = HttpResponses.asResponseString(downloadResponse);
                if (result != null) {
                    MoPubLog.d("Successfully hit tracking endpoint: " + url);
                } else {
                    MoPubLog.d("Failed to hit tracking endpoint: " + url);
                }
            }
        };

        // Hold onto the application context in closure instead of activity context
        final Context appContext = context.getApplicationContext();
        final Runnable trackingHttpRequestRunnable = new Runnable() {
            @Override
            public void run() {
                for (final String url : urls) {
                    try {
                        final HttpGet httpGet = initializeHttpGet(url, appContext);
                        AsyncTasks.safeExecuteOnExecutor(
                                new DownloadTask(downloadTaskListener, type),
                                httpGet
                        );
                    } catch (Exception e) {
                        MoPubLog.d("Failed to hit tracking endpoint: " + url);
                    }
                }
            }
        };

        new Handler(Looper.getMainLooper()).post(trackingHttpRequestRunnable);
    }

    public static void makeTrackingHttpRequest(final String url,
            final Context context) {
        makeTrackingHttpRequest(url, context, null);
    }

    public static void makeTrackingHttpRequest(final String url,
            final Context context,
            final MoPubEvents.Type type) {
        makeTrackingHttpRequest(Arrays.asList(url), context, type);
    }

    /**
     * @param defaultUserAgent the String to return if the WebView user agent hasn't been generated.
     * @return the user agent of an Android WebView, or {@code defaultUserAgent}
     */
    public synchronized static String getWebViewUserAgent(String defaultUserAgent) {
        if (TextUtils.isEmpty(sWebViewUserAgent)) {
            return defaultUserAgent;
        }
        return sWebViewUserAgent;
    }

    /**
     * @return the user agent of an Androd WebView or {@code null}
     */
    public synchronized static String getWebViewUserAgent() {
        return getWebViewUserAgent(null);
    }

    public synchronized static void setWebViewUserAgent(final String userAgent) {
        sWebViewUserAgent = userAgent;
    }
}
