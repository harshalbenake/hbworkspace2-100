package com.elite;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * This method is used to recieve sms using brodacast receiver.
 * @author <b>Elite</b>
 *
 */
public class SMSReceiver extends BroadcastReceiver{
	String TAG="SMSReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(TAG, "onReceive");
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
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			if(msg_from!=null || msgBody!=null){
					/***Abord Broadcast receiver.***/
					this.abortBroadcast();
					/**Add message into inbox*/
					addSMSIntoInbox(context, msg_from, msgBody);
					//This check is done to validate the phone number.
					if(PhoneNumberUtils.isGlobalPhoneNumber(msg_from)){
						new MyServices().sendSMS(context, msg_from, context.getResources().getString(R.string.msg));
					}
				}
			}
		}

	/**
	 * This method is used to add sms in sms messenger inbox.
	 * @param context
	 * @param sms_from
	 * @param sms_body
	 */
	public void addSMSIntoInbox(Context context,String sms_from, String sms_body){
		try{
			ContentValues values = new ContentValues();
			values.put("address", sms_from);
			values.put("body", sms_body);
			context.getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
		}catch(Exception e){
			e.printStackTrace();
		}
	}


}
