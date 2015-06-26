package com.findingfriend_as;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.ToggleButton;

public class AirplaneActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_airplane);
        ToggleButton toggleButton=(ToggleButton)findViewById(R.id.toggle_button);
        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(isChecked){
                    airPlanemodeON();
                }
                else{
                    airPlanemodeOFF();
                }

            }
        });
	}



    @SuppressWarnings("deprecation")
    public void airPlanemodeON() {
        boolean isEnabled = Settings.System.getInt(this.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
        if (isEnabled == false) {
            modifyAirplanemode(true);
            Toast.makeText(getApplicationContext(), "Airplane Mode ON",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void airPlanemodeOFF() {
        boolean isEnabled = Settings.System.getInt(this.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) == 1;
        if (isEnabled == true)// means this is the request to turn ON AIRPLANE mode
        {
            modifyAirplanemode(false);
            Toast.makeText(getApplicationContext(), "Airplane Mode OFF",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void modifyAirplanemode(boolean mode) {
        Settings.System.putInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, mode ? 1 : 0);// Turning ON/OFF Airplane mode.

        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);// creating intent and Specifying action for AIRPLANE mode.
        intent.putExtra("state", !mode);// indicate the "state" of airplane mode is changed to ON/OFF
        sendBroadcast(intent);// Broadcasting and Intent

    }


}
