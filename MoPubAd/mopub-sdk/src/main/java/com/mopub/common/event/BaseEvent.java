package com.mopub.common.event;

import com.mopub.common.ClientMetadata;

import java.text.SimpleDateFormat;
import java.util.Date;

import static com.mopub.common.ClientMetadata.MoPubNetworkType;

abstract class BaseEvent {
    public static enum SdkProduct {
        NONE(0),
        WEB_VIEW(1),
        NATIVE(2);

        public final int mType;
        SdkProduct(int type) {
            mType = type;
        }
    }

    public static enum AppPlatform {
        IOS(0),
        ANDROID(1),
        MOBILE_WEB(2);

        public final int mType;
        AppPlatform(int type) {
            mType = type;
        }
    }

    private final String mEventName;
    private final String mEventCategory;
    private final SdkProduct mSdkProduct;
    private final String mAdUnitId;
    private final String mAdCreativeId;
    private final String mAdType;
    private final String mAdNetworkType;
    private final Double mAdWidthPx;
    private final Double mAdHeightPx;
    private final Double mGeoLat;
    private final Double mGeoLon;
    private final Double mGeoAccuracy;
    private final Double mPerformanceDurationMs;
    private final String mRequestId;
    private final Integer mRequestStatusCode;
    private final String mRequestUri;
    private final Integer mRequestRetries;
    private final long mTimestampUtcMs;

    BaseEvent(final Builder builder) {
        mEventName = builder.mEventName;
        mEventCategory = builder.mEventCategory;
        mSdkProduct = builder.mSdkProduct;
        mAdUnitId = builder.mAdUnitId;
        mAdCreativeId = builder.mAdCreativeId;
        mAdType = builder.mAdType;
        mAdNetworkType = builder.mAdNetworkType;
        mAdWidthPx = builder.mAdWidthPx;
        mAdHeightPx = builder.mAdHeightPx;
        mGeoLat = builder.mGeoLat;
        mGeoLon = builder.mGeoLon;
        mGeoAccuracy = builder.mGeoAccuracy;
        mPerformanceDurationMs = builder.mPerformanceDurationMs;
        mRequestId = builder.mRequestId;
        mRequestStatusCode = builder.mRequestStatusCode;
        mRequestUri = builder.mRequestUri;
        mRequestRetries = builder.mRequestRetries;
        mTimestampUtcMs = System.currentTimeMillis();
    }

    public String getEventName() {
        return mEventName;
    }

    public String getEventCategory() {
        return mEventCategory;
    }

    public SdkProduct getSdkProduct() {
        return mSdkProduct;
    }

    public String getSdkVersion() {
        return ClientMetadata.getInstance().getSdkVersion();
    }

    public String getAdUnitId() {
        return mAdUnitId;
    }

    public String getAdCreativeId() {
        return mAdCreativeId;
    }

    public String getAdType() {
        return mAdType;
    }

    public String getAdNetworkType() {
        return mAdNetworkType;
    }

    public Double getAdWidthPx() {
        return mAdWidthPx;
    }

    public Double getAdHeightPx() {
        return mAdHeightPx;
    }

    public AppPlatform getAppPlatform() {
        return AppPlatform.ANDROID;
    }

    public String getAppName() {
        return ClientMetadata.getInstance().getAppName();
    }

    public String getAppPackageName() {
        return ClientMetadata.getInstance().getAppPackageName();
    }

    public String getAppVersion() {
        return ClientMetadata.getInstance().getAppVersion();
    }

    public String getClientAdvertisingId() {
        return ClientMetadata.getInstance().getAdvertisingId();
    }

    public Boolean getClientDoNotTrack() {
        return ClientMetadata.getInstance().isDoNotTrackSet();
    }

    public String getDeviceManufacturer() {
        return ClientMetadata.getInstance().getDeviceManufacturer();
    }

    public String getDeviceModel() {
        return ClientMetadata.getInstance().getDeviceModel();
    }

    public String getDeviceProduct() {
        return ClientMetadata.getInstance().getDeviceProduct();
    }

    public String getDeviceOsVersion() {
        return ClientMetadata.getInstance().getDeviceOsVersion();
    }

    public Integer getDeviceScreenWidthPx() {
        return ClientMetadata.getInstance().getDeviceScreenWidthPx();
    }

    public Integer getDeviceScreenHeightPx() {
        return ClientMetadata.getInstance().getDeviceScreenHeightPx();
    }

    public Double getGeoLat() {
        return mGeoLat;
    }

    public Double getGeoLon() {
        return mGeoLon;
    }

    public Double getGeoAccuracy() {
        return mGeoAccuracy;
    }

    public Double getPerformanceDurationMs() {
        return mPerformanceDurationMs;
    }

    public MoPubNetworkType getNetworkType() {
        return ClientMetadata.getInstance().getActiveNetworkType();
    }

    public String getNetworkOperatorCode() {
        return ClientMetadata.getInstance().getNetworkOperator();
    }

    public String getNetworkOperatorName() {
        return ClientMetadata.getInstance().getNetworkOperatorName();
    }

    public String getNetworkIsoCountryCode() {
        return ClientMetadata.getInstance().getIsoCountryCode();
    }

    public String getNetworkSimCode() {
        return ClientMetadata.getInstance().getSimOperator();
    }

    public String getNetworkSimOperatorName() {
        return ClientMetadata.getInstance().getSimOperatorName();
    }

    public String getNetworkSimIsoCountryCode() {
        return ClientMetadata.getInstance().getSimIsoCountryCode();
    }

    public String getRequestId() {
        return mRequestId;
    }

    public Integer getRequestStatusCode() {
        return mRequestStatusCode;
    }

    public String getRequestUri() {
        return mRequestUri;
    }

    public Integer getRequestRetries() {
        return mRequestRetries;
    }

    public long getTimestampUtcMs() {
        return mTimestampUtcMs;
    }

    @Override
    public String toString() {
        return  "BaseEvent\n" +
                "EventName: " + getEventName() + "\n" +
                "EventCategory: " + getEventCategory() + "\n" +
                "SdkProduct: " + getSdkProduct() + "\n" +
                "SdkVersion: " + getSdkVersion() + "\n" +
                "AdUnitId: " + getAdUnitId() + "\n" +
                "AdCreativeId: " + getAdCreativeId() + "\n" +
                "AdType: " + getAdType() + "\n" +
                "AdNetworkType: " + getAdNetworkType() + "\n" +
                "AdWidthPx: " + getAdWidthPx() + "\n" +
                "AdHeightPx: " + getAdHeightPx() + "\n" +
                "AppPlatform: " + getAppPlatform() + "\n" +
                "AppName: " + getAppName() + "\n" +
                "AppPackageName: " + getAppPackageName() + "\n" +
                "AppVersion: " + getAppVersion() + "\n" +
                "ClientAdvertisingId: " + getClientAdvertisingId() + "\n" +
                "ClientDoNotTrack: " + getClientDoNotTrack() + "\n" +
                "DeviceManufacturer: " + getDeviceManufacturer() + "\n" +
                "DeviceModel: " + getDeviceModel() + "\n" +
                "DeviceProduct: " + getDeviceProduct() + "\n" +
                "DeviceOsVersion: " + getDeviceOsVersion() + "\n" +
                "DeviceScreenWidth: " + getDeviceScreenWidthPx() + "\n" +
                "DeviceScreenHeight: " + getDeviceScreenHeightPx() + "\n" +
                "GeoLat: " + getGeoLat() + "\n" +
                "GeoLon: " + getGeoLon() + "\n" +
                "GeoAccuracy: " + getGeoAccuracy() + "\n" +
                "PerformanceDurationMs: " + getPerformanceDurationMs() + "\n" +
                "NetworkType: " + getNetworkType() + "\n" +
                "NetworkOperatorCode: " + getNetworkOperatorCode() + "\n" +
                "NetworkOperatorName: " + getNetworkOperatorName() + "\n" +
                "NetworkIsoCountryCode: " + getNetworkIsoCountryCode() + "\n" +
                "NetworkSimCode: " + getNetworkSimCode() + "\n" +
                "NetworkSimOperatorName: " + getNetworkSimOperatorName() + "\n" +
                "NetworkSimIsoCountryCode: " + getNetworkSimIsoCountryCode() + "\n" +
                "RequestId: " + getRequestId() + "\n" +
                "RequestStatusCode: " + getRequestStatusCode() + "\n" +
                "RequestUri: " + getRequestUri() + "\n" +
                "RequestRetries" + getRequestRetries() + "\n" +
                "TimestampUtcMs: " + new SimpleDateFormat().format(new Date(getTimestampUtcMs())) + "\n";
    }

    static abstract class Builder {
        private String mEventName;
        private String mEventCategory;
        private SdkProduct mSdkProduct;
        private String mAdUnitId;
        private String mAdCreativeId;
        private String mAdType;
        private String mAdNetworkType;
        private Double mAdWidthPx;
        private Double mAdHeightPx;
        private Double mGeoLat;
        private Double mGeoLon;
        private Double mGeoAccuracy;
        private Double mPerformanceDurationMs;
        private String mRequestId;
        private Integer mRequestStatusCode;
        private String mRequestUri;
        private Integer mRequestRetries;

        public Builder(String eventName, String eventCategory) {
            mEventName = eventName;
            mEventCategory = eventCategory;
        }

        public Builder withSdkProduct(SdkProduct sdkProduct) {
            mSdkProduct = sdkProduct;
            return this;
        }

        public Builder withAdUnitId(String adUnitId) {
            mAdUnitId = adUnitId;
            return this;
        }

        public Builder withAdCreativeId(String adCreativeId) {
            mAdCreativeId = adCreativeId;
            return this;
        }

        public Builder withAdType(String adType) {
            mAdType = adType;
            return this;
        }

        public Builder withAdNetworkType(String adNetworkType) {
            mAdNetworkType = adNetworkType;
            return this;
        }

        public Builder withAdWidthPx(Double adWidthPx) {
            mAdWidthPx = adWidthPx;
            return this;
        }

        public Builder withAdHeightPx(Double adHeightPx) {
            mAdHeightPx = adHeightPx;
            return this;
        }

        public Builder withGeoLat(Double geoLat) {
            mGeoLat = geoLat;
            return this;
        }

        public Builder withGeoLon(Double geoLon) {
            mGeoLon = geoLon;
            return this;
        }

        public Builder withGeoAccuracy(Double geoAccuracy) {
            mGeoAccuracy = geoAccuracy;
            return this;
        }

        public Builder withPerformanceDurationMs(Double performanceDurationMs) {
            mPerformanceDurationMs = performanceDurationMs;
            return this;
        }

        public Builder withRequestId(String requestId) {
            mRequestId = requestId;
            return this;
        }

        public Builder withRequestStatusCode(Integer requestStatusCode) {
            mRequestStatusCode = requestStatusCode;
            return this;
        }

        public Builder withRequestUri(String requestUri) {
            mRequestUri = requestUri;
            return this;
        }

        public Builder withRequestRetries(Integer requestRetries) {
            mRequestRetries = requestRetries;
            return this;
        }

        public abstract BaseEvent build();
    }
}
