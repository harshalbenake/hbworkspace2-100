package selvin.ListSyncSample;

import selvin.ListSyncSample.provider.ListProvider;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class ListsListActivity extends CustomWindow implements
		AccountManagerCallback<Bundle>, OnItemClickListener {

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenu.ContextMenuInfo menuInfo) {
		menu.setHeaderTitle("List");
		menu.add(0, Constants.MENU_EDIT, 0, R.string.ui_edit);
		menu.add(0, Constants.MENU_DELETE, 0, R.string.ui_delete);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		header.setText(R.string.ui_listslistactivity_title);
		setContentView(R.layout.main);
		ListView listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(this);
		registerForContextMenu(listView);
		listView.setAdapter(new SimpleCursorAdapter(this, R.layout.listrow, managedQuery(
				Uri.withAppendedPath(ListProvider.CONTENT_URI, Database.List.NAME),
				new String[] { BaseColumns._ID, Database.List.C_NAME,
						Database.List.C_DESCRIPTION, Database.List.C_CREATEDATE }, null,
				null, Database.List.C_NAME), new String[] { Database.List.C_NAME,
				Database.List.C_DESCRIPTION, Database.List.C_CREATEDATE }, new int[] {
				R.id.tName, R.id.tDescription, R.id.tCreatedDate }));
		try {
			AccountManager am = AccountManager.get(this);
			Account[] ac = am.getAccountsByType(Constants.ACCOUNT_TYPE);
			if (ac.length == 0) {
				am.addAccount(Constants.ACCOUNT_TYPE, Constants.AUTHTOKEN_TYPE, null,
						null, this, this, null);
			} else {
				ContentResolver.requestSync(ac[0], Constants.AUTHORITY, new Bundle());
			}
		} catch (Exception ex) {
			Log.e("ListSync", ex.getMessage());
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.list, menu);
		return true;
	}

	static final int DIALOG_OK_CANCEL_MESSAGE = 999;

	long lastrowid = -1;

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_OK_CANCEL_MESSAGE:
			return new AlertDialog.Builder(this).setTitle(R.string.ui_alert_delete_list)
					.setMessage(R.string.ui_alert_confirm).setPositiveButton(
							android.R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									getContentResolver().delete(
											Uri.withAppendedPath(Uri.withAppendedPath(
													ListProvider.CONTENT_URI,
													Database.List.NAME), Long
													.toString(lastrowid)), null, null);
									lastrowid = -1;
									Toast.makeText(ListsListActivity.this, "Deleted!",
											Toast.LENGTH_SHORT).show();
								}
							}).setNegativeButton(android.R.string.cancel,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									Toast.makeText(ListsListActivity.this,
											"Deletion canceled!", Toast.LENGTH_SHORT)
											.show();
								}
							}).create();
		}
		return null;
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case Constants.MENU_EDIT:
			AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item
					.getMenuInfo();
			Intent intent = new Intent(Intent.ACTION_EDIT, Uri.withAppendedPath(Uri
					.withAppendedPath(ListProvider.CONTENT_URI, Database.List.NAME), Long
					.toString(menuInfo.id)), this, EditListActivity.class);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.ui_menu_sync:
			doSync();
			return true;
		case R.id.ui_menu_newlist:
			Intent intent = new Intent(Intent.ACTION_INSERT, Uri.withAppendedPath(
					ListProvider.CONTENT_URI, Database.List.NAME), this,
					EditListActivity.class);
			startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void run(AccountManagerFuture<Bundle> arg0) {
	}

	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id) {
		Intent listIntent = new Intent(this, ListActivity.class);
		listIntent.setData(Uri.withAppendedPath(Uri.withAppendedPath(
				ListProvider.CONTENT_URI, Database.List.NAME), Long.toString(id)));
		startActivity(listIntent);

	}
}
