package com.example.viewpagercustomwidth;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;


/**
 * This is class is used for loading category fragment.
 * <b>@author harshalb</b>
 *
 */
public class CategoryFragment extends Fragment {
	Activity activity;
	public LinearLayout category_one;
	public LinearLayout category_two;
	public LinearLayout category_three;
	public LinearLayout category_four;
	public LinearLayout category_five;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View mainView=(View)inflater.inflate(R.layout.main_category, null);
		activity=getActivity();
		category_one=(LinearLayout)mainView.findViewById(R.id.category_one);
		category_two=(LinearLayout)mainView.findViewById(R.id.category_two);
		category_three=(LinearLayout)mainView.findViewById(R.id.category_three);
		category_four=(LinearLayout)mainView.findViewById(R.id.category_four);
		category_five=(LinearLayout)mainView.findViewById(R.id.category_five);
		return mainView;
	}
	
	@Override
	public void onStart() {
		initializeLayout();
		super.onStart();
	}

	
	/**
	 * is method is used to initializeLayout.
	 */
	private void initializeLayout() {
		category_one.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				((MainActivity)activity).getIndexOfFragment(MainActivity.FragmentOne);
			}
		});
		category_two.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				((MainActivity)activity).getIndexOfFragment(MainActivity.FragmentTwo);
			}
		});
		category_three.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				((MainActivity)activity).getIndexOfFragment(MainActivity.FragmentThree);
			}
		});
		category_four.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				((MainActivity)activity).getIndexOfFragment(MainActivity.FragmentFour);
			}
		});
		category_five.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				((MainActivity)activity).getIndexOfFragment(MainActivity.FragmentFive);
			}
		});
	}
}