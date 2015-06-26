package com.androidpeople.drawer;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SlidingDrawer;
import android.widget.SlidingDrawer.OnDrawerCloseListener;
import android.widget.SlidingDrawer.OnDrawerOpenListener;

public class slidingDrawerExample extends Activity {

	Button slideHandleButton;
	@SuppressWarnings("deprecation")
	SlidingDrawer slidingDrawer;
	 private ListView mainListView ;
	  private ArrayAdapter<String> listAdapter ;
	  
	  private ListView mainListViewTop ;
	  private ArrayAdapter<String> listAdapterTOP ;

	@SuppressWarnings("deprecation")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
		slideHandleButton = (Button) findViewById(R.id.slideHandleButton);
		slidingDrawer = (SlidingDrawer) findViewById(R.id.SlidingDrawer);
		
		 
	    // Find the ListView resource. 
	    mainListView = (ListView) findViewById( R.id.mainListView);

	    // Create and populate a List of planet names.
	    String[] planets = new String[] { "Mercury", "Venus", "Earth", "Mars",
	                                      "Jupiter", "Saturn", "Uranus", "Neptune"};  
	    ArrayList<String> planetList = new ArrayList<String>();
	    planetList.addAll( Arrays.asList(planets) );
	    
	    // Create ArrayAdapter using the planet list.
	    listAdapter = new ArrayAdapter<String>(this, R.layout.simplerow, planetList);
	    
	    // Add more planets. If you passed a String[] instead of a List<String> 
	    // into the ArrayAdapter constructor, you must not add more items. 
	    // Otherwise an exception will occur.
	    listAdapter.add( "Ceres" );
	    listAdapter.add( "Pluto" );
	    listAdapter.add( "Haumea" );
	    listAdapter.add( "Makemake" );
	    listAdapter.add( "Eris" );
	    
	    // Set the ArrayAdapter as the ListView's adapter.
	    mainListView.setAdapter( listAdapter );      
	    
	    
	    
	    // Find the ListView resource. 
	    mainListViewTop = (ListView) findViewById( R.id.mainListViewTop);

	    // Create and populate a List of planet names.
	    String[] planetsTop = new String[] { "Mercury", "Venus", "Earth", "Mars",
	                                      "Jupiter", "Saturn", "Uranus", "Neptune"};  
	    ArrayList<String> planetListTop = new ArrayList<String>();
	    planetListTop.addAll( Arrays.asList(planetsTop) );
	    
	    // Create ArrayAdapter using the planet list.
	    listAdapterTOP = new ArrayAdapter<String>(this, R.layout.simplerow, planetListTop);
	    
	    // Add more planets. If you passed a String[] instead of a List<String> 
	    // into the ArrayAdapter constructor, you must not add more items. 
	    // Otherwise an exception will occur.
	    listAdapterTOP.add( "CeresTop" );
	    listAdapterTOP.add( "PlutoTop" );
	    listAdapterTOP.add( "HaumeaTop" );
	    listAdapterTOP.add( "MakemakeTop" );
	    listAdapterTOP.add( "ErisTop" );
	    
	    // Set the ArrayAdapter as the ListView's adapter.
	    mainListViewTop.setAdapter( listAdapterTOP );      
	    
	    

		slidingDrawer.setOnDrawerOpenListener(new OnDrawerOpenListener() {

			@Override
			public void onDrawerOpened() {
				slideHandleButton.setBackgroundResource(R.drawable.openarrow);
			}
		});

		slidingDrawer.setOnDrawerCloseListener(new OnDrawerCloseListener() {

			@Override
			public void onDrawerClosed() {
				slideHandleButton.setBackgroundResource(R.drawable.closearrow);
			}
		});

	}

}