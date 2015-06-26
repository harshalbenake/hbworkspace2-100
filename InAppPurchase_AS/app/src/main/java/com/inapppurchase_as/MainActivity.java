package com.inapppurchase_as;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;

import com.inapppurchase_as.inapp.InAppManager;
import com.inapppurchase_as.inapp.InAppPurchaseStatusCallback;
import com.inapppurchase_as.inapp.utils.Purchase;


public class MainActivity extends Activity {
    private InAppManager mInAppManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final CheckBox checkBox=(CheckBox)findViewById(R.id.cv_in_app_purchase);

        // DO NOT DELETE
        mInAppManager = new InAppManager(MainActivity.this);
        mInAppManager.setUpgradeToPremimumClickListener(checkBox, new InAppPurchaseStatusCallback() {

            @Override
            public void onPurchaseSuccess(Purchase purchase) {

                    setResult(RESULT_OK);

                checkBox.setChecked(true);
                checkBox.setEnabled(false);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the helper for handling
        if (mInAppManager != null && !mInAppManager.getHelper().handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...

            super.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
