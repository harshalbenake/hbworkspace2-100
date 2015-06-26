package com.findingfriend_as;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLayout();
    }

    public void initLayout(){
        Button button1=(Button)findViewById(R.id.button1);
        Button button2=(Button)findViewById(R.id.button2);
        Button button3=(Button)findViewById(R.id.button3);

        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
        button3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if(v.getId()==R.id.button1){
            Intent intent=new Intent(MainActivity.this,AirplaneActivity.class);
            startActivity(intent);
        }
        else if(v.getId()==R.id.button2){
            Intent intent=new Intent(MainActivity.this,GeoFencingActivity.class);
            startActivity(intent);
        }
    }
}
