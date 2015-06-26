package com.example.touchcordinates;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends Activity {
	ImageView touchView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		touchView=(ImageView)findViewById(R.id.touchview);
	}
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	  int eventAction = event.getAction();
    	    switch(eventAction) {
    	        case MotionEvent.ACTION_DOWN:
    	        	touchView.setVisibility(View.VISIBLE);
    	            float TouchX = event.getX();
    	            float TouchY = event.getY();
    	            placeImage(TouchX, TouchY);
    	            break;
//    	        case MotionEvent.ACTION_UP:
//    	        	touchView.setVisibility(View.GONE);

    	    }        
    	    return true;
    }
    
    private void placeImage(float X, float Y) {
        int touchX = (int) X;
        int touchY = (int) Y;


        // placing at bottom right of touch
       // touchView.layout(touchX, touchY, touchX+10, touchY+10);

       // placing at center of touch
        int viewWidth = touchView.getWidth();
        int viewHeight = touchView.getHeight();
        viewWidth = viewWidth / 2;
        viewHeight = viewHeight / 2;

       touchView.layout((int)X - viewWidth, (int)Y - viewHeight, (int)X + viewWidth,(int) Y + viewHeight);
    }

}
