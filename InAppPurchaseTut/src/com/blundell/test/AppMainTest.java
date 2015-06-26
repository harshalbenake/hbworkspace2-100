package com.blundell.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class AppMainTest extends Activity implements OnClickListener{
    
	private static final String TAG = "BillingService";
	
	private Context mContext;
	private ImageView purchaseableItem;
	private Button purchaseButton;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("BillingService", "Starting");
        setContentView(R.layout.main);
         
        mContext = this;
        
        purchaseButton = (Button) findViewById(R.id.main_purchase_yes);
        purchaseButton.setOnClickListener(this);
        purchaseableItem = (ImageView) findViewById(R.id.main_purchase_item);
        
        startService(new Intent(mContext, BillingService.class));
        BillingHelper.setCompletedHandler(mTransactionHandler);
    }

    public Handler mTransactionHandler = new Handler(){
    		public void handleMessage(android.os.Message msg) {
    			Log.i(TAG, "Transaction complete");
    			Log.i(TAG, "Transaction status: "+BillingHelper.latestPurchase.purchaseState);
    			Log.i(TAG, "Item purchased is: "+BillingHelper.latestPurchase.productId);
    			
    			if(BillingHelper.latestPurchase.isPurchased()){
    				showItem();
    			}
    		};
    	
    };
    
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.main_purchase_yes:
			if(BillingHelper.isBillingSupported()){
				BillingHelper.requestPurchase(mContext, "android.test.purchased"); 
				// android.test.purchased or android.test.canceled or android.test.refunded or com.blundell.item.passport
	        } else {
	        	Log.i(TAG,"Can't purchase on this device");
	        	purchaseButton.setEnabled(false); // XXX press button before service started will disable when it shouldnt
	        }
			
			break;
		default:
			// nada
			Log.i(TAG,"default. ID: "+v.getId());
			break;
		}
		
	}
	
	private void showItem() {
		purchaseableItem.setVisibility(View.VISIBLE);
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause())");
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		BillingHelper.stopService();
		super.onDestroy();
	}
}