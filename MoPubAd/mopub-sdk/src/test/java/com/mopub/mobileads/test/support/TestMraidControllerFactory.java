package com.mopub.mobileads.test.support;

import android.content.Context;

import com.mopub.mobileads.AdConfiguration;
import com.mopub.mobileads.factories.MraidControllerFactory;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.PlacementType;

import static org.mockito.Mockito.mock;

public class TestMraidControllerFactory extends MraidControllerFactory {
    private MraidController mockMraidController = mock(MraidController.class);

    public static MraidController getSingletonMock() {
        return getTestFactory().mockMraidController;
    }

    private static TestMraidControllerFactory getTestFactory() {
        return ((TestMraidControllerFactory) MraidControllerFactory.instance);
    }

    @Override
    protected MraidController internalCreate(final Context context,
            final AdConfiguration adConfiguration, final PlacementType placementType) {
        return mockMraidController;
    }
}
