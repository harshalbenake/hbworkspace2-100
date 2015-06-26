package com.androidhub4you.pagingnation;

/**
 * algorithm
 * 0-0,1,2,3
 * 1-4,5,6,7
 * 2-8,9,10,11
 * 3-12,13,14,15
 * n-postion*n and n++
 */

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;


public class MainActivity extends Activity {

	private LinearLayout mLinearScroll;
	private ListView mListView;
	private ArrayList<String> mArrayListFruit;
	private ArrayList<String> mArrayListFruitTemp;
	// change row size according to your need, how many row you needed per page
	int rowSize = 5;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mLinearScroll = (LinearLayout) findViewById(R.id.linear_scroll);
		mListView = (ListView) findViewById(R.id.listview1);

		/**
		 * add item into arraylist
		 */
		mArrayListFruit = new ArrayList<String>();
		mArrayListFruitTemp = new ArrayList<String>();

		mArrayListFruit.add("Apple1");
		mArrayListFruit.add("Apple2");
		mArrayListFruit.add("Apple3");
		mArrayListFruit.add("Apple4");
		mArrayListFruit.add("Apple5");
		mArrayListFruit.add("Apple6");
		mArrayListFruit.add("Apple7");
		mArrayListFruit.add("Apple8");
		mArrayListFruit.add("Apple9");
		mArrayListFruit.add("Apple10");
		mArrayListFruit.add("Apple11");
		mArrayListFruit.add("Apple12");
		mArrayListFruit.add("Apple13");
		mArrayListFruit.add("Apple14");
		mArrayListFruit.add("Apple15");
		mArrayListFruit.add("Apple16");
		mArrayListFruit.add("Apple17");
		mArrayListFruit.add("Apple18");
		mArrayListFruit.add("Apple19");
		mArrayListFruit.add("Apple20");
		mArrayListFruit.add("Apple21");
		mArrayListFruit.add("Apple22");
		mArrayListFruit.add("Apple23");
		mArrayListFruit.add("Apple24");
		mArrayListFruit.add("Apple25");
		mArrayListFruit.add("Apple26");
		mArrayListFruit.add("Apple27");
		mArrayListFruit.add("Apple28");
		mArrayListFruit.add("Apple29");
		mArrayListFruit.add("Apple30");
		mArrayListFruit.add("Apple31");

		/**
		 * create dynamic button according the size of array
		 */

		int rem = mArrayListFruit.size() % rowSize;
		if (rem > 0) {

			for (int i = 0; i < rowSize - rem; i++) {
				mArrayListFruit.add("");
			}
		}

		/**
		 * add arraylist item into list on page load
		 */
		addItem(0);

		int size = mArrayListFruit.size() / rowSize;

		for (int j = 0; j < size; j++) {
			final int k;
			k = j;
			final Button btnPage = new Button(MainActivity.this);
			LayoutParams lp = new LinearLayout.LayoutParams(120,
					LayoutParams.WRAP_CONTENT);
			lp.setMargins(5, 2, 2, 2);
			btnPage.setTextColor(Color.WHITE);
			btnPage.setTextSize(26.0f);
			btnPage.setId(j);
			btnPage.setText(String.valueOf(j + 1));
			mLinearScroll.addView(btnPage, lp);

			btnPage.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					/**
					 * add arraylist item into list
					 */
					addItem(k);

				}
			});
		}
	}

	//create dynamic temp array list from main-list
	public void addItem(int count) {
		mArrayListFruitTemp.clear();
		count = count * rowSize;
		/**
		 * fill temp array list to set on page change
		 */
		for (int j = 0; j < rowSize; j++) {
			mArrayListFruitTemp.add(j, mArrayListFruit.get(count));
			count = count + 1;
		}
		// set view
		setView();
	}

	//set view method
	public void setView() {

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(
				MainActivity.this, android.R.layout.simple_list_item_1,
				mArrayListFruitTemp);
		mListView.setAdapter(adapter);

		/**
		 * On item click listner
		 */
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				// TODO Auto-generated method stub

				System.out.println(mArrayListFruitTemp.get(arg2));
			}
		});

	}
}
