package com.elite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * This class is used to check whether background service is running or not. If not then start them.
 * @author <b>Elite</b>
 *
 */
public class AlarmReceiver extends BroadcastReceiver{
	String TAG="AlarmReceiver";
	
	@Override
	public void onReceive(Context context, Intent arg1) {
		MyServices myServices=new MyServices();
		if(myServices.isMyServiceRunning(context)==false){
			Log.v(TAG,"AlarmReceiver onReceive() service is not running, need to start");
			Intent serviceIntent = new Intent(context, IntentServiceClass.class);
			context.startService(serviceIntent);
		}else{
			Log.v(TAG,"AlarmReceiver onReceive() service is running. ");
		}
	}//onReceive

}
