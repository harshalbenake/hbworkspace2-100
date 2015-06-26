package com.buzzbox.demo.helloworld;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import com.buzzbox.mob.android.scheduler.SchedulerManager;
import com.buzzbox.mob.android.scheduler.analytics.AnalyticsManager;
import com.buzzbox.mob.android.scheduler.ui.SchedulerLogActivity;

public class HelloWorldActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.main);

		// 1. call BuzzBox Analytics
		int openAppStatus = AnalyticsManager.onOpenApp(this); 

		// 2. add the Task to the Scheduler
		if (openAppStatus==AnalyticsManager.OPEN_APP_FIRST_TIME) { 
		      // register the Task when the App in installed
		      SchedulerManager.getInstance().saveTask(this, 
		      		"*/1 * * * *",   // a cron string
		      		ReminderTask.class);
		      SchedulerManager.getInstance().restart(this, ReminderTask.class );
		} else if (openAppStatus==AnalyticsManager.OPEN_APP_UPGRADE){
		     // restart on upgrade
		    SchedulerManager.getInstance().restartAll(this, 0);    
		}
		
		
		// 3. set up UI buttons
        Button settingsButton = (Button) findViewById(R.id.settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				SchedulerManager.getInstance()
	        	.startConfigurationActivity(HelloWorldActivity.this, ReminderTask.class);
			}
		});
        
        Button log = (Button) findViewById(R.id.log);
        log.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(HelloWorldActivity.this, SchedulerLogActivity.class);
				startActivity(intent);
			}
		});   
        
        Button refresh = (Button) findViewById(R.id.notify);
        refresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SchedulerManager.getInstance().runNow(HelloWorldActivity.this, ReminderTask.class, 0);
			}
		}); 
        
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {        
        super.onActivityResult(requestCode, resultCode, data);        
        if (SchedulerManager.SCHEDULER_CONFIG_REQ_CODE == requestCode && data!=null) {
            SchedulerManager.getInstance()
            	.handleConfigurationResult(this, data);        
        }
    }
    
}
