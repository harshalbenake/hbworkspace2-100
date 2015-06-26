package selvin.ListSyncSample;

import selvin.ListSyncSample.provider.ListProvider;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

public class ItemActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.item_activity);

		Intent intent = getIntent();
		if (intent != null) {
			Uri uri = intent.getData();
			if (uri != null) {
				Cursor cursor = managedQuery(uri, new String[] {
						Database.Item.C_NAME + "F", Database.Item.C_DESCRIPTION,
						Database.Item.C_STARTDATE, Database.Item.C_ENDDATE,
						Database.Item.C_PRIORITY + "F", Database.Item.C_STATUS + "F",
						Database.Item.C_ID + "F" }, null, null, null);

				if (cursor == null) {
					finish();
				} else {
					if (cursor.moveToFirst()) {
						((ViewStub) findViewById(R.id.viewStub)).inflate();
						((ImageView) findViewById(R.id.iItem)).setPadding(6, 6, 6, 6);
						((TextView) findViewById(R.id.tName))
								.setText(cursor.getString(0));
						((TextView) findViewById(R.id.tDescription)).setText(cursor
								.getString(1));
						((TextView) findViewById(R.id.tStartDate)).setText(cursor
								.getString(2));
						((TextView) findViewById(R.id.tEndDate)).setText(cursor
								.getString(3));
						((TextView) findViewById(R.id.tPriority)).setText(cursor
								.getString(4));
						((TextView) findViewById(R.id.tStatus)).setText(cursor
								.getString(5));
						Cursor tags = managedQuery(Uri.withAppendedPath(
								ListProvider.CONTENT_URI, String.format("Item/%s/Tags",
										cursor.getString(6))), null, null, null, null);
						if (tags.moveToFirst()) {
							StringBuilder sb = new StringBuilder();
							do{
								if(sb.length() > 0)
									sb.append(", ");
								sb.append(tags.getString(0));
							}while(tags.moveToNext());
							((TextView) findViewById(R.id.tTags)).setText(sb.toString());
						}
					} else {
						finish();
					}

				}
			}
		}
	}
}
