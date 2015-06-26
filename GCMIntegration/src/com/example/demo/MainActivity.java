package com.example.demo;

import com.example.demo.R;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		/**GCM Integration.*/
		new Async_device_registration(getApplicationContext()).execute("");
	}
}
