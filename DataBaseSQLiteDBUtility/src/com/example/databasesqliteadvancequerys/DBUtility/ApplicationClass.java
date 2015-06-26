package com.example.databasesqliteadvancequerys.DBUtility;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.example.databasesqliteadvancequerys.DBUtility.DBHelper;

/**
 * This is application class is used to declare global variable at application level.
 * 
 */
public class ApplicationClass extends Application{
	
	private SQLiteDatabase mSqLiteDatabaseWritable;
	
	@Override
	public void onCreate() {
		DBHelper dbHelper=new DBHelper(getApplicationContext());
		mSqLiteDatabaseWritable=dbHelper.getWritableDatabase();
		super.onCreate();
	}
	
	
	/**
	 * This method is used to get database object.
	 * @return SQLiteDatabase
	 */
	public SQLiteDatabase getReadableDatabase() {
		if(mSqLiteDatabaseWritable==null || mSqLiteDatabaseWritable.isOpen()==false){
			DBHelper dbHelper=new DBHelper(getApplicationContext());
			mSqLiteDatabaseWritable=dbHelper.getWritableDatabase();
		}
			
		return mSqLiteDatabaseWritable;
	}
	
	/**
	 * This method is used to get database object.
	 * @return SQLiteDatabase
	 */
	public SQLiteDatabase getWritableDatabase() {
		if(mSqLiteDatabaseWritable==null|| mSqLiteDatabaseWritable.isOpen()==false){
			DBHelper dbHelper=new DBHelper(getApplicationContext());
			mSqLiteDatabaseWritable=dbHelper.getWritableDatabase();
		}
		return mSqLiteDatabaseWritable;
	}
	
	/**
	 * This method is used to close database object.
	 */
	public void  closeDB(){
		if(mSqLiteDatabaseWritable!=null)
			mSqLiteDatabaseWritable.close();
	}
	
}
