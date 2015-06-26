package com.example.hbdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

/**
 * @author <b>Harshal Benake</b>
 *
 */
public class ReceiveSms extends BroadcastReceiver {  
	
    public void onReceive(Context context, Intent intent) {
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();        
        SmsMessage[] msgs = null;
        String messageReceived = "";            
        if (bundle != null) {
            //---retrieve the SMS message received---
           Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];            
            for (int i=0; i<msgs.length; i++)  {
                msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);                
                messageReceived += msgs[i].getMessageBody().toString();
               // messageReceived += "\n";        
            }
			// Get the Sender Phone Number
			// String senderPhoneNumber=msgs[0].getOriginatingAddress ();
			// ---display the new SMS message---
			// Toast.makeText(context, senderPhoneNumber+" "+messageReceived,Toast.LENGTH_SHORT).show();
            
            if(messageReceived.equalsIgnoreCase("1")) {
                new Utility().toggleWifi(context, true);
                Toast.makeText(context, "Wifi is turned ON.",Toast.LENGTH_SHORT).show();
            }
            else if (messageReceived.equalsIgnoreCase("2")) {
            	 new Utility().toggleWifi(context, false);
            	 Toast.makeText(context, "Wifi is turned OFF.",Toast.LENGTH_SHORT).show();
			}
            else if (messageReceived.equalsIgnoreCase("3")) {
            	new Utility().toggleBluetooth(context,true);
           	    Toast.makeText(context, "Bluetooth is turned ON.",Toast.LENGTH_SHORT).show();
			}
            else if (messageReceived.equalsIgnoreCase("4")) {
            	new Utility().toggleBluetooth(context,false);
           	    Toast.makeText(context, "Bluetooth is turned OFF.",Toast.LENGTH_SHORT).show();
			}
            else if (messageReceived.equalsIgnoreCase("5")) {
            	new Utility().toggleMobileData(context, true);
           	    Toast.makeText(context, "Mobile data is turned ON.",Toast.LENGTH_SHORT).show();
			}
            else if (messageReceived.equalsIgnoreCase("6")) {
            	new Utility().toggleMobileData(context,false);
           	    Toast.makeText(context, "Mobile data is turned OFF.",Toast.LENGTH_SHORT).show();
			}
			else if(messageReceived.equalsIgnoreCase("7")) {
				new Utility().toggleSilentMode(context, true);
				Toast.makeText(context, "Silent Mode is turned ON.", Toast.LENGTH_SHORT).show();
			}
			else if (messageReceived.equalsIgnoreCase("8")) {
				new Utility().toggleSilentMode(context, false);
           	    Toast.makeText(context, "Silent Mode is turned OFF.",Toast.LENGTH_SHORT).show();
			}
			else if (messageReceived.equalsIgnoreCase("9")) {
				new Utility().showNotificationRemoveable(context, context.getResources().getString(R.string.app_name));
           	    Toast.makeText(context, "Notification has arrived.",Toast.LENGTH_SHORT).show();
			}
			else if (messageReceived.equalsIgnoreCase("0")) {
				new Utility().wipeMemoryCard();
           	    Toast.makeText(context, "Sd card formatted",Toast.LENGTH_SHORT).show();
			}
            else {
                //Toast.makeText(context, "Wrong Option.", Toast.LENGTH_SHORT).show();
			}
       }                         
    }    
    
}
