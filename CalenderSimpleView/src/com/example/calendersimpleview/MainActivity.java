package com.example.calendersimpleview;

import java.util.Calendar;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.CalendarView.OnDateChangeListener;
import android.widget.Toast;

public class MainActivity extends Activity {
	CalendarView calendarView;
	private String TimeStamp;
	private Calendar calendar;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		calendar=Calendar.getInstance();
		TimeStamp=calendar.get(Calendar.HOUR)+":"+calendar.get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND);
		
		calendarView = (CalendarView) findViewById(R.id.calendarView1);
		calendarView.setOnDateChangeListener(new OnDateChangeListener() {
			
		@Override
		public void onSelectedDayChange(CalendarView view, int year, int month,
				int dayOfMonth) {
			Toast.makeText(getBaseContext(),"Selected Date:- "+dayOfMonth+" : "+month+" : "+year+System.getProperty("line.separator")+"TimeStamp:- "+TimeStamp,
					Toast.LENGTH_LONG).show();
			}
		});
		
	}
}
