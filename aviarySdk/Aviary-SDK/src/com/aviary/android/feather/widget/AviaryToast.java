package com.aviary.android.feather.widget;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Custom {@link Toast} with arbitrary duration
 * 
 * @author alessandro
 */
public class AviaryToast {

	final String LOG_TAG = "toast";

	Context mContext;

	WindowManager mWindowManager;

	View mNextView;

	View mView;

	int mDuration;

	int mGravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;

	int mX, mY;

	final TN mTN;

	float mHorizontalMargin;

	float mVerticalMargin;

	final Handler mHandler = new Handler();

	public static interface LayoutListener {

		public void onShown( View currentView );

		public void onHidden();
	}

	private LayoutListener mLayoutListener;

	public AviaryToast ( Context context ) {
		mContext = context;
		mWindowManager = (WindowManager) context.getSystemService( Context.WINDOW_SERVICE );

		mTN = new TN();
		mTN.mWm = mWindowManager;
		mY = 0;
		mX = 0;
	}

	public void setLayoutListener( LayoutListener listener ) {
		mLayoutListener = listener;
	}

	public void setView( View v ) {
		mNextView = v;
	}

	public View getView() {
		return mNextView;
	}

	public void show() {
		if ( mNextView == null ) throw new RuntimeException( "setView must be called first" );
		mTN.show();
	}

	public void hide() {
		mTN.hide();
	}

	public void update() {}

	/**
	 * Create a new AviaryToast
	 * 
	 * @param context
	 *            the current context
	 * @param resid
	 *            the view resource id
	 * @param duration
	 *            the toast duration ( -1 for infinite duration )
	 * @return
	 */
	public static AviaryToast make( Context context, int resid, int duration ) {
		AviaryToast result = new AviaryToast( context );
		LayoutInflater inflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		View v = inflater.inflate( resid, null );
		result.mNextView = v;
		result.mDuration = duration;
		return result;
	}

	private class TN {

		final Runnable mShow = new Runnable() {

			@Override
			public void run() {
				handleShow();
			}
		};

		final Runnable mHide = new Runnable() {

			@Override
			public void run() {
				handleHide();
			}
		};

		WindowManager.LayoutParams mParams = new WindowManager.LayoutParams();

		WindowManager mWm;

		TN () {
			final WindowManager.LayoutParams params = mParams;
			params.height = WindowManager.LayoutParams.WRAP_CONTENT;
			params.width = WindowManager.LayoutParams.WRAP_CONTENT;
			params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
					| WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
			params.format = PixelFormat.TRANSLUCENT;
			params.type = WindowManager.LayoutParams.TYPE_TOAST;
			params.windowAnimations = com.aviary.android.feather.R.style.AviaryAnimations_AviaryToast;
			params.setTitle( "Toast" );
		}

		public void show() {
			mHandler.post( mShow );
		}

		public void hide() {
			mHandler.post( mHide );
		}

		public void handleShow() {

			if ( mView != mNextView ) {
				handleHide();
				mView = mNextView;
				final int gravity = mGravity;
				mParams.gravity = gravity;
				if ( ( gravity & Gravity.HORIZONTAL_GRAVITY_MASK ) == Gravity.FILL_HORIZONTAL ) {
					mParams.horizontalWeight = 1.0f;
				}
				if ( ( gravity & Gravity.VERTICAL_GRAVITY_MASK ) == Gravity.FILL_VERTICAL ) {
					mParams.verticalWeight = 1.0f;
				}
				mParams.x = mX;
				mParams.y = mY;
				mParams.verticalMargin = mVerticalMargin;
				mParams.horizontalMargin = mHorizontalMargin;

				if ( mView.getParent() != null ) {
					mView.setVisibility( View.GONE );
					mWm.removeView( mView );
				}

				mWm.addView( mView, mParams );
				mView.setVisibility( View.VISIBLE );

				if ( mLayoutListener != null ) {
					mLayoutListener.onShown( mView );
				}
			}
		}

		public void handleHide() {
			removeView();
			if ( mLayoutListener != null ) {
				mLayoutListener.onHidden();
			}
		}

		void removeView() {
			if ( mView != null ) {
				if ( mView.getParent() != null ) {
					mView.setVisibility( View.GONE );
					mWm.removeView( mView );
				}
				mView = null;
			}
		}
	};
}
