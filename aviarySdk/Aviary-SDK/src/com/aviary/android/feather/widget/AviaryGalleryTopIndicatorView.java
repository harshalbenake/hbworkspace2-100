package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.aviary.android.feather.R;
import com.aviary.android.feather.graphics.GalleryTopIndicatorDrawable;

public class AviaryGalleryTopIndicatorView extends LinearLayout {

	public AviaryGalleryTopIndicatorView ( Context context ) {
		this( context, null );
	}

	public AviaryGalleryTopIndicatorView ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryGalleryTopIndicatorStyle );
	}

	@SuppressWarnings ( "deprecation" )
	public AviaryGalleryTopIndicatorView ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs );

		Theme theme = context.getTheme();

		TypedArray array = theme.obtainStyledAttributes( attrs, R.styleable.AviaryGalleryIndicatorView, defStyle, 0 );
		int resId = array.getResourceId( R.styleable.AviaryGalleryIndicatorView_aviary_drawableStyle, 0 );
		array.recycle();

		if ( resId != 0 ) {
			setBackgroundDrawable( new GalleryTopIndicatorDrawable( context, 0, resId ) );
		} else {
			setBackgroundDrawable( new GalleryTopIndicatorDrawable( context ) );
		}

	}

}
