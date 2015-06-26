package com.aviary.android.feather.utils;

import android.app.ProgressDialog;
import android.os.Handler;

import com.aviary.android.feather.library.MonitoredActivity;

/**
 * Some thread related utilities.
 * 
 * @author alessandro
 */
public class ThreadUtils {

	/**
	 * Start background job.
	 * 
	 * @param activity
	 *            the activity
	 * @param title
	 *            the title
	 * @param message
	 *            the message
	 * @param job
	 *            the job
	 * @param handler
	 *            the handler
	 */
	public static void startBackgroundJob( MonitoredActivity activity, String title, String message, Runnable job, Handler handler ) {
		ProgressDialog dialog = ProgressDialog.show( activity, title, message, true, false );
		new Thread( new BackgroundJob( activity, job, dialog, handler ) ).start();
	}

	/**
	 * The Class BackgroundJob.
	 */
	private static class BackgroundJob extends MonitoredActivity.LifeCycleAdapter implements Runnable {

		private final MonitoredActivity mActivity;

		private final ProgressDialog mDialog;

		private final Runnable mJob;

		private final Handler mHandler;

		private final Runnable mCleanupRunner = new Runnable() {

			@Override
			public void run() {
				mActivity.removeLifeCycleListener( BackgroundJob.this );
				if ( mDialog.getWindow() != null ) mDialog.dismiss();
			}
		};

		/**
		 * Instantiates a new background job.
		 * 
		 * @param activity
		 *            the activity
		 * @param job
		 *            the job
		 * @param dialog
		 *            the dialog
		 * @param handler
		 *            the handler
		 */
		public BackgroundJob ( MonitoredActivity activity, Runnable job, ProgressDialog dialog, Handler handler ) {
			mActivity = activity;
			mDialog = dialog;
			mJob = job;
			mActivity.addLifeCycleListener( this );
			mHandler = handler;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			try {
				mJob.run();
			} finally {
				mHandler.post( mCleanupRunner );
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.aviary.android.feather.MonitoredActivity.LifeCycleAdapter#onActivityDestroyed
		 * (com.aviary.android.feather.MonitoredActivity
		 * )
		 */
		@Override
		public void onActivityDestroyed( MonitoredActivity activity ) {
			mCleanupRunner.run();
			mHandler.removeCallbacks( mCleanupRunner );
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.aviary.android.feather.MonitoredActivity.LifeCycleAdapter#onActivityStopped(
		 * com.aviary.android.feather.MonitoredActivity
		 * )
		 */
		@Override
		public void onActivityStopped( MonitoredActivity activity ) {
			mDialog.hide();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.aviary.android.feather.MonitoredActivity.LifeCycleAdapter#onActivityStarted(
		 * com.aviary.android.feather.MonitoredActivity
		 * )
		 */
		@Override
		public void onActivityStarted( MonitoredActivity activity ) {
			mDialog.show();
		}
	}

}
