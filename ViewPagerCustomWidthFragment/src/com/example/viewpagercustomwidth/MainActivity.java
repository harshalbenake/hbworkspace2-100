package com.example.viewpagercustomwidth;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

/**
 * This class is used to loading viewpager.
 * <b>@author harshalb</b>
 *
 */
public class MainActivity extends FragmentActivity {

	private ViewPager viewpager;
	public static final int CATEGORY=0, DATAPAGE=1;/**indexing for pager. CATEGORY=0, DATAPAGE=1*/
	int itemCount=2;/**Number of pages.*/
	int flag=1;
	public CategoryFragment categoryFragment;
	DataFragment dataFragment;
	public static final int FragmentOne=0,FragmentTwo=1,FragmentThree=2,FragmentFour=3,FragmentFive=4;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initializeLayout();
		dataFragment=new DataFragment();
		categoryFragment=new CategoryFragment();
	}
	
	/**
	 * This method is used to initializeLayout.
	 */
	public void initializeLayout(){
		viewpager = (ViewPager)findViewById(R.id.hbdemo_viewpager);
		ViewPagerAdapter vpAdapter=new ViewPagerAdapter(getSupportFragmentManager());
		viewpager.setAdapter(vpAdapter);
		viewpager.setOnPageChangeListener(new ViewPagerListener());
		if(flag==1)
			changePagerPage(0);
	}
	
	/**
	 * This method is used to change pager page.
	 * @param int position
	 */
	public void changePagerPage(int flag){
		try{
			int position=flag;
			if(position==CATEGORY)
				viewpager.setCurrentItem(DATAPAGE);
			if(position==DATAPAGE){
				viewpager.setCurrentItem(CATEGORY);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * This method is used to change pager page.
	 */
	public void changePagerPage() {
		try{
			int position=viewpager.getCurrentItem();
			if(position==CATEGORY)
				viewpager.setCurrentItem(DATAPAGE);
			if(position==DATAPAGE)
				viewpager.setCurrentItem(CATEGORY);
		}catch(Exception e){
			e.printStackTrace();
		}	
	}

	/**
	 *This is pager listerner class of category and data fragment.
	 *<b>@author harshalb</b>
	 */
	class ViewPagerListener implements ViewPager.OnPageChangeListener {

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}

		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageSelected(int position) {
			switch (position) {
			case CATEGORY:
				break;
			case DATAPAGE:
				break;
			default:
				break;
			}
		}
	}
	
	/**
	 * This class is view pager adapter class.
	 * <b>@author harshalb</b>
	 *
	 */
	class ViewPagerAdapter extends FragmentPagerAdapter{

		public ViewPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public int getCount() {
			return itemCount;
		}

		@Override
		public Fragment getItem(int position) {
			if(position==0){
				categoryFragment=new CategoryFragment();
				return categoryFragment;
			}else if(position==1){
				dataFragment=new DataFragment();
				return dataFragment;
			}
			return null;
		}

		@Override
		public float getPageWidth(int position) {
			if(position==0 || position==2)
				return 0.60f;
			else
				return 1.00f;
			//			return super.getPageWidth(position);
		}
	}
	
	/**
	 * @param position
	 * @return
	 * 
	 * This method is used to replace fragment as per category list 
	 * item selected.
	 */
	public Fragment getIndexOfFragment(int position)
	{
		if (position == FragmentOne) {
			changePagerPage(0);
			FragmentOne fragmentOne = new FragmentOne();
			FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
			fragmentTransaction.replace(R.id.details_datafragment, fragmentOne);
			setCategoryListTitle("FragmentOne");
			fragmentTransaction.commit();
			setCategoryListItemBackground(FragmentOne);
			return fragmentOne;
		}
		else if (position == FragmentTwo) {
			changePagerPage(0);
			FragmentTwo fragmentTwo = new FragmentTwo();
			FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
			fragmentTransaction.replace(R.id.details_datafragment, fragmentTwo);
			setCategoryListTitle("FragmentTwo");
			fragmentTransaction.commit();
			setCategoryListItemBackground(FragmentTwo);
			return fragmentTwo;
		}
		else if (position == FragmentThree) {
			changePagerPage(0);
			FragmentThree fragmentThree = new FragmentThree();
			FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
			fragmentTransaction.replace(R.id.details_datafragment, fragmentThree);
			setCategoryListTitle("FragmentThree");
			fragmentTransaction.commit();
			setCategoryListItemBackground(FragmentThree);
			return fragmentThree;
		}
		else if (position == FragmentFour) {
			changePagerPage(0);
			FragmentFour fragmentFour = new FragmentFour();
			FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
			fragmentTransaction.replace(R.id.details_datafragment, fragmentFour);
			setCategoryListTitle("FragmentFour");
			fragmentTransaction.commit();
			setCategoryListItemBackground(FragmentFour);
			return fragmentFour;
		}
		else if (position == FragmentFive) {
			changePagerPage(0);
			FragmentFive fragmentFive = new FragmentFive();
			FragmentTransaction fragmentTransaction=getSupportFragmentManager().beginTransaction();
			fragmentTransaction.replace(R.id.details_datafragment, fragmentFive);
			setCategoryListTitle("FragmentFive");
			fragmentTransaction.commit();
			setCategoryListItemBackground(FragmentFive);
			return fragmentFive;
		}
		return null;
	}
	
	/**
	 * This method is used to set background color of the category list item.
	 */
	public void setCategoryListItemBackground(int index){
		try
		{
			categoryFragment.category_one.setBackgroundColor(Color.TRANSPARENT);
			categoryFragment.category_two.setBackgroundColor(Color.TRANSPARENT);
			categoryFragment.category_three.setBackgroundColor(Color.TRANSPARENT);
			categoryFragment.category_four.setBackgroundColor(Color.TRANSPARENT);
			categoryFragment.category_five.setBackgroundColor(Color.TRANSPARENT);
			
			switch (index) {
			case FragmentOne:
				categoryFragment.category_one.setBackgroundColor(Color.DKGRAY);
				break;
			case FragmentTwo:
				categoryFragment.category_two.setBackgroundColor(Color.DKGRAY);
				break;
			case FragmentThree:
				categoryFragment.category_three.setBackgroundColor(Color.DKGRAY);
				break;
			case FragmentFour:
				categoryFragment.category_four.setBackgroundColor(Color.DKGRAY);
				break;
			case FragmentFive:
				categoryFragment.category_five.setBackgroundColor(Color.DKGRAY);
				break;
			}
		}
		catch (Exception e) {
		}
	}
	
	/**
	 * This method is used to set category title.
	 * @param title
	 */
	public void setCategoryListTitle(String title) {
		dataFragment.display_category.setText(title);
		dataFragment.display_category.setTextSize(25);
	}
}

