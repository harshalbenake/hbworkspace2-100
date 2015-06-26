package selvin.ListSyncSample;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

public class CustomWindow extends Activity {
	protected TextView header;
    protected TextView title;
    ImageView icSync;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.window_title);
        title = (TextView) findViewById(R.id.title);
        header = (TextView) findViewById(R.id.header);
        icSync  = (ImageView) findViewById(R.id.icSync);
        icSync.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				doSync();
			}
        });
    }
    
    protected void doSync(){
		AccountManager am = AccountManager.get(this);
		Account[] ac = am.getAccountsByType(Constants.ACCOUNT_TYPE);
		if (ac.length > 0)
			ContentResolver.requestSync(ac[0], Constants.AUTHORITY, new Bundle());
    }
    
	BroadcastReceiver startSync = new BroadcastReceiver() {
		public void onReceive(Context arg0, Intent arg1) {
			if (icSync != null) {
				anim = (AnimationDrawable) icSync.getBackground();
				title.setText(R.string.ui_synchronizing);
				anim.start();
			}
		}
	};

	AnimationDrawable anim = null;
	BroadcastReceiver stopSync = new BroadcastReceiver() {
		public void onReceive(Context arg0, Intent arg1) {
			if (anim != null){
				anim.stop();
			}
			anim = null;
			title.setText(R.string.empty);
		}
	};
    
	protected void onResume() {
		super.onResume();
		registerReceiver(startSync, new IntentFilter(Constants.SYNCACTION_START));
		registerReceiver(stopSync, new IntentFilter(Constants.SYNCACTION_STOP));
	}

	protected void onPause() {
		super.onPause();
		unregisterReceiver(startSync);
		unregisterReceiver(stopSync);
	}
}