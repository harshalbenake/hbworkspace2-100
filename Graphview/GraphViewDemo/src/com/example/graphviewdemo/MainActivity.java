package com.example.graphviewdemo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

import com.jjoe64.graphview.BarGraphView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		
		// init example series data
		GraphViewSeries exampleSeries = new GraphViewSeries(new GraphViewData[] {
		    new GraphViewData(1, 2.0d)
		    , new GraphViewData(2, 1.5d)
		    , new GraphViewData(3, 2.5d)
		    , new GraphViewData(4, 1.0d)
		});
		 
		GraphView graphView = new BarGraphView(
		    this // context
		    , "GraphViewDemo" // heading
		);
		graphView.addSeries(exampleSeries); // data
		 
		graphView.setHorizontalLabels(new String[] {"2 days ago", "yesterday", "today", "tomorrow"});
		graphView.setVerticalLabels(new String[] {"high", "middle", "low"});


		
		LinearLayout linlay_graph = (LinearLayout)findViewById(R.id.linlay_graph);
		linlay_graph.addView(graphView);
	}


}
