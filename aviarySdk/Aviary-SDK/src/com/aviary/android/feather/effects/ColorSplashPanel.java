package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.util.Iterator;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.headless.filters.NativeToolFilter.ColorSplashBrushMode;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.headless.moa.MoaStrokeParameter;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.ColorSplashFilter;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.library.utils.UIConfiguration;
import com.aviary.android.feather.widget.AviaryHighlightImageButton;
import com.aviary.android.feather.widget.AviaryHighlightImageButton.OnCheckedChangeListener;
import com.aviary.android.feather.widget.ImageViewSpotDraw;
import com.aviary.android.feather.widget.ImageViewSpotDraw.OnDrawListener;
import com.aviary.android.feather.widget.ImageViewSpotDraw.TouchMode;

/**
 * The Class SpotDrawPanel.
 */
public class ColorSplashPanel extends AbstractContentPanel implements OnDrawListener, OnCheckedChangeListener, OnClickListener {

	private AviaryHighlightImageButton mLensButton;
	private BackgroundDrawThread mBackgroundDrawThread;
	private ColorSplashFilter mFilter;
	private AviaryHighlightImageButton mSmart, mErase, mFree;
	private AviaryHighlightImageButton mCurrent;
	private View mDisabledStatusView;
	private ColorSplashBrushMode mBrushType = ColorSplashBrushMode.Free;

	static double BRUSH_MULTIPLIER = 2;

	MoaActionList mActions = MoaActionFactory.actionList();

	public ColorSplashPanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry );
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );
		ConfigService config = getContext().getService( ConfigService.class );

		int brushSize = config.getDimensionPixelSize( R.dimen.aviary_color_splash_brush_size );

		mLensButton = (AviaryHighlightImageButton) getContentView().findViewById( R.id.aviary_lens_button );
		mFree = (AviaryHighlightImageButton) getOptionView().findViewById( R.id.aviary_button1 );
		mSmart = (AviaryHighlightImageButton) getOptionView().findViewById( R.id.aviary_button2 );
		mErase = (AviaryHighlightImageButton) getOptionView().findViewById( R.id.aviary_button3 );

		mImageView = (ImageViewSpotDraw) getContentView().findViewById( R.id.image );
		( (ImageViewSpotDraw) mImageView ).setBrushSize( (int) ( brushSize * BRUSH_MULTIPLIER ) );
		( (ImageViewSpotDraw) mImageView ).setDrawLimit( 0.0 );
		( (ImageViewSpotDraw) mImageView ).setPaintEnabled( false );
		( (ImageViewSpotDraw) mImageView ).setDisplayType( DisplayType.FIT_IF_BIGGER );

		mDisabledStatusView = getOptionView().findViewById( R.id.aviary_disable_status );

		mPreview = Bitmap.createBitmap( mBitmap.getWidth(), mBitmap.getHeight(), Bitmap.Config.ARGB_8888 );
		mPreview.eraseColor( Color.BLACK );

		mBackgroundDrawThread = new BackgroundDrawThread( "filter-thread", Thread.NORM_PRIORITY );

		// initialize the filter
		mFilter = createFilter();

		if ( mFilter.init( mBitmap, mPreview ) == 0 ) {
			// Render the first time preview
			if ( mFilter.renderPreview() ) {
				@SuppressWarnings ( "unused" )
				Matrix current = getContext().getCurrentImageViewMatrix();
				mImageView.setImageBitmap( mPreview, null, -1, UIConfiguration.IMAGE_VIEW_MAX_ZOOM );
			} else {
				mLogger.error( "Failed to renderPreview" );
			}
		} else {
			mLogger.error( "Failed to initialize ColorSplashFilter" );
		}
	}

	@Override
	public void onActivate() {
		super.onActivate();

		mFree.setOnCheckedChangeListener( this );
		if ( mFree.isChecked() ) mCurrent = mFree;

		mSmart.setOnCheckedChangeListener( this );
		if ( mSmart.isChecked() ) mCurrent = mSmart;

		mErase.setOnCheckedChangeListener( this );
		if ( mErase.isChecked() ) mCurrent = mErase;

		( (ImageViewSpotDraw) mImageView ).setOnDrawStartListener( this );

		mBackgroundDrawThread.start();

		mLensButton.setOnClickListener( this );

		getContentView().setVisibility( View.VISIBLE );
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
	public void onClick( View v ) {
		final int id = v.getId();

		if ( id == mLensButton.getId() ) {
			setSelectedTool( ( (ImageViewSpotDraw) mImageView ).getDrawMode() == TouchMode.DRAW ? TouchMode.IMAGE : TouchMode.DRAW );
		}
	}

	@Override
	public void onCheckedChanged( AviaryHighlightImageButton buttonView, boolean isChecked, boolean fromUser ) {
		if ( mCurrent != null && !buttonView.equals( mCurrent ) ) {
			mCurrent.setChecked( false );
		}
		mCurrent = buttonView;

		if ( fromUser && isChecked ) {
			final int id = buttonView.getId();

			if ( id == mFree.getId() ) {
				mBrushType = ColorSplashBrushMode.Free;
				Tracker.recordTag( Filters.COLOR_SPLASH.name().toLowerCase( Locale.US ) + ": FreeBrushClicked" );
			} else if ( id == mSmart.getId() ) {
				mBrushType = ColorSplashBrushMode.Smart;
				Tracker.recordTag( Filters.COLOR_SPLASH.name().toLowerCase( Locale.US ) + ": SmartBrushClicked" );
			} else if ( id == mErase.getId() ) {
				mBrushType = ColorSplashBrushMode.Erase;
				Tracker.recordTag( Filters.COLOR_SPLASH.name().toLowerCase( Locale.US ) + ": EraserClicked" );
			}

			if ( ( (ImageViewSpotDraw) mImageView ).getDrawMode() != TouchMode.DRAW ) {
				setSelectedTool( TouchMode.DRAW );
			}
		}
	}

	private void setSelectedTool( TouchMode which ) {
		( (ImageViewSpotDraw) mImageView ).setDrawMode( which );
		mLensButton.setSelected( which == TouchMode.IMAGE );
		setPanelEnabled( which != TouchMode.IMAGE );
	}

	@Override
	public void onDeactivate() {

		mFree.setOnCheckedChangeListener( this );
		mSmart.setOnCheckedChangeListener( this );
		mErase.setOnCheckedChangeListener( this );
		mLensButton.setOnClickListener( null );

		( (ImageViewSpotDraw) mImageView ).setOnDrawStartListener( null );

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
	public void onDrawStart( float[] points, int radius ) {
		radius = Math.max( 1, radius );
		mBackgroundDrawThread.pathStart( (int) ( radius / BRUSH_MULTIPLIER ), points, mBrushType );
		setIsChanged( true );
	}

	@Override
	public void onDrawing( float[] points, int radius ) {
		mBackgroundDrawThread.lineTo( points );
	}

	@Override
	public void onDrawEnd() {
		mBackgroundDrawThread.pathEnd();
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

	/**
	 * Sets the panel enabled.
	 */
	public void setPanelEnabled( boolean value ) {

		if ( mOptionView != null ) {
			if ( value != mOptionView.isEnabled() ) {
				mOptionView.setEnabled( value );

				if ( value ) {
					getContext().restoreToolbarTitle();
				} else {
					getContext().setToolbarTitle( R.string.feather_zoom_mode );
				}

				mDisabledStatusView.setVisibility( value ? View.INVISIBLE : View.VISIBLE );
			}
		}
	}

	public boolean getPanelEnabled() {
		if ( mOptionView != null ) {
			return mOptionView.isEnabled();
		}
		return false;
	}

	@SuppressWarnings ( "unused" )
	private String printRect( Rect rect ) {
		return "( left=" + rect.left + ", top=" + rect.top + ", width=" + rect.width() + ", height=" + rect.height() + ")";
	}

	protected ColorSplashFilter createFilter() {
		return (ColorSplashFilter) FilterLoaderFactory.get( Filters.COLOR_SPLASH );
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.aviary_content_spot_draw, null );
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_colorsplash, parent, false );
	}

	static class DrawQueue extends LinkedBlockingQueue<float[]> {

		private static final long serialVersionUID = 1L;

		private ColorSplashBrushMode mode_;
		private int radius_;
		private volatile boolean completed_;

		public DrawQueue ( ColorSplashBrushMode ColorSplashBrushMode, int radius, float points[] ) {
			mode_ = ColorSplashBrushMode;
			radius_ = radius;
			completed_ = false;
			add( points );
		}

		public ColorSplashBrushMode getMode() {
			return mode_;
		}

		public int getRadius() {
			return radius_;
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
		final PointF mLastPoint;

		public BackgroundDrawThread ( String name, int priority ) {
			super( name );
			mQueue = new LinkedBlockingQueue<DrawQueue>();
			mLastPoint = new PointF();
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

		synchronized public void pathStart( int radius, float points[], ColorSplashBrushMode brushType ) {
			if ( !running ) return;

			if ( mCurrentQueue != null ) {
				mCurrentQueue.end();
				mCurrentQueue = null;
			}

			mLastPoint.set( points[0], points[1] );

			DrawQueue queue = new DrawQueue( brushType, radius, points );
			mQueue.add( queue );
			mCurrentQueue = queue;

			mLogger.log( "queue size: " + mQueue.size() );
		}

		synchronized public void pathEnd() {
			if ( !running || mCurrentQueue == null ) return;

			mCurrentQueue.end();
			mCurrentQueue = null;
		}

		public void lineTo( float values[] ) {
			if ( !running || mCurrentQueue == null ) return;

			float length = PointF.length( Math.abs( mLastPoint.x - values[0] ), Math.abs( mLastPoint.y - values[1] ) );

			if ( length > 10 ) {
				mLastPoint.set( values[0], values[1] );
				mCurrentQueue.add( values );
			} else {
				// mLogger.error( "skipping point, too close... " + length );
			}
		}

		public boolean isCompleted() {
			return mQueue.size() == 0;
		}

		public int getQueueSize() {
			return mQueue.size();
		}

		public void getLerp( PointF pt1, PointF pt2, float t, PointF dstPoint ) {
			dstPoint.set( pt1.x + ( pt2.x - pt1.x ) * t, pt1.y + ( pt2.y - pt1.y ) * t );
		}

		/**
		 * Clear the drawing queue
		 */
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
			boolean firstPoint = true;
			float points[];
			Rect drawRect = new Rect();
			PointF currentPoint = new PointF();
			PointF lastPoint = new PointF();
			PointF lerpPoint = new PointF();

			mLogger.log( "thread.start!" );

			MoaStrokeParameter strokeData;

			while ( running ) {

				if ( mQueue.size() > 0 && !isInterrupted() ) {

					mLogger.log( "queue.size: " + mQueue.size() );

					if ( !s ) {
						s = true;
						onProgressStart();
					}

					firstPoint = true;

					DrawQueue element = mQueue.element();

					if ( null == element ) {
						mQueue.poll();
						continue;
					}

					int radius = element.getRadius();
					ColorSplashBrushMode mode = element.getMode();

					strokeData = new MoaStrokeParameter( mode, radius );

					while ( element.size() > 0 || !element.isCompleted() ) {
						if ( !running || isInterrupted() ) break;

						points = element.poll();
						if ( points == null ) continue;

						currentPoint.set( points[0], points[1] );

						if ( firstPoint ) {
							firstPoint = false;
							drawRect.set( (int) ( points[0] - radius * BRUSH_MULTIPLIER ), (int) ( points[1] - radius * BRUSH_MULTIPLIER ),
									(int) ( points[0] + radius * BRUSH_MULTIPLIER ), (int) ( points[1] + radius * BRUSH_MULTIPLIER ) );

							strokeData.addPoint( points );

							mFilter.setColorSplashMode( mode );
							mFilter.drawStart( drawRect.centerX(), drawRect.centerY(), radius, 0 );
							mFilter.renderPreview( drawRect );
							mImageView.postInvalidate();

						} else {

							float x = Math.abs( currentPoint.x - lastPoint.x );
							float y = Math.abs( currentPoint.y - lastPoint.y );
							double length = Math.sqrt( x * x + y * y );
							double currentPosition = 0;
							float lerp;

							while ( currentPosition < length ) {
								lerp = (float) ( currentPosition / length );
								getLerp( currentPoint, lastPoint, lerp, lerpPoint );
								currentPosition += radius;

								drawRect.set( (int) ( lerpPoint.x - radius * BRUSH_MULTIPLIER ), (int) ( lerpPoint.y - radius * BRUSH_MULTIPLIER ),
										(int) ( lerpPoint.x + radius * BRUSH_MULTIPLIER ), (int) ( lerpPoint.y + radius * BRUSH_MULTIPLIER ) );

								strokeData.addPoint( lerpPoint.x, lerpPoint.y );

								mFilter.colorsplash_draw( drawRect.centerX(), drawRect.centerY() );
								mFilter.renderPreview( drawRect );
								mImageView.postInvalidate();

								if ( !running || isInterrupted() ) break;
							}
						}

						lastPoint.set( currentPoint );
					}

					// now remove the element from the queue
					mFilter.addStrokeData( strokeData );
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
