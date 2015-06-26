package com.example.viewpagercustomwidth;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


/**
 * This is class is used for loading data fragment.
 * <b>@author harshalb</b>
 *
 */
public class DataFragment extends Fragment {
		View mainView;
		Activity activity;
		public TextView display_category = null;
				
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mainView=(View)inflater.inflate(R.layout.main_data, null);
			activity = getActivity();
			android.support.v4.app.FragmentManager fragmentManager = getFragmentManager();
			android.support.v4.app.FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
			FragmentOne fragmentOne = new FragmentOne();
			fragmentTransaction.replace(R.id.details_datafragment, fragmentOne);
			fragmentTransaction.addToBackStack(null);
			fragmentTransaction.commit();
			display_category = (TextView)mainView.findViewById(R.id.displayCategory);
			return mainView;
		}
	
		@Override
		public void onStart() {
			initializeLayout();
			super.onStart();
		}
		
		/**
		 * This method is used to initializeLayout.
		 */
		private void initializeLayout() {
			ImageView datafragment_optionMenu = (ImageView)mainView.findViewById(R.id.datafragment_optionMenu);
			datafragment_optionMenu.setOnClickListener(new OnClickListener() {
	
				@Override
				public void onClick(View v) {
					((MainActivity) activity).changePagerPage();
				}
			});
}
}
