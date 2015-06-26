package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Button;

import com.aviary.android.feather.R;
import com.aviary.android.feather.utils.TypefaceUtils;

public class AviaryButton extends Button {

	public AviaryButton ( Context context ) {
		this( context, null );
	}

	public AviaryButton ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryPrimaryButtonStyle );
	}

	public AviaryButton ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );

		final Theme theme = context.getTheme();
		TypedArray a = theme.obtainStyledAttributes( attrs, R.styleable.AviaryTextView, defStyle, 0 );

		String font = a.getString( R.styleable.AviaryTextView_aviary_typeface );
		setTypeface( font );

		a.recycle();
	}

	public void setTypeface( String name ) {
		if ( null != name ) {
			try {
				Typeface font = TypefaceUtils.createFromAsset( getContext().getAssets(), name );
				setTypeface( font );
			} catch ( Throwable t ) {
			}
		}
	}

	@Override
	public void setTextAppearance( Context context, int resid ) {
		Log.i( VIEW_LOG_TAG, "setTextAppearance: " + resid );
		super.setTextAppearance( context, resid );
	}
}
