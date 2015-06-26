package com.example.slidingmenuapi;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

import com.example.slidingmenuapi.fragments.LeftMenuFragment;
import com.example.slidingmenuapi.fragments.RightMenuFragment;
import com.example.slidingmenuapi.slidingmenu.SlidingMenu;
import com.example.slidingmenuapi.slidingmenu.app.SlidingFragmentActivity;

public class MainActivity extends SlidingFragmentActivity
{
	private LeftMenuFragment leftMenuFragment;
	private RightMenuFragment rightMenuFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		 // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
		setContentView(R.layout.responsive_right_frame);
		setBehindContentView(R.layout.responsive_left_frame);
		getSlidingMenu().setMode(SlidingMenu.LEFT_RIGHT);
		getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		getSlidingMenu().setSecondaryMenu(R.layout.fragment_left_menu);
		getSlidingMenu().setSecondaryShadowDrawable(R.drawable.shadow);

		if(savedInstanceState == null)
		{
			FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
			leftMenuFragment = new LeftMenuFragment();
			rightMenuFragment = new RightMenuFragment();

			t.replace(R.id.left_frame, leftMenuFragment);
			t.replace(R.id.right_frame, rightMenuFragment);
			t.commit();
		}
		else
		{
			leftMenuFragment = (LeftMenuFragment) this.getSupportFragmentManager().findFragmentById(R.id.left_frame);
			rightMenuFragment = (RightMenuFragment) this.getSupportFragmentManager().findFragmentById(R.id.right_frame);
		}
		checkOrientation(getResources().getConfiguration());
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		checkOrientation(newConfig);
	}

	private void checkOrientation(Configuration newConfig)
	{
		getSlidingMenu().setShadowWidthRes(R.dimen.shadow_width);
		getSlidingMenu().setShadowDrawable(R.drawable.shadow);
		getSlidingMenu().setBehindOffsetRes(R.dimen.slidingmenu_offset);

		
			// stop screen rotation on phones because <explain>
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
			getSlidingMenu().setSlidingEnabled(true);

		Handler h = new Handler();
		h.postDelayed(new Runnable()
		{
			public void run()
			{
				getSlidingMenu().showMenu(false);
			}
		}, 50);
	}
	
	

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		getSupportFragmentManager().putFragment(outState, "mContent", rightMenuFragment);
	}

	public void switchContent(final Fragment fragment)
	{
		rightMenuFragment = (RightMenuFragment) fragment;
		getSupportFragmentManager().beginTransaction().replace(R.id.right_frame, fragment).commit();
		Handler h = new Handler();
		h.postDelayed(new Runnable()
		{
			public void run()
			{
				getSlidingMenu().showContent();
			}
		}, 50);
	}
	
	public void toggleView()
	{
		super.toggle();
	}}