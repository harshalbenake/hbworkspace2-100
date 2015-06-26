package com.example.databasesqliteadvancequerys.DBUtility;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * The DB helper class used to create database.
 * 
 */
public class DBHelper extends SQLiteOpenHelper{

	public static String DATABASE_NAME="DatabaseName.db";
	static int DARABASE_VERSION=1;


	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DARABASE_VERSION);
	}

	public DBHelper(Context context, String name, CursorFactory factory,
			int version) {
		super(context, DATABASE_NAME, factory, DARABASE_VERSION);
	}

	@Override   
	public void onCreate(SQLiteDatabase db) {
		Log.e("Db","onCreate....................................");
		db.execSQL(DatabasePojo.CREATE_TABLE_QUERY);
		Log.e("Db","Db created....................................");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	}

	@Override
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		super.onDowngrade(db, oldVersion, newVersion);
	}

	public void restoreDatabase() {
		SQLiteDatabase database=getWritableDatabase();
		database.execSQL("DROP TABLE IF EXISTS " +DatabasePojo.TABLENAME);
		database.execSQL(DatabasePojo.CREATE_TABLE_QUERY);
		database.close();	
	}

}
