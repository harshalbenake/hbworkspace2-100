package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.aviary.android.feather.R;
import com.aviary.android.feather.widget.ScrollerRunnable.ScrollableView;

public class AviaryWheel extends View implements OnGestureListener, ScrollableView, VibrationWidget {

	/**
	 * The listener interface for receiving onScroll events. The class that is interested
	 * in processing a onScroll event implements
	 * this interface, and the object created with that class is registered with a
	 * component using the component's <code>addOnScrollListener<code> method. When
	 * the onScroll event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OnScrollEvent
	 */
	public interface OnWheelChangeListener {

		/**
		 * The wheel start scrolling
		 * 
		 * @param view
		 */
		void onStartTrackingTouch( AviaryWheel view );

		/**
		 * The wheel value is changing
		 * 
		 * @param view
		 * @param value
		 */
		void OnValueChanged( AviaryWheel view, int value );

		/**
		 * Scroll completed
		 * 
		 * @param view
		 */
		void onStopTrackingTouch( AviaryWheel view );
	}

	static final String LOG_TAG = "wheel";

	private static final int INVALID_VALUE = -1;
	private static final double EDGE_HEIGHT = 0.5517241379310345;

	private Drawable mShadowBottom;
	private BitmapShader mShader;
	private Matrix mShaderMatrix;

	private GestureDetector mGestureDetector;
	private ScrollerRunnable mScroller;

	private double mCurrentValue;

	private boolean mToLeft;
	private boolean mLayoutCompleted;
	private boolean mInLayout;

	private Drawable mLinesIndicator;
	private Drawable mLinesSingle;

	private Paint mLinesPaint;

	private int mShaderWidth;
	private int mLastMotionValue;
	private int mLineWidth;
	private int mMinX;
	private int mMaxX;
	private int mNextValue;

	// edges
	private float mEdgeOffset;
	private AviaryEdgeEffect mEdgeLeft, mEdgeRight;
	private Matrix mEdgeMatrixLeft = new Matrix();
	private Matrix mEdgeMatrixRight = new Matrix();
	private int mEdgeStyle;

	private VibrationHelper mVibrationHelper;
	private OnWheelChangeListener mScrollListener;

	private float mScaledDensity;

	private Paint debugPaint;

	public AviaryWheel ( Context context ) {
		this( context, null );
	}

	public AviaryWheel ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryWheelStyle );
	}

	public AviaryWheel ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		init( context, attrs, defStyle );
	}

	private void init( Context context, AttributeSet attrs, int defStyle ) {

		ViewConfiguration configuration = ViewConfiguration.get( context );

		// 2.0 is xhdpi
		mScaledDensity = context.getResources().getDisplayMetrics().density / 2.0f;

		final Theme theme = context.getTheme();
		TypedArray a = theme.obtainStyledAttributes( attrs, R.styleable.AviaryWheel, defStyle, 0 );
		mShadowBottom = a.getDrawable( R.styleable.AviaryWheel_aviaryWheelShadowTop );
		mLinesSingle = a.getDrawable( R.styleable.AviaryWheel_aviaryWheelLine );
		mLinesIndicator = a.getDrawable( R.styleable.AviaryWheel_aviaryWheelIndicator );
		mEdgeStyle = a.getResourceId( R.styleable.AviaryWheel_aviary_edgeStyle, 0 );
		a.recycle();

		mEdgeOffset = (float) ( 20.0 * mScaledDensity );

		mLineWidth = mLinesSingle.getIntrinsicWidth();
		mLinesPaint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG );

		// gesture detector
		if ( !isInEditMode() ) {
			mGestureDetector = new GestureDetector( context, this );
			mGestureDetector.setIsLongpressEnabled( false );

		}
		// fling manager
		mScroller = new ScrollerRunnable( this, 200, configuration.getScaledOverflingDistance(), null );

		// vibration helper
		mVibrationHelper = new VibrationHelper( context, true );

		mEdgeLeft = new AviaryEdgeEffect( getContext(), mEdgeStyle );
		mEdgeRight = new AviaryEdgeEffect( getContext(), mEdgeStyle );

		mEdgeLeft.setEdgeMaxAlpha( 100 );
		mEdgeRight.setEdgeMaxAlpha( 100 );

		mNextValue = INVALID_VALUE;
		mCurrentValue = 0;
	}

	public void setOnWheelChangeListener( OnWheelChangeListener listener ) {
		mScrollListener = listener;
	}

	/**
	 * Sets the new wheel value
	 * 
	 * @param value
	 *            an integer value between 0 and 100
	 */
	public void setValue( int value ) {
		value = Math.min( 100, Math.max( value, 0 ) );

		if ( mCurrentValue != value ) {

			// if not layout not ready, just set the next value
			if ( !mLayoutCompleted ) {
				mNextValue = value;
				return;
			}

			if ( !mScroller.isFinished() ) {
				mScroller.stop( false );
			}

			scrollStarted();
			trackMotionValue( value );
			scrollCompleted();
		}
	}

	/**
	 * Gets the current value. It's a value between 0 and 100
	 * 
	 * @return
	 */
	public int getValue() {
		return (int) mCurrentValue;
	}

	/**
	 * From 0..100 value return the scroll x position
	 * 
	 * @param value
	 * @return
	 */
	protected int getPositionFromValue( double value ) {
		return (int) ( ( value / 100.0 ) * ( getRange() ) ) + mMinX;
	}

	/**
	 * From x position return the 0..100 value
	 * 
	 * @param value
	 * @return
	 */
	protected double getValueFromPosition( int value ) {
		return ( (double) ( value - mMinX ) / getRange() ) * 100.0;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {
		boolean retValue = mGestureDetector.onTouchEvent( event );

		int action = event.getAction();
		if ( action == MotionEvent.ACTION_UP ) {
			onUp( event );
		} else if ( action == MotionEvent.ACTION_CANCEL ) {
		}
		return retValue;
	}

	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
		super.onLayout( changed, left, top, right, bottom );

		mInLayout = true;

		if ( changed ) {
			mLayoutCompleted = true;
			if ( mNextValue != INVALID_VALUE ) {
				setValue( mNextValue );
				mNextValue = INVALID_VALUE;
			}
		}

		mInLayout = false;
	}

	int mHeightDiff = 0;

	@Override
	protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
		super.onSizeChanged( w, h, oldw, oldh );

		Rect bounds = new Rect( 0, 0, w, h );
		int max_width = (int) ( w - ( mEdgeOffset * 4 ) );

		// calculate the new max and min
		mMinX = ( w - max_width ) / 2;
		mMaxX = w - mMinX;

		// set the new bounds for the drawables

		if ( null != getBackground() ) {
			mHeightDiff = getBackground().getIntrinsicHeight() - mShadowBottom.getIntrinsicHeight();
		}

		mShadowBottom.setBounds( bounds.left, bounds.top, bounds.right, bounds.top + mShadowBottom.getIntrinsicHeight() );

		// create the shader for drawing the lines
		createShader( w, h );
	}

	private static final int HARD_LIMIT = 2048;
	private static final float SHADER_MULTIPLIER = 2.2f;

	private void createShader( int w, int h ) {
		int bitmapWidth = (int) ( w * SHADER_MULTIPLIER );

		// now draw the lines on the new bitmap
		Bitmap linesBitmap = Bitmap.createBitmap( bitmapWidth, mLinesSingle.getIntrinsicHeight(), Config.ARGB_8888 );
		Shader shader = new BitmapShader( ( (BitmapDrawable) mLinesSingle ).getBitmap(), TileMode.REPEAT, TileMode.CLAMP );
		Canvas canvas = new Canvas( linesBitmap );
		Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG );
		paint.setShader( shader );

		final int w2 = linesBitmap.getWidth() / 2;
		final int indicator_width = mLinesIndicator.getIntrinsicWidth();
		int left = w2 - indicator_width / 2;
		int right = w2 + indicator_width / 2;

		double rest = left % mLineWidth;

		// left side
		Matrix matrix = new Matrix();
		matrix.setTranslate( (float) +rest, 0 );
		shader.setLocalMatrix( matrix );
		canvas.drawRect( 0, 0, left, linesBitmap.getHeight(), paint );

		// indicator
		mLinesIndicator.setBounds( left, 0, right, linesBitmap.getHeight() );
		mLinesIndicator.draw( canvas );

		// right side
		shader.setLocalMatrix( null );
		canvas.drawRect( right, 0, linesBitmap.getWidth(), linesBitmap.getHeight(), paint );

		mShader = new BitmapShader( linesBitmap, TileMode.CLAMP, TileMode.CLAMP );
		mLinesPaint.setShader( mShader );
		mShaderMatrix = new Matrix();
		mShaderWidth = linesBitmap.getWidth();

		final int height = (int) ( EDGE_HEIGHT * linesBitmap.getHeight() );
		final int width = (int) ( 9.0f * mScaledDensity );

		mEdgeMatrixLeft.reset();
		mEdgeMatrixLeft.postRotate( -90 );
		mEdgeMatrixLeft.postTranslate( width, height );
		mEdgeMatrixLeft.postTranslate( 0, mHeightDiff - 3 );
		mEdgeLeft.setSize( height, width * 2 );

		mEdgeMatrixRight.reset();
		mEdgeMatrixRight.postRotate( 90 );
		mEdgeMatrixRight.postTranslate( w - width, 0 );
		mEdgeMatrixRight.postTranslate( 0, mHeightDiff - 3 );
		mEdgeRight.setSize( height, width * 2 );
	}

	@Override
	protected void onDraw( Canvas canvas ) {
		super.onDraw( canvas );

		Drawable background = getBackground();

		if ( background == null ) return;

		if ( null != mShader && mShaderMatrix != null ) {
			int w2 = mShaderWidth / 2;

			int scroll_x = getPositionFromValue( mCurrentValue );

			int x = ( scroll_x - w2 );

			mShaderMatrix.setTranslate( x, 0 );
			mShader.setLocalMatrix( mShaderMatrix );

			canvas.drawRect( mEdgeOffset, 0, getWidth() - mEdgeOffset, getHeight(), mLinesPaint );
		}

		canvas.translate( 0, mHeightDiff );
		mShadowBottom.draw( canvas );
		canvas.translate( 0, -mHeightDiff );

		if ( null != debugPaint ) {
			debugPaint.setColor( Color.WHITE );
			debugPaint.setAlpha( 127 );
			canvas.drawLine( mMinX, 0, mMinX, getHeight(), debugPaint );
			canvas.drawLine( mMaxX, 0, mMaxX, getHeight(), debugPaint );
			canvas.drawLine( getWidth() / 2, 0, getWidth() / 2, getHeight(), debugPaint );
		}

		if ( null != mEdgeLeft ) {

			if ( !mEdgeLeft.isFinished() ) {

				final int restoreCount = canvas.save();

				canvas.concat( mEdgeMatrixLeft );

				if ( mEdgeLeft.draw( canvas ) ) {
					postInvalidate();
				}
				canvas.restoreToCount( restoreCount );
			}

			if ( !mEdgeRight.isFinished() ) {

				final int restoreCount = canvas.save();

				canvas.concat( mEdgeMatrixRight );

				if ( mEdgeRight.draw( canvas ) ) {
					postInvalidate();
				}
				canvas.restoreToCount( restoreCount );
			}
		}
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {

		Drawable background = getBackground();
		if ( null != background ) {

			int finalWidth, finalHeigth;

			int widthMode = MeasureSpec.getMode( widthMeasureSpec );
			int widthSize = MeasureSpec.getSize( widthMeasureSpec );

			int heightMode = MeasureSpec.getMode( heightMeasureSpec );
			int heightSize = MeasureSpec.getSize( heightMeasureSpec );

			if ( widthMode == MeasureSpec.AT_MOST || widthMode == MeasureSpec.UNSPECIFIED ) {
				finalWidth = background.getIntrinsicWidth();
			} else {
				finalWidth = widthSize;
			}

			finalWidth = Math.min( finalWidth, (int) ( HARD_LIMIT / SHADER_MULTIPLIER ) );

			if ( heightMode == MeasureSpec.AT_MOST || heightMode == MeasureSpec.UNSPECIFIED ) {
				finalHeigth = background.getIntrinsicHeight();
			} else {
				finalHeigth = heightSize;
			}
			setMeasuredDimension( finalWidth, finalHeigth );
		} else {
			super.onMeasure( widthMeasureSpec, heightMeasureSpec );
		}
	}

	@Override
	protected int getSuggestedMinimumHeight() {
		if ( null != getBackground() ) {
			return getBackground().getIntrinsicHeight();
		}
		return super.getSuggestedMinimumHeight();
	}

	@Override
	protected int getSuggestedMinimumWidth() {
		if ( null != getBackground() ) {
			return getBackground().getIntrinsicWidth();
		}
		return super.getSuggestedMinimumWidth();
	}

	// Gesture Detector Listeners

	public boolean onUp( MotionEvent e ) {
		mEdgeLeft.onRelease();
		mEdgeRight.onRelease();

		if ( mScroller.isFinished() ) {
			scrollIntoSlots();
		}
		return true;
	}

	@Override
	public void computeScroll() {
		super.computeScroll();

		if ( mScroller.hasMore() ) {
			int oldx = mScroller.getPreviousX();
			int x = mScroller.getCurrX();

			if ( oldx != x ) {
				if ( x < mMinX && oldx >= mMinX && oldx > x ) {
					mEdgeLeft.onAbsorb( (int) mScroller.getCurrVelocity() );
				} else if ( x > mMaxX && oldx <= mMaxX && x > oldx ) {
					mEdgeRight.onAbsorb( (int) mScroller.getCurrVelocity() );
				}
			}
		}
	}

	@Override
	public boolean onDown( MotionEvent e ) {
		getParent().requestDisallowInterceptTouchEvent( true );
		mScroller.stop( false );
		scrollStarted();
		return true;
	}

	@Override
	public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
		int max_x = getMaxX();
		int min_x = getMinX();
		int current_x = getPositionFromValue( mCurrentValue );
		boolean toleft = velocityX < 0;

		if ( !toleft ) {

			if ( current_x >= max_x ) {
				return false;
			}

			if ( current_x > max_x ) {
				mScroller.startUsingDistance( current_x, max_x - current_x );
				return true;
			}
		} else {

			if ( current_x <= min_x ) {
				return false;
			}

			if ( current_x < min_x ) {
				mScroller.startUsingDistance( current_x, min_x - current_x );
				return true;
			}
		}

		mScroller.startUsingVelocity( current_x, (int) velocityX / 2 );
		return true;
	}

	@Override
	public void onLongPress( MotionEvent e ) {}

	private static final int MAX_DELTA = 50;
	private float mOverscrollX = 0;

	@Override
	public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
		float delta = -1 * distanceX;
		mToLeft = delta < 0;

		int current_x = getPositionFromValue( mCurrentValue );
		int new_x = (int) ( current_x + delta );
		int max_x = getMaxX();
		int min_x = getMinX();

		if ( !mToLeft ) {
			if ( current_x + delta > max_x ) {

				delta = Math.min( delta, MAX_DELTA );
				delta *= 1.0 - ( ( ( mOverscrollX + delta ) - max_x ) / MAX_DELTA );

				mOverscrollX = current_x + delta;

				new_x = max_x;
				mEdgeRight.onPull( delta / MAX_DELTA );

			} else {
				mOverscrollX = current_x + delta;
			}
		} else {
			if ( current_x + delta < min_x ) {

				delta = Math.max( delta, -MAX_DELTA );
				delta *= 1 - ( ( ( current_x - delta ) - min_x ) / MAX_DELTA );

				new_x = min_x;
				mEdgeLeft.onPull( delta / MAX_DELTA );
			} else {
				mOverscrollX = current_x + delta;
			}
		}
		trackMotionScroll( new_x );
		return false;
	}

	@Override
	public void onShowPress( MotionEvent e ) {}

	@Override
	public boolean onSingleTapUp( MotionEvent e ) {
		return false;
	}

	// Fling detector Listeners

	/**
	 * Range value between minimum and maximum x
	 * 
	 * @return
	 */
	public int getRange() {
		return mMaxX - mMinX;
	}

	@Override
	public int getMaxX() {
		return mMaxX;
	}

	@Override
	public int getMinX() {
		return mMinX;
	}

	@Override
	public void scrollIntoSlots() {

		if ( !mScroller.isFinished() ) {
			return;
		}

		int max_x = getMaxX();
		int min_x = getMinX();
		int scroll_x = getPositionFromValue( mCurrentValue );

		if ( scroll_x > max_x ) {
			mScroller.startUsingDistance( scroll_x, max_x - scroll_x );
			return;
		} else if ( scroll_x < min_x ) {
			mScroller.startUsingDistance( scroll_x, min_x - scroll_x );
			return;
		}

		onFinishedMovement();
	}

	private void onFinishedMovement() {
		scrollCompleted();
	}

	@Override
	public void trackMotionScroll( int newX ) {
		double newValue = getValueFromPosition( newX );
		if ( newValue != mCurrentValue ) {
			mCurrentValue = newValue;
			scrollRunning();
		}
		postInvalidate();
	}

	private void trackMotionValue( int value ) {
		mCurrentValue = value;
		scrollRunning();
		postInvalidate();
	}

	void scrollCompleted() {
		if ( mScrollListener != null ) {
			mScrollListener.onStopTrackingTouch( this );
		}
	}

	void scrollStarted() {
		if ( mScrollListener != null ) {
			mScrollListener.onStartTrackingTouch( this );
		}
	}

	void scrollRunning() {
		int value = getValue();

		if ( !mInLayout && ( Math.abs( value - mLastMotionValue ) > 3 ) ) {
			mVibrationHelper.vibrate( 8 );
			mLastMotionValue = value;
		}

		if ( mScrollListener != null ) {
			mScrollListener.OnValueChanged( this, getValue() );
		}
	}

	@Override
	public void setVibrationEnabled( boolean value ) {
		Log.i( LOG_TAG, "setVibrationEnabled: " + value );
		mVibrationHelper.setEnabled( value );
	}

	@Override
	public boolean getVibrationEnabled() {
		return mVibrationHelper.enabled();
	}

}
