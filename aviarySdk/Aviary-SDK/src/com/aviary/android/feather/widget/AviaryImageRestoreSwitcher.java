package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.widget.ViewSwitcher;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;

public class AviaryImageRestoreSwitcher extends ViewSwitcher {

	static Logger logger = LoggerFactory.getLogger( "AviaryImageRestoreSwitcher", LoggerType.ConsoleLoggerType );

	static final int DEFAULT_RESTORE_TIMEOUT = 2000;

	public static enum RestoreState {
		None, Begin, Applied_Begin, Applied_End,
	};

	public static interface OnRestoreStateListener {
		public boolean onRestoreBegin();

		public void onRestoreChanged();

		public void onRestoreEnd();
	}

	private boolean restoreEnabled = true;
	private RestoreState status = RestoreState.None;
	private OnRestoreStateListener mRestoreListener;

	OnGestureListener mGestureListener = new MyGestureListener();
	OnScaleGestureListener mScaleGestureListener = new MyScaleGestureListener();

	private ScaleGestureDetector mScaleGestureDetector;
	private GestureDetector mGestureDetector;

	private ImageViewTouch mDefaultView;
	private ImageViewTouch mRestoredView;

	private int mRestoreTimeout = DEFAULT_RESTORE_TIMEOUT;

	public AviaryImageRestoreSwitcher ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryImageRestoreViewStyle );
	}

	public AviaryImageRestoreSwitcher ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs );

		Theme theme = context.getTheme();
		TypedArray array = theme.obtainStyledAttributes( attrs, R.styleable.AviaryImageRestoreSwitcher, defStyle, 0 );

		mRestoreTimeout = array.getInt( R.styleable.AviaryImageRestoreSwitcher_aviary_restoreTimeout, DEFAULT_RESTORE_TIMEOUT );

		array.recycle();

		logger.info( "timeout: " + mRestoreTimeout );
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		logger.info( "onFinishInflate: " + getChildCount() );

		mDefaultView = (ImageViewTouch) getChildAt( 0 );
		mRestoredView = (ImageViewTouch) getChildAt( 1 );

		mRestoredView.setDoubleTapEnabled( false );
		mRestoredView.setScaleEnabled( false );
		mRestoredView.setScrollEnabled( false );
		mRestoredView.setLongClickable( false );
	}

	public void setOnRestoreStateListener( OnRestoreStateListener listener ) {
		mRestoreListener = listener;
	}

	public void clearStatus() {
		logger.info( "clearStatus" );

		if ( status != RestoreState.None ) {
			status = RestoreState.None;

			getHandler().removeCallbacks( mCancelStatusRunnable );

			if ( mRestoreListener != null ) mRestoreListener.onRestoreEnd();
		}
	}

	public void setRestoreEnabled( boolean value ) {
		restoreEnabled = value;

		if ( restoreEnabled ) {
			status = RestoreState.None;
			mScaleGestureDetector = new ScaleGestureDetector( getContext(), mScaleGestureListener );
			mGestureDetector = new GestureDetector( getContext(), mGestureListener, null, false );
			mGestureDetector.setIsLongpressEnabled( true );
		} else {
			status = RestoreState.None;
			mScaleGestureDetector = null;
			mGestureDetector = null;
		}
	}

	public boolean getRestoreEnabled() {
		return restoreEnabled;
	}

	@Override
	public void setDisplayedChild( int whichChild ) {
		super.setDisplayedChild( whichChild );
	}

	protected boolean isRestoreEnabled() {
		return restoreEnabled && getDisplayedChild() == 0;
	}

	private boolean isValidScale() {
		return mDefaultView.getScale() == mDefaultView.getMinScale();
	}

	public RestoreState getStatus() {
		return status;
	}

	private boolean setStatus( RestoreState newState ) {
		if ( !restoreEnabled ) {
			return false;
		}

		logger.info( "setStatus. from: " + status + " to " + newState );

		boolean handled = false;

		switch ( newState ) {
			case Begin:
				if ( !isRestoreEnabled() || !isValidScale() || getVisibility() != View.VISIBLE ) {
					return false;
				}

				if ( status == RestoreState.None ) {
					if ( null != mRestoreListener ) {
						handled = mRestoreListener.onRestoreBegin();
					} else {
						handled = true;
					}
				}
				break;

			case Applied_Begin:
				if ( status == RestoreState.Begin ) {
					if ( null != mRestoreListener ) mRestoreListener.onRestoreChanged();
					handled = true;
				}
				break;

			case Applied_End:
				if ( status == RestoreState.Applied_Begin ) {
					handled = true;
				}
				break;

			case None:
				if ( status == RestoreState.Begin ) {
					if ( null != mRestoreListener ) mRestoreListener.onRestoreEnd();
					handled = true;
				} else if ( status == RestoreState.Applied_End ) {
					if ( null != mRestoreListener ) mRestoreListener.onRestoreEnd();
					handled = true;
				}
				break;

			default:
				break;
		}

		if ( !handled ) {
			logger.error( "setStatus. from: " + status + " to " + newState );
		} else {
			status = newState;
		}

		return handled;
	}

	@Override
	public boolean dispatchTouchEvent( MotionEvent ev ) {

		if ( restoreEnabled ) {
			boolean handled;

			if ( getDisplayedChild() == 0 ) {
				handled = mScaleGestureDetector.onTouchEvent( ev );

				if ( handled ) {
					if ( !mScaleGestureDetector.isInProgress() ) {
						handled = mGestureDetector.onTouchEvent( ev );
					}
				}
				logger.log( "handled: " + handled );
			}

			int action = ev.getAction();
			switch ( action & MotionEvent.ACTION_MASK ) {
				case MotionEvent.ACTION_UP:
					handled = onUp( ev );
					break;
			}
		}

		if ( status == RestoreState.None ) {
			return super.dispatchTouchEvent( ev );
		}

		return true;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		return restoreEnabled;
	}

	private boolean onUp( MotionEvent event ) {
		logger.info( "onUp" );

		if ( restoreEnabled && null != getHandler() ) {
			if ( status == RestoreState.Begin ) {
				return setStatus( RestoreState.None );
			} else if ( status == RestoreState.Applied_Begin ) {

				if ( setStatus( RestoreState.Applied_End ) ) {
					getHandler().removeCallbacks( mCancelStatusRunnable );
					getHandler().postDelayed( mCancelStatusRunnable, mRestoreTimeout );
				}

				return true;
			}
		}
		return false;
	}

	public ImageViewTouch getDefaultImageView() {
		return mDefaultView;
	}

	public ImageViewTouch getRestoredImageView() {
		return mRestoredView;
	}

	class MyGestureListener implements OnGestureListener {

		@Override
		public boolean onDown( MotionEvent e ) {
			logger.info( "onDown" );
			return true;
		}

		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
			logger.info( "onFling" );
			return false;
		}

		@Override
		public void onLongPress( MotionEvent e ) {
			logger.info( "onLongPress" );
			setStatus( RestoreState.Applied_Begin );
		}

		@Override
		public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
			logger.info( "onScroll" );
			return false;
		}

		@Override
		public void onShowPress( MotionEvent e ) {
			logger.info( "onShowPress" );
			setStatus( RestoreState.Begin );
		}

		@Override
		public boolean onSingleTapUp( MotionEvent e ) {
			logger.info( "onSingleTapUp" );
			return false;
		}
	}

	class MyScaleGestureListener implements OnScaleGestureListener {

		@Override
		public boolean onScale( ScaleGestureDetector detector ) {
			logger.info( "onScale" );
			return false;
		}

		@Override
		public boolean onScaleBegin( ScaleGestureDetector detector ) {
			logger.info( "onScaleBegin" );
			return false;
		}

		@Override
		public void onScaleEnd( ScaleGestureDetector detector ) {
			logger.info( "onScaleEnd" );
		}
	}

	class CancelStatusRunnable implements Runnable {
		@Override
		public void run() {
			logger.info( "CancelStatusRunnable::run" );
			setStatus( RestoreState.None );
		}
	}

	private CancelStatusRunnable mCancelStatusRunnable = new CancelStatusRunnable();
}
