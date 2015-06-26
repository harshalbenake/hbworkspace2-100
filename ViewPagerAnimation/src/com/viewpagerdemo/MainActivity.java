package com.viewpagerdemo;

import com.viewpagerdemo.R;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.PageTransformer;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ToggleButton;

public class MainActivity extends FragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		MyPagerAdapter pageAdapter = new MyPagerAdapter(getSupportFragmentManager());
		final ViewPager pager = (ViewPager)findViewById(R.id.myViewPager);
		pager.setAdapter(pageAdapter);
		pager.setPageTransformer(false, new PageTransformer() {
			
			@Override
			public void transformPage(View page, float position) {
				
				/**Remove below comment for any single transformation**/
				
				/**ALPHA TRANSFORMATION**/
//				final float normalizedposition = Math.abs(Math.abs(position) - 1);
//				page.setAlpha(normalizedposition);

				/**SCALING TRANSFORMATION**/
//				final float normalizedposition = Math.abs(Math.abs(position) - 1);
//				page.setScaleX(normalizedposition / 2 + 0.5f);
//				page.setScaleY(normalizedposition / 2 + 0.5f);

				/**ROATAION TRANSFORMATION**/
//				page.setRotationY(position * -30);
			}
		});
	}

}
