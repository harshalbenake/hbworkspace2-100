package com.inapppurchase_as.inapp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;
import android.widget.Toast;

import com.inapppurchase_as.R;
import com.inapppurchase_as.inapp.utils.IabException;
import com.inapppurchase_as.inapp.utils.IabHelper;
import com.inapppurchase_as.inapp.utils.IabResult;
import com.inapppurchase_as.inapp.utils.Inventory;
import com.inapppurchase_as.inapp.utils.Purchase;
import com.inapppurchase_as.inapp.utils.SkuDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to make InApp payments using Google Play.
 *
 */
public class InAppManager {
    // Debug tag, for logging
    static final String TAG = "InAppManager";
    // Does the user have the premium upgrade?
    boolean mIsPremium = false;
    boolean mIsSubscribedToPremimum = false;
    // static final String SKU_PREMIUM = "fastre_inapp_test";
//	static final String SKU_PREMIUM = "fastre_monthly_subscription";
    static final String SKU_PREMIUM = "android.test.purchased"; //
    // android.test.refunded android.test.purchased android.test.canceled
    // SKU for our subscription
    static final String SKU_SUBSCRIPTION = "flashreee_subscribed_account";
    // (arbitrary) request code for the purchase flow
    public static final int RC_REQUEST = 10001;
    // The helper object
    IabHelper mHelper;
    private Activity mActivity;
    private ProgressDialog mProgressDialog;
    private SkuDetails skuDetails;
    private String mPayload;
    private InAppPurchaseStatusCallback inAppPurchaseStatusCallback;

    public InAppManager(Activity activity) {
        this.mActivity = activity;
        String base64EncodedPublicKey = mActivity.getString(R.string.app_name);
        mPayload = mActivity.getResources().getString(R.string.in_app_purchase_payload_key); // secret key
        prepareLoadingLayout();
        // Create the helper, passing it our context and the public key to
        // verify signatures with
        mHelper = new IabHelper(mActivity, base64EncodedPublicKey);
        // enable debug logging (for a production application, you should set
        // this to false).
        mHelper.enableDebugLogging(false);
        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                Log.d(TAG, "Setup finished.");
                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    complain("Problem setting up in-app billing: " + result.getMessage());
                    return;
                }
                // Have we been disposed of in the meantime? If so, quit.
                if (mHelper == null)
                    return;
                // this is used to send details on fastre server if user paid
                // money & while
                try {
                    List<String> skuList = new ArrayList<String>();
                    skuList.add(SKU_PREMIUM);
                    Inventory inventory = mHelper.queryInventory(true, skuList);
                    skuDetails = inventory.getSkuDetails(SKU_PREMIUM);
                } catch (IabException e) {
                    e.printStackTrace();
                }
                // Hooray, IAB is fully set up. Now, let's get an inventory of
                // stuff we own.
                Log.d(TAG, "Setup successful. Querying inventory.");
                mHelper.queryInventoryAsync(mGotInventoryListener);
            }
        });
    }

    private void prepareLoadingLayout() {
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setMessage("Please wait...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
    }

    // Listener that's called when we finish querying the items and
    // subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            Log.d(TAG, "Query inventory finished.");
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result);
                return;
            }
            Log.d(TAG, "Query inventory was successful.");
            /*
             * Check for items we own. Notice that for each purchase, we check the developer payload to see if it's correct! See verifyDeveloperPayload().
			 */
            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(SKU_PREMIUM);
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
            // alert("User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));
            // if(mIsPremium && (new
            // UserManager().isUserPremiumUser(mActivity)==false)){
            // changeUserAccountStatus(premiumPurchase);
            // }
            // // Do we have the subscription?
            // Purchase isSubcribedUser =
            // inventory.getPurchase(SKU_SUBSCRIPTION);
            // mIsSubscribedToPremimum = (isSubcribedUser != null &&
            // verifyDeveloperPayload(isSubcribedUser));
            // Log.d(TAG, "User " + (mIsSubscribedToPremimum ? "HAS" :
            // "DOES NOT HAVE")
            // + " infinite gas subscription.");
            // // updateUi();
            // Log.d(TAG,
            // "Initial inventory query finished; enabling main UI.");
        }
    };

    /**
     * Verifies the developer payload of a purchase.
     */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();
		/*
		 * TODO: verify that the developer payload of the purchase is correct. It will be the same one that you sent when initiating the purchase. WARNING: Locally generating a random string when starting a purchase and verifying it here might seem like a good approach, but this will fail in the case where the user purchases an item on one device and then uses your app on a different device, because on the other device you will not have access to the random string you originally generated. So a
		 * good developer payload has these characteristics: 1. If two different users purchase an item, the payload is different between them, so that one user's purchase can't be replayed to another user. 2. The payload must be such that you can verify it even when the app wasn't the one who initiated the purchase flow (so that items purchased by the user on one device work on other devices owned by the user). Using your own server to store and verify developer payloads across app
		 * installations is recommended.
		 */
        if (payload != null && payload.equalsIgnoreCase(mPayload))
            return true;
        else
            return false;
    }

    public void setUpgradeToPremimumClickListener(View view, InAppPurchaseStatusCallback statusCallback) {
        this.inAppPurchaseStatusCallback = statusCallback;
        view.setOnClickListener(mClickListener);
    }

    private OnClickListener mClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v instanceof CheckBox) {
                ((CheckBox) v).setChecked(false);
            }

            // boolean mIsPremiumUser = new
            // UserManager().isUserPremiumUser(mActivity);
            // if(mIsPremiumUser==false){
            if (!mHelper.subscriptionsSupported()) {
                complain("Subscriptions not supported on your device yet. Sorry!");
                return;
            }
            // /* TODO: for security, generate your payload here for
            // verification. See the comments on
            // * verifyDeveloperPayload() for more info. Since this is a SAMPLE,
            // we just use
            // * an empty string, but on a production app you should carefully
            // generate this. */
            // String payload = "";
            // // String payload =
            // mActivity.getResources().getString(R.string.in_app_purchase_payload_key);
            // //secret key
            //
            // setWaitScreen(true);
            // Log.d(TAG,
            // "Launching purchase flow for infinite gas subscription.");
            // mHelper.launchPurchaseFlow((Activity)mActivity,
            // SKU_SUBSCRIPTION, IabHelper.ITEM_TYPE_SUBS,
            // RC_REQUEST, mPurchaseFinishedListener, payload);
            Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
            setWaitScreen(true);
			/*
			 * TODO: for security, generate your payload here for verification. See the comments on verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use an empty string, but on a production app you should carefully generate this.
			 */
            // String payload = "";
            mHelper.launchPurchaseFlow((Activity) mActivity, SKU_PREMIUM, RC_REQUEST, mPurchaseFinishedListener, mPayload);
            // }else {
            // CustomToast.makeText(mActivity,
            // "You are already a premimum user.",
            // CustomToast.LENGTH_SHORT).show();
            // }
        }
    };
    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                // complain("Error purchasing: " + result);
                setWaitScreen(false);
                mHelper.flagEndAsync();
                return;
            }
            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                setWaitScreen(false);
                mHelper.flagEndAsync();
                return;
            }
            Log.d(TAG, "Purchase successful.");
            if (purchase.getSku().equals(SKU_PREMIUM)) {
                // bought the premium upgrade!
                alert("Thank you for upgrading to ProVersion!");
                mIsPremium = true;
                changeUserAccountStatus(purchase);
                setWaitScreen(false);
                mHelper.consumeAsync(purchase, new IabHelper.OnConsumeFinishedListener() {
                    @Override
                    public void onConsumeFinished(Purchase purchase, IabResult result) {
                        if (result.isSuccess()) {
                            Log.v(TAG, "In-App consumption is successfully done.");
                        } else {
                            Log.v(TAG, "Error while In-App consumption.");
                        }
                    }
                });
                mHelper.flagEndAsync();
            } else if (purchase.getSku().equals(SKU_SUBSCRIPTION)) {
                // bought the subscription
                alert("Thank you for subscribing to ProVersion!");
                mIsSubscribedToPremimum = true;
                setWaitScreen(false);
                mHelper.flagEndAsync();
                // alert("You are subscribed to premimum...");
            }
        }
    };

    void complain(String message) {
        Log.e(TAG, "**** InAppManager Error: " + message);
        alert(message);
    }

    // Enables or disables the "please wait" screen.
    void setWaitScreen(boolean set) {
        if (set)
            mProgressDialog.show();
        else if (set == false && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }

    void alert(String message) {
        Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
        // AlertDialog.Builder bld = new AlertDialog.Builder(mActivity);
        // bld.setMessage(message);
        // bld.setNeutralButton("OK", null);
        // Log.d(TAG, "Showing alert dialog: " + message);
        // bld.create().show();
    }

    public IabHelper getHelper() {
        return mHelper;
    }

    private void changeUserAccountStatus(Purchase purchase) {
        if (purchase != null && inAppPurchaseStatusCallback != null) {
            inAppPurchaseStatusCallback.onPurchaseSuccess(purchase);
        }
    }
}