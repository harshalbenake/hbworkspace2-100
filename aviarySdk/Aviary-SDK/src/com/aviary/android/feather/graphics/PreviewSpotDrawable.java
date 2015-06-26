package com.aviary.android.feather.graphics;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.Xfermode;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.aviary.android.feather.R;

public class PreviewSpotDrawable extends Drawable {

	private static final String LOG_TAG = "PreviewSpotDrawable";

	final int mStrokeWidth, mStrokeWidthOuter;
	final int mGlowColor;
	final int mStrokeColor;
	final int mBackgroundColorUnselected, mBackgroundColorSelected;
	final Paint mPaint;
	float mRadius;
	final Rect mDstRect = new Rect();
	final LinearGradient mGradientShaderUnselected;
	final LinearGradient mGradientShaderSelected;
	final BlurMaskFilter mGlowBlurMaskFilter;
	final Matrix mGradientMatrix;
	boolean mRadiusFixed = false;

	LinearGradient mGradient;

	private boolean mChecked;
	private boolean mPressed;

	private Xfermode mPorterDuffSrcInMode = new PorterDuffXfermode( Mode.SRC_IN );

	public PreviewSpotDrawable ( Context context ) {
		super();

		Theme theme = context.getTheme();
		TypedArray a = theme.obtainStyledAttributes( null, R.styleable.AviaryPreviewSpotDrawable, R.attr.aviaryPreviewSpotDrawableStyle, 0 );
		mStrokeWidth = a.getDimensionPixelSize( R.styleable.AviaryPreviewSpotDrawable_aviary_strokeWidth, 20 );
		mStrokeWidthOuter = (int) ( mStrokeWidth * 1.7 );

		int color1 = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_color1, Color.WHITE );
		int color2 = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_color2, Color.BLACK );
		int color3 = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_color3, Color.BLACK );
		int color4 = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_color4, Color.WHITE );
		mGlowColor = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_highlightColorChecked, Color.WHITE );
		mBackgroundColorUnselected = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_bg_color1, Color.WHITE );
		mBackgroundColorSelected = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_bg_color2, Color.BLACK );
		mStrokeColor = a.getColor( R.styleable.AviaryPreviewSpotDrawable_aviary_strokeColor, Color.BLACK );

		int glowSize = a.getInteger( R.styleable.AviaryPreviewSpotDrawable_aviary_glowSize, 3 );

		a.recycle();

		mPaint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG );
		mPaint.setStyle( Paint.Style.STROKE );

		mGradientShaderUnselected = new LinearGradient( 0, 0, 0, 1, new int[] { color1, color2 }, new float[] { 0.5f, 1 }, TileMode.CLAMP );
		mGradientShaderSelected = new LinearGradient( 0, 0, 0, 1, new int[] { color3, color4 }, new float[] { 0.5f, 1 }, TileMode.CLAMP );
		mGradient = mGradientShaderUnselected;

		mGlowBlurMaskFilter = new BlurMaskFilter( glowSize, BlurMaskFilter.Blur.NORMAL );

		mGradientMatrix = new Matrix();
		mGradientShaderUnselected.setLocalMatrix( mGradientMatrix );
		mRadius = 10;
	}

	public void setRadius( float value ) {
		Log.i( LOG_TAG, "setRadius: " + value );
		mRadius = value;
		invalidateSelf();
	}

	public void setFixedRadius( float value ) {
		mRadiusFixed = true;
		mRadius = value;
		invalidateSelf();
	}

	@Override
	public void draw( Canvas canvas ) {
		copyBounds( mDstRect );

		float radius = mRadius;

		if ( !mRadiusFixed ) {
			radius = Math.min( mDstRect.width(), mDstRect.height() ) * 0.8f * mRadius;
		}

		// reset the paint
		mPaint.setShader( null );
		mPaint.setMaskFilter( null );
		mPaint.setXfermode( null );

		// outer stroke width

		// outside glow when selected
		if ( mChecked ) {
			mPaint.setStrokeWidth( mStrokeWidth );
			mPaint.setMaskFilter( mGlowBlurMaskFilter );
			mPaint.setColor( mGlowColor );
			canvas.drawCircle( mDstRect.centerX(), mDstRect.centerY(), radius, mPaint );
		}

		// draw the black border around the circle
		mPaint.setStrokeWidth( mStrokeWidthOuter );
		mPaint.setMaskFilter( null );
		mPaint.setColor( mStrokeColor );
		canvas.drawCircle( mDstRect.centerX(), mDstRect.centerY(), radius, mPaint );

		// draw the circle fill
		canvas.saveLayer( mDstRect.left, mDstRect.top, mDstRect.right, mDstRect.bottom, mPaint, Canvas.ALL_SAVE_FLAG );

		mPaint.setStrokeWidth( mStrokeWidth );
		mPaint.setColor( mChecked ? mBackgroundColorSelected : mBackgroundColorUnselected );
		canvas.drawCircle( mDstRect.centerX(), mDstRect.centerY(), radius, mPaint );

		mGradientMatrix.reset();
		mGradientMatrix.postScale( 1, radius * 2 );
		mGradientMatrix.postTranslate( 0, mDstRect.centerY() + 3 - radius * 2 );
		mGradient.setLocalMatrix( mGradientMatrix );

		mPaint.setXfermode( mPorterDuffSrcInMode );
		mPaint.setColor( Color.WHITE );
		mPaint.setShader( mGradient );
		canvas.drawCircle( mDstRect.centerX(), mDstRect.centerY() + 3, radius, mPaint );

		// Paint p = new Paint();
		// p.setColor( 0x33ff0000 );
		// canvas.drawRect( mDstRect.centerX() - radius, mDstRect.centerY() - radius,
		// mDstRect.centerX() + radius, mDstRect.centerY() + radius, p );

		canvas.restore();
	}

	@Override
	public boolean isStateful() {
		return true;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha( int alpha ) {}

	@Override
	public void setColorFilter( ColorFilter cf ) {}

	@Override
	protected boolean onStateChange( int[] state ) {

		boolean checked = mChecked;
		boolean pressed = mPressed;

		mChecked = false;
		mPressed = false;

		for ( int i = 0; i < state.length; i++ ) {
			if ( state[i] == android.R.attr.state_pressed ) {
				mPressed = true;
			}

			if ( state[i] == android.R.attr.state_selected ) {
				mChecked = true;
			}
		}

		if ( mChecked ) {
			mGradient = mGradientShaderSelected;
		} else {
			mGradient = mGradientShaderUnselected;
		}

		return checked != mChecked || pressed != mPressed;

	}
}
