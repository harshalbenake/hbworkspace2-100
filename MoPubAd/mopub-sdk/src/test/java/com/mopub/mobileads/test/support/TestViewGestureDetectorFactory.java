package com.mopub.mobileads.test.support;

import android.content.Context;
import android.view.View;

import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.ViewGestureDetector;
import com.mopub.mobileads.factories.ViewGestureDetectorFactory;

import static org.mockito.Mockito.mock;

public class TestViewGestureDetectorFactory extends ViewGestureDetectorFactory {
    private ViewGestureDetector mockViewGestureDetector = mock(ViewGestureDetector.class);

    public static ViewGestureDetector getSingletonMock() {
        return getTestFactory().mockViewGestureDetector;
    }

    private static TestViewGestureDetectorFactory getTestFactory() {
        return ((TestViewGestureDetectorFactory) instance);
    }

    @Override
    protected ViewGestureDetector internalCreate(Context context, View view, AdConfiguration adConfiguration) {
        return mockViewGestureDetector;
    }
}