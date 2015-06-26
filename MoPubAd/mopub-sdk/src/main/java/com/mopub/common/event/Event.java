package com.mopub.common.event;

/**
 * Immutable data class with client event data.
 */
public class Event extends BaseEvent {
    private Event(Builder builder) {
        super(builder);
    }

    public static class Builder extends BaseEvent.Builder {
        public Builder(String eventName, String eventCategory) {
            super(eventName, eventCategory);
        }

        @Override
        public Event build() {
            return new Event(this);
        }
    }
}
