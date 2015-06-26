package com.example.downloadmanagerandroid1;

import android.net.Uri;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

@SuppressLint("NewApi")
public class MainActivity extends Activity {
	private Button button;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				String servicestring = Context.DOWNLOAD_SERVICE;
				DownloadManager downloadmanager;
				downloadmanager = (DownloadManager) getSystemService(servicestring);
				Uri uri = Uri
						.parse("https://sites.google.com/site/compiletimeerrorcom/android-programming/CameraApp.rar");
				DownloadManager.Request request = new Request(uri);
				Long reference = downloadmanager.enqueue(request);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}