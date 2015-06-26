package com.aviary.android.feather.graphics;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.aviary.android.feather.R;

/**
 * Drawable used to draw the gallery top indicator
 * 
 * @author alessandro
 */
public class GalleryTopIndicatorDrawable extends Drawable {

	static final String LOG_TAG = "gallery-top-indicator";

	static final int DIRECTION_TOP_DOWN = 1;
	static final int DIRECTION_BOTTOM_TOP = 2;

	int mShadowColor, mFillColor, mStrokeColor1, mStrokeColor2;
	float mIndicatorSize;
	int mStrokeWidth;
	int mShadowDy;
	int mDirection;
	int mOffsetY;
	int mMinHeight;
	Matrix mMatrix = new Matrix();

	final Rect destRect = new Rect();
	final Paint paint;

	public GalleryTopIndicatorDrawable ( Context context ) {
		this( context, R.attr.aviaryOptionPanelTopIndicatorStyle, 0 );
	}

	public GalleryTopIndicatorDrawable ( Context context, int defStyle ) {
		this( context, defStyle, 0 );
	}

	public GalleryTopIndicatorDrawable ( Context context, int defStyle, int defStyleRes ) {
		Theme theme = context.getTheme();
		TypedArray array = theme.obtainStyledAttributes( null, R.styleable.AviaryGalleryTopIndicator, defStyle, defStyleRes );

		Log.d( LOG_TAG, "defaultStyle: " + defStyle );

		mShadowColor = array.getColor( R.styleable.AviaryGalleryTopIndicator_android_shadowColor, 0 );
		mShadowDy = (int) array.getFloat( R.styleable.AviaryGalleryTopIndicator_android_shadowDy, 0f );
		mFillColor = array.getColor( R.styleable.AviaryGalleryTopIndicator_aviary_color1, Color.WHITE );
		mStrokeColor1 = array.getColor( R.styleable.AviaryGalleryTopIndicator_aviary_strokeColor, Color.WHITE );
		mStrokeColor2 = array.getColor( R.styleable.AviaryGalleryTopIndicator_aviary_strokeColor2, Color.WHITE );
		mIndicatorSize = array.getFloat( R.styleable.AviaryGalleryTopIndicator_aviary_indicatorSize, 1f );
		mStrokeWidth = array.getDimensionPixelSize( R.styleable.AviaryGalleryTopIndicator_aviary_strokeWidth, 2 );
		mDirection = array.getInteger( R.styleable.AviaryGalleryTopIndicator_aviary_direction, 1 );
		mOffsetY = array.getDimensionPixelSize( R.styleable.AviaryGalleryTopIndicator_aviary_offsety, 0 );
		mMinHeight = array.getDimensionPixelSize( R.styleable.AviaryGalleryTopIndicator_android_minHeight, 0 );

		Log.i( LOG_TAG, "strokeWidth: " + mStrokeWidth );
		Log.i( LOG_TAG, "direction: " + mDirection );
		Log.i( LOG_TAG, "offset: " + mOffsetY );

		array.recycle();

		paint = new Paint( Paint.ANTI_ALIAS_FLAG );
	}

	@Override
	public int getMinimumHeight() {
		return mMinHeight;
	}

	@Override
	public int getIntrinsicHeight() {
		return mMinHeight;
	}

	@Override
	public void draw( Canvas canvas ) {
		copyBounds( destRect );

		int halfHeight = destRect.height() / 2;
		int halfWidth = destRect.width() / 2;
		int triangleSize = (int) ( halfHeight * mIndicatorSize );

		int top = destRect.top + mStrokeWidth / 2;
		int left = destRect.left - mStrokeWidth;
		int right = destRect.right + mStrokeWidth;

		Path path = new Path();
		path.moveTo( left, top );
		path.lineTo( right, top );
		path.lineTo( right, top + halfHeight );

		path.lineTo( left + halfWidth + triangleSize, top + halfHeight );
		path.lineTo( left + halfWidth, top + halfHeight + triangleSize );
		path.lineTo( left + halfWidth - triangleSize, top + halfHeight );

		path.lineTo( left, top + halfHeight );
		path.lineTo( left, top );

		if ( mDirection == DIRECTION_BOTTOM_TOP ) {
			mMatrix.reset();
			mMatrix.setScale( 1, -1, 0, destRect.height() / 2 );
			mMatrix.postTranslate( 0, mOffsetY );
			canvas.save( Canvas.MATRIX_SAVE_FLAG );
			canvas.concat( mMatrix );
		}

		paint.setStyle( Style.FILL );

		if ( mShadowDy > 0 ) {
			path.offset( 0, mShadowDy );
			paint.setColor( mShadowColor );
			canvas.drawPath( path, paint );
			path.offset( 0, -mShadowDy );
		}

		paint.setColor( mFillColor );
		canvas.drawPath( path, paint );

		paint.setStyle( Style.STROKE );
		paint.setColor( mStrokeColor1 );
		paint.setStrokeWidth( mStrokeWidth );
		canvas.drawPath( path, paint );

		paint.setStyle( Style.STROKE );
		paint.setColor( mStrokeColor2 );

		if ( mDirection == DIRECTION_TOP_DOWN ) {
			canvas.drawLine( left + mStrokeWidth, top + mStrokeWidth, right - mStrokeWidth, top + mStrokeWidth, paint );
		} else {
			top = top + halfHeight - mStrokeWidth;
			canvas.drawLine( left, top, left + halfWidth - triangleSize, top, paint );
			canvas.drawLine( left + halfWidth + triangleSize, top, right, top, paint );
			canvas.restore();
		}
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha( int alpha ) {}

	@Override
	public void setColorFilter( ColorFilter cf ) {}

}
