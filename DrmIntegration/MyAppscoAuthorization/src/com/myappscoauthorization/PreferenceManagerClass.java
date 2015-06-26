package com.myappscoauthorization;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * To validate user authenticate flags.
 * @author Amol Wadekar
 */
public class PreferenceManagerClass {
	Context context;
	String PREFERENCENAME = "MyAppsCo_Autherization";
	String Prefrence_ValidateUser = "isValidateUser";
	
	public PreferenceManagerClass(Context context) {
		this.context = context;
	}

	/**
	 * This method is used to manage user validation flag. 
	 * @param String isValidateUser
	 */
	public void addUserValidatePreference(String isValidateUser) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(PREFERENCENAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPreferences.edit();
		editor.putString(Prefrence_ValidateUser, isValidateUser);
		editor.commit();
	}

	/**
	 * This method is used to get user validation flag. 
	 * @return String isValidateUser
	 */
	public String getUserValidatePreference() {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				PREFERENCENAME, Context.MODE_PRIVATE);
		return sharedPreferences.getString(Prefrence_ValidateUser, "-1");
	}
	
}
