package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.SeekBar;

import com.aviary.android.feather.R;

public class AviarySeekBar extends SeekBar {

	@SuppressWarnings ( "unused" )
	private static final String LOG_TAG = "seekbar";

	protected Drawable mSecondary;
	protected Drawable mSecondaryInverted;
	protected Drawable mSecondaryCenter;

	protected int mSecondaryMinWidth;
	protected int mSecondaryMinHeight;
	protected double mSecondaryCenterOffset;
	protected int mBackgroundOffset;
	private int mRealWidth;
	private Drawable mCurrentDrawable;

	public AviarySeekBar ( Context context ) {
		this( context, null );
	}

	public AviarySeekBar ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviarySeekBarStyle );
	}

	public AviarySeekBar ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );

		final Theme theme = context.getTheme();
		TypedArray typedArray = theme.obtainStyledAttributes( attrs, R.styleable.AviarySeekBar, defStyle, 0 );
		Drawable thumb = typedArray.getDrawable( R.styleable.AviarySeekBar_aviarySeekBarThumb );
		int offset = typedArray.getDimensionPixelOffset( R.styleable.AviarySeekBar_aviarySeekBarThumbOffset, 0 );
		mSecondary = typedArray.getDrawable( R.styleable.AviarySeekBar_aviarySeekBarSecondary );
		mSecondaryInverted = typedArray.getDrawable( R.styleable.AviarySeekBar_aviarySeekBarSecondaryInverted );
		mSecondaryCenter = typedArray.getDrawable( R.styleable.AviarySeekBar_aviarySeekBarSecondaryCenter );
		typedArray.recycle();

		mSecondaryMinWidth = mSecondary.getIntrinsicWidth();
		mSecondaryMinHeight = mSecondary.getIntrinsicHeight();
		mSecondaryCenterOffset = (double) mSecondaryMinWidth * 0.5;

		mBackgroundOffset = (int) ( getProgressDrawable().getIntrinsicWidth() * 0.12 );

		// thumb.setAlpha( 50 );
		setThumb( thumb );
		setThumbOffset( offset );
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();

		int[] state = getDrawableState();
		mSecondary.setState( state );
		mSecondaryInverted.setState( state );
		mSecondaryCenter.setState( state );
	}

	@Override
	protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
		super.onSizeChanged( w, h, oldw, oldh );
		mRealWidth = ( w - ( getPaddingLeft() + getPaddingRight() ) ) - mBackgroundOffset;
	}

	@Override
	protected synchronized void onDraw( Canvas canvas ) {

		if ( mRealWidth < 1 ) return;

		double progress = ( getProgress() - 50.0 ) / 50.0; // 0.0 - 1.0
		double center = -getPaddingLeft() + ( getWidth() / 2.0 );
		double w = progress * mRealWidth / 2.0;

		if ( getProgressDrawable() instanceof LayerDrawable ) {

			// retrieve the current drawable state ( selected, pressed, ... )
			LayerDrawable layerDrawable = (LayerDrawable) getProgressDrawable();

			int left = 0;
			int right = 0;
			Drawable drawable = null;

			if ( progress > 0 ) {
				// right
				left = (int) ( center - mSecondaryCenterOffset );
				right = (int) ( left + w + mSecondaryCenterOffset );
				drawable = mSecondary;
			} else if ( progress < 0 ) {
				// left
				left = (int) ( center + w );
				right = (int) ( center + mSecondaryCenterOffset );
				drawable = mSecondaryInverted;
			}

			if ( ( right - left ) < mSecondaryMinWidth ) {
				// center
				left = (int) ( center - ( mSecondaryMinWidth / 2 ) );
				right = (int) ( center + ( mSecondaryMinWidth / 2 ) );
				drawable = mSecondaryCenter;
			}

			if ( mCurrentDrawable != drawable ) {
				mCurrentDrawable = drawable;
				layerDrawable.setDrawableByLayerId( android.R.id.secondaryProgress, mCurrentDrawable );
			}

			if ( null != drawable ) {
				drawable.setBounds( left, 0, right, mSecondaryMinHeight );
			}
		}
		super.onDraw( canvas );
	}
}
