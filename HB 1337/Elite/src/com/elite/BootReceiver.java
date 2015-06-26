package com.elite;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;


/**
 * This class is used to receive device boot event.
 * @author <b>Elite</b>
 *
 */
public class BootReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent arg1) {
		Intent msgIntent = new Intent(context, IntentServiceClass.class);
		context.startService(msgIntent);
		
		AlarmManagerTXTShield alarmManager=new AlarmManagerTXTShield();
		alarmManager.setAlarm(context);
	}

}
