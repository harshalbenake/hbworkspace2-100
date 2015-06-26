package com.elite;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.Html;

/**
 * This class is used for device admin activation manager.
 * @author <b>Elite</b>
 *
 */
public class DeviceManager {
	public static final int REQUEST_CODE_ENABLE_ADMIN=1000;
	public static boolean isActive=false;
	
	/**
	 * This method is used to call intent which enabled device admin app. 
	 */
	public void activateDeviceAdmin(Activity activity, int resultCode){
		try {
			ComponentName comp = new ComponentName(activity, AdminReciever.class);
			Intent intent=new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
			intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp);
			intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, Html.fromHtml(activity.getResources().getString(R.string.device_admin_manager_message)));
			intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
			activity.startActivityForResult(intent, resultCode);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is used to call intent which disabled device admin app. 
	 */
	public void deactivateDeviceAdmin(Context context){
		try {
			ComponentName comp = new ComponentName(context, AdminReciever.class);
			DevicePolicyManager devicePolicyManager=(DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
			devicePolicyManager.removeActiveAdmin(comp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method is used to check whether device admin is active or not.
	 * @return true if active else false 
	 */
	public boolean isDeviceAdminActive(Context context){
		boolean flag=false;
		try {
			DevicePolicyManager devicePolicyManager=(DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
			AdminReciever adminReciver=new AdminReciever();
			flag=devicePolicyManager.isAdminActive(adminReciver.getWho(context));
			isActive=flag;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flag;
	}
}