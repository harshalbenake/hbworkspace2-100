package com.mopub.common.event;

import android.os.HandlerThread;

import com.mopub.common.VisibleForTesting;

import java.util.ArrayList;

/**
 * Public interface used to record client events.
 */
public class MoPubEvents {
    public enum Type {
        // Networking
        AD_REQUEST("ad_request"),
        IMPRESSION_REQUEST("impression_request"),
        CLICK_REQUEST("click_request"),
        POSITIONING_REQUEST("positioning_request"),

        // Errors
        AD_REQUEST_ERROR("ad_request_error"),
        TRACKING_ERROR("track_error"),

        // The SDK doesn't distinguish types of tracking at a level where this more-specific logging works yet.
        IMPRESSION_ERROR("imp_track_error"),
        CLICK_ERROR("click_track_error"),
        CONVERSION_ERROR("conv_track_error"),
        DATA_ERROR("invalid_data");

        public final String mName;
        Type(String name) {
            mName = name;
        }
    }

    private static volatile EventDispatcher sEventDispatcher;

    /**
     * Log a BaseEvent. MoPub uses logged events to analyze and improve performance.
     * This method should not be called by app developers.
     */
    public static void log(BaseEvent baseEvent) {
        MoPubEvents.getDispatcher().dispatch(baseEvent);
    }

    @VisibleForTesting
    public static void setEventDispatcher(EventDispatcher dispatcher) {
        sEventDispatcher = dispatcher;
    }

    private static EventDispatcher getDispatcher() {
        EventDispatcher result = sEventDispatcher;
        if (result == null) {
            synchronized (MoPubEvents.class) {
                result = sEventDispatcher;
                if (result == null) {
                    ArrayList<EventRecorder> recorders = new ArrayList<EventRecorder>();
                    recorders.add(new NoopEventRecorder());
                    HandlerThread handlerThread = new HandlerThread("mopub_event_queue");
                    result = sEventDispatcher = new EventDispatcher(recorders, handlerThread);
                }
            }
        }
        return result;
    }
}
