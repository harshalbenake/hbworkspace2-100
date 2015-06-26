package com.example.hbdemo;

import org.apache.cordova.DroidGap;

import android.os.Bundle;

public class MainActivity extends DroidGap {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		super.setStringProperty("errorUrl","file:///android_asset/www/hbdemo.html");
		super.setStringProperty("loadingDialog", "Loading HBdemo...");
		super.setIntegerProperty("loadUrlTimeoutValue", 60000);
		super.init();
		 KeyBoard keyboard = new KeyBoard(MainActivity.this, appView);
	        appView.addJavascriptInterface(keyboard, "KeyBoard");
	        keyboard.showKeyBoard();
		super.setIntegerProperty("splashscreen", R.drawable.ic_launcher);
		super.loadUrl("file:///android_asset/www/tempindex.html",3000);
	}

}
