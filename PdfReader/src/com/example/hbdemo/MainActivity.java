package com.example.hbdemo;

import java.io.File;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private String path="Environment.getExternalStorageDirectory()+/sample.pdf";

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_main);
	    Button button=(Button)findViewById(R.id.button1);
	    button.setOnClickListener(new OnClickListener()
		{
			
			@Override
			public void onClick(View v)
			{
				  File file = new File("/sdcard/sample.pdf"); 

	                if (file.exists()) { 
	                    Uri path = Uri.fromFile(file); 
	                    Intent intent = new Intent(Intent.ACTION_VIEW); 
	                    intent.setDataAndType(path, "application/pdf"); 
	                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 

	                    try { 
	                        startActivity(intent); 
	                    }  
	                    catch (ActivityNotFoundException e) { 
	                        Toast.makeText(getApplicationContext(),  
	                            "No Application Available to View PDF",  
	                            Toast.LENGTH_SHORT).show(); 
	                    } 
	                } 
			}
		});
	}

}
