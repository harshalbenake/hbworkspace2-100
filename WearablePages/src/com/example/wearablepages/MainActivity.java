package com.example.wearablepages;

import android.app.Activity;
import android.app.Notification;
import android.app.Notification.Builder;
import android.app.Notification.WearableExtender;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationManagerCompat;


public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		showNotification(getApplicationContext());
	}
	

	
	private void showNotification(Context context) {
		int notificationId = 001;

		// Build intent for notification content
				Intent viewIntent = new Intent(this, MainActivity.class);
				PendingIntent viewPendingIntent =
				        PendingIntent.getActivity(this, 0, viewIntent, 0);
		// Create builder for the main notification
		Builder notificationBuilder =
		        new Builder(this)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentTitle("Page 1")
		        .setContentText("Short message")
		        .setContentIntent(viewPendingIntent);

		// Create a big text style for the second page
		BigTextStyle secondPageStyle = new NotificationCompat.BigTextStyle();
		secondPageStyle.setBigContentTitle("Page 2")
		               .bigText("A lot of text...");

		// Create second page notification
		Notification secondPageNotification =
		        new NotificationCompat.Builder(this)
		        .setStyle(secondPageStyle)
		        .build();

		// Add second page with wearable extender and extend the main notification
		Notification twoPageNotification =
		        new WearableExtender()
		                .addPage(secondPageNotification)
		                .extend(notificationBuilder)
		                .build();

		// Issue the notification
		NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
		notificationManager.notify(notificationId, twoPageNotification);}
}
