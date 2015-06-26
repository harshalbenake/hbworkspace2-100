package com.example.calendarprovider;

import java.util.Calendar;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * This class is used to perform various operations related to android calendarprovider.
 * Link:- http://developer.android.com/guide/topics/providers/calendar-provider.html
 * @author <b>Harshal Benake</b>
 *
 */
public class MainActivity extends Activity {
	String EmailID="bits.qa27@gmail.com";
	
	// Projection array. Creating indices for this array instead of doing
	// dynamic lookups improves performance.
	public static final String[] EVENT_PROJECTION = new String[] {
	    Calendars._ID,                           // 0
	    Calendars.ACCOUNT_NAME,                  // 1
	    Calendars.CALENDAR_DISPLAY_NAME,         // 2
	    Calendars.OWNER_ACCOUNT                  // 3
	};
	  
	// The indices for the projection array above.
	private static final int PROJECTION_ID_INDEX = 0;
	private static final int PROJECTION_ACCOUNT_NAME_INDEX = 1;
	private static final int PROJECTION_DISPLAY_NAME_INDEX = 2;
	private static final int PROJECTION_OWNER_ACCOUNT_INDEX = 3;
	
	long eventIDIntent = 208;
	
	// A date-time specified in milliseconds since the epoch.
	long startMillisIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button btn_info=(Button)findViewById(R.id.btn_info);
		Button btn_displayname=(Button)findViewById(R.id.btn_displayname);
		Button btn_open_calendar=(Button)findViewById(R.id.btn_open_calendar);
		Button btn_insert_event=(Button)findViewById(R.id.btn_insert_event);
		Button btn_edit_event=(Button)findViewById(R.id.btn_edit_event);
		Button btn_view_event=(Button)findViewById(R.id.btn_view_event);
		
		btn_info.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				// Run query
				Cursor cur = null;
				Uri uri = Calendars.CONTENT_URI;   
				String selection = "((" + Calendars.ACCOUNT_NAME + " = ?) AND (" 
				                        + Calendars.ACCOUNT_TYPE + " = ?) AND ("
				                        + Calendars.OWNER_ACCOUNT + " = ?))";
				String[] selectionArgs = new String[] {EmailID, "com.google",EmailID}; 
				// Submit the query and get a Cursor object back. 
				cur = getContentResolver().query(uri, EVENT_PROJECTION, selection, selectionArgs, null);
				// Use the cursor to step through the returned records
				while (cur.moveToNext()) {
				    long calID = 0;
				    String displayName = null;
				    String accountName = null;
				    String ownerName = null;
				      
				    // Get the field values
				    calID = cur.getLong(PROJECTION_ID_INDEX);
				    displayName = cur.getString(PROJECTION_DISPLAY_NAME_INDEX);
				    accountName = cur.getString(PROJECTION_ACCOUNT_NAME_INDEX);
				    ownerName = cur.getString(PROJECTION_OWNER_ACCOUNT_INDEX);
				              
				    // Do something with the values...
				    System.out.println("calID "+calID+System.getProperty("line.separator")
				    		+"displayName "+displayName+System.getProperty("line.separator")
				    		+"accountName "+accountName+System.getProperty("line.separator")
				    		+"ownerName "+ownerName);
				}
			}
		});
		btn_displayname.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				/***Display calender name*/
			    long calIDDisplay = 2;
			    ContentValues valuesDisplay = new ContentValues();
			    // The new display name for the calendar
			    valuesDisplay.put(Calendars.CALENDAR_DISPLAY_NAME, "HB Demo Calendar");
			    Uri updateUri = ContentUris.withAppendedId(Calendars.CONTENT_URI, calIDDisplay);
			    int rowsDisplay = getContentResolver().update(updateUri, valuesDisplay, null, null);
			    System.out.println("rowsDisplay" + rowsDisplay);
			}
		});
		btn_open_calendar.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//Intent to View event.
				//open the Calendar to a particular date
				 Uri.Builder builderIntent = CalendarContract.CONTENT_URI.buildUpon();
				 builderIntent.appendPath("time");
				 ContentUris.appendId(builderIntent, startMillisIntent);
				 Intent intentViewDate = new Intent(Intent.ACTION_VIEW)
				 	.setData(builderIntent.build());
				 startActivity(intentViewDate);
			}
		});
		btn_insert_event.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				/**Calender Intents**/
				 //Intent to insert event.
				 Calendar beginTimeIntents = Calendar.getInstance();
				 beginTimeIntents.set(2014, 6, 16, 10, 10);
				 Calendar endTimeIntents = Calendar.getInstance();
				 endTimeIntents.set(2014, 6, 16, 20, 10);
				 Intent intentInsert = new Intent(Intent.ACTION_INSERT)
				         .setData(Events.CONTENT_URI)
				         .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTimeIntents.getTimeInMillis())
				         .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTimeIntents.getTimeInMillis())
				         .putExtra(Events.TITLE, "TITLE_Intents")
				         .putExtra(Events.DESCRIPTION, "DESCRIPTION_Intents")
				         .putExtra(Events.EVENT_LOCATION, "EVENT_LOCATION_Intents")
				         .putExtra(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
				         .putExtra(Intent.EXTRA_EMAIL, EmailID);
				 startActivity(intentInsert);
			}
		});
		btn_edit_event.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//Intent to edit event.
				 Uri uriEdit = ContentUris.withAppendedId(Events.CONTENT_URI, eventIDIntent);
				 Intent intentEdit = new Intent(Intent.ACTION_EDIT)
				     .setData(uriEdit)
				     .putExtra(Events.TITLE, "TITLE_Intents_Edit");
				 startActivity(intentEdit);
			}
		});
		btn_view_event.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				 //open an event for viewing
				 Uri uriView = ContentUris.withAppendedId(Events.CONTENT_URI, eventIDIntent);
				 Intent intentViewEvent = new Intent(Intent.ACTION_VIEW)
				    .setData(uriView);
				 startActivity(intentViewEvent);				
			}
		});
	}
}
