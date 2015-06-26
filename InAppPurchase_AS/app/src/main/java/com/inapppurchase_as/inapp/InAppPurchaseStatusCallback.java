package com.inapppurchase_as.inapp;


import com.inapppurchase_as.inapp.utils.Purchase;

/**
 * This interface is used to handle in app purchase success callback
 */
public interface InAppPurchaseStatusCallback {
    public void onPurchaseSuccess(Purchase purchase);
}
