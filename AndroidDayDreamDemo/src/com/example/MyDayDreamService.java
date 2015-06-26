package com.example;


import android.app.ActionBar.LayoutParams;
import android.service.dreams.DreamService;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
 
public class MyDayDreamService extends DreamService {
    @Override
   public void onAttachedToWindow() {
       super.onAttachedToWindow();

       //allow user touch
       setInteractive(true);
 
       // Allow full screen
       setFullscreen(true);
 
       //Add text and image to dream layout
       RelativeLayout r1 = new RelativeLayout(this);
      
       RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
       params.addRule(RelativeLayout.CENTER_HORIZONTAL);
      
       TextView textView = new TextView(this);
       textView.setId(1);
       textView.setText("This is a HB demo of the DayDream.");
       textView.setTextSize(35);
       textView.setLayoutParams(params);
       
       r1.addView(textView,params);

       RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(300, 250);
       params2.addRule(RelativeLayout.BELOW,1); 
       params2.addRule(RelativeLayout.CENTER_HORIZONTAL);
       ImageView iv = new ImageView(this);
       iv.setImageResource(R.drawable.android);
       iv.setLayoutParams(params2);
       
       
       r1.addView(iv,params2);
      
       setContentView(r1);
          
   }
}