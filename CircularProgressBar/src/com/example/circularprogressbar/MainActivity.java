package com.example.circularprogressbar;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

	ProgressBar progress;
	TextView textView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
	    textView=(TextView)findViewById(R.id.textview);
		
		progress= (ProgressBar) findViewById(R.id.ProgressBar01);
		progress.setProgress(0);
		progress.setMax(100);
		
		
		callAsynchronousTask();
		
	}



public void callAsynchronousTask() {
    final Handler handler = new Handler();
    Timer timer = new Timer();
    TimerTask doAsynchronousTask = new TimerTask() {       
        @Override
        public void run() {
            handler.post(new Runnable() {
                public void run() {       
                    try {
                      
                    	progress.incrementProgressBy(1);
                    	int stringProgress=progress.getProgress();
                		textView.setText(stringProgress+"");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                    }
                }
            });
        }
    };
    timer.schedule(doAsynchronousTask, 0, 10); 
}

}
