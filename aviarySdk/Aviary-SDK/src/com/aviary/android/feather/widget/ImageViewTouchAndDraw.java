package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.graphics.IBitmapDrawable;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class ImageViewTouchAndDraw extends ImageViewTouch {

	public static enum TouchMode {
		IMAGE, DRAW
	};

	public static interface OnDrawStartListener {
		void onDrawStart();
	};

	public static interface OnDrawPathListener {
		void onStart();

		void onMoveTo( float x, float y );

		void onLineTo( float x, float y );

		void onQuadTo( float x, float y, float x1, float y1 );

		void onEnd();
	}

	protected Paint mPaint;

	protected Path tmpPath = new Path();

	protected Canvas mCanvas;

	protected TouchMode mTouchMode = TouchMode.DRAW;

	protected float mX, mY;

	protected Matrix mIdentityMatrix = new Matrix();

	protected Matrix mInvertedMatrix = new Matrix();

	protected Bitmap mCopy;

	protected static final float TOUCH_TOLERANCE = 4;

	private OnDrawStartListener mDrawListener;

	private OnDrawPathListener mDrawPathListener;

	public ImageViewTouchAndDraw ( Context context, AttributeSet attrs ) {
		this( context, attrs, 0 );
	}

	public ImageViewTouchAndDraw ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	public void setOnDrawStartListener( OnDrawStartListener listener ) {
		mDrawListener = listener;
	}

	public void setOnDrawPathListener( OnDrawPathListener listener ) {
		mDrawPathListener = listener;
	}

	@Override
	protected void init( Context context, AttributeSet attrs, int defStyle ) {
		super.init( context, attrs, defStyle );

		Log.i( LOG_TAG, "init, paint: " + mPaint );

		mPaint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG );
		mPaint.setFilterBitmap( false );
		mPaint.setColor( 0xFFFF0000 );
		mPaint.setStyle( Paint.Style.STROKE );
		mPaint.setStrokeJoin( Paint.Join.ROUND );
		mPaint.setStrokeCap( Paint.Cap.ROUND );
		mPaint.setStrokeWidth( 10.0f );

		tmpPath = new Path();
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

			Matrix m1 = new Matrix( getImageMatrix() );
			mInvertedMatrix.reset();

			float[] v1 = getMatrixValues( m1 );
			m1.invert( m1 );
			float[] v2 = getMatrixValues( m1 );

			mInvertedMatrix.postTranslate( -v1[Matrix.MTRANS_X], -v1[Matrix.MTRANS_Y] );
			mInvertedMatrix.postScale( v2[Matrix.MSCALE_X], v2[Matrix.MSCALE_Y] );
			mCanvas.setMatrix( mInvertedMatrix );

			Log.d( LOG_TAG, "scale: " + getScale( mInvertedMatrix ) );
		}
	}

	public float getDrawingScale() {
		return getScale( mInvertedMatrix );
	}

	@Override
	protected void onLayoutChanged( int left, int top, int right, int bottom ) {
		super.onLayoutChanged( left, top, right, bottom );
		onDrawModeChanged();
	}

	public Paint getPaint() {
		return mPaint;
	}

	public void setPaint( Paint paint ) {
		mPaint.set( paint );
	}

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );

		// canvas.drawPath( tmpPath, mPaint );

		if ( mCopy != null ) {
			final int saveCount = canvas.getSaveCount();
			canvas.save();
			canvas.drawBitmap( mCopy, getImageMatrix(), null );
			canvas.restoreToCount( saveCount );
		}
	}

	/**
	 * Commit the current changes to the passed {@link Bitmap}.<br />
	 * Call this method when you want to save the current status of the drawing operations
	 * to an output bitmap.
	 * 
	 * @param canvas
	 */
	public void commit( Canvas canvas ) {
		Drawable drawable = getDrawable();
		if ( null != drawable && drawable instanceof IBitmapDrawable ) {
			canvas.drawBitmap( ( (IBitmapDrawable) drawable ).getBitmap(), new Matrix(), null );
			canvas.drawBitmap( mCopy, new Matrix(), null );
		}
	}

	@Override
	protected void onDrawableChanged( Drawable drawable ) {
		super.onDrawableChanged( drawable );

		if ( mCopy != null ) {
			mCopy.recycle();
			mCopy = null;
		}

		if ( drawable != null && ( drawable instanceof IBitmapDrawable ) ) {
			final Bitmap bitmap = ( (IBitmapDrawable) drawable ).getBitmap();
			mCopy = Bitmap.createBitmap( bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888 );
			mCanvas = new Canvas( mCopy );
			mCanvas.drawColor( 0 );
			onDrawModeChanged();
		}
	}

	private void touch_start( float x, float y ) {
		tmpPath.reset();
		tmpPath.moveTo( x, y );

		mX = x;
		mY = y;

		if ( mDrawListener != null ) mDrawListener.onDrawStart();
		if ( mDrawPathListener != null ) {

			mDrawPathListener.onStart();

			float[] pts = new float[] { x, y };
			mInvertedMatrix.mapPoints( pts );
			mDrawPathListener.onMoveTo( pts[0], pts[1] );
		}
	}

	private void touch_move( float x, float y ) {
		float dx = Math.abs( x - mX );
		float dy = Math.abs( y - mY );
		if ( dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE ) {

			float x1 = ( x + mX ) / 2;
			float y1 = ( y + mY ) / 2;

			tmpPath.quadTo( mX, mY, x1, y1 );

			mCanvas.drawPath( tmpPath, mPaint );
			tmpPath.reset();
			tmpPath.moveTo( x1, y1 );

			if ( mDrawPathListener != null ) {

				float[] pts = new float[] { mX, mY, x1, y1 };
				mInvertedMatrix.mapPoints( pts );
				mDrawPathListener.onQuadTo( pts[0], pts[1], pts[2], pts[3] );
			}

			mX = x;
			mY = y;
		}
	}

	private void touch_up() {

		tmpPath.reset();
		if ( mDrawPathListener != null ) {
			mDrawPathListener.onEnd();
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

	public Bitmap getOverlayBitmap() {
		return mCopy;
	}
}
