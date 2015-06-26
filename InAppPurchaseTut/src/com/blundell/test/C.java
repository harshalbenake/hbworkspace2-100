package com.blundell.test;


public class C {

	// The response codes for a request, defined by Android Market.
	public enum ResponseCode {
		RESULT_OK, 
		RESULT_USER_CANCELED, 
		RESULT_SERVICE_UNAVAILABLE, 
		RESULT_BILLING_UNAVAILABLE, 
		RESULT_ITEM_UNAVAILABLE, 
		RESULT_DEVELOPER_ERROR, 
		RESULT_ERROR;

		// Converts from an ordinal value to the ResponseCode
		public static ResponseCode valueOf(int index) {
			ResponseCode[] values = ResponseCode.values();
			if (index < 0 || index >= values.length) {
				return RESULT_ERROR;
			}
			return values[index];
		}
	}

	// The possible states of an in-app purchase, as defined by Android Market.
    public enum PurchaseState {
        // Responses to requestPurchase or restoreTransactions.
        PURCHASED,   // User was charged for the order.
        CANCELED,    // The charge failed on the server.
        REFUNDED;    // User received a refund for the order.

        // Converts from an ordinal value to the PurchaseState
        public static PurchaseState valueOf(int index) {
            PurchaseState[] values = PurchaseState.values();
            if (index < 0 || index >= values.length) {
                return CANCELED;
            }
            return values[index];
        }
    }
	
	// These are the names of the extras that are passed in an intent from
    // Market to this application and cannot be changed.
    public static final String NOTIFICATION_ID = "notification_id";
    public static final String INAPP_SIGNED_DATA = "inapp_signed_data";
    public static final String INAPP_SIGNATURE = "inapp_signature";
    public static final String INAPP_REQUEST_ID = "request_id";
    public static final String INAPP_RESPONSE_CODE = "response_code";
	
	// Intent actions that we send from the BillingReceiver to the
	// BillingService. Defined by this application.
	public static final String ACTION_CONFIRM_NOTIFICATION = "com.example.dungeons.CONFIRM_NOTIFICATION";
	public static final String ACTION_GET_PURCHASE_INFORMATION = "com.example.dungeons.GET_PURCHASE_INFORMATION";
	public static final String ACTION_RESTORE_TRANSACTIONS = "com.example.dungeons.RESTORE_TRANSACTIONS";

	// Intent actions that we receive in the BillingReceiver from Market.
	// These are defined by Market and cannot be changed.
	public static final String ACTION_NOTIFY = "com.android.vending.billing.IN_APP_NOTIFY";
	public static final String ACTION_RESPONSE_CODE = "com.android.vending.billing.RESPONSE_CODE";
	public static final String ACTION_PURCHASE_STATE_CHANGED = "com.android.vending.billing.PURCHASE_STATE_CHANGED";
	
}
