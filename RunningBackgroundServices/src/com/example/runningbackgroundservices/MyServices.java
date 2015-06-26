package com.example.runningbackgroundservices;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Context;

public class MyServices {
	
	public boolean isMyServiceRunning(Context context) {
		ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (IntentServiceClass.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
	
	
	public void getTopActivity(Context context) {

		ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName topActivity = mActivityManager.getRunningTasks(1).get(0).topActivity;
		String packageName = topActivity.getPackageName();
		System.out.println("packageName:-"+packageName);
	}
	
}
