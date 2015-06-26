package com.example.demo;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Prefernce Manageer class.
 * 
 *
 */
public class PreferncesManagerClass {
	public static final String PREFERENCENAME="txtshield";
	private final String IS_NOTIFICATION_CLEAR = "notification_clear";
	private Context context;

	public PreferncesManagerClass(Context context){
		this.context=context;
	}

	/**
	 * This method is used to clear all shared preferences.
	 */
	public void clearSharedPreferences() {
		SharedPreferences sharedPreferences=context.getSharedPreferences(PREFERENCENAME, Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.clear();
		editor.commit();
	}

	
	/**
	 * This method is used to set Notification display status. True user able to see notification else not.
	 * @param boolean isNotificationEnabled 
	 */
	public void addNotificationFlag(boolean isNotificationEnabled){
		SharedPreferences sharedPreferences=context.getSharedPreferences(PREFERENCENAME, Context.MODE_MULTI_PROCESS);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putBoolean(IS_NOTIFICATION_CLEAR, isNotificationEnabled);
		editor.commit();
	}

	/**
	 * This method is used to get Notification display status.
	 * @return boolean isNotificationEnabled [true: is notification is display else false]
	 */
	public boolean getNotificationFlagStatus(){
		SharedPreferences sharedPreferences=context.getSharedPreferences(PREFERENCENAME, Context.MODE_MULTI_PROCESS);
		return sharedPreferences.getBoolean(IS_NOTIFICATION_CLEAR, false);
	}
	
	
}
