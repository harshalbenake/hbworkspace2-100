package com.example.controlviewheight;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		

	    ((SeekBar)findViewById(R.id.sbHeight)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

	        @Override
	        public void onStopTrackingTouch(SeekBar seekBar) {
	        }

	        @Override
	        public void onStartTrackingTouch(SeekBar seekBar) {
	        }

	        @Override
	        public void onProgressChanged(SeekBar seekBar, int progress,
	                boolean fromUser) {

	            if(progress > 0 && progress < 30)
	                progress = 40;
	            else if(progress > 30 && progress < 60)
	                progress = 60;
	            else if(progress > 60 && progress < 100)
	                progress = 80;

	            if(progress != 0)
	                setViewHeight(progress);                
	        }
	    });

	    ((LinearLayout)findViewById(R.id.llHeight)).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,60));        
	

	}

	private void setViewHeight(int progress)
	{
		((LinearLayout)findViewById(R.id.llHeight)).setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT,progress));
	}


}
