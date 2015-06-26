package recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.widget.Toast;

/**
 *This clss is used for sms Broadcast Receiver.
 * Created by <b>Harshal Benake</b>
 */
public class SMSReceiver extends BroadcastReceiver{
	String TAG="SMSReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		String msg_from = null,msgBody = null;
		if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
			Bundle bundle = intent.getExtras();           //---get the SMS message passed in---
			SmsMessage[] msgs = null;
			if (bundle != null){
				//---retrieve the SMS message received---
				try{
					Object[] pdus = (Object[]) bundle.get("pdus");
					msgs = new SmsMessage[pdus.length];
					for(int i=0; i<msgs.length; i++){
						msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i]);
						msg_from = msgs[i].getOriginatingAddress();
						msgBody = msgs[i].getMessageBody();
                        Toast.makeText(context,"SMSReceiver msg_from "+msg_from+" msgBody "+msgBody,Toast.LENGTH_SHORT).show();
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
	}


}
