package com.example.hbmapdemo;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

	public class MainActivity extends FragmentActivity implements OnMarkerDragListener {

	 public GoogleMap googleMap;
	 private int distance;
	 private SupportMapFragment mapFragment;

	 @Override
	 public void onCreate(Bundle savedInstanceState) {
	  super.onCreate(savedInstanceState);
	  setContentView(R.layout.activity_main);
	  mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
	  googleMap = mapFragment.getMap();
	  googleMap.setOnMarkerDragListener(MainActivity.this);
	  distance = 100;
	  googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(
	    26.788707, 75.828108), 15));
	  createGeofence(26.788707, 75.828108, distance, "CIRCLE", "GEOFENCE");
	 }
	 private void createGeofence(double latitude, double longitude, int radius,
	   String geofenceType, String title) {

	  Marker stopMarker = googleMap.addMarker(new MarkerOptions()
	    .draggable(true)
	    .position(new LatLng(latitude, longitude))
	    .title(title)
	    .icon(BitmapDescriptorFactory
	    .fromResource(R.drawable.ic_launcher)));

	  googleMap.addCircle(new CircleOptions()
	    .center(new LatLng(latitude, longitude)).radius(radius)
	    .fillColor(Color.parseColor("#B2A9F6"))); 
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
	  createGeofence(dragLat, dragLong, distance, "CIRCLE", "GEOFENCE");
	  Toast.makeText(
	    MainActivity.this,
	    "onMarkerDragEnd dragLat :" + dragLat + " dragLong :"
	      + dragLong, Toast.LENGTH_SHORT).show();
	  Log.i("info", "on drag end :" + dragLat + " dragLong :" + dragLong);

	 }
	 @Override
	 public void onMarkerDragStart(Marker marker) {
	 }

	

}
