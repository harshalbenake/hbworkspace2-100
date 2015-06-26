package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.graphics.IBitmapDrawable;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class ImageViewSpotDraw extends ImageViewTouch {

	public static enum TouchMode {
		// mode for pan and zoom
		IMAGE,
		// mode for drawing
		DRAW
	};

	public static interface OnDrawListener {
		void onDrawStart( float points[], int radius );

		void onDrawing( float points[], int radius );

		void onDrawEnd();
	};

	protected boolean mPaintEnabled = true;
	protected Paint mPaint;
	protected float mCurrentScale = 1;
	protected float mBrushSize = 30;
	protected Path tmpPath = new Path();
	protected Canvas mCanvas;
	protected TouchMode mTouchMode = TouchMode.DRAW;
	protected float mX, mY;
	protected float mStartX, mStartY;
	protected Matrix mIdentityMatrix = new Matrix();
	protected Matrix mInvertedMatrix = new Matrix();
	protected static final float TOUCH_TOLERANCE = 2;
	private OnDrawListener mDrawListener;
	private double mRestiction = 0;

	public ImageViewSpotDraw ( Context context, AttributeSet attrs ) {
		this( context, attrs, 0 );
	}

	public ImageViewSpotDraw ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	public void setOnDrawStartListener( OnDrawListener listener ) {
		mDrawListener = listener;
	}

	@Override
	protected void init( Context context, AttributeSet attrs, int defStyle ) {
		super.init( context, attrs, defStyle );
		mPaint = new Paint( Paint.ANTI_ALIAS_FLAG );
		mPaint.setFilterBitmap( false );
		mPaint.setDither( true );
		mPaint.setColor( 0x66FFFFCC );
		mPaint.setStyle( Paint.Style.STROKE );
		mPaint.setStrokeCap( Paint.Cap.ROUND );
		tmpPath = new Path();
	}

	public void setDrawLimit( double value ) {
		mRestiction = value;
	}

	public void setBrushSize( float value ) {
		mBrushSize = value;

		if ( mPaint != null ) {
			mPaint.setStrokeWidth( mBrushSize );
		}
	}

	public TouchMode getDrawMode() {
		return mTouchMode;
	}

	public void setDrawMode( TouchMode mode ) {
		if ( mode != mTouchMode ) {
			mTouchMode = mode;
			onDrawModeChanged();
		}
	}

	protected void onDrawModeChanged() {
		if ( mTouchMode == TouchMode.DRAW ) {
			Log.i( LOG_TAG, "onDrawModeChanged" );

			Matrix m1 = new Matrix( getImageMatrix() );
			mInvertedMatrix.reset();

			float[] v1 = getMatrixValues( m1 );
			m1.invert( m1 );
			float[] v2 = getMatrixValues( m1 );

			mInvertedMatrix.postTranslate( -v1[Matrix.MTRANS_X], -v1[Matrix.MTRANS_Y] );
			mInvertedMatrix.postScale( v2[Matrix.MSCALE_X], v2[Matrix.MSCALE_Y] );
			mCanvas.setMatrix( mInvertedMatrix );

			mCurrentScale = getScale() * getBaseScale();

			mPaint.setStrokeWidth( mBrushSize );
		}
	}

	public Paint getPaint() {
		return mPaint;
	}

	public void setPaint( Paint paint ) {
		mPaint.set( paint );
	}

	public void setPaintEnabled( boolean enabled ) {
		mPaintEnabled = enabled;
	}

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );

		if ( mPaintEnabled ) canvas.drawPath( tmpPath, mPaint );
	}

	public RectF getImageRect() {
		if ( getDrawable() != null ) {
			return new RectF( 0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight() );
		} else {
			return null;
		}
	}

	@Override
	protected void onDrawableChanged( Drawable drawable ) {
		super.onDrawableChanged( drawable );

		if ( drawable != null && ( drawable instanceof IBitmapDrawable ) ) {
			mCanvas = new Canvas();
			mCanvas.drawColor( 0 );
			onDrawModeChanged();
		}
	}

	@Override
	protected void onLayoutChanged( int left, int top, int right, int bottom ) {
		super.onLayoutChanged( left, top, right, bottom );

		if ( null != getDrawable() ) {
			onDrawModeChanged();
		}
	}

	private boolean mMoved = false;

	private void touch_start( float x, float y ) {

		mMoved = false;

		tmpPath.reset();
		tmpPath.moveTo( x, y );

		mX = x;
		mY = y;
		mStartX = x;
		mStartY = y;

		if ( mDrawListener != null ) {
			float mappedPoints[] = new float[2];
			mappedPoints[0] = x;
			mappedPoints[1] = y;
			mInvertedMatrix.mapPoints( mappedPoints );
			tmpPath.lineTo( x + .1f, y );
			Log.d( LOG_TAG, "brushSize: " + mBrushSize + ", currentScale: " + mCurrentScale + ", basescale: " + getBaseScale() );
			mDrawListener.onDrawStart( mappedPoints, (int) ( mBrushSize / mCurrentScale ) );
		}
	}

	private void touch_move( float x, float y ) {

		float dx = Math.abs( x - mX );
		float dy = Math.abs( y - mY );

		if ( dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE ) {

			if ( !mMoved ) {
				tmpPath.setLastPoint( mX, mY );
			}

			mMoved = true;

			if ( mRestiction > 0 ) {
				double r = Math.sqrt( Math.pow( x - mStartX, 2 ) + Math.pow( y - mStartY, 2 ) );
				double theta = Math.atan2( y - mStartY, x - mStartX );

				final float w = getWidth();
				final float h = getHeight();

				double scale = ( mRestiction / mCurrentScale ) / (double) ( w + h ) / ( mBrushSize / mCurrentScale );
				double rNew = Math.log( r * scale + 1 ) / scale;

				x = (float) ( mStartX + rNew * Math.cos( theta ) );
				y = (float) ( mStartY + rNew * Math.sin( theta ) );
			}

			tmpPath.quadTo( mX, mY, ( x + mX ) / 2, ( y + mY ) / 2 );
			mX = x;
			mY = y;
		}

		if ( mDrawListener != null ) {
			float mappedPoints[] = new float[2];
			mappedPoints[0] = x;
			mappedPoints[1] = y;
			mInvertedMatrix.mapPoints( mappedPoints );
			mDrawListener.onDrawing( mappedPoints, (int) ( mBrushSize / mCurrentScale ) );
		}
	}

	private void touch_up() {

		tmpPath.reset();

		if ( mDrawListener != null ) {
			mDrawListener.onDrawEnd();
		}
	}

	public static float[] getMatrixValues( Matrix m ) {
		float[] values = new float[9];
		m.getValues( values );
		return values;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		if ( mTouchMode == TouchMode.DRAW && event.getPointerCount() == 1 ) {
			float x = event.getX();
			float y = event.getY();

			switch ( event.getAction() ) {
				case MotionEvent.ACTION_DOWN:
					touch_start( x, y );
					invalidate();
					break;
				case MotionEvent.ACTION_MOVE:
					touch_move( x, y );
					invalidate();
					break;
				case MotionEvent.ACTION_UP:
					touch_up();
					invalidate();
					break;
			}
			return true;
		} else {
			if ( mTouchMode == TouchMode.IMAGE ) return super.onTouchEvent( event );
			else return false;
		}
	}
}
