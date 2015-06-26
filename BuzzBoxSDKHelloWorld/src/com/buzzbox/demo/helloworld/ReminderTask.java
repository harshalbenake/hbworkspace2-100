package com.buzzbox.demo.helloworld;

import android.content.ContextWrapper;

import com.buzzbox.mob.android.scheduler.NotificationMessage;
import com.buzzbox.mob.android.scheduler.Task;
import com.buzzbox.mob.android.scheduler.TaskResult;

/**
 * Recurring Task that implements your business logic.
 * The BuzzBox SDK Scheduler will take care of running the doWork method according to
 * the scheduling.
 * 
 */
public class ReminderTask implements Task {

	@Override
    public String getTitle() {                        
        return "Reminder";
    }
    
    @Override
    public String getId() {                        
        return "reminder"; // give it an ID
    }
    
    @Override
    public TaskResult doWork(ContextWrapper ctx) {
        TaskResult res = new TaskResult();
       
        // TODO implement your business logic here
        // i.e. query the DB, connect to a web service using HttpUtils, etc..
        
        NotificationMessage notification = new NotificationMessage(
        		"HB Demo",
        		"Don't forget to open HB Demo App");
        notification.notificationIconResource = R.drawable.icon_notification_cards_clubs;
        notification.setNotificationClickIntentClass(HelloWorldActivity.class);
        
        res.addMessage( notification );    
        
        return res;
    }
	
}
