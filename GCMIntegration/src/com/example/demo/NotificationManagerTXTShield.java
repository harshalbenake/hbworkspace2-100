package com.example.demo;

import com.example.demo.R;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.Html;

/**
 * This class is used to show notifications of application. 
 *
 */
public class NotificationManagerTXTShield {
	final int NOTI_REMOVEABLE=1002;
	public String SENDER_ID="";
	
	/**
	 * This method is used to add notifications onto notification bar.
	 * This notification is removeable manually.
	 * @param context
	 */
	public void showNotificationRemoveableGCM(Context context,String message){
		try {
			NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context)
			.setSmallIcon(R.drawable.ic_launcher)
			.setContentTitle(Html.fromHtml("notification title"))
			.setContentText(message)
			.setAutoCancel(false);

			// Creates an explicit intent for an Activity in your app
				String applicationPackageName=context.getPackageName();
			
			Intent resultIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id="+applicationPackageName));

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
			mNotificationManager.notify(NOTI_REMOVEABLE, mBuilder.build());
		} catch (NotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to clear notifiaction from notification bar.
	 */
	public void clearNotification(Context context){
		/**Set notification clear flag on preferences.*/
		PreferncesManagerClass preferncesManagerClass=new PreferncesManagerClass(context);
		preferncesManagerClass.addNotificationFlag(false);
	}

	public String getGCMSenderID(Context context) {
		SENDER_ID=context.getResources().getString(R.string.gcm_project_id);
		return SENDER_ID;
	}
}
