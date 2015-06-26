package com.example.hbdemo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubView;
import com.mopub.mobileads.MoPubView.BannerAdListener;

public class MainActivity extends Activity implements BannerAdListener{
	private MoPubView moPubView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		moPubView = (MoPubView) findViewById(R.id.adview);
		moPubView.setAdUnitId("321a7406611a44fba9ec5a16eca84d3a"); // Enter your Ad Unit ID from www.mopub.com
		moPubView.loadAd();
		moPubView.setBannerAdListener(this);
		moPubView.setAutorefreshEnabled(true);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		moPubView.destroy();
	}

	@Override
	public void onBannerClicked(MoPubView arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBannerCollapsed(MoPubView arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBannerExpanded(MoPubView arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBannerFailed(MoPubView arg0, MoPubErrorCode arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBannerLoaded(MoPubView arg0) {
		Toast.makeText(getApplicationContext(),"Banner successfully loaded.", Toast.LENGTH_SHORT).show();		
	}
}
