package selvin.ListSyncSample.provider;

import java.util.HashMap;
import java.util.UUID;

import selvin.ListSyncSample.Constants;
import selvin.ListSyncSample.Database;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class ListProvider extends ContentProvider {

	static final String LTAG = "ListProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + Constants.AUTHORITY);

	private static final int LISTS = 1;
	private static final int LIST = 2;
	private static final int ITEMS = 3;
	private static final int ITEM = 4;
	private static final int USERS = 5;
	private static final int USER = 6;
	private static final int TAGS = 7;
	private static final int TAG = 8;
	private static final int STATUSES = 9;
	private static final int STATUS = 10;
	private static final int PRIORITIES = 11;
	private static final int PRIORITY = 12;
	private static final int ITEMS_FULL = 13;
	private static final int ITEM_TAGS = 14;
	private static final int TAGITEMMAPPING = 15;
	private static final int LIST_ITEMS = 16;

	public static final String LISTS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/List";
	public static final String LIST_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/List";
	public static final String ITEMS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/Item";
	public static final String ITEM_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/Item";
	public static final String USERS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/User";
	public static final String USER_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/User";
	public static final String TAGS_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/Tag";
	public static final String TAG_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/Tag";
	public static final String TAGSNAME_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/TagName";
	public static final String STATUSES_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/Status";
	public static final String STATUS_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/Status";
	public static final String PRIORITIES_MIME_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/Priority";
	public static final String PRIORITY_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/Priority";
	public static final String TAGITEMMAPPING_MIME_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/TagItemMapping";

	static final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
	static final HashMap<String, String> map = new HashMap<String, String>();
	static {
		// to get definitions...
		matcher.addURI(Constants.AUTHORITY, "List", LISTS);
		matcher.addURI(Constants.AUTHORITY, "List/*", LIST);
		matcher.addURI(Constants.AUTHORITY, "List/*/Item", LIST_ITEMS);
		matcher.addURI(Constants.AUTHORITY, "Item", ITEMS);
		matcher.addURI(Constants.AUTHORITY, "ItemFull/#", ITEMS_FULL);
		matcher.addURI(Constants.AUTHORITY, "Item/*/Tags", ITEM_TAGS);
		matcher.addURI(Constants.AUTHORITY, "Item/*", ITEM);
		matcher.addURI(Constants.AUTHORITY, "User", USERS);
		matcher.addURI(Constants.AUTHORITY, "User/*", USER);
		matcher.addURI(Constants.AUTHORITY, "Tag", TAGS);
		matcher.addURI(Constants.AUTHORITY, "Tag/#", TAG);
		matcher.addURI(Constants.AUTHORITY, "Status", STATUSES);
		matcher.addURI(Constants.AUTHORITY, "Status/#", STATUS);
		matcher.addURI(Constants.AUTHORITY, "Priority", PRIORITIES);
		matcher.addURI(Constants.AUTHORITY, "Priority/#", PRIORITY);
		matcher.addURI(Constants.AUTHORITY, "TagItemMapping/#", TAGITEMMAPPING);
		map.put(BaseColumns._ID, "ROWID AS _id");
		map.put(Database.IDAS_ID, "ID AS _id");
		map.put(Database.List.C_ID, Database.List.C_ID);
		map.put(Database.List.C_NAME, Database.List.C_NAME);
		map.put(Database.List.C_DESCRIPTION, Database.List.C_DESCRIPTION);
		map.put(Database.List.C_CREATEDATE, Database.List.C_CREATEDATE);
		map.put(Database.Item.C_STARTDATE, Database.Item.C_STARTDATE);
		map.put(Database.Item.C_ENDDATE, Database.Item.C_ENDDATE);
		map.put(Database.Item.C_STATUS, Database.Item.C_STATUS);
		map.put(Database.Item.C_PRIORITY, Database.Item.C_PRIORITY);
		map
				.put(Database.Item.C_STATUS + "F", "S.Name AS " + Database.Item.C_STATUS
						+ "F");
		map.put(Database.Item.C_PRIORITY + "F", "P.Name AS " + Database.Item.C_PRIORITY
				+ "F");
		map.put(Database.Item.C_ID + "F", "I." + Database.Item.C_ID + " AS "
				+ Database.Item.C_ID + "F");
		map.put(Database.Item.C_NAME + "F", "I." + Database.Item.C_NAME + " AS "
				+ Database.Item.C_NAME + "F");
		map.put(Database.Item.C_NAME + "M", "T." + Database.Item.C_NAME + " AS "
				+ Database.Item.C_NAME + "M");
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String table, rowid;
		switch (matcher.match(uri)) {

		case LIST:
			table = uri.getPathSegments().get(0);
			rowid = uri.getPathSegments().get(1);
			Cursor list = mDB.getWritableDatabase().query(table,
					new String[] { Database.List.C_ID }, "ROWID=?",
					new String[] { rowid }, null, null, null);
			if (list.moveToFirst()) {
				// cascade delete all Items with ListID = ID of this list
				Cursor items = mDB.getWritableDatabase().query(Database.Item.NAME,
						new String[] { "ROWID" }, "ListID=?",
						new String[] { list.getString(0) }, null, null, null);
				if (items.moveToFirst()) {
					do {
						//just call recursive delete function
						delete(Uri.withAppendedPath(Uri.withAppendedPath(
								ListProvider.CONTENT_URI, "Item"), items.getString(0)),
								null, null);
					} while (items.moveToNext());
				}
				items.close();
			}
			list.close();
			break;
		case ITEM:
			table = uri.getPathSegments().get(0);
			rowid = uri.getPathSegments().get(1);
			Cursor item = mDB.getWritableDatabase().query(table,
					new String[] { Database.Item.C_ID }, "ROWID=?",
					new String[] { rowid }, null, null, null);
			if (item.moveToFirst()) {
				// cascade delete all TagItemMappings with LItemID = ID of this item
				Cursor tagm = mDB.getWritableDatabase().query(Database.TagItemMapping.NAME,
						new String[] { "ROWID" }, "ItemID=?",
						new String[] { item.getString(0) }, null, null, null);
				if (tagm.moveToFirst()) {
					do {
						//just call recursive delete function
						delete(Uri.withAppendedPath(Uri.withAppendedPath(
								ListProvider.CONTENT_URI, "TagItemMapping"), tagm.getString(0)),
								null, null);
					} while (tagm.moveToNext());
				}
				tagm.close();
			}
			item.close();
			break;
		case TAGITEMMAPPING:
			table = uri.getPathSegments().get(0);
			rowid = uri.getPathSegments().get(1);
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
		ContentValues values = new ContentValues(2);
		values.put("isDirty", 1);
		values.put("isDeleted", 1);
		int ret = mDB.getWritableDatabase().update(table, values,
				"tempId IS NULL AND ROWID=?", new String[] { rowid });
		// if we have tempId it means that we didn't send changed to server.
		// so we can delete row from local db.
		// in other case we just mark row as deleted it will be deleted in sync
		// operation.
		if (ret == 0) {
			ret = mDB.getWritableDatabase().delete(table, "ROWID=?",
					new String[] { rowid });
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return ret;
	}

	@Override
	public String getType(Uri uri) {
		switch (matcher.match(uri)) {
		case LISTS:
			return LISTS_MIME_TYPE;
		case LIST:
			return LIST_MIME_TYPE;
		case LIST_ITEMS:
		case ITEMS_FULL:
		case ITEMS:
			return ITEMS_MIME_TYPE;
		case ITEM:
			return ITEM_MIME_TYPE;
		case USERS:
			return USERS_MIME_TYPE;
		case USER:
			return USER_MIME_TYPE;
		case TAGS:
			return TAGS_MIME_TYPE;
		case TAG:
			return TAG_MIME_TYPE;
		case STATUSES:
			return STATUSES_MIME_TYPE;
		case STATUS:
			return STATUS_MIME_TYPE;
		case PRIORITIES:
			return PRIORITIES_MIME_TYPE;
		case PRIORITY:
			return PRIORITY_MIME_TYPE;
		case ITEM_TAGS:
			return TAGSNAME_MIME_TYPE;
		case TAGITEMMAPPING:
			return TAGITEMMAPPING_MIME_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String table = null;
		switch (matcher.match(uri)) {
		case LISTS:
		case ITEMS:
			table = uri.getPathSegments().get(0);
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
		String userID = null;
		Cursor c = mDB.getReadableDatabase().query(Database.User.NAME,
				new String[] { Database.User.C_ID }, null, null, null, null, null);
		if (c.moveToFirst()) {
			userID = c.getString(0);
			String guid = UUID.randomUUID().toString();
			values.put("UserID", userID);
			values.put("ID", guid);
			values.put("tempId", guid);
			values.put("isDirty", 1);
			values.put("isDeleted", 0);
			mDB.getWritableDatabase().insert(table, null, values);
		}
		c.close();
		getContext().getContentResolver().notifyChange(uri, null);
		return null;
	}

	private Database.OpenHelper mDB;

	@Override
	public boolean onCreate() {
		try {
			mDB = new Database.OpenHelper(getContext());
		} catch (Exception e) {
			Log.e(LTAG, e.getLocalizedMessage());
		}
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
		switch (matcher.match(uri)) {
		case LISTS:
		case ITEMS:
		case USERS:
		case TAGS:
		case STATUSES:
		case PRIORITIES:
			String table = uri.getPathSegments().get(0);
			builder.setTables(table);
			if (selection != null) {
				selection = "(" + selection + ") AND isDeleted=0";
			} else {
				selection = "isDeleted=0";
			}
			break;
		case LIST:
		case ITEM:
		case USER:
		case TAG:
		case STATUS:
		case PRIORITY:
			table = uri.getPathSegments().get(0);
			builder.setTables(table);
			selection = "ROWID=? AND isDeleted=0";
			selectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		case ITEMS_FULL:
			builder.setTables("Item I LEFT OUTER JOIN Priority P ON I.Priority = P.ID "
					+ "LEFT OUTER JOIN Status S ON I.Status = S.ID");
			selection = "I.ROWID=?";
			selectionArgs = new String[] { uri.getPathSegments().get(1) };
			break;
		case ITEM_TAGS:
			builder.setTables("TagItemMapping M INNER JOIN Tag T ON M.TagID = T.ID");
			selection = "M.ItemID=? AND M.isDeleted=0";
			selectionArgs = new String[] { uri.getPathSegments().get(1) };
			projection = new String[] { Database.Item.C_NAME + "M" };
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
		builder.setProjectionMap(map);

		Cursor cursor = builder.query(mDB.getReadableDatabase(), projection, selection,
				selectionArgs, null, null, sortOrder);

		if (cursor == null) {
			return null;
		}
		// else if (!cursor.moveToFirst()) {
		// cursor.close();
		// return null;
		// }
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		String table, rowid;
		switch (matcher.match(uri)) {
		case LIST:
		case ITEM:
			table = uri.getPathSegments().get(0);
			rowid = uri.getPathSegments().get(1);
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
		values.put("isDirty", 1);
		int ret = mDB.getWritableDatabase().update(table, values, "ROWID=?",
				new String[] { rowid });
		getContext().getContentResolver().notifyChange(
				Uri.withAppendedPath(CONTENT_URI, table), null);
		return ret;
	}

}
