package com.mopub.common;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;

import com.mopub.common.event.ErrorEvent;
import com.mopub.common.event.Event;
import com.mopub.common.event.MoPubEvents;
import com.mopub.common.logging.MoPubLog;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

public class DownloadTask extends AsyncTask<HttpUriRequest, Void, DownloadResponse> {
    private final DownloadTaskListener mDownloadTaskListener;
    private final MoPubEvents.Type mEventType;
    private String mUrl;

    public static interface DownloadTaskListener {
        abstract void onComplete(String url, DownloadResponse downloadResponse);
    }

    public DownloadTask(final DownloadTaskListener downloadTaskListener) throws IllegalArgumentException {
        this(downloadTaskListener, null);
    }

    public DownloadTask(final DownloadTaskListener downloadTaskListener, final MoPubEvents.Type eventType) {
        if (downloadTaskListener == null) {
            throw new IllegalArgumentException("DownloadTaskListener must not be null.");
        }

        mDownloadTaskListener = downloadTaskListener;
        mEventType = eventType;
    }

    @Override
    protected DownloadResponse doInBackground(final HttpUriRequest... httpUriRequests) {
        if (httpUriRequests == null || httpUriRequests.length == 0 || httpUriRequests[0] == null) {
            MoPubLog.d("Download task tried to execute null or empty url");
            return null;
        }

        final HttpUriRequest httpUriRequest = httpUriRequests[0];
        mUrl = httpUriRequest.getURI().toString();
        if (mEventType != null) {
            MoPubEvents.log(new Event.Builder("", "").build());
        }

        AndroidHttpClient httpClient = null;
        try {
            httpClient = HttpClient.getHttpClient();
            final HttpResponse httpResponse = httpClient.execute(httpUriRequest);
            return new DownloadResponse(httpResponse);
        } catch (Exception e) {
            MoPubLog.d("Download task threw an internal exception", e);
            return null;
        } finally {
            if (httpClient != null) {
                httpClient.close();
            }
        }
    }

    @Override
    protected void onPostExecute(final DownloadResponse downloadResponse) {
        if (isCancelled()) {
            onCancelled();
            return;
        }

        mDownloadTaskListener.onComplete(mUrl, downloadResponse);
    }

    @Override
    protected void onCancelled() {
        MoPubLog.d("DownloadTask was cancelled.");
    }
}
