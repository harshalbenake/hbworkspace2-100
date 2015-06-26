package com.example.swiperefreshlayout;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * http://www.incredibleandros.com/swiperefershlayout-android-example/
 * @author harshalb
 *
 */
public class MainActivity extends Activity implements OnRefreshListener{

	 SwipeRefreshLayout swipeLayout; 
     ListView listView;
	@SuppressWarnings("rawtypes")
	ArrayAdapter adapter;
     ArrayList< String> arrayList;
     String [] array = new String[]{"AAA","BBB","CCC","EEE"};

	@SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        swipeLayout = (SwipeRefreshLayout)findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(this);
        swipeLayout.setColorScheme(android.R.color.holo_blue_bright, 
                android.R.color.holo_orange_dark, 
                android.R.color.white, 
                android.R.color.holo_green_dark);
        listView= (ListView)findViewById(R.id.listview);

        adapter = new ArrayAdapter(MainActivity.this,android.R.layout.simple_list_item_1, initialArrayData());
        listView.setAdapter(adapter);       
    }
	
	@SuppressWarnings({"rawtypes", "unchecked"})
	private ArrayList initialArrayData(){
		   if(arrayList==null)
	    		arrayList =  new ArrayList();
	    	for (String items : array) {
	    		arrayList.add(items);
	    	}
    	return arrayList;
    }

	@SuppressWarnings({"rawtypes", "unchecked"})
	private ArrayList appendData(){
    	if(arrayList==null)
    		arrayList =  new ArrayList();
    		arrayList.add("HB");
    	return arrayList;
    }
	@Override
	public void onRefresh() {
		  // TODO Auto-generated method stub
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                appendData();
                adapter.notifyDataSetChanged();
                swipeLayout.setRefreshing(false);
            }
        }, 5000);
		
	}

}

