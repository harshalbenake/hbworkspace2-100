package com.kpbird.androidinstagramdemo;

import java.util.List;

import br.com.dina.oauth.instagram.example.R;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	// Listener for Button Clicked - Defined in button:onClilck property
	public void buttonClicked(View v) {

		// Let's check Instagram is installed or not
		if (!appInstalledOrNot()) {
			Toast.makeText(this, "Instagram Application is not installed",Toast.LENGTH_LONG).show();
			return;
		}

		if (v.getId() == R.id.btngallery) {
			shareFromGallery(); // share image from gallery

		} else if (v.getId() == R.id.btnsdcard) {
			shareFromSDCard(); // share image from sdcard
		}
	}

	/*
	 * This method use PackageManager Class to check for instagram package.
	 * */
	private boolean appInstalledOrNot() {

		boolean app_installed = false;
		try {
			ApplicationInfo info = getPackageManager().getApplicationInfo("com.instagram.android", 0);
			app_installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			app_installed = false;
		}
		return app_installed;
	}
	
	/*This method invoke gallery or any application which support image/* mime type */
	private void shareFromGallery(){
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		startActivityForResult(Intent.createChooser(intent, "Select Picture"), 0);
	}
	/*this method share test.jpg file from sd card */
	private void shareFromSDCard(){
		shareInstagram(Uri.parse("file://"+Environment.getExternalStorageDirectory()+"/test.jpg"));
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	
			Uri uri = data.getData();
			 shareInstagram(uri);	
	}
	
	/* this mathod actually share image to Instagram, It accept Uri */
	private void shareInstagram(Uri uri){
		
		/// Method 1 : Optimize
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
		shareIntent.setType("image/*"); // set mime type 
		shareIntent.putExtra(Intent.EXTRA_STREAM,uri); // set uri 
		shareIntent.setPackage("com.instagram.android");
		startActivity(shareIntent);
		
		
		// Intent for action_send  
//		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
//		shareIntent.setType("image/*"); // set mime type 
//		shareIntent.putExtra(Intent.EXTRA_STREAM,uri); // set uri 
//		
//		//following logic is to avoide option menu, If you remove following logic then android will display list of application which support image/* mime type
//		PackageManager pm = getPackageManager();
//		List<ResolveInfo> activityList = pm.queryIntentActivities(shareIntent, 0);
//		for (final ResolveInfo app : activityList) {
//		    if ((app.activityInfo.name).contains("instagram")) { // search for instagram from app list
//		        final ActivityInfo activity = app.activityInfo;
//		        final ComponentName name = new ComponentName(activity.applicationInfo.packageName, activity.name); 
//		        shareIntent.addCategory(Intent.CATEGORY_LAUNCHER);
//		        shareIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |             Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//		        shareIntent.setComponent(name); // set component 
//		        startActivity(shareIntent);
//		        break;
//		   }
//		}
	}
	
	
}
