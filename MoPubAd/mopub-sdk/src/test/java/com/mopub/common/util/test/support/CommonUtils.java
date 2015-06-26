package com.mopub.common.util.test.support;

import org.apache.http.HttpRequest;
import org.robolectric.Robolectric;

import static com.mopub.common.util.ResponseHeader.USER_AGENT;
import static org.fest.assertions.api.Assertions.assertThat;

public class CommonUtils {
    public static void assertHttpRequestsMade(final String userAgent, final String... urls) {
        final int numberOfReceivedHttpRequests = Robolectric.getFakeHttpLayer().getSentHttpRequestInfos().size();
        assertThat(numberOfReceivedHttpRequests).isEqualTo(urls.length);

        for (final String url : urls) {
            assertThat(Robolectric.httpRequestWasMade(url)).isTrue();
        }

        if (userAgent != null) {
            while (true) {
                final HttpRequest httpRequest = Robolectric.getNextSentHttpRequest();
                if (httpRequest == null) {
                    break;
                }

                assertThat(httpRequest.getFirstHeader(USER_AGENT.getKey()).getValue())
                        .isEqualTo(userAgent);
            }
        }
    }
}
