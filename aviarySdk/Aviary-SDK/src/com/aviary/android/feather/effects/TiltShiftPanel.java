package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnDrawableChangeListener;

import java.util.Iterator;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.headless.filters.NativeToolFilter.TiltShiftMode;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.filters.TiltShiftFilter;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.UIConfiguration;
import com.aviary.android.feather.widget.AviaryHighlightImageButton;
import com.aviary.android.feather.widget.AviaryHighlightImageButton.OnCheckedChangeListener;
import com.aviary.android.feather.widget.ImageViewTiltiShiftTouch;
import com.aviary.android.feather.widget.ImageViewTiltiShiftTouch.OnTiltShiftDrawListener;
import com.aviary.android.feather.widget.ImageViewTiltiShiftTouch.TiltShiftDrawMode;

/**
 * The Class SpotDrawPanel.
 */
public class TiltShiftPanel extends AbstractContentPanel implements OnTiltShiftDrawListener, OnDrawableChangeListener, OnCheckedChangeListener {

	private BackgroundDrawThread mBackgroundDrawThread;
	private TiltShiftFilter mFilter;
	private AviaryHighlightImageButton mCircleButton;
	private AviaryHighlightImageButton mRectButton;

	static float BRUSH_MULTIPLIER = 2f;

	MoaActionList mActions = MoaActionFactory.actionList();
	TiltShiftMode mTiltShiftMode;

	public TiltShiftPanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry );
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		mRectButton = (AviaryHighlightImageButton) getOptionView().findViewById( R.id.aviary_button_rectangle );
		mCircleButton = (AviaryHighlightImageButton) getOptionView().findViewById( R.id.aviary_button_circle );
		mImageView = (ImageViewTiltiShiftTouch) getContentView().findViewById( R.id.aviary_image );

		mPreview = Bitmap.createBitmap( mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888 );
		mPreview.eraseColor( Color.BLACK );

		final ImageViewTiltiShiftTouch image = (ImageViewTiltiShiftTouch) mImageView;
		image.setOnDrawableChangedListener( this );
		image.setDisplayType( DisplayType.FIT_IF_BIGGER );
		// image.setPointWaveEnabled( true );

		mCircleButton.setOnCheckedChangeListener( this );
		mRectButton.setOnCheckedChangeListener( this );

		// create the background thread for drawing chanched
		mBackgroundDrawThread = new BackgroundDrawThread( "filter-thread", Thread.NORM_PRIORITY );

		// initialize the filter
		mFilter = createFilter();
	}

	@Override
	public void onActivate() {
		super.onActivate();

		mPreview = BitmapUtils.copy( mBitmap, Config.ARGB_8888 );
		mBackgroundDrawThread.start();

		( (ImageViewTiltiShiftTouch) mImageView ).setOnDrawStartListener( this );
		mImageView.setDisplayType( DisplayType.FIT_IF_BIGGER );
		mImageView.setImageBitmap( mPreview, null, ImageViewTouchBase.ZOOM_INVALID, UIConfiguration.IMAGE_VIEW_MAX_ZOOM );
		contentReady();
	}

	@Override
	protected void onDispose() {
		mContentReadyListener = null;

		// dispose the filter
		mFilter.dispose();
		super.onDispose();
	}

	@Override
	public void onCheckedChanged( AviaryHighlightImageButton button, boolean isChecked, boolean fromUser ) {

		mLogger.info( "onCheckedChanged: " + isChecked + ", fromUser: " + fromUser );

		final int id = button.getId();

		// button already checked, skip
		if ( !isChecked ) {
			return;
		}

		if ( id == mRectButton.getId() ) {
			mLogger.log( "rect" );
			mTiltShiftMode = TiltShiftMode.Linear;
			mCircleButton.setChecked( false );
		} else if ( id == mCircleButton.getId() ) {
			mLogger.log( "circle" );
			mTiltShiftMode = TiltShiftMode.Radial;
			mRectButton.setChecked( false );
		}

		if ( !fromUser ) {
			mLogger.log( "return" );
			return;
		}

		if ( mTiltShiftMode == TiltShiftMode.Radial ) {
			( (ImageViewTiltiShiftTouch) mImageView ).setTiltShiftDrawMode( TiltShiftDrawMode.Radial );
			Tracker.recordTag( Filters.TILT_SHIFT.name().toLowerCase( Locale.US ) + ": CircleClicked" );
		} else if ( mTiltShiftMode == TiltShiftMode.Linear ) {
			( (ImageViewTiltiShiftTouch) mImageView ).setTiltShiftDrawMode( TiltShiftDrawMode.Linear );
			Tracker.recordTag( Filters.TILT_SHIFT.name().toLowerCase( Locale.US ) + ": RectangleClicked" );
		}
	}

	@Override
	public void onDeactivate() {

		mImageView.setOnDrawableChangedListener( null );
		mCircleButton.setOnCheckedChangeListener( this );
		mRectButton.setOnCheckedChangeListener( this );

		( (ImageViewTiltiShiftTouch) mImageView ).setOnDrawStartListener( null );

		if ( mBackgroundDrawThread != null ) {

			mBackgroundDrawThread.clear();

			if ( mBackgroundDrawThread.isAlive() ) {
				mBackgroundDrawThread.quit();
				while ( mBackgroundDrawThread.isAlive() ) {
					mLogger.log( "isAlive..." );
					// wait...
				}
			}
		}
		onProgressEnd();
		super.onDeactivate();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mBackgroundDrawThread = null;
		mImageView.clear();
	}

	@Override
	public void onCancelled() {
		super.onCancelled();
	}

	@Override
	public void onDrawStart( float[] points, TiltShiftDrawMode mode, float radius, float angle, float left, float top, float right, float bottom ) {
		mBackgroundDrawThread.drawStart( points, mode, radius, angle, left, top, right, bottom );
		setIsChanged( true );
	}

	@Override
	public void onDrawing( float[] points, float radius, float angle, float left, float top, float right, float bottom ) {
		mBackgroundDrawThread.draw( points, radius, angle, left, top, right, bottom );
	}

	@Override
	public void onDrawEnd() {
		mBackgroundDrawThread.pathEnd();
	}

	@Override
	public void onDrawableChanged( Drawable drawable ) {

		mLogger.info( "onDrawableChanged: " + drawable );

		if ( mCircleButton.isChecked() ) {
			mTiltShiftMode = TiltShiftMode.Radial;
		} else if ( mRectButton.isChecked() ) {
			mTiltShiftMode = TiltShiftMode.Linear;
		}

		getHandler().postDelayed( new Runnable() {

			@Override
			public void run() {
				ImageViewTiltiShiftTouch image = (ImageViewTiltiShiftTouch) mImageView;

				if ( mTiltShiftMode == TiltShiftMode.Radial ) {
					mCircleButton.setChecked( true );
					image.setTiltShiftDrawMode( TiltShiftDrawMode.Radial );
				} else {
					mRectButton.setChecked( true );
					image.setTiltShiftDrawMode( TiltShiftDrawMode.Linear );
				}
			}
		}, 500 );
	}

	@Override
	protected void onGenerateResult() {
		if ( mBackgroundDrawThread.isAlive() && !mBackgroundDrawThread.isCompleted() ) {
			mBackgroundDrawThread.finish();
			GenerateResultTask task = new GenerateResultTask();
			task.execute();
		} else {
			onComplete( mPreview, mFilter.getActions() );
		}
	}

	@Override
	protected void onComplete( Bitmap bitmap, MoaActionList actions ) {

		if ( mTiltShiftMode == TiltShiftMode.Radial ) mTrackingAttributes.put( "shape", "Circle" );
		else mTrackingAttributes.put( "shape", "Rectangle" );

		super.onComplete( bitmap, actions );
	}

	@SuppressWarnings ( "unused" )
	private String printRect( Rect rect ) {
		return "( left=" + rect.left + ", top=" + rect.top + ", width=" + rect.width() + ", height=" + rect.height() + ")";
	}

	protected TiltShiftFilter createFilter() {
		return (TiltShiftFilter) FilterLoaderFactory.get( Filters.TILT_SHIFT );
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.aviary_content_focus, null );
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_focus, parent, false );
	}

	static class DrawQueue extends LinkedBlockingQueue<float[]> {

		private static final long serialVersionUID = 1L;

		private TiltShiftMode mode_;
		private volatile boolean completed_;

		public DrawQueue ( TiltShiftMode tiltShiftMode ) {
			mode_ = tiltShiftMode;
			completed_ = false;
		}

		public TiltShiftMode getMode() {
			return mode_;
		}

		public void end() {
			completed_ = true;
		}

		public boolean isCompleted() {
			return completed_;
		}
	}

	/**
	 * background thread
	 */
	class BackgroundDrawThread extends Thread {

		boolean started;
		volatile boolean running;
		final Queue<DrawQueue> mQueue;
		DrawQueue mCurrentQueue;

		public BackgroundDrawThread ( String name, int priority ) {
			super( name );
			mQueue = new LinkedBlockingQueue<DrawQueue>();
			setPriority( priority );
			init();
		}

		void init() {}

		@Override
		synchronized public void start() {
			started = true;
			running = true;
			super.start();
		}

		synchronized public void quit() {
			started = true;
			running = false;
			interrupt();
		};

		synchronized public void drawStart( float points[], TiltShiftDrawMode type, float radius, float angle, float left, float top, float right, float bottom ) {
			if ( !running ) return;

			if ( mCurrentQueue != null ) {
				mCurrentQueue.end();
				mCurrentQueue = null;
			}

			DrawQueue queue = new DrawQueue( type == TiltShiftDrawMode.Radial ? TiltShiftMode.Radial : TiltShiftMode.Linear );
			queue.add( new float[] { points[0], points[1], radius, angle, left, top, right, bottom } );

			mQueue.add( queue );
			mCurrentQueue = queue;
		}

		synchronized public void draw( float points[], float radius, float angle, float left, float top, float right, float bottom ) {
			if ( !running || mCurrentQueue == null ) return;

			mCurrentQueue.clear();
			mCurrentQueue.add( new float[] { points[0], points[1], radius, angle, left, top, right, bottom } );
		}

		synchronized public void pathEnd() {
			if ( !running || mCurrentQueue == null ) return;
			mCurrentQueue.end();
			mCurrentQueue = null;
		}

		public boolean isCompleted() {
			return mQueue.size() == 0;
		}

		public int getQueueSize() {
			return mQueue.size();
		}

		public void clear() {

			if ( running && mQueue != null ) {

				synchronized ( mQueue ) {
					while ( mQueue.size() > 0 ) {
						DrawQueue element = mQueue.poll();
						if ( null != element ) {
							mLogger.log( "end element..." );
							element.end();
						}
					}
				}
			}
		}

		/**
		 * Ensure the drawing queue is completed
		 */
		public void finish() {

			if ( running && mQueue != null ) {
				synchronized ( mQueue ) {
					Iterator<DrawQueue> iterator = mQueue.iterator();
					while ( iterator.hasNext() ) {
						DrawQueue element = iterator.next();
						if ( null != element ) {
							mLogger.log( "end element..." );
							element.end();
						}
					}
				}
			}
		}

		@Override
		public void run() {

			while ( !started ) {
				// wait until is not yet started
			}

			boolean s = false;

			float points[];
			float x, y, radius, angle;
			float left, top, right, bottom;

			mLogger.log( "thread.start!" );
			mLogger.log( "filter.init" );

			mFilter.init( mBitmap, mPreview );

			RectF invalidateRect = new RectF( 0, 0, mPreview.getWidth(), mPreview.getHeight() );
			RectF tempRect = new RectF();

			while ( running ) {

				if ( mQueue.size() > 0 && !isInterrupted() ) {

					mLogger.log( "queue.size: " + mQueue.size() );

					if ( !s ) {
						s = true;
						onProgressStart();
					}

					DrawQueue element = mQueue.element();

					if ( null == element ) {
						mQueue.poll();
						continue;
					}

					TiltShiftMode mode = element.mode_; // tilt shift mode
					mFilter.setTiltShiftMode( mode );

					while ( element.size() > 0 || !element.isCompleted() ) {
						if ( !running || isInterrupted() ) break;

						points = element.poll();
						if ( points == null ) continue;

						x = points[0];
						y = points[1];
						radius = points[2];
						angle = points[3];

						left = points[4];
						top = points[5];
						right = points[6];
						bottom = points[7];

						mFilter.tiltshift_draw( x, y, radius, angle );

						tempRect.set( left, top, right, bottom );

						// Log.d( getName(), "--------" );
						// Log.d( getName(), "previous rect: " + invalidateRect );
						// Log.d( getName(), "current  rect: " + tempRect );

						invalidateRect.union( tempRect );

						// Log.d( getName(), "union    rect: " + invalidateRect );

						mFilter.renderPreview( invalidateRect );

						invalidateRect.set( tempRect );

						mImageView.postInvalidate();

					}

					// now remove the element from the queue
					mQueue.poll();

				} else {
					if ( s ) {
						onProgressEnd();
						s = false;
					}
				}
			}

			onProgressEnd();
			mLogger.log( "thread.end" );
		};
	};

	/**
	 * GenerateResultTask is used when the background draw operation is still running.
	 * Just wait until the draw operation completed.
	 */
	class GenerateResultTask extends AviaryAsyncTask<Void, Void, Void> {

		/** The m progress. */
		ProgressDialog mProgress = new ProgressDialog( getContext().getBaseContext() );

		@Override
		protected void PreExecute() {
			mProgress.setTitle( getContext().getBaseContext().getString( R.string.feather_loading_title ) );
			mProgress.setMessage( getContext().getBaseContext().getString( R.string.feather_effect_loading_message ) );
			mProgress.setIndeterminate( true );
			mProgress.setCancelable( false );
			mProgress.show();
		}

		@Override
		protected Void doInBackground( Void... params ) {

			if ( mBackgroundDrawThread != null ) {

				while ( mBackgroundDrawThread != null && !mBackgroundDrawThread.isCompleted() ) {
					mLogger.log( "waiting.... " + mBackgroundDrawThread.getQueueSize() );
					try {
						Thread.sleep( 50 );
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
				}
			}

			mActions.add( mFilter.getActions().get( 0 ) );
			return null;
		}

		@Override
		protected void PostExecute( Void result ) {
			if ( getContext().getBaseActivity().isFinishing() ) return;

			if ( mProgress.isShowing() ) {
				try {
					mProgress.dismiss();
				} catch ( IllegalArgumentException e ) {
				}
			}

			onComplete( mPreview, mActions );
		}
	}
}
