package com.mopub.mobileads.factories;

import android.content.Context;

import com.mopub.common.VisibleForTesting;
import com.mopub.mobileads.AdConfiguration;
import com.mopub.mraid.MraidController;
import com.mopub.mraid.PlacementType;

public class MraidControllerFactory {
    protected static MraidControllerFactory instance = new MraidControllerFactory();

    @VisibleForTesting
    public static void setInstance(MraidControllerFactory factory) {
        instance = factory;
    }

    public static MraidController create(final Context context,
            final AdConfiguration adConfiguration, final PlacementType placementType) {
        return instance.internalCreate(context, adConfiguration, placementType);
    }

    protected MraidController internalCreate(final Context context,
            final AdConfiguration adConfiguration, final PlacementType placementType) {
        return new MraidController(context, adConfiguration, placementType);
    }
}
