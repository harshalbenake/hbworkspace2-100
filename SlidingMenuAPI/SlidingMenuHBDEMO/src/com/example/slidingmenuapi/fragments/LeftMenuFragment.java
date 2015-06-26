package com.example.slidingmenuapi.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.slidingmenuapi.R;

public class LeftMenuFragment extends Fragment {
	Activity activity;
	 
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View mainView=(View)inflater.inflate(R.layout.fragment_left_menu, null);
		activity=getActivity();
		return mainView; 
	}

	
}
