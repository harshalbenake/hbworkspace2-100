package com.mopub.mobileads.factories;

import android.content.Context;
import android.view.View;

import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.ViewGestureDetector;

public class ViewGestureDetectorFactory {
    protected static ViewGestureDetectorFactory instance = new ViewGestureDetectorFactory();

    @Deprecated // for testing
    public static void setInstance(ViewGestureDetectorFactory factory) {
        instance = factory;
    }

    public static ViewGestureDetector create(Context context, View view, AdConfiguration adConfiguration) {
        return instance.internalCreate(context, view, adConfiguration);
    }

    protected ViewGestureDetector internalCreate(Context context, View view, AdConfiguration adConfiguration) {
        return new ViewGestureDetector(context, view, adConfiguration);
    }
}

