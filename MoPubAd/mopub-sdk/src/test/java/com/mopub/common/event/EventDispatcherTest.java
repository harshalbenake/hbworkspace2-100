package com.mopub.common.event;

import android.os.HandlerThread;
import android.os.Message;

import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


@RunWith(SdkTestRunner.class)
public class EventDispatcherTest {

    private EventDispatcher subject;
    private EventRecorder testRecorder;
    private HandlerThread testHandlerThread;

    @Before
    public void setup() {
        // Set up test event recorders and test handler thread.
        List<EventRecorder> recorders = new ArrayList<EventRecorder>();
        testRecorder = mock(EventRecorder.class);
        recorders.add(testRecorder);
        testHandlerThread = new HandlerThread("mopub-test-events");
        subject = new EventDispatcher(recorders, testHandlerThread);
        // The test runner uses a mock dispatcher that does nothing.
        MoPubEvents.setEventDispatcher(subject);
    }

    @Test
    public void createEvent_testCallbackCallsHandler() throws Exception {
        Message message = new Message();
        message.obj = new Event.Builder("","").build();
        subject.mHandlerCallback.handleMessage(message);
        verify(testRecorder).record(eq((Event) message.obj));
    }
}