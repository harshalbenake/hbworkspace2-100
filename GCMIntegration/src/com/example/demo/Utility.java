package com.example.demo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.telephony.TelephonyManager;
import android.telephony.gsm.GsmCellLocation;
import android.view.inputmethod.InputMethodManager;

/**
 * Class contain all general purpose methods.
 * 
 *
 */
public class Utility {
	/** Basic app changes **/
	boolean isGoldVersion=false;
	boolean isBasicVersion=false;

	/**
	 * This method is use to check the device internet connectivity.
	 * 
	 * @param context
	 * @return true :if your device is connected to internet.
	 *         false :if your device is not connected to internet. 
	 */
	public static boolean isConnected(Context context)
	{
		//		return true;
		//TODO un-comment when prototype demo done.

		ConnectivityManager manager = (ConnectivityManager)
		context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		if (info == null)
			return false;
		if (info.getState() != State.CONNECTED)
			return false;

		return true;
	}
	
	/**
	 * This method is used to get tower unique cid.
	 */
	public String getDeviceProvider_CID(Context context){
		String cid="";
		final TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (telephony.getPhoneType() == TelephonyManager.PHONE_TYPE_GSM) {
			final GsmCellLocation location = (GsmCellLocation) telephony.getCellLocation();

			if (location != null) {

				cid= "LAC: " + location.getLac() + " CID: " + location.getCid();

			}
		}
		return cid;
	}

	/**
	 * This method return device name like device Manufacture + Model name. 
	 * @return String deviceName
	 */
	public String getDeviceName(){
		String deviceName="";
		deviceName=android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL;
		return deviceName;
	}
	
	/**
	 * This method return device name like device Model name. 
	 * @return String deviceName
	 */
	public String getDeviceModelName(){
		String deviceModelName="";
		deviceModelName=android.os.Build.MODEL;
		return deviceModelName;
	}
	

	/**
	 * This method return device name like device Manufacture + Model name. 
	 * @return String deviceName
	 */
	public String getDeviceSDKVersion(){
		String deviceSDK=android.os.Build.VERSION.RELEASE;
		return deviceSDK;
	}

	/**
	 * This function returns whether the inputed string is valid or not.
	 * @param inputString
	 * @return
	 */
	public boolean isValidText(CharSequence inputStr){

		String expression = "^[a-zA-Z]+[a-zA-Z0-9 '$._]*$";
		Pattern pattern = Pattern.compile(expression);
		Matcher matcher = pattern.matcher(inputStr);
		if(matcher.matches())
		{
			return true;
		}
		else {
			return false;
		}
	}


	/**
	 * This function returns whether the inputed string is valid or not.
	 * This is made for username spl validation.
	 * @param inputStr
	 * @return
	 */
	public boolean isValidTextSpl(CharSequence inputStr){

		String expression = "^[0-9a-zA-Z]+$";
		Pattern pattern = Pattern.compile(expression);
		Matcher matcher = pattern.matcher(inputStr);
		if(matcher.matches())
		{
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * This function returns whether the inputed email ID is valid or not.
	 * @param inputString
	 * @return
	 */
	public boolean isValidEmail(CharSequence inputStr){

		String expression = "^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$";

		Pattern pattern = Pattern.compile(expression);
		Matcher matcher = pattern.matcher(inputStr);
		if(matcher.matches())
		{
			return true;
		}
		else {
			return false;
		}
	}

	/**
	 * This method is used to hide soft keyboard.
	 * @param activity
	 */
	public void hideSoftKeyboard(Activity activity) {
		InputMethodManager inputMethodManager = (InputMethodManager)activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
	}

	/**
	 * This method is used to show soft keyboard.
	 * @param activity
	 */
	public void showSoftKeyboard(Activity activity) {
		InputMethodManager inputMgr = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMgr.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
	}

	/**
	 * This function let you to convert string in Title Case.
	 * @param givenString
	 * @return
	 */
	public String toTitleCase(String givenString) {
		String[] arr = givenString.split(" ");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < arr.length; i++) {
			sb.append(Character.toUpperCase(arr[i].charAt(0))).append(arr[i].substring(1)).append(" ");
		}          
		return sb.toString().trim();
	}


	/**
	 * This function returns the device's unique serial number.
	 * @return
	 */
	public String getDeviceSerialNumber() {
		String serial = null; 

		try {
			Class<?> c = Class.forName("android.os.SystemProperties");
			Method get = c.getMethod("get", String.class);
			serial = (String) get.invoke(c, "ro.serialno");
		} catch (Exception ignored) {
			ignored.printStackTrace();
		}
		return serial;
	} 
	public byte [] bitmapTobyteArray(Context context, Bitmap bitmap){
		ByteArrayOutputStream baos=new  ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
		byte [] rawBytes=baos.toByteArray();

		return rawBytes;
	}

	/**
	 * This method is used to write content into file.
	 * @param Context context
	 * @param String text
	 */
	public void writeLogIntoFile(Context context,String path,String text){
		try{
			File file=new File(path);
			FileOutputStream outputStream=new FileOutputStream(file, true);

			outputStream.write(text.getBytes());
			outputStream.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to delete file from sd-card.
	 * @param Context context
	 * @param String text
	 */
	public void deleteLogFile(Context context,String path){
		try{
			File file = new File(path);
			file.delete();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * This method is used to get notification ID from cloud.
	 */
	public String getNotificationID(Context context){
		String regId=null;
		try{
			NotificationManagerTXTShield notificationManagerTXTShield=new NotificationManagerTXTShield();
			GCMRegistrar.checkDevice(context);
			GCMRegistrar.checkManifest(context);
			regId= GCMRegistrar.getRegistrationId(context);
			if (regId.equals("")) {
				GCMRegistrar.register(context, notificationManagerTXTShield.getGCMSenderID(context));
			} else {
			
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return regId;
	}
	
	
}
