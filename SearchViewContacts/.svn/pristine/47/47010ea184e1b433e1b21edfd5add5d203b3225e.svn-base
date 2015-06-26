package pl.looksok.searchviewdemo;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

public class SearchViewActivity extends Activity {

	private TextView resultText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search_view);

		resultText = (TextView)findViewById(R.id.searchViewResult);

		setupSearchView();
	}

	private void setupSearchView() {
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		final SearchView searchView = (SearchView) findViewById(R.id.searchView);
		SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
		searchView.setSearchableInfo(searchableInfo);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
			//handles suggestion clicked query
			String displayName = getDisplayNameForContact(intent);
			resultText.setText(displayName);
		} else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			// handles a search query
			String query = intent.getStringExtra(SearchManager.QUERY);
			resultText.setText("should search for query: '" + query + "'...");
		}
	}

	private String getDisplayNameForContact(Intent intent) {
		Cursor phoneCursor = getContentResolver().query(intent.getData(), null, null, null, null);
		phoneCursor.moveToFirst();
		int idDisplayName = phoneCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
		String name = phoneCursor.getString(idDisplayName);
		phoneCursor.close();
		return name;
	}
}
