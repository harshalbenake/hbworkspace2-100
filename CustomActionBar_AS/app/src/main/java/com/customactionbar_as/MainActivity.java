package com.customactionbar_as;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initActionBar();
    }

    /**
     * initialise action bar
     */
    public void initActionBar(){
        View view=(View)findViewById(R.id.custom_action_bar_layout);
        CustomActionBar customActionBar=new CustomActionBar(getBaseContext(),view);
        customActionBar.setTitle("MY Title");
        customActionBar.setTitleColor(Color.RED);
        customActionBar.setBackButtonOnClickListner(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getBaseContext(),"Back Button Clicked",Toast.LENGTH_SHORT).show();
            }
        });

    }
}
