package com.example.hbdemo;

import java.lang.reflect.Field;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button button=(Button)findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				 Field field[] = First.class.getDeclaredFields();  
		         for (int i = 0; i < field.length; i++)  
		         {  
		             System.out.println("Variable Name is : " + field[i].getName());  
		         }        
			}
		});
		
		
		     
	}
}
