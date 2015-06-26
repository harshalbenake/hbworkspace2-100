package com.mopub.mobileads.util;

import android.annotation.TargetApi;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import com.mopub.common.logging.MoPubLog;
import com.mopub.common.util.Reflection.MethodBuilder;

public class WebViews {
    @TargetApi(VERSION_CODES.HONEYCOMB)
    public static void onResume(@NonNull WebView webView) {
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            webView.onResume();
            return;
        }

        // Method is still available, but hidden. Invoke using reflection.
        try {
            new MethodBuilder(webView, "onResume").setAccessible().execute();
        } catch (Exception e) {
            // no-op
        }
    }

    @TargetApi(VERSION_CODES.HONEYCOMB)
    public static void onPause(@NonNull WebView webView) {
        if (VERSION.SDK_INT >= VERSION_CODES.HONEYCOMB) {
            webView.onPause();
            return;
        }

        // Method is still available, but hidden. Invoke using reflection.
        try {
            new MethodBuilder(webView, "onPause").setAccessible().execute();
        } catch (Exception e) {
            // no-op
        }
    }

    public static void setDisableJSChromeClient(WebView webView) {
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }

            @Override
            public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
                MoPubLog.d(message);
                result.confirm();
                return true;
            }
        });
    }
}
