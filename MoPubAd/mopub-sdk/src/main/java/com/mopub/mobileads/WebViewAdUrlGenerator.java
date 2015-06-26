package com.mopub.mobileads;

import android.content.Context;
import android.location.Location;

import com.mopub.common.AdUrlGenerator;
import com.mopub.common.ClientMetadata;
import com.mopub.common.Constants;
import com.mopub.common.LocationService;
import com.mopub.common.MoPub;
import com.mopub.common.util.DateAndTime;

public class WebViewAdUrlGenerator extends AdUrlGenerator {
    private final boolean mIsStorePictureSupported;

    public WebViewAdUrlGenerator(Context context, boolean isStorePictureSupported) {
        super(context);
        mIsStorePictureSupported = isStorePictureSupported;
    }

    @Override
    public String generateUrlString(String serverHostname) {
        initUrlString(serverHostname, Constants.AD_HANDLER);

        final ClientMetadata clientMetadata = ClientMetadata.getInstance(mContext);

        setApiVersion("6");

        setAdUnitId(mAdUnitId);

        setSdkVersion(clientMetadata.getSdkVersion());

        setDeviceInfo(clientMetadata.getDeviceManufacturer(),
                clientMetadata.getDeviceModel(),
                clientMetadata.getDeviceProduct());

        setUdid(clientMetadata.getAdvertisingId());

        setDoNotTrack(clientMetadata.isDoNotTrackSet());

        setKeywords(mKeywords);

        setLocation(mLocation);

        setTimezone(DateAndTime.getTimeZoneOffsetString());

        setOrientation(clientMetadata.getOrientationString());

        setDensity(clientMetadata.getDensity());

        setMraidFlag(true);

        String networkOperator = clientMetadata.getNetworkOperatorForUrl();
        setMccCode(networkOperator);
        setMncCode(networkOperator);

        setIsoCountryCode(clientMetadata.getIsoCountryCode());
        setCarrierName(clientMetadata.getNetworkOperatorName());

        setNetworkType(clientMetadata.getActiveNetworkType());

        setAppVersion(clientMetadata.getAppVersion());

        setExternalStoragePermission(mIsStorePictureSupported);

        setTwitterAppInstalledFlag();

        return getFinalUrlString();
    }
}
