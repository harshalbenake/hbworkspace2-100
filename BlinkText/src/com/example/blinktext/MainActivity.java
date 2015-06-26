package com.example.blinktext;

import java.util.Random;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		blink();
	}


	private void blink() {
		final Handler handler = new Handler();
		new Thread(new Runnable() {
			@Override
			public void run() {
				int timeToBlink = 500; // in milissegunds
				try {
					Thread.sleep(timeToBlink);
				} catch (Exception e) {
				}
				handler.post(new Runnable() {
					@Override
					public void run() {
						TextView txt = (TextView) findViewById(R.id.textView1);
						txt.setText("HB Blink Text Demo without animation.");
						Random rnd = new Random(); 
						int color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));   
						txt.setTextColor(color);
						if (txt.getVisibility() == View.VISIBLE) {
							txt.setVisibility(View.INVISIBLE);
						} else {
							txt.setVisibility(View.VISIBLE);
						}
						blink();
					}
				});
			}
		}).start();
	}
}
