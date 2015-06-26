package selvin.ListSyncSample;

import selvin.ListSyncSample.provider.ListProvider;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ListActivity extends CustomWindow implements OnItemClickListener {

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("Item");
		menu.add(0, Constants.MENU_EDIT, 0, R.string.ui_edit);
		menu.add(0, Constants.MENU_DELETE, 0, R.string.ui_delete);
	}

	String listID = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list_activity);
		header.setText(R.string.ui_listactivity_title);
		Intent intent = getIntent();
		if (intent != null) {
			Uri uri = intent.getData();
			if (uri != null) {
				Cursor cursor = managedQuery(uri, new String[] { BaseColumns._ID,
						Database.List.C_NAME, Database.List.C_DESCRIPTION,
						Database.List.C_CREATEDATE, Database.List.C_ID }, null, null,
						null);

				if (cursor == null) {
					finish();
				} else {
					if (cursor.moveToFirst()) {
						listID = cursor.getString(4);
						((ViewStub) findViewById(R.id.viewStub)).inflate();
						ListView listView = (ListView) findViewById(R.id.listView);
						registerForContextMenu(listView);
						listView.setOnItemClickListener(this);
						((TextView) findViewById(R.id.tName))
								.setText(cursor.getString(1));
						((TextView) findViewById(R.id.tDescription)).setText(cursor
								.getString(2));
						((TextView) findViewById(R.id.tCreatedDate)).setText(cursor
								.getString(3));
						listView.setAdapter(new SimpleCursorAdapter(this,
								R.layout.itemrow, managedQuery(Uri.withAppendedPath(
										ListProvider.CONTENT_URI, Database.Item.NAME),
										new String[] { BaseColumns._ID,
												Database.Item.C_NAME,
												Database.Item.C_DESCRIPTION },
										"ListID=?", new String[] { listID }, null),
								new String[] { Database.Item.C_NAME,
										Database.Item.C_DESCRIPTION }, new int[] {
										R.id.tName, R.id.tDescription }));
					} else {
						finish();
					}

				}
			}
		}
	}

	static final int DIALOG_OK_CANCEL_MESSAGE = 999;
	static long lastrowid = -1;

	protected Dialog onCreateDialog(int id, Bundle bundle) {
		switch (id) {
		case DIALOG_OK_CANCEL_MESSAGE:

			return new AlertDialog.Builder(this).setTitle(R.string.ui_alert_delete_item)
					.setMessage(R.string.ui_alert_confirm).setPositiveButton(
							android.R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									getContentResolver().delete(
											Uri.withAppendedPath(Uri.withAppendedPath(
													ListProvider.CONTENT_URI,
													Database.Item.NAME), Long
													.toString(lastrowid)), null, null);
									lastrowid = -1;
									Toast.makeText(ListActivity.this, "Deleted!",
											Toast.LENGTH_SHORT).show();
								}
							}).setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Toast.makeText(ListActivity.this,
											"Deletion canceled!", Toast.LENGTH_SHORT)
											.show();
								}
							}).create();
		}
		return null;
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Constants.MENU_EDIT:
			AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
			Intent intent = new Intent(Intent.ACTION_EDIT, Uri.withAppendedPath(Uri
					.withAppendedPath(ListProvider.CONTENT_URI, Database.Item.NAME), Long
					.toString(menuInfo.id)), this, EditItemActivity.class);
			startActivity(intent);
			return true;
		case Constants.MENU_DELETE:
			menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
			lastrowid = menuInfo.id;
			showDialog(DIALOG_OK_CANCEL_MESSAGE);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		Intent itemIntent = new Intent(this, ItemActivity.class);
		// Cursor c = (Cursor)l.getItemAtPosition(position);
		// c.getString(c.getColumnIndex(Database.Item.C_ID));
		itemIntent.setData(Uri.withAppendedPath(ListProvider.CONTENT_URI, "ItemFull/"
				+ Long.toString(id)));
		startActivity(itemIntent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.item, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ui_menu_sync:
			doSync();
			return true;
		case R.id.ui_menu_newitem:
			if (listID != null) {
				Intent intent = new Intent(Intent.ACTION_INSERT, Uri.withAppendedPath(Uri
						.withAppendedPath(ListProvider.CONTENT_URI, Database.Item.NAME),
						listID), this, EditItemActivity.class);
				startActivity(intent);
			} else {
				Toast.makeText(this, "Can't add new item to unsaved list!",
						Toast.LENGTH_LONG);
			}
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
