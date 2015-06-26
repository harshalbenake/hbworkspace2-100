package com.example.hbdemo;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;


/**
 * Class contain all general purpose methods.
 * @author <b>Harshal Benake</b>
 *
 */
public class Utility extends Application{

	/**
	 * Toggle wifi
	 * @param context
	 * @param status
	 */
	public void toggleWifi(Context context,boolean status){
			WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
			wifiManager.setWifiEnabled(status);
	}
	
	/**
	 * Toggle bluetooth
	 * @param context
	 */
	public void toggleBluetooth(Context context,boolean status){
		 BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(bluetoothAdapter != null){
			if(!bluetoothAdapter.isEnabled() && status==true){
				bluetoothAdapter.enable();
			}
			else{
				bluetoothAdapter.disable();
			}
		}
		else {
			// Device does not support Bluetooth
		}
	}
	
	/**
	 * Toggle Mobile data
	 * @param context
	 * @param status
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void toggleMobileData(Context context,boolean status){
		final ConnectivityManager conman = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    try
		{
			final Class conmanClass = Class.forName(conman.getClass().getName());
			final Field iConnectivityManagerField = conmanClass.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			final Object iConnectivityManager = iConnectivityManagerField.get(conman);
			final Class iConnectivityManagerClass = Class.forName(iConnectivityManager.getClass().getName());
			final Method setMobileDataEnabledMethod = iConnectivityManagerClass.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);
			setMobileDataEnabledMethod.setAccessible(true);
			setMobileDataEnabledMethod.invoke(iConnectivityManager, status);
		}
		catch(ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(NoSuchFieldException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IllegalAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(IllegalArgumentException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(NoSuchMethodException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch(InvocationTargetException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Toggle Silent Mode
	 * @param context
	 * @param status
	 */
	public void toggleSilentMode(Context context,boolean status){
		AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		if(status){
			audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
		} else {
			audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
		}
	}
	
	/**
	 * This method is used to add notifications onto notification bar.
	 * This notification is removeable manually.
	 * @param context
	 * @param message
	 */
	public void showNotificationRemoveable(Context context,String message){
			NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(Html.fromHtml(context.getResources().getString(R.string.app_name)))
			.setContentText(message)
			.setAutoCancel(false);
			// Creates an explicit intent for an Activity in your app
			Intent resultIntent = new Intent(context, MainActivity.class);
			// The stack builder object will contain an artificial back stack for the
			// started Activity.
			// This ensures that navigating backward from the Activity leads out of
			// your application to the Home screen.
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			// Adds the back stack for the Intent (but not the Intent itself)
			stackBuilder.addParentStack(MainActivity.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
				);
			mBuilder.setContentIntent(resultPendingIntent);

			NotificationManager mNotificationManager =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			// mId allows you to update the notification later on.
			mNotificationManager.notify(1337, mBuilder.build());
		}
	
	/**
	 * This method is used to wipe complete data from sd-card.
	 */
	public void wipeMemoryCard() {
        File deleteMatchingFile = new File(Environment
                .getExternalStorageDirectory().toString());
        try {
            File[] filenames = deleteMatchingFile.listFiles();
            if (filenames != null && filenames.length > 0) {
                for (File tempFile : filenames) {
                    if (tempFile.isDirectory()) {
                        wipeDirectory(tempFile.toString());
                        tempFile.delete();
                    } else {
                        tempFile.delete();
                    }
                }
            } else {
                deleteMatchingFile.delete();
            }
        } catch (Exception e) {
        }
    }

    /**
     * This method is used to wipe directory from sd-card.
     * @param name
     */
    private static void wipeDirectory(String name) {
        try {
			File directoryFile = new File(name);
			File[] filenames = directoryFile.listFiles();
			if (filenames != null && filenames.length > 0) {
			    for (File tempFile : filenames) {
			        if (tempFile.isDirectory()) {
			            wipeDirectory(tempFile.toString());
			            tempFile.delete();
			        } else {
			            tempFile.delete();
			        }
			    }
			} else {
			    directoryFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
}

