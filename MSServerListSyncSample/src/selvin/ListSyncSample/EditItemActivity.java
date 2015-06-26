package selvin.ListSyncSample;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import selvin.ListSyncSample.provider.ListProvider;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class EditItemActivity extends Activity implements OnClickListener {
	EditText edName, edDesc;
	TextView tStartDate, tStartTime, tEndDate, tEndTime;
	Spinner sPriority, sStatus;
	Uri id = null;
	TextView lastDateView = null;

	final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	Calendar cal = Calendar.getInstance();

	int mYear = cal.get(Calendar.YEAR);
	int mMonth = cal.get(Calendar.MONTH);
	int mDay = cal.get(Calendar.DAY_OF_MONTH);
	int mHour = cal.get(Calendar.HOUR_OF_DAY);
	int mMinute = cal.get(Calendar.MINUTE);

	String listID = null;
	static final int TIME_DIALOG_ID = 0;
	static final int DATE_DIALOG_ID = 1;
	static final int TAG_DIALOG_ID = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_item_activity);
		edName = (EditText) findViewById(R.id.edName);
		edDesc = (EditText) findViewById(R.id.edDesc);
		sPriority = (Spinner) findViewById(R.id.sPriority);
		sStatus = (Spinner) findViewById(R.id.sStatus);
		tStartDate = (TextView) findViewById(R.id.tStartDate);
		tStartTime = (TextView) findViewById(R.id.tStartTime);
		tEndDate = (TextView) findViewById(R.id.tEndDate);
		tEndTime = (TextView) findViewById(R.id.tEndTime);
		tStartDate.setOnClickListener(this);
		tStartTime.setOnClickListener(this);
		tEndDate.setOnClickListener(this);
		tEndTime.setOnClickListener(this);
		SimpleCursorAdapter adPriority = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, managedQuery(Uri.withAppendedPath(
						ListProvider.CONTENT_URI, "Priority"), new String[] {
						Database.IDAS_ID, Database.Priority.C_NAME }, null, null, null),
				new String[] { Database.Priority.C_NAME },
				new int[] { android.R.id.text1 });
		adPriority.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sPriority.setAdapter(adPriority);
		SimpleCursorAdapter adStatus = new SimpleCursorAdapter(this,
				android.R.layout.simple_spinner_item, managedQuery(Uri.withAppendedPath(
						ListProvider.CONTENT_URI, "Status"), new String[] {
						Database.IDAS_ID, Database.Status.C_NAME }, null, null, null),
				new String[] { Database.Status.C_NAME }, new int[] { android.R.id.text1 });
		adStatus.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sStatus.setAdapter(adStatus);
		Intent intent = getIntent();
		if (!Intent.ACTION_INSERT.endsWith(intent.getAction())) {
			id = intent.getData();
			Cursor cursor = managedQuery(id, new String[] { Database.Item.C_NAME,
					Database.Item.C_DESCRIPTION, Database.Item.C_PRIORITY,
					Database.Item.C_STATUS, Database.Item.C_STARTDATE,
					Database.Item.C_ENDDATE }, null, null, null);
			if (cursor.moveToFirst()) {
				setTitle(R.string.ui_edit_item);
				edName.setText(cursor.getString(0));
				edDesc.setText(cursor.getString(1));
				int sid = cursor.getInt(2);
				for (int i = 0; i < sPriority.getCount(); i++) {
					Cursor value = (Cursor) sPriority.getItemAtPosition(i);
					int id = value.getInt(value.getColumnIndex(BaseColumns._ID));
					if (id == sid) {
						sPriority.setSelection(i);
						break;
					}
				}
				sid = cursor.getInt(3);
				for (int i = 0; i < sStatus.getCount(); i++) {
					Cursor value = (Cursor) sStatus.getItemAtPosition(i);
					int id = value.getInt(value.getColumnIndex(BaseColumns._ID));
					if (id == sid) {
						sStatus.setSelection(i);
						break;
					}
				}
				String time = cursor.getString(4);
				tStartDate.setText(time.substring(0, 10));
				tStartTime.setText(time.substring(10, 16));
				time = cursor.getString(5);
				tEndDate.setText(time.substring(0, 10));
				tEndTime.setText(time.substring(10, 16));
			} else {
				finish();
			}
		} else {
			listID = intent.getData().getPathSegments().get(1);
			tStartDate.setText(String.format("%04d-%02d-%02d", mYear, mMonth, mDay));
			tEndDate.setText(String.format("%04d-%02d-%02d", mYear, mMonth, mDay + 1));
			String time = String.format("%02d:%02d", mHour, mMinute);
			tStartTime.setText(time);
			tEndTime.setText(time);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.save_cancel, menu);
		return true;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case TIME_DIALOG_ID:
			return new TimePickerDialog(this, mTimeSetListener, mHour, mMinute,
					DateFormat.is24HourFormat(this));
		case DATE_DIALOG_ID:
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);
		case TAG_DIALOG_ID:
			return new AlertDialog.Builder(this).setTitle("Tags").create();//.setMultiChoiceItems(arg0, arg1, arg2, arg3)
		}
		return null;
	}

	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {

		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			if (lastDateView != null) {
				lastDateView.setText(String.format("%04d-%02d-%02d", year,
						monthOfYear + 1, dayOfMonth));
				lastDateView = null;
			}
		}
	};

	private TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {

		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			if (lastDateView != null) {
				lastDateView.setText(String.format("%02d:%02d", hourOfDay, minute));
				lastDateView = null;
			}
		}
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ui_edit_cancel:
			finish();
			return true;
		case R.id.ui_edit_save:
			String name = edName.getText().toString(),
			desc = edDesc.getText().toString();
			if (name.length() != 0 && desc.length() != 0) {
				ContentValues values = new ContentValues();
				values.put(Database.Item.C_NAME, name);
				values.put(Database.Item.C_DESCRIPTION, desc);
				Cursor c = (Cursor) sPriority.getSelectedItem();
				values.put(Database.Item.C_PRIORITY, c.getInt(c
						.getColumnIndex(BaseColumns._ID)));
				c = (Cursor) sStatus.getSelectedItem();
				values.put(Database.Item.C_STATUS, c.getInt(c
						.getColumnIndex(BaseColumns._ID)));
				values.put(Database.Item.C_STARTDATE, String.format("%s %s:00",
						tStartDate.getText(), tStartTime.getText()));
				values.put(Database.Item.C_ENDDATE, String.format("%s %s:00", tEndDate
						.getText(), tEndTime.getText()));
				if (id == null) {
					values.put(Database.Item.C_LISTID, listID);
					getContentResolver().insert(
							Uri.withAppendedPath(ListProvider.CONTENT_URI, "Item"),
							values);
				} else {
					getContentResolver().update(id, values, null, null);
				}
				finish();
			} else {
				Toast.makeText(this,
						getResources().getString(R.string.ui_name_description_empty),
						Toast.LENGTH_SHORT).show();
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.tStartDate:
		case R.id.tEndDate:
			TextView tv = lastDateView = (TextView) v;
			try {
				cal.setTime(sdf.parse(tv.getText() + " 00:00:00"));
				mYear = cal.get(Calendar.YEAR);
				mMonth = cal.get(Calendar.MONTH);
				mDay = cal.get(Calendar.DAY_OF_MONTH);
				showDialog(DATE_DIALOG_ID);
			} catch (ParseException e) {
				e.printStackTrace();
			}

			break;
		case R.id.tStartTime:
		case R.id.tEndTime:
			tv = lastDateView = (TextView) v;
			try {
				cal.setTime(sdf.parse("2011-01-01 " + tv.getText() + ":00"));
				mHour = cal.get(Calendar.HOUR_OF_DAY);
				mMinute = cal.get(Calendar.MINUTE);
				showDialog(TIME_DIALOG_ID);
			} catch (ParseException e) {
				e.printStackTrace();
			}

			break;
		}
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case TIME_DIALOG_ID:
			((TimePickerDialog) dialog).updateTime(mHour, mMinute);
			break;
		case DATE_DIALOG_ID:
			((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
			break;
		}
	}
}
