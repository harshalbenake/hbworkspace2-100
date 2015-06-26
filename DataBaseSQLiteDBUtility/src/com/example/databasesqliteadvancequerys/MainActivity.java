package com.example.databasesqliteadvancequerys;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.example.databasesqliteadvancequerys.DBUtility.DBManager;
import com.example.databasesqliteadvancequerys.DBUtility.DatabasePojo;

public class MainActivity extends Activity {

	private DBManager dbManager;
	private DatabasePojo databasePojo;

	private ListView mainListView ;
	private ArrayAdapter<String> listAdapter ;
	  
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initializeDataInDB();
		Button btn_add=(Button)findViewById(R.id.btn_add);
		Button btn_update=(Button)findViewById(R.id.btn_update);

		btn_add.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				addDataValue(getApplicationContext());
			}
		});
		
		btn_update.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				updateDataValue(getApplicationContext());
			}
		});
		
		/**Select * from TableName rawQuery to show values in ListView**/
		// Find the ListView resource. 
	    mainListView=(ListView)findViewById(R.id.listView1);
	    // Create and populate a List by database query.
	   
		List<String> columnNames=dbManager.getSpecificColumnData(getApplicationContext());
		
		ArrayList<String> arrayList = new ArrayList<String>();
		arrayList.addAll(columnNames);
		// Create ArrayAdapter using the list.
		listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, arrayList);
		// Set the ArrayAdapter as the ListView's adapter.
		mainListView.setAdapter(listAdapter);      
	}

	/**
	 * Initialize data in database table.
	 */
	private void initializeDataInDB(){
		try {
			dbManager = new DBManager(getApplicationContext());
			databasePojo = new DatabasePojo();
			databasePojo.rollno=1;
			databasePojo.name="InitialValue";
			System.out.println("initializeDataInDB addData"+databasePojo.rollno+databasePojo.name);
			dbManager.addData(getApplicationContext(), databasePojo);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	
	/**
	 * Add values in database table.
	 * @param context
	 */
	private void addDataValue(Context context) {
		EditText et_rollno=(EditText)findViewById(R.id.et_rollno);
		EditText et_name=(EditText)findViewById(R.id.et_name);

		int rollno = Integer.parseInt(et_rollno.getText().toString());
		String name = et_name.getText().toString();
		
		if(databasePojo.name!=null && !databasePojo.name.equalsIgnoreCase("")
		   && databasePojo.name!=null && !databasePojo.name.equalsIgnoreCase("")){
				databasePojo.rollno=rollno;
				databasePojo.name=name;
				dbManager.addData(context, databasePojo);
				System.out.println("addOrUpdateTable addData"+databasePojo.rollno+databasePojo.name);
			}
		}	
	
	/**
	 * Update values in database table.
	 * @param context
	 */
	private void updateDataValue(Context context) {
		EditText et_rollno=(EditText)findViewById(R.id.et_rollno);
		EditText et_name=(EditText)findViewById(R.id.et_name);

		int rollno = Integer.parseInt(et_rollno.getText().toString());
		String name = et_name.getText().toString();
		
		if(databasePojo.name!=null && !databasePojo.name.equalsIgnoreCase("")){
			databasePojo.name=name;
			dbManager.updateData(context,databasePojo.name,rollno);
				System.out.println("addOrUpdateTable updateData"+databasePojo.rollno+databasePojo.name);
		}
	}	
	
	/**
	 * Add or update values in database table.
	 * @param context
	 */
	@SuppressWarnings("unused")
	private void addOrUpdateValue(Context context) {
		EditText et_name=(EditText)findViewById(R.id.et_name);
		String name = et_name.getText().toString();
		
		if(databasePojo.name!=null && !databasePojo.name.equalsIgnoreCase("")){
			databasePojo.name=name;
		}
		
		boolean tableStatus = dbManager.tableStatus(context);
		if(databasePojo!=null){
			if(tableStatus==false){
				//table is empty...
//				databasePojo.rollno=0;
//				databasePojo.name="TableEmptyDefaultValue";
//				dbManager.addData(context, databasePojo);
			}
			else{
				//table is not empty...
//				dbManager.updateData(context,databasePojo.name);
			}
		}
	}
		
}
