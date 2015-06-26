package com.example.calendarprovider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Attendees;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Instances;
import android.provider.CalendarContract.Reminders;
import android.util.Log;

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
	
	private static final String DEBUG_TAG = "CalendarProvider";
	
	long eventIDUpdate = 188;
	long eventIDDelete = 201;
	long eventIDAttendees = 202;
	long eventIDReminders = 221;
	long eventIDIntentEdit = 208;
	long eventIDIntentView = 208;


	
	public static final String[] INSTANCE_PROJECTION = new String[] {
	    Instances.EVENT_ID,      // 0
	    Instances.BEGIN,         // 1
	    Instances.TITLE          // 2
	  };
	  
	// The indices for the projection array above.
	private static final int PROJECTION_BEGIN_INDEX = 1;
	private static final int PROJECTION_TITLE_INDEX = 2;
	
	// A date-time specified in milliseconds since the epoch.
	long startMillisIntent;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
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
		 
		/***Display calender name*/
	    long calIDDisplay = 2;
	    ContentValues valuesDisplay = new ContentValues();
	    // The new display name for the calendar
	    valuesDisplay.put(Calendars.CALENDAR_DISPLAY_NAME, "HB Demo Calendar");
	    Uri updateUri = ContentUris.withAppendedId(Calendars.CONTENT_URI, calIDDisplay);
	    int rowsDisplay = getContentResolver().update(updateUri, valuesDisplay, null, null);
	    System.out.println("rowsDisplay" + rowsDisplay);
	    Log.i(DEBUG_TAG, "Rows updated: " + rowsDisplay);
	    
	    /**Add Event**/
	    long calIDAddEvent = 60;
	    long startMillis = 0; 
	    long endMillis = 0;     
	    Calendar beginTime = Calendar.getInstance();
	    beginTime.set(2014, 6, 16, 10, 10);
	    startMillis = beginTime.getTimeInMillis();
	    Calendar endTime = Calendar.getInstance();
	    endTime.set(2014, 6, 16, 15, 10);
	    endMillis = endTime.getTimeInMillis();

	    ContentValues valuesAddEvent = new ContentValues();
	    valuesAddEvent.put(Events.DTSTART, startMillis);
	    valuesAddEvent.put(Events.DTEND, endMillis);
	    valuesAddEvent.put(Events.TITLE, "TITLE");
	    valuesAddEvent.put(Events.DESCRIPTION, "DESCRIPTION");
	    valuesAddEvent.put(Events.CALENDAR_ID, calIDAddEvent);
	    valuesAddEvent.put(Events.EVENT_TIMEZONE, "EVENT_TIMEZONE");
	    Uri uriAddEvent = getContentResolver().insert(Events.CONTENT_URI, valuesAddEvent);

	    // get the event ID that is the last element in the Uri
	    long eventID = Long.parseLong(uriAddEvent.getLastPathSegment());
	    
	    // ... do something with event ID
	    System.out.println("eventID "+eventID);
	    
	    /**Update Event**/
	    ContentValues valuesUpdate = new ContentValues();
	    Uri UriUpdate = null;
	    // The new title for the event
	    valuesUpdate.put(Events.TITLE, "TITLE_UPDATE"); 
	    UriUpdate = ContentUris.withAppendedId(Events.CONTENT_URI, eventIDUpdate);
	    int rowsUpdate = getContentResolver().update(UriUpdate, valuesUpdate, null, null);
	    Log.i(DEBUG_TAG, "Rows updated: " + rowsUpdate);  
	    System.out.println("rowsUpdate "+rowsUpdate);

	    /**Delete Event**/
	    ContentValues valuesDelete = new ContentValues();
	    Uri deleteUri = null;
	    deleteUri = ContentUris.withAppendedId(Events.CONTENT_URI,eventIDDelete);
	    int rowsDelete = getContentResolver().delete(deleteUri, null, null);
	    Log.i(DEBUG_TAG, "Rows deleted: " + rowsDelete);  
	    System.out.println("rowsDelete "+rowsDelete);
	    
	    /**Adding Attendees**/
	    ContentValues valuesAttendees = new ContentValues();
	    valuesAttendees.put(Attendees.ATTENDEE_NAME, "ATTENDEE_NAME");
	    valuesAttendees.put(Attendees.ATTENDEE_EMAIL, EmailID);
	    valuesAttendees.put(Attendees.ATTENDEE_RELATIONSHIP, Attendees.RELATIONSHIP_ATTENDEE);
	    valuesAttendees.put(Attendees.ATTENDEE_TYPE, Attendees.TYPE_OPTIONAL);
	    valuesAttendees.put(Attendees.ATTENDEE_STATUS, Attendees.ATTENDEE_STATUS_INVITED);
	    valuesAttendees.put(Attendees.EVENT_ID, eventIDAttendees);
	    Uri uriAttendees = getContentResolver().insert(Attendees.CONTENT_URI, valuesAttendees);
	    
	    /**Adding Reminders**/
	    ContentValues valuesReminders = new ContentValues();
	    valuesReminders.put(Reminders.MINUTES, 15);
	    valuesReminders.put(Reminders.EVENT_ID, eventIDReminders);
	    valuesReminders.put(Reminders.METHOD, Reminders.METHOD_ALERT);
	    Uri uriReminders = getContentResolver().insert(Reminders.CONTENT_URI, valuesReminders);
	    
	    /**Instances table**/
	 // Specify the date range you want to search for recurring
	 // event instances
	 Calendar beginTimeInstances = Calendar.getInstance();
	 beginTimeInstances.set(2014, 6, 16, 10, 10);
	 long startMillisInstances = beginTimeInstances.getTimeInMillis();
	 Calendar endTimeInstances = Calendar.getInstance();
	 endTimeInstances.set(2014, 6, 16, 15, 10);
	 long endMillisInstances = endTimeInstances.getTimeInMillis();
	   
	 Cursor curInstances = null;

	 // The ID of the recurring event whose instances you are searching
	 // for in the Instances table
	 String selectionInstances = Instances.EVENT_ID + " = ?";
	 String[] selectionArgsInstances = new String[] {"207"};

	 // Construct the query with the desired date range.
	 Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
	 ContentUris.appendId(builder, startMillisInstances);
	 ContentUris.appendId(builder, endMillisInstances);

	 // Submit the query
	 curInstances =  getContentResolver().query(builder.build(), 
	     INSTANCE_PROJECTION, 
	     selectionInstances, 
	     selectionArgsInstances, 
	     null);
	    
	 while (curInstances.moveToNext()) {
	     String title = null;
	     long eventIDInstances = 0;
	     long beginVal = 0;    
	     
	     // Get the field values
	     eventIDInstances = curInstances.getLong(PROJECTION_ID_INDEX);
	     beginVal = curInstances.getLong(PROJECTION_BEGIN_INDEX);
	     title = curInstances.getString(PROJECTION_TITLE_INDEX);
	               
	     // Do something with the values. 
	     Log.i(DEBUG_TAG, "Event:  " + title); 
	     Calendar calendar = Calendar.getInstance();
	     calendar.setTimeInMillis(beginVal);  
	     DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy");
	     Log.i(DEBUG_TAG, "Date: " + formatter.format(calendar.getTime()));    
	     System.out.println("eventIDInstances "+eventIDInstances+"beginVal "+beginVal+"title "+title);
	     System.out.println("Date "+formatter.format(calendar.getTime()));
	     }
	 
	 /**Calender Intents**/
	 //Intent to insert event.
	 Calendar beginTimeIntents = Calendar.getInstance();
	 beginTimeIntents.set(2014, 6, 16, 10, 10);
	 Calendar endTimeIntents = Calendar.getInstance();
	 endTimeIntents.set(2014, 6, 16, 15, 10);
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
	 
	//Intent to edit event.
	 Uri uriEdit = ContentUris.withAppendedId(Events.CONTENT_URI, eventIDIntentEdit);
	 Intent intentEdit = new Intent(Intent.ACTION_EDIT)
	     .setData(uriEdit)
	     .putExtra(Events.TITLE, "TITLE_Intents_Edit");
	 startActivity(intentEdit);
	 
	//Intent to View event.
	//open the Calendar to a particular date
	 Uri.Builder builderIntent = CalendarContract.CONTENT_URI.buildUpon();
	 builderIntent.appendPath("time");
	 ContentUris.appendId(builderIntent, startMillisIntent);
	 Intent intentViewDate = new Intent(Intent.ACTION_VIEW)
	 	.setData(builderIntent.build());
	 startActivity(intentViewDate);
	 
	 //open an event for viewing
	 Uri uriView = ContentUris.withAppendedId(Events.CONTENT_URI, eventIDIntentView);
	 Intent intentViewEvent = new Intent(Intent.ACTION_VIEW)
	    .setData(uriView);
	 startActivity(intentViewEvent);
	}
	
	public static Uri asSyncAdapter(Uri uri, String account, String accountType) {
	    return uri.buildUpon()
	        .appendQueryParameter(android.provider.CalendarContract.CALLER_IS_SYNCADAPTER,"true")
	        .appendQueryParameter(Calendars.ACCOUNT_NAME, account)
	        .appendQueryParameter(Calendars.ACCOUNT_TYPE, accountType).build();
	 }
}
