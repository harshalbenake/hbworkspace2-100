package com.example.demo;

import android.content.Context;
import android.os.AsyncTask;

/**
 * This async task is used to send device inforamtion for gcm notification. 
 * @author <b>harshalb</b>
 *
 */
public class Async_device_registration extends AsyncTask<String, String, String>{
	Context context;
    boolean status;

	public Async_device_registration(Context context){
		this.context=context;
	}

	@Override
	protected void onPreExecute() {
		if(!Utility.isConnected(context)){
			cancel(true);
		}
		super.onPreExecute();
	}
	@Override
	protected String doInBackground(String... params) {
		if(!isCancelled()){
			try {
				AccountManager accountManager=new AccountManager(context);
				String userId="";
				String response=accountManager.requestDeviceRegistration(context, userId);
				System.out.println("response"+response);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}