package com.example.demo;

import com.google.android.gcm.GCMBaseIntentService;

import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * GCM Service used to receive messages from cloud.
 * 
 */
public class GCMIntentService extends GCMBaseIntentService {
	String TAG="GCM";
	@Override
	protected void onError(Context arg0, String arg1) {
		try {
			Log.v(TAG, "onError() "+arg1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onMessage(Context context, Intent intent) {
		try {
			String action = intent.getAction();
			Log.v(TAG, "onMessage() "+action);
			if (action.equals("com.google.android.c2dm.intent.REGISTRATION")) {
				Log.v(TAG,"Handle Registration");
			} else if (action.equals("com.google.android.c2dm.intent.RECEIVE")) {
				Log.v(TAG,"intent.getExtras() "+intent.getExtras());
				
				/**Show notification.*/
				String messageText= intent.getStringExtra("messageText")+"";
				NotificationManagerTXTShield managerTXTShield=new NotificationManagerTXTShield();
				if(messageText!=null && !messageText.equalsIgnoreCase("") && !messageText.equalsIgnoreCase("null"))
				{
				managerTXTShield.showNotificationRemoveableGCM(context,messageText);
				}
				else
				{
				managerTXTShield.showNotificationRemoveableGCM(context,"notification message google play update");
				}
			}
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onRegistered(Context arg0, String arg1) {
		try {
			Log.v(TAG, "onRegistered() "+arg1);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void onUnregistered(Context arg0, String arg1) {
		try {
			Log.v(TAG, "onUnregistered() "+arg1);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

@Override
protected String[] getSenderIds(Context context) {
	 String[] ids = new String[1];
	 NotificationManagerTXTShield managerTXTShield=new NotificationManagerTXTShield();
     ids[0] = managerTXTShield.getGCMSenderID(context);
     return ids;
}

}
