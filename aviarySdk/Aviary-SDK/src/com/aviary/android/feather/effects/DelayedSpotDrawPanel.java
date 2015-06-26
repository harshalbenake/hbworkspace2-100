package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.FloatMath;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.SystemUtils;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.graphics.PreviewSpotDrawable;
import com.aviary.android.feather.headless.filters.IFilter;
import com.aviary.android.feather.headless.moa.Moa;
import com.aviary.android.feather.headless.moa.MoaAction;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.filters.SpotBrushFilter;
import com.aviary.android.feather.library.graphics.FlattenPath;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.UIConfiguration;
import com.aviary.android.feather.utils.UIUtils;
import com.aviary.android.feather.widget.AviaryAdapterView;
import com.aviary.android.feather.widget.AviaryGallery;
import com.aviary.android.feather.widget.AviaryGallery.OnItemsScrollListener;
import com.aviary.android.feather.widget.AviaryHighlightImageButton;
import com.aviary.android.feather.widget.ImageViewSpotDraw;
import com.aviary.android.feather.widget.ImageViewSpotDraw.OnDrawListener;
import com.aviary.android.feather.widget.ImageViewSpotDraw.TouchMode;

public class DelayedSpotDrawPanel extends AbstractContentPanel implements OnDrawListener, OnClickListener, OnItemsScrollListener {

	protected int mBrushSize;
	protected Filters mFilterType;
	protected AviaryGallery mGallery;
	protected int[] mBrushSizes;
	protected int mSelectedPosition = -1;
	protected AviaryHighlightImageButton mLensButton;
	private MyHandlerThread mBackgroundDrawThread;
	private View mDisabledStatusView;
	String mSizeContentDescription;

	// preview toast
	protected Toast mToast;
	// drawable used for the toast
	protected PreviewSpotDrawable mDrawable;

	MoaActionList mActions = MoaActionFactory.actionList();

	private boolean mLimitDrawArea;

	float minRadiusSize;
	float maxRadiusSize;
	private int mBrushSizeIndex;
	private int minBrushSize;
	private int maxBrushSize;

	private void showSizePreview( int size ) {
		if ( !isActive() ) return;

		updateSizePreview( size );
	}

	private void updateSizePreview( int size ) {
		if ( !isActive() ) return;

		if ( null != mToast ) {
			mDrawable.setFixedRadius( size );
			mToast.show();
		}
	}

	public DelayedSpotDrawPanel ( IAviaryController context, ToolEntry entry, Filters filter_type, boolean limit_area ) {
		super( context, entry );
		mFilterType = filter_type;
		mLimitDrawArea = limit_area;
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );
		ConfigService config = getContext().getService( ConfigService.class );

		mBrushSizeIndex = config.getInteger( R.integer.aviary_spot_brush_index );
		mBrushSizes = config.getSizeArray( R.array.aviary_spot_brush_sizes );
		mBrushSize = mBrushSizes[mBrushSizeIndex];

		minBrushSize = mBrushSizes[0];
		maxBrushSize = mBrushSizes[mBrushSizes.length - 1];

		minRadiusSize = config.getInteger( R.integer.aviary_spot_gallery_item_min_size ) / 100f;
		maxRadiusSize = config.getInteger( R.integer.aviary_spot_gallery_item_max_size ) / 100f;

		mLensButton = (AviaryHighlightImageButton) getContentView().findViewById( R.id.aviary_lens_button );

		mSizeContentDescription = config.getString( R.string.feather_acc_size );

		mImageView = (ImageViewSpotDraw) getContentView().findViewById( R.id.image );
		( (ImageViewSpotDraw) mImageView ).setBrushSize( (float) mBrushSize * 2 );
		( (ImageViewSpotDraw) mImageView ).setDrawLimit( mLimitDrawArea ? 1000.0 : 0.0 );
		( (ImageViewSpotDraw) mImageView ).setDisplayType( DisplayType.FIT_IF_BIGGER );

		mPreview = BitmapUtils.copy( mBitmap, Config.ARGB_8888 );

		@SuppressWarnings ( "unused" )
		Matrix current = getContext().getCurrentImageViewMatrix();
		mImageView.setImageBitmap( mPreview, null, -1, UIConfiguration.IMAGE_VIEW_MAX_ZOOM );

		// setup the drawing thread
		mBackgroundDrawThread = new MyHandlerThread( "filter-thread", Thread.MIN_PRIORITY );

		mDisabledStatusView = getOptionView().findViewById( R.id.aviary_disable_status );

		// setup the gallery view
		mGallery = (AviaryGallery) getOptionView().findViewById( R.id.aviary_gallery );
		mGallery.setDefaultPosition( mBrushSizeIndex );
		mGallery.setAutoSelectChild( true );
		mGallery.setCallbackDuringFling( false );
		mGallery.setAdapter( new GalleryAdapter( getContext().getBaseContext(), mBrushSizes ) );
		mSelectedPosition = mBrushSizeIndex;
	}

	@Override
	public void onActivate() {
		super.onActivate();

		mToast = makeToast();

		disableHapticIsNecessary( mGallery );

		mBackgroundDrawThread.start();

		mLensButton.setOnClickListener( this );
		mGallery.setOnItemsScrollListener( this );

		( (ImageViewSpotDraw) mImageView ).setOnDrawStartListener( this );

		getContentView().setVisibility( View.VISIBLE );
		contentReady();
	}

	@Override
	protected void onDispose() {
		mContentReadyListener = null;
		super.onDispose();
	}

	@Override
	public void onClick( View v ) {
		final int id = v.getId();

		if ( id == mLensButton.getId() ) {
			setSelectedTool( ( (ImageViewSpotDraw) mImageView ).getDrawMode() == TouchMode.DRAW ? TouchMode.IMAGE : TouchMode.DRAW );
		}
	}

	private void setSelectedTool( TouchMode which ) {
		( (ImageViewSpotDraw) mImageView ).setDrawMode( which );
		mLensButton.setSelected( which == TouchMode.IMAGE );
		setPanelEnabled( which != TouchMode.IMAGE );
	}

	@Override
	public void onDeactivate() {
		mLensButton.setOnClickListener( null );
		mGallery.setOnItemsScrollListener( null );

		( (ImageViewSpotDraw) mImageView ).setOnDrawStartListener( null );

		if ( mBackgroundDrawThread != null ) {
			mBackgroundDrawThread.mQueue.clear();

			if ( mBackgroundDrawThread.started ) {
				mBackgroundDrawThread.pause();
			}

			if ( mBackgroundDrawThread.isAlive() ) {
				mBackgroundDrawThread.quit();
				while ( mBackgroundDrawThread.isAlive() ) {
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

		if ( null != mToast ) {
			mToast.cancel();
		}
	}

	@Override
	public void onCancelled() {
		super.onCancelled();
	}

	private Toast makeToast() {
		mDrawable = new PreviewSpotDrawable( this.getContext().getBaseContext() );
		Toast t = UIUtils.makeCustomToast( this.getContext().getBaseContext() );
		ImageView image = (ImageView) t.getView().findViewById( R.id.image );
		image.setImageDrawable( mDrawable );
		return t;
	}

	@Override
	public void onDrawStart( float[] points, int radius ) {
		radius = Math.max( 1, radius );
		mBackgroundDrawThread.pathStart( (double) radius / 2, mPreview.getWidth() );
		mBackgroundDrawThread.moveTo( points );
		mBackgroundDrawThread.lineTo( points );

		setIsChanged( true );
	}

	@Override
	public void onDrawing( float[] points, int radius ) {
		mBackgroundDrawThread.quadTo( points );
	}

	@Override
	public void onDrawEnd() {
		mBackgroundDrawThread.pathEnd();
	}

	@Override
	protected void onGenerateResult() {
		mLogger.info( "onGenerateResult: " + mBackgroundDrawThread.isCompleted() + ", " + mBackgroundDrawThread.isAlive() );

		if ( !mBackgroundDrawThread.isCompleted() && mBackgroundDrawThread.isAlive() ) {
			GenerateResultTask task = new GenerateResultTask();
			task.execute();
		} else {
			onComplete( mPreview, mActions );
		}
	}

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

	@SuppressWarnings ( "unused" )
	private String printRect( Rect rect ) {
		return "( left=" + rect.left + ", top=" + rect.top + ", width=" + rect.width() + ", height=" + rect.height() + ")";
	}

	protected IFilter createFilter() {
		return FilterLoaderFactory.get( mFilterType );
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.aviary_content_spot_draw, null );
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_spot, parent, false );
	}

	class MyHandlerThread extends Thread {

		boolean started;
		volatile boolean running;
		boolean paused;
		Queue<SpotBrushFilter> mQueue = new LinkedBlockingQueue<SpotBrushFilter>();
		SpotBrushFilter mCurrentFilter = null;

		public MyHandlerThread ( String name, int priority ) {
			super( name );
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
			running = false;
			pause();
			interrupt();
		};

		synchronized public void pathStart( double radius, int bitmapWidth ) {
			SpotBrushFilter filter = (SpotBrushFilter) createFilter();
			filter.setRadius( radius, bitmapWidth );

			RectF rect = ( (ImageViewSpotDraw) mImageView ).getImageRect();
			if ( null != rect ) {
				( (ImageViewSpotDraw) mImageView ).getImageViewMatrix().mapRect( rect );
				double ratio = rect.width() / mImageView.getWidth();
				mLogger.log( "ratio: " + ratio );
				filter.getActions().get( 0 ).setValue( "image2displayratio", ratio );
			}

			mCurrentFilter = filter;
		}

		synchronized public void pathEnd() {
			if ( mCurrentFilter != null ) {
				mQueue.add( mCurrentFilter );
			}

			mCurrentFilter = null;
		}

		public void pause() {
			if ( !started ) throw new IllegalAccessError( "thread not started" );
			paused = true;
		}

		public void unpause() {
			if ( !started ) throw new IllegalAccessError( "thread not started" );
			paused = false;
		}

		public void moveTo( float values[] ) {
			mCurrentFilter.moveTo( values );
		}

		public void lineTo( float values[] ) {
			mCurrentFilter.lineTo( values );
		}

		public void quadTo( float values[] ) {
			mCurrentFilter.quadTo( values );
		}

		public boolean isCompleted() {
			return mQueue.size() == 0;
		}

		public int queueSize() {
			return mQueue.size();
		}

		public PointF getLerp( PointF pt1, PointF pt2, float t ) {
			return new PointF( pt1.x + ( pt2.x - pt1.x ) * t, pt1.y + ( pt2.y - pt1.y ) * t );
		}

		@SuppressLint ( "FloatMath" )
		@Override
		public void run() {

			while ( !started ) {
			}

			boolean s = false;

			mLogger.log( "thread.start!" );

			while ( running ) {

				if ( paused ) {
					continue;
				}

				int currentSize;

				currentSize = queueSize();

				if ( currentSize > 0 && !isInterrupted() ) {

					if ( !s ) {
						s = true;
						onProgressStart();
					}

					PointF firstPoint, lastPoint;

					SpotBrushFilter filter = mQueue.peek();
					FlattenPath path = filter.getFlattenPath();

					firstPoint = path.remove();
					while ( firstPoint == null && path.size() > 0 ) {
						firstPoint = path.remove();
					}

					final int w = mPreview.getWidth();
					final int h = mPreview.getHeight();

					while ( path.size() > 0 ) {
						lastPoint = path.remove();

						float x = Math.abs( firstPoint.x - lastPoint.x );
						float y = Math.abs( firstPoint.y - lastPoint.y );
						float length = FloatMath.sqrt( x * x + y * y );
						float currentPosition = 0;
						float lerp;

						if ( length == 0 ) {
							filter.addPoint( firstPoint.x / w, firstPoint.y / h );
						} else {
							while ( currentPosition < length ) {
								lerp = currentPosition / length;
								PointF point = getLerp( lastPoint, firstPoint, lerp );
								currentPosition += filter.getRealRadius() / 2.0;
								filter.addPoint( point.x / w, point.y / h );
							}
						}
						firstPoint = lastPoint;
					}

					filter.draw( mPreview );
					if ( paused ) continue;

					if ( SystemUtils.isHoneyComb() ) {
						// There's a bug in Honeycomb which prevent the bitmap to be
						// updated on a glcanvas
						// so we need to force it
						Moa.notifyPixelsChanged( mPreview );
					}

					try {
						mActions.add( (MoaAction) filter.getActions().get( 0 ).clone() );
					} catch ( CloneNotSupportedException e ) {
					}

					mQueue.remove();
					mImageView.postInvalidate();
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

	class GalleryAdapter extends BaseAdapter {

		private final int VALID_POSITION = 0;
		private final int INVALID_POSITION = 1;

		private int[] sizes;
		LayoutInflater mLayoutInflater;
		Resources mRes;

		public GalleryAdapter ( Context context, int[] values ) {
			mLayoutInflater = LayoutInflater.from( context );
			sizes = values;
			mRes = context.getResources();
		}

		@Override
		public int getCount() {
			return sizes.length;
		}

		@Override
		public Object getItem( int position ) {
			return sizes[position];
		}

		@Override
		public long getItemId( int position ) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType( int position ) {
			final boolean valid = position >= 0 && position < getCount();
			return valid ? VALID_POSITION : INVALID_POSITION;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {

			final int type = getItemViewType( position );

			PreviewSpotDrawable drawable = null;
			int size = 1;

			if ( convertView == null ) {

				convertView = mLayoutInflater.inflate( R.layout.aviary_gallery_item_view, mGallery, false );

				if ( type == VALID_POSITION ) {
					drawable = new PreviewSpotDrawable( getContext().getBaseContext() );
					ImageView image = (ImageView) convertView.findViewById( R.id.image );
					image.setImageDrawable( drawable );
					convertView.setTag( drawable );
				}
			} else {
				if ( type == VALID_POSITION ) {
					drawable = (PreviewSpotDrawable) convertView.getTag();
				}
			}

			if ( drawable != null && type == VALID_POSITION ) {
				size = sizes[position];
				// float value = (float) size / biggest;

				float value = minRadiusSize
						+ ( ( ( (float) size - minBrushSize ) / ( maxBrushSize - minBrushSize ) * ( maxRadiusSize - minRadiusSize ) ) * 0.55f );

				drawable.setRadius( value );
				convertView.setContentDescription( mSizeContentDescription + " " + Float.toString( value ) );
			}

			convertView.setSelected( mSelectedPosition == position );
			convertView.setId( position );
			return convertView;
		}
	}

	class GenerateResultTask extends AviaryAsyncTask<Void, Void, Void> {

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
					mLogger.log( "waiting.... " + mBackgroundDrawThread.queueSize() );
					try {
						Thread.sleep( 100 );
					} catch ( InterruptedException e ) {
						e.printStackTrace();
					}
				}
			}

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

	@Override
	public void onScrollStarted( AviaryAdapterView<?> parent, View view, int position, long id ) {
		showSizePreview( mBrushSizes[position] );
		setSelectedTool( TouchMode.DRAW );
	}

	@Override
	public void onScroll( AviaryAdapterView<?> parent, View view, int position, long id ) {
		updateSizePreview( mBrushSizes[position] );
	}

	@Override
	public void onScrollFinished( AviaryAdapterView<?> parent, View view, int position, long id ) {
		mBrushSize = mBrushSizes[position];

		( (ImageViewSpotDraw) mImageView ).setBrushSize( (float) mBrushSize * 2 );
		setSelectedTool( TouchMode.DRAW );
	}
}
