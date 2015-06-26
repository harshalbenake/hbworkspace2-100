package com.example.dragndroplowversion;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
	ImageView mImageView;

	LinearLayout.LayoutParams params;

	/** Called when the activity is first created. */

	@Override
	public void onCreate(Bundle savedInstanceState)

	{

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		mImageView = (ImageView) findViewById(R.id.mImageView);

		mImageView.setOnTouchListener(new MyTouchListener());

		params = (LinearLayout.LayoutParams) mImageView.getLayoutParams();

	}

	public class MyTouchListener implements OnTouchListener

	{

		public boolean onTouch(View v, MotionEvent event)

		{

			switch (event.getAction())

			{

				case MotionEvent.ACTION_DOWN :

					// do nothing

					break;

				case MotionEvent.ACTION_MOVE :

					setViewPosition(mImageView, event);

					break;

				case MotionEvent.ACTION_UP :

					// do nothing

					break;

			}

			return true;

		}

	}

	private void setViewPosition(View view, MotionEvent event)

	{

		int leftdist = (int) event.getRawX();

		int topdist = (int) event.getRawY();

		// since, coordinate got from event.getRawX() and event.getRawY() will
		// act as the top left most

		// coordinate for view's new position. So, subtract the half of width
		// and height from leftdist

		// and topdist respectively to get the image right in center of your
		// finger tip.

		params.setMargins(leftdist - (view.getWidth() / 2),
				topdist - (view.getHeight() / 2), 0, 0);

		view.setLayoutParams(params);

	}

}
