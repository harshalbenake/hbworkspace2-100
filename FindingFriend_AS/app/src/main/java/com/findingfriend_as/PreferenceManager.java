package com.findingfriend_as;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * This class is used for Shared Preferences
 * Created by <b>Harshal Benake</b> on 9/2/15.
 */
public class PreferenceManager {

    Context mContext;
    public static final String PREFERENCE = "Bmonitored";
    public final String LATITUDE = "latitude";
    public final String LONGITUDE = "longitude";

    public PreferenceManager(Context context) {
        this.mContext = context;
    }


    /**
     * Add latitude in SharedPreferences.
     *
     * @param latitude
     */
    public void addLatitude(String latitude) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREFERENCE, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LATITUDE, latitude);
        editor.commit();
    }

    /**
     * Get latitude from SharedPreferences.
     *
     * @return
     */
    public String getLatitude() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREFERENCE, Context.MODE_MULTI_PROCESS);
//        System.out.println("getLatitude: "+sharedPreferences.getString(LATITUDE, ""));
        return sharedPreferences.getString(LATITUDE, "");
    }

    /**
     * Add longitude in SharedPreferences.
     *
     * @param longitude
     */
    public void addLongitude(String longitude) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREFERENCE, Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LONGITUDE, longitude);
        editor.commit();
    }

    /**
     * Get longitude from SharedPreferences.
     *
     * @return
     */
    public String getLongitude() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences(PREFERENCE, Context.MODE_MULTI_PROCESS);
//        System.out.println("getLatitude: "+sharedPreferences.getString(LONGITUDE, ""));
        return sharedPreferences.getString(LONGITUDE, "");
    }
}
