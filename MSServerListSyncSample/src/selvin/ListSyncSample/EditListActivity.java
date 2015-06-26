package selvin.ListSyncSample;

import java.text.SimpleDateFormat;
import java.util.Date;

import selvin.ListSyncSample.provider.ListProvider;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

public class EditListActivity extends Activity {
	EditText edName, edDesc;
	Uri id = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.edit_list_activity);
		edName = (EditText) findViewById(R.id.edName);
		edDesc = (EditText) findViewById(R.id.edDesc);
		Intent intent = getIntent();
		if (!Intent.ACTION_INSERT.endsWith(intent.getAction())) {
			id = intent.getData();
			Cursor cursor = managedQuery(id, new String[] { Database.List.C_NAME,
					Database.List.C_DESCRIPTION }, null, null, null);
			if (cursor.moveToFirst()) {
				edName.setText(cursor.getString(0));
				edDesc.setText(cursor.getString(1));
				setTitle(R.string.ui_edit_list);
			} else {
				finish();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.save_cancel, menu);
		return true;
	}

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
				values.put(Database.List.C_NAME, name);
				values.put(Database.List.C_DESCRIPTION, desc);
				if (id == null) {
					values.put(Database.List.C_CREATEDATE, new SimpleDateFormat(
							"yyyy-MM-dd HH:mm:ss").format(new Date()));
					getContentResolver().insert(
							Uri.withAppendedPath(ListProvider.CONTENT_URI, "List"),
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
}
