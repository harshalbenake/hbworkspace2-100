package com.blundell.test;

import static com.blundell.test.C.*;

import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.util.Log;

import com.blundell.test.C;
import com.blundell.test.BillingSecurity.VerifiedPurchase;

public class BillingReceiver extends BroadcastReceiver {

	private static final String TAG = "BillingService";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i(TAG, "Received action: " + action);
        if (ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
            String signedData = intent.getStringExtra(INAPP_SIGNED_DATA);
            String signature = intent.getStringExtra(INAPP_SIGNATURE);
            purchaseStateChanged(context, signedData, signature);
        } else if (ACTION_NOTIFY.equals(action)) {
            String notifyId = intent.getStringExtra(NOTIFICATION_ID);
            notify(context, notifyId);
        } else if (ACTION_RESPONSE_CODE.equals(action)) {
            long requestId = intent.getLongExtra(INAPP_REQUEST_ID, -1);
            int responseCodeIndex = intent.getIntExtra(INAPP_RESPONSE_CODE, C.ResponseCode.RESULT_ERROR.ordinal());
            checkResponseCode(context, requestId, responseCodeIndex);
        } else {
           Log.e(TAG, "unexpected action: " + action);
        }
	}


	private void purchaseStateChanged(Context context, String signedData, String signature) {
		Log.i(TAG, "purchaseStateChanged got signedData: " + signedData);
		Log.i(TAG, "purchaseStateChanged got signature: " + signature);
		BillingHelper.verifyPurchase(signedData, signature);
	}
	
	private void notify(Context context, String notifyId) {
		Log.i(TAG, "notify got id: " + notifyId);
		String[] notifyIds = {notifyId};
		BillingHelper.getPurchaseInformation(notifyIds);
	}
	
	private void checkResponseCode(Context context, long requestId, int responseCodeIndex) {
		Log.i(TAG, "checkResponseCode got requestId: " + requestId);
		Log.i(TAG, "checkResponseCode got responseCode: " + C.ResponseCode.valueOf(responseCodeIndex));
	}
}