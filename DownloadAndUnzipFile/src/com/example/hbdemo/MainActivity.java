package com.example.hbdemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * This is main activity class.
 * @author <b>Harshal Benake</b>
 *
 */
public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button download=(Button)findViewById(R.id.button1);
		download.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Async_download async_download=new Async_download(MainActivity.this);
				async_download.execute("Enter your download URL here.");
			}
		});
		
		Button unzip=(Button)findViewById(R.id.button2);
		unzip.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
			Async_unzipping async_unzipping=new Async_unzipping(MainActivity.this);
			async_unzipping.execute("");
			}
		});
	}
}
