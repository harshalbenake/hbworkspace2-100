package com.example.animateLayoutChanges;

import com.example.animateLayoutChanges.R;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

	}

	public void add(View view) {
		Toast.makeText(getApplicationContext(), "add", Toast.LENGTH_LONG)
				.show();
		TextView tv = new TextView(MainActivity.this);
		tv.setText("This is a demo text");
		LinearLayout linLay = (LinearLayout) findViewById(R.id.linlay);
		linLay.removeAllViews();
		linLay.addView(tv);

	}
	public void remove(View view) {
		Toast.makeText(getApplicationContext(), "remove", Toast.LENGTH_LONG)
				.show();
		LinearLayout linLay = (LinearLayout) findViewById(R.id.linlay);
		linLay.removeAllViews();

	}

}
