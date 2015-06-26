package com.aviary.android.feather.widget;

public interface VibrationWidget {

	/**
	 * Enable the vibration feedback
	 * 
	 * @param value
	 */
	public void setVibrationEnabled( boolean value );

	/**
	 * Get the vibration feedback enabled status
	 * 
	 * @return
	 */
	public boolean getVibrationEnabled();
}
