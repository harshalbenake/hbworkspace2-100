package com.example.phonegapcordovasms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class Display extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.display);
		Intent intent=getIntent();
		String phoneNumber=intent.getStringExtra("phoneNumber");
		String message=intent.getStringExtra("message");

		
		TextView textView1=(TextView)findViewById(R.id.textView1);
		TextView textView2=(TextView)findViewById(R.id.textView2);
		TextView textView3=(TextView)findViewById(R.id.textView3);
		TextView textView4=(TextView)findViewById(R.id.textView4);
		
		textView1.setText("phoneNumber");
		textView2.setText(phoneNumber);
		textView3.setText("message");
		textView4.setText(message);

	}
}
