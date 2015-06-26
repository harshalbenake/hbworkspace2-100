package com.example.databasesqliteadvancequerys.DBUtility;


/**
 * The POJO class.
 *
 *
 */
public class DatabasePojo {
	public int rollno;
	public String name;

	
	public static String TABLENAME="TableName";

	public static String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS "+TABLENAME+" (" 
	+ "_id INTEGER PRIMARY KEY, " //Don't remove this column.
	+ "rollno integer default 1, "
	+ "name VARCHAR default 1 "
	+")";
	
	
}

