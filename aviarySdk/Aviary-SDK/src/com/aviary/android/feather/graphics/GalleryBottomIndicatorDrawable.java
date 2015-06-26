package com.aviary.android.feather.graphics;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.aviary.android.feather.R;

/**
 * Drawable used to draw the gallery top indicator
 * 
 * @author alessandro
 */
public class GalleryBottomIndicatorDrawable extends Drawable {

	int mShadowColor, mFillColor, mStrokeColor1, mStrokeColor2;
	float mIndicatorSize;
	int mStrokeWidth;
	int mShadowDy;

	final Rect destRect = new Rect();
	final Paint paint;

	public GalleryBottomIndicatorDrawable ( Context context ) {
		Theme theme = context.getTheme();
		TypedArray array = theme.obtainStyledAttributes( null, R.styleable.AviaryGalleryTopIndicator, R.attr.aviaryOptionPanelTopIndicatorStyle, 0 );

		mShadowColor = array.getColor( R.styleable.AviaryGalleryTopIndicator_android_shadowColor, 0 );
		mShadowDy = (int) array.getFloat( R.styleable.AviaryGalleryTopIndicator_android_shadowDy, 0f );
		mFillColor = array.getColor( R.styleable.AviaryGalleryTopIndicator_aviary_color1, Color.WHITE );
		mStrokeColor1 = array.getColor( R.styleable.AviaryGalleryTopIndicator_aviary_strokeColor, Color.WHITE );
		mStrokeColor2 = array.getColor( R.styleable.AviaryGalleryTopIndicator_aviary_strokeColor2, Color.WHITE );
		mIndicatorSize = array.getFloat( R.styleable.AviaryGalleryTopIndicator_aviary_indicatorSize, 1f );
		mStrokeWidth = array.getDimensionPixelSize( R.styleable.AviaryGalleryTopIndicator_aviary_strokeWidth, 2 );

		array.recycle();

		paint = new Paint( Paint.ANTI_ALIAS_FLAG );
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
		canvas.drawLine( left + mStrokeWidth, top + mStrokeWidth, right - mStrokeWidth, top + mStrokeWidth, paint );

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
