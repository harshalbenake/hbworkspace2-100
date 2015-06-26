package com.elite;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
/**
*This class is used to show uninstall popup from another app.
* @author <b>Elite</b>
*/
public class UninstallAdminDevice extends Activity {
	
	private final int UNINSTALL_PACKAGE_EXISTED = 5002;
	private final int UNINSTALL_PACKAGE_EXISTED_PWD = 5004;
	boolean isCallfromPasswordScreen;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_uninstall_admin_device);
		Intent intent=getIntent();
		isCallfromPasswordScreen=intent.getBooleanExtra("isCallfromPasswordScreen",false);
		AdminSettingCheckPackageExisted(getPackageName());
	}
	
	/**
	* This is uninstall process & this method is used to show admin setting deactivation popup.  
	*/
	public void AdminSettingCheckPackageExisted(final String packageName){
				try{
					if(!isCallfromPasswordScreen){
				DeviceManager deviceManager=new DeviceManager();
				if(deviceManager.isDeviceAdminActive(getApplicationContext())){
					deviceManager.deactivateDeviceAdmin(getApplicationContext());
				}
				}
				Uri packageURI = Uri.parse("package:"+packageName);
				Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
				if(!isCallfromPasswordScreen){
				startActivityForResult(uninstallIntent,UNINSTALL_PACKAGE_EXISTED);
				}
				else{
					startActivityForResult(uninstallIntent,UNINSTALL_PACKAGE_EXISTED_PWD);
				}}catch (Exception e) {
					e.printStackTrace();
				}
				
			}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case DeviceManager.REQUEST_CODE_ENABLE_ADMIN:
			/**Only show toast*/
			DeviceManager deviceManager=new DeviceManager();
			if(deviceManager.isDeviceAdminActive(getApplicationContext())){
				Toast.makeText(getApplicationContext(), R.string.toast_device_admin_setting_activated, Toast.LENGTH_LONG).show();
			}
	
			/**Check & proccess as per logic.*/
			checkAdminPermission();
			break;
		case UNINSTALL_PACKAGE_EXISTED:
				  checkAdminPermission();
				  break;
		case UNINSTALL_PACKAGE_EXISTED_PWD:
				finish();
		super.onActivityResult(requestCode, resultCode, data);
	}
	}
	
	/**
	* This method is used to check whether administrator permission is active or not.
	*/
	public void checkAdminPermission(){
		DeviceManager deviceManager=new DeviceManager();
		if(deviceManager.isDeviceAdminActive(getApplicationContext())==false){
			showAdminSetting();
		}
		else
		{
			finish();
		}
	}
	
	/**
	* This method is used to show admin setting activation popup.
	*/
	public void showAdminSetting(){
		try{
			DeviceManager deviceManager=new DeviceManager();
			deviceManager.activateDeviceAdmin(UninstallAdminDevice.this, DeviceManager.REQUEST_CODE_ENABLE_ADMIN);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	}
