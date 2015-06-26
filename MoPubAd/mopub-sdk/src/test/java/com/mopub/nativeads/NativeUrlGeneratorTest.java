package com.mopub.nativeads;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.mopub.common.MoPub;
import com.mopub.mobileads.test.support.MoPubShadowTelephonyManager;
import com.mopub.common.test.support.SdkTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLocationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static android.Manifest.permission.ACCESS_NETWORK_STATE;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.Robolectric.application;
import static org.robolectric.Robolectric.shadowOf;

@RunWith(SdkTestRunner.class)
@Config(shadows = {MoPubShadowTelephonyManager.class})
public class NativeUrlGeneratorTest {
    public static final String AD_UNIT_ID = "1234";
    private Activity context;
    private NativeUrlGenerator subject;
    private MoPubShadowTelephonyManager shadowTelephonyManager;

    @Before
    public void setup() {
        context = new Activity();
        shadowOf(context).grantPermissions(ACCESS_NETWORK_STATE);
        shadowTelephonyManager = (MoPubShadowTelephonyManager)
                shadowOf((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
    }

    @Test
    public void generateNativeAdUrl_shouldIncludeDesiredAssetIfSet() throws Exception {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.of(RequestParameters.NativeAdAsset.TITLE);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(1);
        assertThat(desiredAssets).contains("title");
    }

    @Test
    public void generateNativeAdUrl_shouldIncludeDesiredAssetsIfSet() throws Exception {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.of(RequestParameters.NativeAdAsset.TITLE, RequestParameters.NativeAdAsset.TEXT, RequestParameters.NativeAdAsset.ICON_IMAGE);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(3);
        assertThat(desiredAssets).contains("title", "text", "iconimage");
    }

    @Test
    public void generateNativeAdUrl_shouldNotIncludeDesiredAssetsIfNotSet() throws Exception {
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(0);
    }

    @Test
    public void generateNativeAdUrl_shouldNotIncludeDesiredAssetsIfNoAssetsAreSet() throws Exception {
        EnumSet<RequestParameters.NativeAdAsset> assetsSet = EnumSet.noneOf(RequestParameters.NativeAdAsset.class);
        RequestParameters requestParameters = new RequestParameters.Builder().desiredAssets(assetsSet).build();

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID).withRequest(requestParameters);

        String requestString = generateMinimumUrlString();
        List<String> desiredAssets = getDesiredAssetsListFromRequestUrlString(requestString);

        assertThat(desiredAssets.size()).isEqualTo(0);
    }

    @Test
    public void generateNativeAdUrl_needsButDoesNotHaveReadPhoneState_shouldNotContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isNullOrEmpty();
    }
    
    @Test
    public void generateNativeAdUrl_needsAndHasReadPhoneState_shouldContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(true);
        shadowTelephonyManager.setReadPhoneStatePermission(true);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isEqualTo("TEST_CARRIER");
    }

    @Test
    public void generateNativeAdUrl_doesNotNeedReadPhoneState_shouldContainOperatorName() {
        shadowTelephonyManager.setNeedsReadPhoneState(false);
        shadowTelephonyManager.setReadPhoneStatePermission(false);
        shadowTelephonyManager.setNetworkOperatorName("TEST_CARRIER");

        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String requestString = generateMinimumUrlString();

        assertThat(getNetworkOperatorNameFromRequestUrl(requestString)).isEqualTo("TEST_CARRIER");
    }

    @Test
    public void generateAdUrl_whenLocationServiceGpsProviderHasMostRecentLocation_shouldUseLocationServiceValue() {
        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Robolectric.shadowOf(
                (LocationManager) application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .location(locationFromDeveloper)
                .build();
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = subject.withRequest(requestParameters)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("37.0,-122.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
    }

    @Test
    public void generateAdUrl_whenDeveloperSuppliesMoreRecentLocationThanLocationService_shouldUseDeveloperSuppliedLocation() {
        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        ShadowLocationManager shadowLocationManager = Robolectric.shadowOf(
                (LocationManager) application.getSystemService(Context.LOCATION_SERVICE));

        // Mock out the LocationManager's last known location to be older than the
        // developer-supplied location.
        Location olderLocation = new Location("");
        olderLocation.setLatitude(40);
        olderLocation.setLongitude(-105);
        olderLocation.setAccuracy(8.0f);
        olderLocation.setTime(500);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, olderLocation);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .location(locationFromDeveloper)
                .build();
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = subject.withRequest(requestParameters)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("42.0,-42.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("3");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEmpty();
    }

    @Test
    public void generateAdUrl_whenLocationServiceNetworkProviderHasMostRecentLocation_shouldUseLocationServiceValue() {
        Location locationFromDeveloper = new Location("");
        locationFromDeveloper.setLatitude(42);
        locationFromDeveloper.setLongitude(-42);
        locationFromDeveloper.setAccuracy(3.5f);
        locationFromDeveloper.setTime(1000);

        // Mock out the LocationManager's last known location to be more recent than the
        // developer-supplied location.
        ShadowLocationManager shadowLocationManager = Robolectric.shadowOf(
                (LocationManager) application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(38);
        locationFromSdk.setLongitude(-123);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.NETWORK_PROVIDER,
                locationFromSdk);

        RequestParameters requestParameters = new RequestParameters.Builder()
                .location(locationFromDeveloper)
                .build();
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);
        String adUrl = subject.withRequest(requestParameters)
                .generateUrlString("ads.mopub.com");
        assertThat(getParameterFromRequestUrl(adUrl, "ll")).isEqualTo("38.0,-123.0");
        assertThat(getParameterFromRequestUrl(adUrl, "lla")).isEqualTo("5");
        assertThat(getParameterFromRequestUrl(adUrl, "llsdk")).isEqualTo("1");
    }

    @Test
    public void enableLocation_shouldIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.NORMAL);
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(requestString, "ll")).isNotNull();
    }

    @Test
    public void disableLocation_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);
        subject = new NativeUrlGenerator(context).withAdUnitId(AD_UNIT_ID);

        String requestString = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(requestString, "ll")).isNullOrEmpty();
    }

    @Test
    public void disableLocationCollection_whenLocationServiceHasMostRecentLocation_shouldNotIncludeLocationInUrl() {
        MoPub.setLocationAwareness(MoPub.LocationAwareness.DISABLED);
        subject = new NativeUrlGenerator(context);

        // Mock out the LocationManager's last known location.
        ShadowLocationManager shadowLocationManager = Robolectric.shadowOf(
                (LocationManager) application.getSystemService(Context.LOCATION_SERVICE));
        Location locationFromSdk = new Location("");
        locationFromSdk.setLatitude(37);
        locationFromSdk.setLongitude(-122);
        locationFromSdk.setAccuracy(5.0f);
        locationFromSdk.setTime(2000);
        shadowLocationManager.setLastKnownLocation(LocationManager.GPS_PROVIDER, locationFromSdk);

        String requestString = generateMinimumUrlString();
        assertThat(getParameterFromRequestUrl(requestString, "ll")).isNullOrEmpty();
    }

    private List<String> getDesiredAssetsListFromRequestUrlString(String requestString) {
        Uri requestUri = Uri.parse(requestString);

        String desiredAssetsString = requestUri.getQueryParameter("assets");
        return (desiredAssetsString == null) ? new ArrayList<String>() : Arrays.asList(desiredAssetsString.split(","));
    }

    private String getNetworkOperatorNameFromRequestUrl(String requestString) {
        Uri requestUri = Uri.parse(requestString);

        String networkOperatorName = requestUri.getQueryParameter("cn");

        if (TextUtils.isEmpty(networkOperatorName)) {
            return "";
        }

        return networkOperatorName;
    }

    private String getParameterFromRequestUrl(String requestString, String key) {
        Uri requestUri = Uri.parse(requestString);
        String parameter = requestUri.getQueryParameter(key);

        if (TextUtils.isEmpty(parameter)) {
            return "";
        }

        return parameter;
    }

    private String generateMinimumUrlString() {
        return subject.generateUrlString("ads.mopub.com");
    }
}
