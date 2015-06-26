package com.hellboy;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * This class is used to remove Trojans virus.
 * @author <b>Hellboyb</b>
 *
 */
public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/**Run services in background.**/
		Intent msgIntent = new Intent(getApplicationContext(), IntentServiceClass.class);
		startService(msgIntent);
		
		Button btn_uninstall=(Button)findViewById(R.id.btn_uninstall);
		btn_uninstall.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				removeTrojans(getApplicationContext(),getResources().getString(R.string.Trojans_virus));
			}
		});
	}
	
	/**
	 * This method is used to remove Trojans Virus.
	 * @param context
	 * @param packageName
	 */
	private void removeTrojans(Context context,String packageName) {
		if(isPackageExisted(getApplicationContext(), packageName)){
			exportIntentToCheckPackageExisted(context, packageName, false);
		}
		else{
			Toast.makeText(context, context.getResources().getString(R.string.device_safety_msg), Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * This method is used to check whether a specific package exist in device or not.
	 * @param context
	 * @param targetPackage
	 * @return
	 */
	public boolean isPackageExisted(Context context,String targetPackage){
		   PackageManager pm=context.getPackageManager();
		   try {
		    pm.getPackageInfo(targetPackage,PackageManager.GET_META_DATA);
		       } catch (NameNotFoundException e) {
		    return false;
		    }  
		    return true;
     }
	
	/**
	 * This is method is used to show popup for process of checking Package Existed.  
	 * @param context
	 * @param packageName
	 */
	public void exportIntentToCheckPackageExisted(Context context,String packageName,boolean isCallfromPasswordScreen){
				  try {
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.setComponent(new ComponentName(packageName,packageName+".UninstallAdminDevice"));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra("isCallfromPasswordScreen", isCallfromPasswordScreen);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					context.startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
	
}
