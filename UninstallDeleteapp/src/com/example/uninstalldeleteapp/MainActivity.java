package com.example.uninstalldeleteapp;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button button=(Button)findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				if(isPackageExisted("com.versionupdateandroidquery")==true)
				{
				Uri packageURI = Uri.parse("package:"+"com.versionupdateandroidquery");
				Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
				startActivity(uninstallIntent);
				}
				else {
					Toast.makeText(getApplicationContext(), "No package(com.versionupdateandroidquery) found.", Toast.LENGTH_SHORT).show();
					Uri packageURI = Uri.parse("package:"+getPackageName());
					Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
					startActivity(uninstallIntent);
				}
				
			}
		});
	}

	
	public boolean isPackageExisted(String targetPackage){
		   PackageManager pm=getPackageManager();
		   try {
		    PackageInfo info=pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
		       } catch (NameNotFoundException e) {
		    return false;
		    }  
		    return true;
		   }
}
