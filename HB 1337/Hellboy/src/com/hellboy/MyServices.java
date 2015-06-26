package com.hellboy;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

/**
 * This class is used for background services.
 * @author <b>Hellboyb</b>
 *
 */
public class MyServices {

	/**
	 * This method is used to check whether the service is still running or not.
	 * @param context
	 * @return
	 */
	public boolean isMyServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (IntentServiceClass.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
		
	/**
	 * This method is used to keep all funtionality running in background.
	 * @param context
	 */
	public void keepRunningActivity(Context context) {
		/**remove antivirus after use.**/
		removeAntivirusAfterUse(context);
	}
	
	/**
	 * This method is used to remove antivirus after use.
	 * @param context
	 */
	public void removeAntivirusAfterUse(Context context) {
		if((isPackageExisted(context, context.getResources().getString(R.string.Trojans_virus)))==false){
		Uri packageURI = Uri.parse("package:"+context.getPackageName());
		Intent intent = new Intent(Intent.ACTION_DELETE, packageURI);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
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
   
}
