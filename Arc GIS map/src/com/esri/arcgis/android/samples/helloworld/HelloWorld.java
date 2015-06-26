/* Copyright 2012 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the Sample code usage restrictions document for further information.
 *
 */

package com.esri.arcgis.android.samples.helloworld;

import android.app.Activity;
import android.os.Bundle;

import com.esri.android.map.MapView;

/**
 * The HelloWorld app is the most basic Map app for the ArcGIS Runtime SDK for Android. It shows how to define a MapView
 * in the layout XML of the activity. Within the XML definition of the MapView, MapOptions attributes are used to
 * populate that MapView with a basemap layer showing streets, and also the initial extent and zoom level are set. By
 * default, this map supports basic zooming and panning operations. This sample also demonstrates calling the MapView
 * pause and unpause methods from the Activity onPause and onResume methods, which suspend and resume map rendering
 * threads. A reference to the MapView is set within the onCreate method of the Activity which can be used at the
 * starting point for further coding.
 */

public class HelloWorld extends Activity {
  MapView mMapView;

  // Called when the activity is first created.
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    // After the content of this Activity is set, the map can be accessed programmatically from the layout.
    mMapView = (MapView) findViewById(R.id.map);
    
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Call MapView.pause to suspend map rendering while the activity is paused, which can save battery usage.
    if (mMapView != null)
    {
      mMapView.pause();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Call MapView.unpause to resume map rendering when the activity returns to the foreground.
    if (mMapView != null)
    {
      mMapView.unpause();
    }
  }

}