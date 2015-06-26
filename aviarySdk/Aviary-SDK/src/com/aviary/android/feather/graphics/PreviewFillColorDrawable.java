package com.aviary.android.feather.graphics;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
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
import android.graphics.drawable.Drawable;

import com.aviary.android.feather.R;

public class PreviewFillColorDrawable extends Drawable {

	final int mStrokeWidth;
	final int mStrokeColor;
	final Paint mPaint;
	float mRadius;
	final Rect mDstRect = new Rect();
	final Matrix mGradientMatrix;
	boolean mRadiusFixed = false;

	LinearGradient mGradient;
	int mColor;
	int mLightenColor;

	private boolean mChecked;
	private boolean mPressed;

	public PreviewFillColorDrawable ( Context context ) {
		super();

		Theme theme = context.getTheme();
		TypedArray a = theme.obtainStyledAttributes( null, R.styleable.AviaryPreviewFillDrawable, R.attr.aviaryPreviewFillDrawableStyle, 0 );

		mStrokeWidth = a.getDimensionPixelSize( R.styleable.AviaryPreviewFillDrawable_aviary_strokeWidth, 20 );
		mStrokeColor = a.getColor( R.styleable.AviaryPreviewFillDrawable_aviary_strokeColor, Color.BLACK );
		mRadius = a.getInteger( R.styleable.AviaryPreviewFillDrawable_aviary_radius, 50 ) / 100.0f;
		a.recycle();

		mPaint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG | Paint.FILTER_BITMAP_FLAG );
		mPaint.setStyle( Paint.Style.FILL );

		mGradient = new LinearGradient( 0, 0, 0, 1, new int[] { Color.WHITE, Color.BLACK }, new float[] { 0.5f, 1 }, TileMode.CLAMP );
		mGradientMatrix = new Matrix();
	}

	public int getColor() {
		return mColor;
	}

	public float getRadius() {
		return mRadius;
	}

	public boolean isFixedRadius() {
		return mRadiusFixed;
	}

	public void setRadius( float value ) {
		mRadius = value;
		invalidateSelf();
	}

	public void setColor( int color ) {
		mColor = color;

		float[] hsv = new float[3];
		Color.colorToHSV( color, hsv );

		int red = Color.red( color );
		int green = Color.green( color );
		int blue = Color.blue( color );

		mLightenColor = Color.argb( 255, (int) ( red * 0.5 + 127 ), (int) ( green * 0.5 + 127 ), (int) ( blue * 0.5 + 127 ) );

		hsv[1] *= 1.1f; // saturation
		hsv[2] *= 0.3f; // value

		int bottomColor = Color.HSVToColor( hsv );

		mGradient = new LinearGradient( 0, 0, 0, 1, new int[] { mColor, bottomColor }, new float[] { 0.0f, 1 }, TileMode.CLAMP );
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
			radius = Math.min( mDstRect.width(), mDstRect.height() ) * 0.5f * mRadius;
		}

		// reset the paint
		mPaint.setShader( null );
		mPaint.setMaskFilter( null );
		mPaint.setXfermode( null );

		// draw the black border around the circle
		mPaint.setMaskFilter( null );
		mPaint.setColor( mStrokeColor );
		canvas.drawCircle( mDstRect.centerX(), mDstRect.centerY(), radius + mStrokeWidth, mPaint );

		// draw the circle fill
		canvas.saveLayer( mDstRect.left, mDstRect.top, mDstRect.right, mDstRect.bottom, mPaint, Canvas.ALL_SAVE_FLAG );

		mPaint.setColor( mLightenColor );
		canvas.drawCircle( mDstRect.centerX(), mDstRect.centerY(), radius, mPaint );

		mGradientMatrix.reset();
		mGradientMatrix.postScale( 1, radius * 4 );
		mGradientMatrix.postTranslate( 0, mDstRect.centerY() + 2 - radius * 2 );
		mGradient.setLocalMatrix( mGradientMatrix );

		mPaint.setXfermode( new PorterDuffXfermode( Mode.SRC_IN ) );
		mPaint.setColor( Color.WHITE );
		mPaint.setShader( mGradient );
		canvas.drawCircle( mDstRect.centerX(), mDstRect.centerY() + 2, radius, mPaint );

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

		return checked != mChecked || pressed != mPressed;

	}
}
