package com.example.googlecloudwirelessprinting;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button button=(Button)findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent printIntent = new Intent(MainActivity.this, PrintDialogActivity.class);
				File file = new File("file://android_asset/pdf_file.pdf");
				printIntent.setDataAndType(Uri.fromFile(file), "application/pdf");
				printIntent.putExtra("title", "docTitle");
				startActivity(printIntent);				
			}
		});
	}
}
