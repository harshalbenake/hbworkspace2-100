package com.aviary.android.feather.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.StateSet;

import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.utils.UIUtils;

public class GlowDrawable extends Drawable {

	private static int mCount = 0;

	private int mThisCount;

	private static Logger logger = LoggerFactory.getLogger( "glow-drawable", LoggerType.ConsoleLoggerType );

	// The original drawable
	protected Drawable mDrawable;

	// paint used to draw the current bitmap
	protected Paint mPaint;

	// paint used to generate the highlight bitmap
	protected Paint mPaintBlur;

	// holds the current bounds
	private Rect mDstRect = new Rect();

	// the background bitmap used to generate the current bitmap
	private Bitmap mBackground;

	/**
	 * boolean to indicate if the generated bitmap should be drawn or
	 * the original drawable instead
	 */
	private boolean mDraw;

	// canvas used to generate the current bitmap
	private Canvas tmpCanvas = new Canvas();

	/**
	 * Holds the current {@link StateSet}
	 */
	private GlowDrawableState mCurrentStateSet = new GlowDrawableState();

	private int mHighlightColorPressed;
	private int mHighlightColorChecked;
	private int mHighlightColorSelected;
	private int mBlurValue;
	private int mGlowMode, mHighlightMode;

	public GlowDrawable ( Resources res, Drawable drawable, int color_pressed, int color_checked, int color_selected, int blur_size, int highlightMode,
			int glowMode ) {
		super();

		mThisCount = ++mCount;

		mPaint = new Paint();
		mPaint.setDither( true );
		mPaint.setFilterBitmap( true );

		mPaintBlur = new Paint();
		mPaintBlur.setXfermode( new PorterDuffXfermode( Mode.DARKEN ) );

		initialize( color_pressed, color_checked, color_selected, blur_size, highlightMode, glowMode );
		setDrawable( drawable );
	}

	private void initialize( int color_pressed, int color_checked, int color_selected, int blur_size, int highlightMode, int glowMode ) {
		mHighlightColorChecked = color_checked;
		mHighlightColorPressed = color_pressed;
		mHighlightColorSelected = color_selected;
		mBlurValue = blur_size;
		mGlowMode = glowMode;
		mHighlightMode = highlightMode;
	}

	public void update( int color_pressed, int color_checked, int color_selected, int blur_size, int highlightMode, int glowMode ) {
		initialize( color_pressed, color_checked, color_selected, blur_size, highlightMode, glowMode );
		setState( getState() );
	}

	public void setDrawable( Drawable drawable ) {
		mDrawable = drawable;
	}

	void invalidateBackground( int width, int height ) {

		logger.info( this + ", invalidateBitmap, current: " + mBackground + ", size: " + width + "x" + height );

		if ( width > 0 && height > 0 ) {
			if ( mBackground != null ) {
				if ( mBackground.getWidth() != width || mBackground.getHeight() != height || mBackground.isRecycled() ) {
					recycleBackground();
					mBackground = Bitmap.createBitmap( width, height, Config.ARGB_8888 );
				}
			} else {
				mBackground = Bitmap.createBitmap( width, height, Config.ARGB_8888 );
			}
		} else {
			recycleBackground();
		}
	}

	private void recycleBackground() {
		if ( null != mBackground && !mBackground.isRecycled() ) {
			mBackground.recycle();
		}
		mBackground = null;
	}

	public Bitmap generateBitmap( Drawable src, int color, boolean glow ) {
		logger.info( this + ", generateBitmap" );

		Bitmap bitmap;

		if ( src instanceof BitmapDrawable ) {
			bitmap = ( (BitmapDrawable) src ).getBitmap();
		} else {
			bitmap = Bitmap.createBitmap( mBackground.getWidth(), mBackground.getHeight(), Config.ARGB_8888 );
			tmpCanvas.setBitmap( bitmap );
			src.draw( tmpCanvas );
		}

		mBackground.eraseColor( 0 );

		tmpCanvas.setBitmap( mBackground );
		Bitmap alpha = bitmap.extractAlpha();
		src.draw( tmpCanvas );

		mPaintBlur.setMaskFilter( null );
		mPaintBlur.setColor( color );
		tmpCanvas.drawBitmap( alpha, 0, 0, mPaintBlur );

		if ( glow ) {
			BlurMaskFilter maskFilter = new BlurMaskFilter( mBlurValue, BlurMaskFilter.Blur.NORMAL );
			mPaintBlur.setMaskFilter( maskFilter );
			mPaintBlur.setAlpha( 100 );
			tmpCanvas.drawBitmap( alpha, 0, 0, mPaintBlur );
		}

		return mBackground;
	}

	@Override
	public boolean isStateful() {
		return true;
	}

	@Override
	public void draw( Canvas canvas ) {
		copyBounds( mDstRect );

		if ( mDraw && mBackground != null && !mBackground.isRecycled() ) {
			canvas.drawBitmap( mBackground, null, mDstRect, getPaint() );
		} else {
			mDrawable.draw( canvas );
		}
	}

	public Paint getPaint() {
		return mPaint;
	}

	@Override
	public int getIntrinsicHeight() {
		return mDrawable.getIntrinsicHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return mDrawable.getIntrinsicWidth();
	}

	@Override
	public int getMinimumHeight() {
		return mDrawable.getMinimumHeight();
	}

	@Override
	public int getMinimumWidth() {
		return mDrawable.getMinimumWidth();
	}

	@Override
	public boolean setState( int[] stateSet ) {
		mDrawable.setState( stateSet );
		return super.setState( stateSet );
	}

	@Override
	protected void onBoundsChange( Rect bounds ) {
		mDrawable.setBounds( bounds );
		super.onBoundsChange( bounds );
		invalidateBackground( bounds.width(), bounds.height() );
	}

	@Override
	protected boolean onStateChange( int[] state ) {

		boolean isChanged = mCurrentStateSet.updateStateSet( state );

		if ( isChanged && mBackground != null ) {

			logger.log( this + ", onStateChange: " + mCurrentStateSet.toString() + ", changed: " + isChanged );

			// state priority:
			// - pressed
			// - checked
			// - selected

			if ( mCurrentStateSet.pressed && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_PRESSED ) ) {
				generateBitmap( mDrawable, mHighlightColorPressed, UIUtils.checkBits( mGlowMode, UIUtils.GLOW_MODE_PRESSED ) );
				mDraw = true;
			} else if ( mCurrentStateSet.checked && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_CHECKED ) ) {
				generateBitmap( mDrawable, mHighlightColorChecked, UIUtils.checkBits( mGlowMode, UIUtils.GLOW_MODE_CHECKED ) );
				mDraw = true;
			} else if ( mCurrentStateSet.selected && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_SELECTED ) ) {
				generateBitmap( mDrawable, mHighlightColorSelected, UIUtils.checkBits( mGlowMode, UIUtils.GLOW_MODE_SELECTED ) );
				mDraw = true;
			} else {
				// mCurrent = null;
				mDraw = false;
			}
		}

		return isChanged;

	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha( int alpha ) {
		mPaint.setAlpha( alpha );
	}

	@Override
	public void setColorFilter( ColorFilter cf ) {
		mPaint.setColorFilter( cf );
	}

	@Override
	public String toString() {
		return "GlowDrawable(" + mThisCount + ")";
	}

	static public boolean stateSetContains( int[] stateSpec, int value ) {
		int stateSetSpecSize = stateSpec.length;

		for ( int i = 0; i < stateSetSpecSize; i++ ) {
			int stateSpecState = stateSpec[i];

			if ( stateSpecState > 0 ) {
				if ( stateSpecState == value ) {
					return true;
				}
			}
		}
		return false;
	}

	static class GlowDrawableState {
		boolean pressed, checked, selected;

		public boolean updateStateSet( int[] stateSpec ) {
			int stateSetSpecSize = stateSpec.length;
			boolean tmp_pressed = false, tmp_checked = false, tmp_selected = false;

			for ( int i = 0; i < stateSetSpecSize; i++ ) {
				int stateSpecState = stateSpec[i];

				if ( stateSpecState > 0 ) {
					if ( stateSpecState == android.R.attr.state_selected ) {
						tmp_selected = true;
					} else if ( stateSpecState == android.R.attr.state_pressed ) {
						tmp_pressed = true;
					} else if ( stateSpecState == android.R.attr.state_checked ) {
						tmp_checked = true;
					}
				}
			}

			boolean changed = ( pressed != tmp_pressed ) || ( checked != tmp_checked ) || ( selected != tmp_selected );

			pressed = tmp_pressed;
			checked = tmp_checked;
			selected = tmp_selected;

			return changed;
		}

		@Override
		public String toString() {
			return "{ pressed: " + pressed + ", checked: " + checked + ", selected: " + selected + " }";
		}
	}

}
