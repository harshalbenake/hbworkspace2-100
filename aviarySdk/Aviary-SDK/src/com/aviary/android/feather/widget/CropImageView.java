package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.graphics.IBitmapDrawable;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

import com.aviary.android.feather.R;
import com.aviary.android.feather.library.graphics.RectD;
import com.aviary.android.feather.library.utils.UIConfiguration;

public class CropImageView extends ImageViewTouch {

	public interface OnHighlightSingleTapUpConfirmedListener {
		void onSingleTapUpConfirmed();
	}

	public static final int GROW = 0;
	public static final int SHRINK = 1;
	private int mMotionEdge = HighlightView.GROW_NONE;
	private HighlightView mHighlightView;
	private OnHighlightSingleTapUpConfirmedListener mHighlightSingleTapUpListener;
	private HighlightView mMotionHighlightView;
	private int mCropMinSize = 10;

	protected Handler mHandler = new Handler();
	private int mHighlighStyle;

	public CropImageView ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryCropImageViewStyle );
	}

	public CropImageView ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	public void setOnHighlightSingleTapUpConfirmedListener( OnHighlightSingleTapUpConfirmedListener listener ) {
		mHighlightSingleTapUpListener = listener;
	}

	@Deprecated
	public void setMinCropSize( int value ) {
		mCropMinSize = value;
		if ( mHighlightView != null ) {
			mHighlightView.setMinSize( value );
		}
	}

	@Override
	protected void init( Context context, AttributeSet attrs, int defStyle ) {
		super.init( context, attrs, defStyle );
		mGestureDetector = null;
		mGestureListener = null;
		mScaleListener = null;
		mGestureDetector = new GestureDetector( getContext(), new CropGestureListener(), null, true );
		mGestureDetector.setIsLongpressEnabled( false );

		Theme theme = context.getTheme();

		TypedArray array = theme.obtainStyledAttributes( attrs, R.styleable.AviaryCropImageView, defStyle, 0 );
		mCropMinSize = array.getDimensionPixelSize( R.styleable.AviaryCropImageView_aviary_minCropSize, 50 );
		mHighlighStyle = array.getResourceId( R.styleable.AviaryCropImageView_aviary_highlightStyle, -1 );

		array.recycle();

	}

	@Override
	public void setImageDrawable( Drawable drawable, Matrix initial_matrix, float min_zoom, float max_zoom ) {
		mMotionHighlightView = null;
		super.setImageDrawable( drawable, initial_matrix, min_zoom, max_zoom );
	}

	@Override
	protected void onLayoutChanged( int left, int top, int right, int bottom ) {
		super.onLayoutChanged( left, top, right, bottom );
		mHandler.post( onLayoutRunnable );
	}

	Runnable onLayoutRunnable = new Runnable() {

		@Override
		public void run() {
			final Drawable drawable = getDrawable();

			if ( drawable != null && ( (IBitmapDrawable) drawable ).getBitmap() != null ) {
				if ( mHighlightView != null ) {
					if ( mHighlightView.isRunning() ) {
						mHandler.post( this );
					} else {
						Log.d( LOG_TAG, "onLayoutRunnable.. running" );
						mHighlightView.getMatrix().set( getImageMatrix() );
						mHighlightView.invalidate();
					}
				}
			}
		}
	};

	@Override
	protected void postTranslate( float deltaX, float deltaY ) {
		super.postTranslate( deltaX, deltaY );

		if ( mHighlightView != null ) {

			if ( mHighlightView.isRunning() ) {
				return;
			}

			if ( getScale() != 1 ) {
				float[] mvalues = new float[9];
				getImageMatrix().getValues( mvalues );
				final float scale = mvalues[Matrix.MSCALE_X];
				mHighlightView.getCropRectD().offset( -deltaX / scale, -deltaY / scale );
			}

			mHighlightView.getMatrix().set( getImageMatrix() );
			mHighlightView.invalidate();
		}
	}

	private Rect mRect1 = new Rect();
	private Rect mRect2 = new Rect();

	@Override
	protected void postScale( float scale, float centerX, float centerY ) {
		if ( mHighlightView != null ) {

			if ( mHighlightView.isRunning() ) return;

			RectD cropRect = mHighlightView.getCropRectD();
			mHighlightView.getDisplayRect( getImageViewMatrix(), mHighlightView.getCropRectD(), mRect1 );

			super.postScale( scale, centerX, centerY );

			mHighlightView.getDisplayRect( getImageViewMatrix(), mHighlightView.getCropRectD(), mRect2 );

			float[] mvalues = new float[9];
			getImageViewMatrix().getValues( mvalues );
			final float currentScale = mvalues[Matrix.MSCALE_X];

			cropRect.offset( ( mRect1.left - mRect2.left ) / currentScale, ( mRect1.top - mRect2.top ) / currentScale );
			cropRect.right += -( mRect2.width() - mRect1.width() ) / currentScale;
			cropRect.bottom += -( mRect2.height() - mRect1.height() ) / currentScale;

			mHighlightView.getMatrix().set( getImageMatrix() );
			mHighlightView.getCropRectD().set( cropRect );
			mHighlightView.invalidate();
		} else {
			super.postScale( scale, centerX, centerY );
		}
	}

	private boolean ensureVisible( HighlightView hv ) {
		Rect r = hv.getDrawRect();
		int panDeltaX1 = Math.max( 0, getLeft() - r.left );
		int panDeltaX2 = Math.min( 0, getRight() - r.right );
		int panDeltaY1 = Math.max( 0, getTop() - r.top );
		int panDeltaY2 = Math.min( 0, getBottom() - r.bottom );
		int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
		int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

		if ( panDeltaX != 0 || panDeltaY != 0 ) {
			panBy( panDeltaX, panDeltaY );
			return true;
		}
		return false;
	}

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );
		if ( mHighlightView != null ) {
			mHighlightView.draw( canvas );
		}
	}

	@Override
	protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
		super.onSizeChanged( w, h, oldw, oldh );

		if ( null != mHighlightView ) {
			mHighlightView.onSizeChanged( this, w, h, oldw, oldh );
		}
	}

	public void setHighlightView( HighlightView hv ) {
		if ( mHighlightView != null ) {
			mHighlightView.dispose();
		}

		mMotionHighlightView = null;
		mHighlightView = hv;
		invalidate();
	}

	public HighlightView getHighlightView() {
		return mHighlightView;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		super.onTouchEvent( event );
		int action = event.getAction() & MotionEvent.ACTION_MASK;

		switch ( action ) {
			case MotionEvent.ACTION_UP:

				if ( mHighlightView != null ) {
					mHighlightView.setMode( HighlightView.Mode.None );
					postInvalidate();
				}

				mMotionHighlightView = null;
				mMotionEdge = HighlightView.GROW_NONE;
				break;

		}

		return true;
	}

	class CropGestureListener extends GestureDetector.SimpleOnGestureListener {

		@Override
		public boolean onDown( MotionEvent e ) {
			mMotionHighlightView = null;
			HighlightView hv = mHighlightView;

			if ( hv != null ) {

				int edge = hv.getHit( e.getX(), e.getY() );
				if ( edge != HighlightView.GROW_NONE ) {
					mMotionEdge = edge;
					mMotionHighlightView = hv;
					mMotionHighlightView.setMode( ( edge == HighlightView.MOVE ) ? HighlightView.Mode.Move : HighlightView.Mode.Grow );
					postInvalidate();
				}
			}
			return super.onDown( e );
		}

		@Override
		public boolean onSingleTapConfirmed( MotionEvent e ) {
			mMotionHighlightView = null;

			return super.onSingleTapConfirmed( e );
		}

		@Override
		public boolean onSingleTapUp( MotionEvent e ) {
			mMotionHighlightView = null;

			if ( mHighlightView != null && mMotionEdge == HighlightView.MOVE ) {

				if ( mHighlightSingleTapUpListener != null ) {
					mHighlightSingleTapUpListener.onSingleTapUpConfirmed();
				}
			}
			return super.onSingleTapUp( e );
		}

		@Override
		public boolean onDoubleTap( MotionEvent e ) {
			if ( mDoubleTapEnabled ) {
				mMotionHighlightView = null;

				float scale = getScale();
				float targetScale = scale;
				targetScale = CropImageView.this.onDoubleTapPost( scale, getMaxScale() );
				targetScale = Math.min( getMaxScale(), Math.max( targetScale, 1 ) );
				zoomTo( targetScale, e.getX(), e.getY(), 200 );
				invalidate();
			}
			return super.onDoubleTap( e );
		}

		@Override
		public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
			if ( e1 == null || e2 == null ) return false;
			if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
			if ( mScaleDetector.isInProgress() ) return false;

			if ( mMotionHighlightView != null && mMotionEdge != HighlightView.GROW_NONE ) {
				mMotionHighlightView.handleMotion( mMotionEdge, -distanceX, -distanceY );

				if ( mMotionEdge == HighlightView.MOVE ) {
					invalidate( mMotionHighlightView.getInvalidateRect() );
				} else {
					postInvalidate();
				}

				ensureVisible( mMotionHighlightView );
				return true;
			} else {
				scrollBy( -distanceX, -distanceY );
				invalidate();
				return true;
			}
		}

		@Override
		public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
			if ( e1.getPointerCount() > 1 || e2.getPointerCount() > 1 ) return false;
			if ( mScaleDetector.isInProgress() ) return false;
			if ( mMotionHighlightView != null ) return false;

			float diffX = e2.getX() - e1.getX();
			float diffY = e2.getY() - e1.getY();

			if ( Math.abs( velocityX ) > 800 || Math.abs( velocityY ) > 800 ) {
				scrollBy( diffX / 2, diffY / 2, 300 );
				invalidate();
			}
			return super.onFling( e1, e2, velocityX, velocityY );
		}
	}

	class CropScaleListener extends SimpleOnScaleGestureListener {

		@Override
		public boolean onScaleBegin( ScaleGestureDetector detector ) {
			return super.onScaleBegin( detector );
		}

		@Override
		public void onScaleEnd( ScaleGestureDetector detector ) {
			super.onScaleEnd( detector );
		}

		@Override
		public boolean onScale( ScaleGestureDetector detector ) {
			float span = detector.getCurrentSpan() - detector.getPreviousSpan();
			float targetScale = getScale() * detector.getScaleFactor();
			if ( span != 0 ) {
				targetScale = Math.min( getMaxScale(), Math.max( targetScale, 1 ) );
				zoomTo( targetScale, detector.getFocusX(), detector.getFocusY() );
				mDoubleTapDirection = 1;
				invalidate();
			}
			return true;
		}
	}

	protected double mAspectRatio = 0;

	private boolean mAspectRatioFixed;

	public void setImageBitmap( Bitmap bitmap, double aspectRatio, boolean isFixed ) {
		mAspectRatio = aspectRatio;
		mAspectRatioFixed = isFixed;
		setImageBitmap( bitmap, null, ImageViewTouchBase.ZOOM_INVALID, UIConfiguration.IMAGE_VIEW_MAX_ZOOM );
	}

	public void setAspectRatio( double value, boolean isFixed ) {

		if ( getDrawable() != null ) {
			mAspectRatio = value;
			mAspectRatioFixed = isFixed;
			updateCropView( false );
		}
	}

	@Override
	protected void onDrawableChanged( Drawable drawable ) {
		super.onDrawableChanged( drawable );

		if ( null != getHandler() ) {
			getHandler().post( new Runnable() {

				@Override
				public void run() {
					updateCropView( true );
				}
			} );
		}
	}

	public void updateCropView( boolean bitmapChanged ) {

		if ( bitmapChanged ) {
			setHighlightView( null );
		}

		if ( getDrawable() == null ) {
			setHighlightView( null );
			invalidate();
			return;
		}

		if ( getHighlightView() != null ) {
			updateAspectRatio( mAspectRatio, getHighlightView(), true );
		} else {
			HighlightView hv = new HighlightView( this, mHighlighStyle );
			hv.setMinSize( mCropMinSize );
			updateAspectRatio( mAspectRatio, hv, false );
			setHighlightView( hv );
		}
		invalidate();
	}

	private void updateAspectRatio( double aspectRatio, HighlightView hv, boolean animate ) {
		Log.d( LOG_TAG, "updateAspectRatio: " + aspectRatio );

		float width = getDrawable().getIntrinsicWidth();
		float height = getDrawable().getIntrinsicHeight();
		RectD imageRect = new RectD( 0, 0, (int) width, (int) height );
		Matrix mImageMatrix = getImageMatrix();
		RectD cropRect = computeFinalCropRect( aspectRatio );

		if ( animate ) {
			hv.animateTo( this, mImageMatrix, imageRect, cropRect, mAspectRatioFixed );
		} else {
			hv.setup( mImageMatrix, imageRect, cropRect, mAspectRatioFixed );
			postInvalidate();
		}
	}

	public void onConfigurationChanged( Configuration config ) {
		// Log.d( LOG_TAG, "onConfigurationChanged" );
		if ( null != getHandler() ) {
			getHandler().postDelayed( new Runnable() {

				@Override
				public void run() {
					setAspectRatio( mAspectRatio, getAspectRatioIsFixed() );
				}
			}, 500 );
		}
		postInvalidate();
	}

	private RectD computeFinalCropRect( double aspectRatio ) {

		float scale = getScale();

		float width = getDrawable().getIntrinsicWidth();
		float height = getDrawable().getIntrinsicHeight();

		RectF viewRect = new RectF( 0, 0, getWidth(), getHeight() );
		RectF bitmapRect = getBitmapRect();

		RectF rect = new RectF( Math.max( viewRect.left, bitmapRect.left ), Math.max( viewRect.top, bitmapRect.top ),
				Math.min( viewRect.right, bitmapRect.right ), Math.min( viewRect.bottom, bitmapRect.bottom ) );

		double cropWidth = Math.min( Math.min( width / scale, rect.width() ), Math.min( height / scale, rect.height() ) ) * 0.8f;
		double cropHeight = cropWidth;

		if ( aspectRatio != 0 ) {
			if ( aspectRatio > 1 ) {
				cropHeight = cropHeight / (double) aspectRatio;
			} else {
				cropWidth = cropWidth * (double) aspectRatio;
			}
		}

		Matrix mImageMatrix = getImageMatrix();
		Matrix tmpMatrix = new Matrix();

		if ( !mImageMatrix.invert( tmpMatrix ) ) {
			Log.e( LOG_TAG, "cannot invert matrix" );
		}

		tmpMatrix.mapRect( viewRect );

		double x = viewRect.centerX() - cropWidth / 2;
		double y = viewRect.centerY() - cropHeight / 2;
		RectD cropRect = new RectD( x, y, ( x + cropWidth ), ( y + cropHeight ) );

		return cropRect;
	}

	public double getAspectRatio() {
		return mAspectRatio;
	}

	public boolean getAspectRatioIsFixed() {
		return mAspectRatioFixed;
	}
}