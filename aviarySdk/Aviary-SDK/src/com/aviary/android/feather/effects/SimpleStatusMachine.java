package com.aviary.android.feather.effects;

public class SimpleStatusMachine {

	public static int INVALID_STATUS = -1;

	private int currentStatus = INVALID_STATUS;
	private int previousStatus = INVALID_STATUS;

	private OnStatusChangeListener mStatusListener;

	public void setOnStatusChangeListener( OnStatusChangeListener listener ) {
		mStatusListener = listener;
	}

	public void setStatus( int newStatus ) {
		if ( newStatus != currentStatus ) {
			previousStatus = currentStatus;
			currentStatus = newStatus;

			if ( null != mStatusListener ) {
				mStatusListener.OnStatusChanged( previousStatus, currentStatus );
			}
		} else {
			if ( null != mStatusListener ) {
				mStatusListener.OnStatusUpdated( newStatus );
			}
		}
	}

	public int getCurrentStatus() {
		return currentStatus;
	}

	public static interface OnStatusChangeListener {

		public void OnStatusChanged( int oldStatus, int newStatus );

		public void OnStatusUpdated( int status );
	}
}
