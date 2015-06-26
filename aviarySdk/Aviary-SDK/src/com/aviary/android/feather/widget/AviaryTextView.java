package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.aviary.android.feather.R;
import com.aviary.android.feather.utils.TypefaceUtils;

public class AviaryTextView extends TextView {

	public AviaryTextView ( Context context ) {
		super( context );
	}

	public AviaryTextView ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryDefaultTextStyle );
	}

	public AviaryTextView ( Context context, AttributeSet attrs, int defStyle ) {
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
		super.setTextAppearance( context, resid );
	}

}
