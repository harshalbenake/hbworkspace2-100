package com.elite;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

/**
 * This class is used for background services.
 * @author <b>Elite</b>
 *
 */
public class IntentServiceClass extends Service{
	private Context context;

	private static Timer timer = new Timer();
	public IntentServiceClass() {
		//		super("IntentServiceClass");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		startService();
	}

	/**
	 * This method is used to start service backgroundly.
	 */
	private void startService(){
		timer.scheduleAtFixedRate(new mainTask(), 0, 500);
		context=this;
	}

	private class mainTask extends TimerTask {
		public void run() {
			toastHandler.sendEmptyMessage(0);
		}
	}

	/**
	 * This handler is used to check current front activity & launch password activity.
	 */
	private final Handler toastHandler = new Handler() {
		public void handleMessage(Message msg) {
			MyServices myServices=new MyServices();
			myServices.keepRunningActivity(context);
		};
	};			
}
