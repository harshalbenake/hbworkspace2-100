package com.aviary.android.feather.graphics;

import it.sephiroth.android.library.imagezoom.graphics.FastBitmapDrawable;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import com.aviary.android.feather.utils.UIUtils;

public class GlowBitmapDrawable extends FastBitmapDrawable {

	@SuppressWarnings ( "unused" )
	private static final String LOG_TAG = "glow-drawable";

	private Rect mDstRect = new Rect();

	private Bitmap mPressedBitmap;
	private Bitmap mCheckedBitmap;
	private Bitmap mSelectedBitmap;

	private Bitmap mCurrent;

	private boolean mPressed;
	private boolean mChecked;
	private boolean mSelected;

	private int mHighlightColorPressed;
	private int mHighlightColorChecked;
	private int mHighlightColorSelected;

	private int mBlurValue;
	private int mGlowMode, mHighlightMode;

	public GlowBitmapDrawable ( Resources res, Bitmap bitmap, int color_pressed, int color_checked, int color_selected, int blur_size, int highlightMode,
			int glowMode ) {
		super( bitmap );
		init( color_pressed, color_checked, color_selected, blur_size, highlightMode, glowMode );
	}

	private void init( int color_pressed, int color_checked, int color_selected, int blur_size, int highlightMode, int glowMode ) {
		mHighlightColorChecked = color_checked;
		mHighlightColorPressed = color_pressed;
		mHighlightColorSelected = color_selected;
		mBlurValue = blur_size;
		mGlowMode = glowMode;
		mHighlightMode = highlightMode;

		mCurrent = getBitmap();
		recycleBitmaps();
	}

	public void setBitmap( Bitmap bitmap ) {
		super.setBitmap( bitmap );
		mCurrent = bitmap;
		recycleBitmaps();
	}

	private void recycleBitmaps() {
		if ( null != mCheckedBitmap ) {
			mCheckedBitmap.recycle();
			mCheckedBitmap = null;
		}

		if ( null != mPressedBitmap ) {
			mPressedBitmap.recycle();
			mPressedBitmap = null;
		}

		if ( null != mSelectedBitmap ) {
			mSelectedBitmap.recycle();
			mSelectedBitmap = null;
		}
	}

	public void updateConfig( int color_pressed, int color_checked, int color_selected, int blur_size, int highlightMode, int glowMode ) {
		init( color_pressed, color_checked, color_selected, blur_size, highlightMode, glowMode );
		setState( getState() );
	}

	public static Bitmap generateBlurBitmap( Bitmap src, int blurValue, int color, Mode mode, boolean glow, Paint paint ) {

		int width = src.getWidth();
		int height = src.getHeight();

		Bitmap dest = Bitmap.createBitmap( width, height, src.getConfig() );

		Canvas canvas = new Canvas( dest );
		Bitmap alpha = src.extractAlpha();
		canvas.drawBitmap( src, 0, 0, paint );

		Paint paintBlur = new Paint();
		paintBlur.setXfermode( new PorterDuffXfermode( mode ) );
		paintBlur.setColor( color );
		canvas.drawBitmap( alpha, 0, 0, paintBlur );

		if ( glow ) {
			BlurMaskFilter maskFilter = new BlurMaskFilter( blurValue, BlurMaskFilter.Blur.NORMAL );
			paintBlur.setMaskFilter( maskFilter );
			paintBlur.setAlpha( 100 );
			canvas.drawBitmap( alpha, 0, 0, paintBlur );
		}

		return dest;
	}

	@Override
	public boolean isStateful() {
		return true;
	}

	@Override
	public void draw( Canvas canvas ) {
		copyBounds( mDstRect );
		canvas.drawBitmap( mCurrent, null, mDstRect, getPaint() );
	}

	@Override
	protected boolean onStateChange( int[] state ) {

		boolean checked = mChecked;
		boolean pressed = mPressed;
		boolean selected = mSelected;

		mChecked = false;
		mPressed = false;
		mSelected = false;

		for ( int i = 0; i < state.length; i++ ) {
			if ( state[i] == android.R.attr.state_pressed ) {
				mPressed = true;
				continue;
			}

			if ( state[i] == android.R.attr.state_checked ) {
				mChecked = true;
				continue;
			}

			if ( state[i] == android.R.attr.state_selected ) {
				mSelected = true;
				continue;
			}
		}

		if ( mPressed && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_PRESSED ) ) {
			if ( mPressedBitmap == null ) {
				mPressedBitmap = generateBlurBitmap( getBitmap(), mBlurValue, mHighlightColorPressed, Mode.DARKEN,
						UIUtils.checkBits( mGlowMode, UIUtils.GLOW_MODE_PRESSED ), getPaint() );
			}
			mCurrent = mPressedBitmap;

		} else if ( mChecked && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_CHECKED ) ) {
			if ( mCheckedBitmap == null ) {
				mCheckedBitmap = generateBlurBitmap( getBitmap(), mBlurValue, mHighlightColorChecked, Mode.DARKEN,
						UIUtils.checkBits( mGlowMode, UIUtils.GLOW_MODE_CHECKED ), getPaint() );
			}
			mCurrent = mCheckedBitmap;

		} else if ( mSelected && UIUtils.checkBits( mHighlightMode, UIUtils.HIGHLIGHT_MODE_SELECTED ) ) {
			if ( mSelectedBitmap == null ) {
				mSelectedBitmap = generateBlurBitmap( getBitmap(), mBlurValue, mHighlightColorSelected, Mode.DARKEN,
						UIUtils.checkBits( mGlowMode, UIUtils.GLOW_MODE_SELECTED ), getPaint() );
			}
			mCurrent = mSelectedBitmap;

		} else {
			mCurrent = getBitmap();
		}

		return checked != mChecked || pressed != mPressed || selected != mSelected;

	}

}
