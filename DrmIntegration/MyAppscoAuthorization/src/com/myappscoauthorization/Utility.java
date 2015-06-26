package com.myappscoauthorization;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;

/**
 * To get general device information for DRM service.
 * @author Amol Wadekar
 */
public class Utility {

	
	/**
	 * This method return used to get device id from TelephonyManager / SECURE .
	 * <Br><Br>
	 * <B>
	 * A 64-bit number (as a hex string) that is randomly generated on the
	 * devices first boot and should remain constant for the lifetime of the
	 * device. (The value may change if a factory reset is performed on the
	 * device.)
	 * </B>
	 
	 * <Br>
	 * @return <B>String deviceID</B>
	 */
	public String getDeviceId(Context context) {
		/*TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String deviceId = tm.getDeviceId();// default actual device ID
		if(deviceId==null || deviceId.isEmpty()){
			deviceId = Secure.getString(context.getContentResolver(),Secure.ANDROID_ID);
		}*/
		
		String deviceId = Secure.getString(context.getContentResolver(),Secure.ANDROID_ID);
		System.out.println("############## Secure Device ID : "+deviceId);
		return deviceId;
	}


	/**
	 * This method return device name like device Manufacture + Model name. 
	 * @return String deviceName
	 */
	public String getDeviceName(){
		String deviceName="";
		//		deviceName=android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL;
		deviceName=android.os.Build.MODEL;
		return deviceName;
	}

	/**
	 * This method return device name like device Manufacture + Model name. 
	 * @return String deviceName
	 */
	public String getDeviceSDKVersion(){
		System.out.println(" SDK_INT "+android.os.Build.VERSION.SDK_INT);
		System.out.println(" RELEASE "+android.os.Build.VERSION.RELEASE);
		System.out.println(" CODENAME "+android.os.Build.VERSION.CODENAME);
		String deviceSDK=android.os.Build.VERSION.RELEASE;
		return deviceSDK;
	}
	
	/**
	 * This method is use to check the device internet connectivity.
	 * 
	 * @param context
	 * @return true :if your device is connected to internet. false :if your
	 *         device is not connected to internet.
	 */
	public static boolean isConnected(Context context) {
		// return true;
		// TODO un-comment when prototype demo done.

		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		if (info == null)
			return false;
		if (info.getState() != State.CONNECTED)
			return false;

		return true;
	}
}
