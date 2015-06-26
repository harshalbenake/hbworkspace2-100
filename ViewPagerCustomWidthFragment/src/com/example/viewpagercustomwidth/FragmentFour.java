package com.example.viewpagercustomwidth;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


/**
 * This is class is used for loading fragment four.
 * <b>@author harshalb</b>
 *
 */
public class FragmentFour extends Fragment {				
		private View mainView;
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			mainView=(View)inflater.inflate(R.layout.fragment_four,container,false);
			return mainView;
		}
}
