package com.elite;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * This class is used to check periodically whether background service is running or not.
 * If not then start.
 * @author <b>Elite</b>
 *
 */
public class AlarmManagerTXTShield {
	long alarmTimeInterval=1000 * 60 * 1;// Millisecond * Second * Minute
	
	/**
	 * This method is used to set alarm for 10 minute until device is not shut down.
	 * @param Context context
	 */
	public void setAlarm(Context context){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), alarmTimeInterval, pi); 
    }

	/**
	 * This method is used to cancel alarm.
	 * @param Context context
	 */
    public void cancelAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
	
}
