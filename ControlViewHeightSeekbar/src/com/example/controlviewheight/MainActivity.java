package com.example.controlviewheight;


import android.app.Activity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {
	ListView list1,list2;
	 int height1,height2;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		DisplayMetrics displaymetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
		int height = displaymetrics.heightPixels;
		int width = displaymetrics.widthPixels;
		
		
		  list1= (ListView) findViewById(R.id.list1);
		  String[] items1 = { "Alfa1", "Alfa2", "Alfa3", "Alfa4", "Alfa5","Alfa1", "Alfa2", "Alfa3", "Alfa4", "Alfa5","Alfa1", "Alfa2", "Alfa3", "Alfa4", "Alfa5","Alfa1", "Alfa2", "Alfa3", "Alfa4", "Alfa5"  };
		  String[] items2 = { "Beta1", "Beta2", "Beta3", "Beta4", "Beta5","Beta1", "Beta2", "Beta3", "Beta4", "Beta5","Beta1", "Beta2", "Beta3", "Beta4", "Beta5","Beta1", "Beta2", "Beta3", "Beta4", "Beta5"  };

		   ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this,
	                android.R.layout.simple_list_item_1, items1);
	        list1.setAdapter(adapter1);
	        
	        list1.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		   height1 = list1.getMeasuredHeight();
		 
	        
	        list2= (ListView) findViewById(R.id.list2);
			   ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this,
		                android.R.layout.simple_list_item_1, items2);
		        list2.setAdapter(adapter2);
		        
		 
			    list2.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
			   height2 = list2.getMeasuredHeight();
		        
			   VerticalSeekBar verticalSeekBar=(VerticalSeekBar)findViewById(R.id.seebar1_listview1);
		        verticalSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

	        @Override
	        public void onStopTrackingTouch(SeekBar seekBar) {
	        }

	        @Override
	        public void onStartTrackingTouch(SeekBar seekBar) {
	        }

	        @Override
	        public void onProgressChanged(SeekBar seekBar, int progress,
	                boolean fromUser) {

//	            if(progress > 0 && progress < 20)
//	                progress = 20;
//	            else if(progress > 20 && progress < 40)
//	                progress = 40;
//	            else if(progress > 40 && progress < 60)
//	                progress = 60;
//	            else if(progress > 60 && progress < 80)
//	                progress = 80;
//	            else if(progress > 80 && progress < 100)
//	                progress = 100;

	            if(progress != 0)
	                setViewHeight1(height1-progress,progress);      
	            	setViewHeight2(height2+progress,progress);
	        }
	    });
//
//	    list1.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,height1));     
//	    list1.requestLayout();
//
//	    list2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,height2));
//	    list2.requestLayout();
//	    
//	   

	}

	private void setViewHeight1(int progress,int defaultprogress)
	{
		list1.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,progress,100-defaultprogress));
		list1.requestLayout();
		
	}

	private void setViewHeight2(int progress,int defaultprogress)
	{
		list2.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,progress,100+defaultprogress));
		list2.requestLayout();
	}


}
