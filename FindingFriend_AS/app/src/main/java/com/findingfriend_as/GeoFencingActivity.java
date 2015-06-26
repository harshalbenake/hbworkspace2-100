package com.findingfriend_as;


import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class GeoFencingActivity extends FragmentActivity implements OnMarkerDragListener, LocationListener {

    public GoogleMap googleMap;
    private int distance;
    private SupportMapFragment mapFragment;
    private Marker createGeofenceMarker;
    private Marker addPinMarker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofencing);

        Button button = (Button) findViewById(R.id.add_pin);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPin(26.788707, 75.828108, "Meet at Beer Tent");
                PreferenceManager preferenceManager = new PreferenceManager(GeoFencingActivity.this);
                createGeofence(Double.valueOf(preferenceManager.getLatitude()), Double.valueOf(preferenceManager.getLongitude()), 100, "Meet at Beer Tent");

            }
        });

        mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
        googleMap = mapFragment.getMap();
        googleMap.setMyLocationEnabled(true);
        googleMap.setOnMarkerDragListener(GeoFencingActivity.this);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 500, this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 500, this);
    }

    private void createGeofence(double latitude, double longitude, int radius, String title) {
        Marker createGeofenceMarker = googleMap.addMarker(new MarkerOptions()
                .draggable(true)
                .position(new LatLng(latitude, longitude))
                .title(title)
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.ic_map_pin_blue)));

        CircleOptions circleOptions = new CircleOptions().center(new LatLng(latitude, longitude)).radius(radius).fillColor(Color.parseColor("#B2A9F6"));
        googleMap.addCircle(circleOptions);
        System.out.println("circleOptions.getRadius: " + circleOptions.getRadius());

        if (addPinMarker != null) {
            float[] distance = new float[2];
            Location.distanceBetween(addPinMarker.getPosition().latitude, addPinMarker.getPosition().longitude,
                    circleOptions.getCenter().latitude, circleOptions.getCenter().longitude, distance);

            if (distance[0] > circleOptions.getRadius()) {
                Toast.makeText(getBaseContext(), "Outside", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getBaseContext(), "Inside", Toast.LENGTH_LONG).show();
            }
        }
        // googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));
    }

    private void addPin(double latitude, double longitude, String title) {
        addPinMarker = googleMap.addMarker(new MarkerOptions()
                .draggable(true)
                .position(new LatLng(latitude, longitude))
                .title(title)
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.ic_map_pin_red)));
    }


    @Override
    public void onMarkerDrag(Marker marker) {
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng dragPosition = marker.getPosition();
        double dragLat = dragPosition.latitude;
        double dragLong = dragPosition.longitude;
        googleMap.clear();
        // createGeofence(dragLat, dragLong, distance, "CIRCLE", "GEOFENCE");

        addPin(dragLat, dragLong, "Meet at Beer Tent");
//        Toast.makeText(
//                GeoFencingActivity.this,
//                "onMarkerDragEnd dragLat :" + dragLat + " dragLong :"
//                        + dragLong, Toast.LENGTH_SHORT).show();
        Log.i("info", "on drag end :" + dragLat + " dragLong :" + dragLong);

        PreferenceManager preferenceManager = new PreferenceManager(GeoFencingActivity.this);
        createGeofence(Double.valueOf(preferenceManager.getLatitude()), Double.valueOf(preferenceManager.getLongitude()), 100, "Meet at Beer Tent");

    }

    @Override
    public void onMarkerDragStart(Marker marker) {
    }

    @Override
    public void onLocationChanged(Location location) {
        PreferenceManager preferenceManager = new PreferenceManager(GeoFencingActivity.this);
        preferenceManager.addLatitude("" + location.getLatitude());
        preferenceManager.addLongitude("" + location.getLongitude());
        createGeofence(location.getLatitude(), location.getLongitude(), 100, "Meet at Beer Tent");

        Toast.makeText(getApplicationContext(), "Location Updated: " + location.getLatitude() + " " + location.getLongitude(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    public void sendLocationSMS(String phoneNumber, Location currentLocation) {
        SmsManager smsManager = SmsManager.getDefault();
        StringBuffer smsBody = new StringBuffer();
        smsBody.append("http://maps.google.com?q=");
        smsBody.append(currentLocation.getLatitude());
        smsBody.append(",");
        smsBody.append(currentLocation.getLongitude());
        smsManager.sendTextMessage(phoneNumber, null, smsBody.toString(), null, null);
    }
}
