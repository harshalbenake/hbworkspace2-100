package com.example.databasesqliteadvancequerys.DBUtility;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * This is a manager class used to manage database related information.
 * 
 */
public class DBManager {
	Context mContext;
	
	public DBManager(Context mContext){
		this.mContext=mContext;		
	}
	
	/**
	 * This method is used to add data info into DataBase.
	 * @param context
	 * @param mUserSettings
	 */
	public void addData(Context context,DatabasePojo databasePojo){
		try{
			ApplicationClass applicationClass=(ApplicationClass)context.getApplicationContext();
			SQLiteDatabase sqLiteDatabase=applicationClass.getWritableDatabase();
			
			ContentValues mContentValues=new ContentValues();
			mContentValues.put("rollno", databasePojo.rollno);
			mContentValues.put("name", databasePojo.name);
			
			//sqLiteDatabase.delete(DatabasePojo.TABLENAME, null, null);
			sqLiteDatabase.insert(DatabasePojo.TABLENAME, null, mContentValues);

		}catch(Exception e){  
			e.printStackTrace();    
		}
	}
	
	/**
	 * This method is used to get data info from DataBase.
	 * @param context
	 * @return
	 */
	public DatabasePojo getData(Context context){
		DatabasePojo databasePojo=null;
		try{
			ApplicationClass applicationClass=(ApplicationClass)context.getApplicationContext();
			SQLiteDatabase sqLiteDatabase=applicationClass.getReadableDatabase();
			Cursor cursor=sqLiteDatabase.query(DatabasePojo.TABLENAME, null, null, null, null, null, null);

			if(cursor.moveToFirst()){
				databasePojo=new DatabasePojo();
				databasePojo.rollno=cursor.getInt(cursor.getColumnIndex("rollno"));
				databasePojo.name=cursor.getString(cursor.getColumnIndex("name"));
			}
			cursor.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return databasePojo;
	}
	
	/**
	 * This method is used to update data of name.
	 * @param context
	 * @param toggleButtonStatus
	 */
	public void updateData(Context context,String name,int whereRollno){
		try{
			ApplicationClass applicationClass=(ApplicationClass)context.getApplicationContext();
			SQLiteDatabase sqLiteDatabase=applicationClass.getWritableDatabase();
			ContentValues cVal=new ContentValues();
			cVal.put("name", name);
			
			//update name if rollno="3"
			sqLiteDatabase.update(DatabasePojo.TABLENAME, cVal, "rollno="+whereRollno,null);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * This method is used to check whether the table is empty or not.
	 * @param context
	 * @return
	 */
	public boolean tableStatus(Context context) {
		// TODO Auto-generated method stub
		SQLiteDatabase sqLiteDatabase;
		ApplicationClass applicationClass=(ApplicationClass)context.getApplicationContext();
		sqLiteDatabase=applicationClass.getReadableDatabase();
		Cursor cursor = sqLiteDatabase.rawQuery("select * from "+DatabasePojo.TABLENAME, null);
		if(cursor != null && cursor.getCount()>0){
			cursor.moveToFirst();
			//do your action
			//Fetch your data
			cursor.close();
			//System.out.println("Table has userSettings");
			return true;
		}
		else {
			//System.out.println("Table dont have userSettings");
			cursor.close();
			return false;
		}  		
	}
	
	 public List<String> getSpecificColumnData(Context context){
	        List<String> list = new ArrayList<String>();

	        // Select All Query
	        String selectQuery = " SELECT * FROM "+DatabasePojo.TABLENAME;

	        ApplicationClass applicationClass=(ApplicationClass)context.getApplicationContext();
			SQLiteDatabase sqLiteDatabase = applicationClass.getReadableDatabase();
	        Cursor cursor = sqLiteDatabase.rawQuery(selectQuery, null);

	        // looping through all rows and adding to list
	        if (cursor.moveToFirst()) {
	            do {
	            	list.add(cursor.getString(2));
	            } while (cursor.moveToNext());
	        }

	        // closing connection
	        cursor.close();
	        // returning lables
	        return list;
	    }
}
