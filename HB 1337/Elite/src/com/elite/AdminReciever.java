package com.elite;

import android.annotation.SuppressLint;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class is used to recieve device admin activation.
 * @author <b>Elite</b>
 *
 */
public class AdminReciever extends DeviceAdminReceiver{
	
	@Override
	public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
	}

	@Override
	public CharSequence onDisableRequested(Context context, Intent intent) {
		return super.onDisableRequested(context, intent);
	}

	@Override
	public void onDisabled(Context context, Intent intent) {
		super.onDisabled(context, intent);
	}

	@Override
	public void onEnabled(Context context, Intent intent) {
		super.onEnabled(context, intent);
	}

	@Override
	public void onPasswordChanged(Context context, Intent intent) {
		super.onPasswordChanged(context, intent);
	}

	@SuppressLint("NewApi")
	@Override
	public void onPasswordExpiring(Context context, Intent intent) {
		super.onPasswordExpiring(context, intent);
	}

	@Override
	public void onPasswordFailed(Context context, Intent intent) {
		super.onPasswordFailed(context, intent);
	}

	@Override
	public void onPasswordSucceeded(Context context, Intent intent) {
		super.onPasswordSucceeded(context, intent);
	}
	
}
