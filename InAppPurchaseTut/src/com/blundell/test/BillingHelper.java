package com.blundell.test;

import java.util.ArrayList;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;

import com.android.vending.billing.IMarketBillingService;
import com.blundell.test.BillingSecurity.VerifiedPurchase;
import com.blundell.test.C.ResponseCode;

public class BillingHelper {

	private static final String TAG = "BillingService";
	
	private static IMarketBillingService mService;
	private static Context mContext;
	private static Handler mCompletedHandler;
	
	protected static VerifiedPurchase latestPurchase;
	
	protected static void instantiateHelper(Context context, IMarketBillingService service) {
		mService = service;
		mContext = context;
	}

	protected static void setCompletedHandler(Handler handler){
		mCompletedHandler = handler;
	}
	
	protected static boolean isBillingSupported() {
		if (amIDead()) {
			return false;
		}
		Bundle request = makeRequestBundle("CHECK_BILLING_SUPPORTED");
		if (mService != null) {
			try {
				Bundle response = mService.sendBillingRequest(request);
				ResponseCode code = ResponseCode.valueOf((Integer) response.get("RESPONSE_CODE"));
				Log.i(TAG, "isBillingSupported response was: " + code.toString());
				if (ResponseCode.RESULT_OK.equals(code)) {
					return true;
				} else {
					return false;
				}
			} catch (RemoteException e) {
				Log.e(TAG, "isBillingSupported response was: RemoteException", e);
				return false;
			}
		} else {
			Log.i(TAG, "isBillingSupported response was: BillingService.mService = null");
			return false;
		}
	}
	
	/**
	 * A REQUEST_PURCHASE request also triggers two asynchronous responses (broadcast intents). 
	 * First, the Android Market application sends a RESPONSE_CODE broadcast intent, which provides error information about the request. (which I ignore)
	 * Next, if the request was successful, the Android Market application sends an IN_APP_NOTIFY broadcast intent. 
	 * This message contains a notification ID, which you can use to retrieve the transaction details for the REQUEST_PURCHASE
	 * @param activityContext
	 * @param itemId
	 */
	protected static void requestPurchase(Context activityContext, String itemId){
		if (amIDead()) {
			return;
		}
		Log.i(TAG, "requestPurchase()");
		Bundle request = makeRequestBundle("REQUEST_PURCHASE");
		request.putString("ITEM_ID", itemId);
		try {
			Bundle response = mService.sendBillingRequest(request);
			
			//The RESPONSE_CODE key provides you with the status of the request
			Integer responseCodeIndex 	= (Integer) response.get("RESPONSE_CODE");
			//The PURCHASE_INTENT key provides you with a PendingIntent, which you can use to launch the checkout UI
			PendingIntent pendingIntent = (PendingIntent) response.get("PURCHASE_INTENT");
			//The REQUEST_ID key provides you with a unique request identifier for the request
			Long requestIndentifier 	= (Long) response.get("REQUEST_ID");
			Log.i(TAG, "current request is:" + requestIndentifier);
			C.ResponseCode responseCode = C.ResponseCode.valueOf(responseCodeIndex);
			Log.i(TAG, "REQUEST_PURCHASE Sync Response code: "+responseCode.toString());
			
			startBuyPageActivity(pendingIntent, new Intent(), activityContext);
		} catch (RemoteException e) {
			Log.e(TAG, "Failed, internet error maybe", e);
			Log.e(TAG, "Billing supported: "+isBillingSupported());
		}
	}
	
	/**
	 * A GET_PURCHASE_INFORMATION request also triggers two asynchronous responses (broadcast intents). 
	 * First, the Android Market application sends a RESPONSE_CODE broadcast intent, which provides status and error information about the request.  (which I ignore)
	 * Next, if the request was successful, the Android Market application sends a PURCHASE_STATE_CHANGED broadcast intent. 
	 * This message contains detailed transaction information. 
	 * The transaction information is contained in a signed JSON string (unencrypted). 
	 * The message includes the signature so you can verify the integrity of the signed string
	 * @param notifyIds
	 */
	protected static void getPurchaseInformation(String[] notifyIds){
		if (amIDead()) {
			return;
		}
		Log.i(TAG, "getPurchaseInformation()");
		Bundle request = makeRequestBundle("GET_PURCHASE_INFORMATION");
		// The REQUEST_NONCE key contains a cryptographically secure nonce (number used once) that you must generate.
		// The Android Market application returns this nonce with the PURCHASE_STATE_CHANGED broadcast intent so you can verify the integrity of the transaction information.
		request.putLong("NONCE", BillingSecurity.generateNonce());
		// The NOTIFY_IDS key contains an array of notification IDs, which you received in the IN_APP_NOTIFY broadcast intent.
		request.putStringArray("NOTIFY_IDS", notifyIds);
		try {
			Bundle response = mService.sendBillingRequest(request);
			
			//The REQUEST_ID key provides you with a unique request identifier for the request
			Long requestIndentifier 	= (Long) response.get("REQUEST_ID");
			Log.i(TAG, "current request is:" + requestIndentifier);
			//The RESPONSE_CODE key provides you with the status of the request
			Integer responseCodeIndex 	= (Integer) response.get("RESPONSE_CODE");
			C.ResponseCode responseCode = C.ResponseCode.valueOf(responseCodeIndex);
			Log.i(TAG, "GET_PURCHASE_INFORMATION Sync Response code: "+responseCode.toString());
			
		} catch (RemoteException e) {
			Log.e(TAG, "Failed, internet error maybe", e);
			Log.e(TAG, "Billing supported: "+isBillingSupported());
		}
	}

	/**
	 * To acknowledge that you received transaction information you send a
	 * CONFIRM_NOTIFICATIONS request.
	 * 
	 * A CONFIRM_NOTIFICATIONS request triggers a single asynchronous response—a RESPONSE_CODE broadcast intent. 
	 * This broadcast intent provides status and error information about the request.
	 * 
	 * Note: As a best practice, you should not send a CONFIRM_NOTIFICATIONS request for a purchased item until you have delivered the item to the user. 
	 * This way, if your application crashes or something else prevents your application from delivering the product,
	 * your application will still receive an IN_APP_NOTIFY broadcast intent from Android Market indicating that you need to deliver the product
	 * @param notifyIds
	 */
	protected static void confirmTransaction(String[] notifyIds) {
		if (amIDead()) {
			return;
		}
		Log.i(TAG, "confirmTransaction()");
		Bundle request = makeRequestBundle("CONFIRM_NOTIFICATIONS");
		request.putStringArray("NOTIFY_IDS", notifyIds);
		try {
			Bundle response = mService.sendBillingRequest(request);

			//The REQUEST_ID key provides you with a unique request identifier for the request
			Long requestIndentifier 	= (Long) response.get("REQUEST_ID");
			Log.i(TAG, "current request is:" + requestIndentifier);
			
			//The RESPONSE_CODE key provides you with the status of the request
			Integer responseCodeIndex 	= (Integer) response.get("RESPONSE_CODE");
			C.ResponseCode responseCode = C.ResponseCode.valueOf(responseCodeIndex);
			
			Log.i(TAG, "CONFIRM_NOTIFICATIONS Sync Response code: "+responseCode.toString());
		} catch (RemoteException e) {
			Log.e(TAG, "Failed, internet error maybe", e);
			Log.e(TAG, "Billing supported: " + isBillingSupported());
		}
	}
	
	/**
	 * 
	 * Can be used for when a user has reinstalled the app to give back prior purchases. 
	 * if an item for sale's purchase type is "managed per user account" this means google will have a record ofthis transaction
	 * 
	 * A RESTORE_TRANSACTIONS request also triggers two asynchronous responses (broadcast intents). 
	 * First, the Android Market application sends a RESPONSE_CODE broadcast intent, which provides status and error information about the request. 
	 * Next, if the request was successful, the Android Market application sends a PURCHASE_STATE_CHANGED broadcast intent. 
	 * This message contains the detailed transaction information. The transaction information is contained in a signed JSON string (unencrypted).
	 * The message includes the signature so you can verify the integrity of the signed string
	 * @param nonce
	 */
	protected static void restoreTransactionInformation(Long nonce) {
		if (amIDead()) {
			return;
		}
		Log.i(TAG, "confirmTransaction()");
		Bundle request = makeRequestBundle("RESTORE_TRANSACTIONS");
		// The REQUEST_NONCE key contains a cryptographically secure nonce (number used once) that you must generate
		request.putLong("NONCE", nonce);
		try {
			Bundle response = mService.sendBillingRequest(request);

			//The REQUEST_ID key provides you with a unique request identifier for the request
			Long requestIndentifier 	= (Long) response.get("REQUEST_ID");
			Log.i(TAG, "current request is:" + requestIndentifier);
			
			//The RESPONSE_CODE key provides you with the status of the request
			Integer responseCodeIndex 	= (Integer) response.get("RESPONSE_CODE");
			C.ResponseCode responseCode = C.ResponseCode.valueOf(responseCodeIndex);
			Log.i(TAG, "RESTORE_TRANSACTIONS Sync Response code: "+responseCode.toString());
		} catch (RemoteException e) {
			Log.e(TAG, "Failed, internet error maybe", e);
			Log.e(TAG, "Billing supported: " + isBillingSupported());
		}
	}
	
	private static boolean amIDead() {
		if (mService == null || mContext == null) {
			Log.e(TAG, "BillingHelper not fully instantiated");
			return true;
		} else {
			return false;
		}
	}

	private static Bundle makeRequestBundle(String method) {
		Bundle request = new Bundle();
		request.putString("BILLING_REQUEST", method);
		request.putInt("API_VERSION", 1);
		request.putString("PACKAGE_NAME", mContext.getPackageName());
		return request;
	}
	
	/**
	 * 
	 * 
	 * You must launch the pending intent from an activity context and not an application context
	 * You cannot use the singleTop launch mode to launch the pending intent
	 * @param pendingIntent
	 * @param intent
	 * @param context
	 */
	private static void startBuyPageActivity(PendingIntent pendingIntent, Intent intent, Context context){
		//TODO add above 2.0 implementation with reflection, for now just using 1.6 implem
		
		// This is on Android 1.6. The in-app checkout page activity will be on its
	    // own separate activity stack instead of on the activity stack of
	    // the application.
		try {
			pendingIntent.send(context, 0, intent);			
		} catch (CanceledException e){
			Log.e(TAG, "startBuyPageActivity CanceledException");
		}
	}

	protected static void verifyPurchase(String signedData, String signature) {
		ArrayList<VerifiedPurchase> purchases = BillingSecurity.verifyPurchase(signedData, signature);
		latestPurchase = purchases.get(0);
		
		confirmTransaction(new String[]{latestPurchase.notificationId});
		
		if(mCompletedHandler != null){
			mCompletedHandler.sendEmptyMessage(0);
		} else {
			Log.e(TAG, "verifyPurchase error. Handler not instantiated. Have you called setCompletedHandler()?");
		}
	}
	
	public static void stopService(){
		mContext.stopService(new Intent(mContext, BillingService.class));
		mService = null;
		mContext = null;
		mCompletedHandler = null;
		Log.i(TAG, "Stopping Service");
	}
}
