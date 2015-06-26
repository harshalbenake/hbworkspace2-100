package selvin.ListSyncSample;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentValues;
import android.content.Context;
import android.content.SyncStats;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

public class Database {

	final OpenHelper openHelper;
	static final String DATABASE_NAME = "listdb";
	static final int DATABASE_VERSION = 2;

	public static enum ColumnsTypes {
		integer, varchar, guid, datetime, numeric
	};

	static final String COLLATE = "COLLATE LOCALIZED";
	static final String EMPTY = "";
	public static final String IDAS_ID = "IDAS_ID";
	// Table definition
	public static interface Settings {
		// Table name
		public static final String NAME = "Settings";
		// Columns
		public static final String C_NAME = "Name";
		public static final String C_VALUE = "Value";
		// all columns
		public final static String[] C = new String[] { C_NAME, C_VALUE };
		// column type
		public final static ColumnsTypes[] CT = new ColumnsTypes[] {
				ColumnsTypes.varchar, ColumnsTypes.varchar };
		// column special
		public final static String[] CS = new String[] { EMPTY, COLLATE };
		// nullable
		public final static boolean[] CN = new boolean[] { false, false };
		// primary key
		public final static String[] PK = new String[] { C_NAME };
	}

	public static interface Status {
		public static final String NAME = "Status";
		public static final String C_ID = "ID";
		public static final String C_NAME = "Name";
		public final static String[] C = new String[] { C_ID, C_NAME };
		public final static ColumnsTypes[] CT = new ColumnsTypes[] {
				ColumnsTypes.integer, ColumnsTypes.varchar };
		public final static boolean[] CN = new boolean[] { false, false };
		public final static String[] CS = new String[] { EMPTY, COLLATE };
		public final static String[] PK = new String[] { C_ID };
	}

	public static interface Tag {
		public static final String NAME = "Tag";
		public static final String C_ID = "ID";
		public static final String C_NAME = "Name";
		public final static String[] C = new String[] { C_ID, C_NAME };
		public final static ColumnsTypes[] CT = new ColumnsTypes[] {
				ColumnsTypes.integer, ColumnsTypes.varchar };
		public final static boolean[] CN = new boolean[] { false, false };
		public final static String[] CS = new String[] { EMPTY, COLLATE };
		public final static String[] PK = new String[] { C_ID };
	}

	public static interface Priority {
		public static final String NAME = "Priority";
		public static final String C_ID = "ID";
		public static final String C_NAME = "Name";
		public final static String[] C = new String[] { C_ID, C_NAME };
		public final static ColumnsTypes[] CT = new ColumnsTypes[] {
				ColumnsTypes.integer, ColumnsTypes.varchar };
		public final static boolean[] CN = new boolean[] { false, false };
		public final static String[] CS = new String[] { EMPTY, COLLATE };
		public final static String[] PK = new String[] { C_ID };

	}

	public static interface User {
		public static final String NAME = "User";
		public static final String C_ID = "ID";
		public static final String C_NAME = "Name";
		public final static String[] C = new String[] { C_ID, C_NAME };
		public final static ColumnsTypes[] CT = new ColumnsTypes[] { ColumnsTypes.guid,
				ColumnsTypes.varchar };
		public final static boolean[] CN = new boolean[] { false, false };
		public final static String[] CS = new String[] { EMPTY, COLLATE };
		public final static String[] PK = new String[] { C_ID };
	}

	public static interface List {
		public static final String NAME = "List";
		public static final String C_ID = "ID";
		public static final String C_NAME = "Name";
		public static final String C_DESCRIPTION = "Description";
		public static final String C_USERID = "UserID";
		public static final String C_CREATEDATE = "CreatedDate";
		public final static String[] C = new String[] { C_ID, C_NAME, C_DESCRIPTION,
				C_USERID, C_CREATEDATE };
		public final static ColumnsTypes[] CT = new ColumnsTypes[] { ColumnsTypes.guid,
				ColumnsTypes.varchar, ColumnsTypes.varchar, ColumnsTypes.guid,
				ColumnsTypes.datetime };
		public final static boolean[] CN = new boolean[] { false, false, true, false,
				false };
		public final static String[] CS = new String[] { EMPTY, COLLATE, COLLATE, EMPTY,
				EMPTY /* DEFAULT (CURRENT_TIMESTAMP) */};
		public final static String[] PK = new String[] { C_ID };
	}

	public static interface Item {
		public static final String NAME = "Item";
		public static final String C_ID = "ID";
		public static final String C_LISTID = "ListID";
		public static final String C_USERID = "UserID";
		public static final String C_NAME = "Name";
		public static final String C_DESCRIPTION = "Description";
		public static final String C_PRIORITY = "Priority";
		public static final String C_STATUS = "Status";
		public static final String C_STARTDATE = "StartDate";
		public static final String C_ENDDATE = "EndDate";
		public final static String[] C = new String[] { C_ID, C_LISTID, C_USERID, C_NAME,
				C_DESCRIPTION, C_PRIORITY, C_STATUS, C_STARTDATE, C_ENDDATE };
		public static final ColumnsTypes[] CT = new ColumnsTypes[] { ColumnsTypes.guid,
				ColumnsTypes.guid, ColumnsTypes.guid, ColumnsTypes.varchar,
				ColumnsTypes.varchar, ColumnsTypes.integer, ColumnsTypes.integer,
				ColumnsTypes.datetime, ColumnsTypes.datetime };
		public final static boolean[] CN = new boolean[] { false, false, false, false,
				true, true, true, true, true };
		public final static String[] CS = new String[] { EMPTY, EMPTY, EMPTY, COLLATE,
				COLLATE, EMPTY, EMPTY, EMPTY, EMPTY };
		public final static String[] PK = new String[] { C_ID };
	}

	public static interface TagItemMapping {
		public static final String NAME = "TagItemMapping";
		public static final String C_TAGID = "TagID";
		public static final String C_ITEMID = "ItemID";
		public static final String C_USERID = "UserID";
		public final static String[] C = new String[] { C_TAGID, C_ITEMID, C_USERID };
		public final static ColumnsTypes[] CT = new ColumnsTypes[] {
				ColumnsTypes.integer, ColumnsTypes.guid, ColumnsTypes.guid };
		public final static boolean[] CN = new boolean[] { false, false, false };
		public final static String[] CS = new String[] { EMPTY, EMPTY, EMPTY };
		public final static String[] PK = new String[] { C_TAGID, C_ITEMID, C_USERID };
	}

	public static class Tables {
		String[] columns = null;
		ColumnsTypes[] columnsTypes = null;
		String[] columnsSpecials = null;
		String name = null;
		boolean[] columnsNullable = null;
		String[] primaryKey = null;

		Tables(String name, String[] columns, ColumnsTypes[] columnsTypes,
				String[] columnsSpecials, boolean[] columnsNullable, String[] primaryKey) {
			this.name = name;
			this.columns = columns;
			this.columnsTypes = columnsTypes;
			this.columnsSpecials = columnsSpecials;
			this.columnsNullable = columnsNullable;
			this.primaryKey = primaryKey;
		}

		public boolean GetChanges(SQLiteDatabase db, final StringBuilder ret,
				boolean first, final ArrayList<String> notifyTables) {
			String[] cols = new String[columns.length + 3];
			int i = 0;
			for (; i < columns.length; i++)
				cols[i] = columns[i];
			cols[i] = "uri";
			cols[i + 1] = "tempId";
			cols[i + 2] = "isDeleted";
			Cursor c = db.query(name, cols, "isDirty=?", new String[] { "1" }, null,
					null, null);
			if (c.moveToFirst()) {
				if (!notifyTables.contains(name))
					notifyTables.add(name);
				do {
					if (!first) {
						ret.append(',');
					} else {
						first = false;
					}
					ret
							.append("{\"__metadata\":{\"isDirty\":true,\"type\":\"DefaultScope.");
					ret.append(name);
					ret.append('\"');

					String uri = c.getString(i);
					if (uri == null) {
						ret.append(",\"tempId\":\"");
						ret.append(c.getString(0));
						ret.append('\"');
					} else {
						ret.append(",\"uri\":\"");
						ret.append(uri);
						ret.append('\"');
						ContentValues vals = new ContentValues(1);
						vals.put("isDirty", 0);
						db.update(name, vals, "uri=?", new String[] { uri });
					}
					boolean isDeleted = c.getInt(i + 2) == 1;
					if (isDeleted) {
						ret.append(",\"isDeleted\":true}");
						// real delete
						db.delete(name, "uri=?", new String[] { uri });
					} else {
						ret.append('}');
						for (i = 0; i < columns.length; i++) {
							ret.append(',');
							ret.append('\"');
							ret.append(columns[i]);
							ret.append("\":");
							switch (columnsTypes[i]) {
							case integer:
								ret.append(c.getInt(i));
								break;
							case datetime:
								try {
									ret.append("\"/Date(");
									ret.append(sdf.parse(c.getString(i)).getTime());
									ret.append(")/\"");
								} catch (ParseException e) {
									Log.e("ListSync", e.getLocalizedMessage());
								}
								break;
							default:
								ret.append('\"');
								ret.append(c.getString(i));
								ret.append('\"');
								break;
							}

						}
					}
					ret.append('}');
				} while (c.moveToNext());
			}
			c.close();

			return first;
		}

		public void DeleteWithUri(String uri, SQLiteDatabase db) {
			db.delete(name, "uri=?", new String[] { uri });
		}

		final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		public void UpdateResultJSON(JSONObject obj, JSONObject metadata,
				SQLiteDatabase db) throws JSONException {
			ContentValues vals = new ContentValues(columns.length + 1);
			for (int i = 0; i < columns.length; i++) {
				String column = columns[i];
				switch (columnsTypes[i]) {
				case integer:
					vals.put(column, obj.getInt(column));
					break;
				case datetime:
					String date = obj.getString(column);
					date = sdf.format(new Date(Long.parseLong(date.substring(6, date
							.length() - 2))));
					vals.put(column, date);
					break;
				default:
					vals.put(column, obj.getString(column));
					break;
				}
			}
			vals.put("tempId", (String) null);
			vals.put("isDirty", 0);
			if (metadata.has("tempId")) {
				vals.put("uri", metadata.getString("uri"));
				db.update(name, vals, "tempId=?", new String[] { metadata
						.getString("tempId") });
			} else {
				db
						.update(name, vals, "uri=?", new String[] { metadata
								.getString("uri") });
			}
		}

		public void SyncJSON(JSONObject obj, JSONObject metadata, SQLiteDatabase db,
				final SyncStats stats) throws JSONException {
			ContentValues vals = new ContentValues(columns.length + 1);
			for (int i = 0; i < columns.length; i++) {
				String column = columns[i];
				switch (columnsTypes[i]) {
				case integer:
					vals.put(column, obj.getInt(column));
					break;
				case datetime:
					String date = obj.getString(column);
					date = sdf.format(new Date(Long.parseLong(date.substring(6, date
							.length() - 2))));
					vals.put(column, date);
					break;
				default:
					vals.put(column, obj.getString(column));
					break;
				}
			}
			String uri = metadata.getString("uri");
			vals.put("tempId", (String) null);
			if (db.update(name, vals, "uri=?", new String[] { uri }) == 0) {
				vals.put("uri", uri);
				db.insert(name, null, vals);
				stats.numInserts++;
			} else {
				stats.numUpdates++;
			}
		}

		public String DropStatment() {
			return "DROP TABLE IF EXISTS " + name;
		}

		public String CreateStatment() {
			StringBuilder sb = new StringBuilder("CREATE TABLE ");
			sb.append(name);
			sb.append(" ([");
			for (int i = 0; i < columns.length; i++) {
				sb.append(columns[i]);
				sb.append("] ");
				sb.append(columnsTypes[i].name());
				sb.append(' ');
				sb.append(columnsSpecials[i]);
				if (!columnsNullable[i])
					sb.append(" NOT NULL ");
				sb.append(", [");
			}
			sb.append("uri] varchar, [tempId] ");
			sb.append(columnsTypes[0].name());
			sb.append(", [isDeleted] INTEGER NOT NULL DEFAULT (0)"
					+ " , [isDirty] INTEGER NOT NULL DEFAULT (0), PRIMARY KEY (");
			sb.append(TextUtils.join(", ", primaryKey));
			sb.append("));");
			return sb.toString();
		}

		public final static Map<String, Tables> AllTables;
		static {
			HashMap<String, Tables> aMap = new HashMap<String, Tables>();
			aMap.put(Priority.NAME, new Tables(Priority.NAME, Priority.C, Priority.CT,
					Priority.CS, Priority.CN, Priority.PK));
			aMap.put(Tag.NAME,
					new Tables(Tag.NAME, Tag.C, Tag.CT, Tag.CS, Tag.CN, Tag.PK));
			aMap.put(Status.NAME, new Tables(Status.NAME, Status.C, Status.CT, Status.CS,
					Status.CN, Status.PK));
			aMap.put(User.NAME, new Tables(User.NAME, User.C, User.CT, User.CS, User.CN,
					User.PK));
			aMap.put(List.NAME, new Tables(List.NAME, List.C, List.CT, List.CS, List.CN,
					List.PK));
			aMap.put(Item.NAME, new Tables(Item.NAME, Item.C, Item.CT, Item.CS, Item.CN,
					Item.PK));
			aMap.put(TagItemMapping.NAME, new Tables(TagItemMapping.NAME,
					TagItemMapping.C, TagItemMapping.CT, TagItemMapping.CS,
					TagItemMapping.CN, TagItemMapping.PK));
			aMap.put(Settings.NAME, new Tables(Settings.NAME, Settings.C, Settings.CT,
					Settings.CS, Settings.CN, Settings.PK));
			AllTables = Collections.unmodifiableMap(aMap);
		}
	}

	public Database(Context context) throws Exception {
		openHelper = new OpenHelper(context);
	}

	public void Destroy() {
		openHelper.close();
	}

	public static class OpenHelper extends SQLiteOpenHelper {

		public OpenHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			try {
				for (Tables table : Tables.AllTables.values()) {
					String create = table.CreateStatment();
					db.execSQL(create);
				}
			} catch (Exception e) {
				Log.e("ListSync", e.toString());
			}
		}

		public OpenHelper Recreate() {
			onUpgrade(getWritableDatabase(), 1, 2);
			return this;
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			for (Tables table : Tables.AllTables.values()) {
				db.execSQL(table.DropStatment());
			}
			onCreate(db);
		}
	}
}
