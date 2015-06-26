package com.googleanalytics_as;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button=(Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int a[] = new int[2];
                System.out.println("Access element three :" + a[3]);
            }
        });

        // May return null if a EasyTracker has not yet been initialized with a
        // property ID.
        EasyTracker easyTracker = EasyTracker.getInstance(this);
        // MapBuilder.createEvent().build() returns a Map of event fields and values
        // that are set and sent with the hit.
        easyTracker.send(MapBuilder
                        .createEvent("Demo Event category",     // Event category (required)
                                "Demo Event actio",  // Event action (required)
                                "Demo Event label",   // Event label
                                (long)1234)            // Event value
                        .build()
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        // Google Analytics Tracker onStart() code.
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // Google Analytics Tracker onStop() code.
        EasyTracker.getInstance(this).activityStop(this);
    }

}
