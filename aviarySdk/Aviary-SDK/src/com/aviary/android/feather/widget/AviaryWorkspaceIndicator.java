package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.aviary.android.feather.R;

public class AviaryWorkspaceIndicator extends LinearLayout {

	int mResId;
	int mSelected;
	int mResWidth = -1;
	int mResHeight = -1;

	public AviaryWorkspaceIndicator ( Context context, AttributeSet attrs ) {
		super( context, attrs );
		init( context, attrs, 0 );
	}

	private void init( Context context, AttributeSet attrs, int defStyle ) {
		Theme theme = context.getTheme();
		TypedArray a = theme.obtainStyledAttributes( attrs, R.styleable.AviaryWorkspaceIndicator, defStyle, 0 );
		setOrientation( LinearLayout.HORIZONTAL );

		mResId = a.getResourceId( R.styleable.AviaryWorkspaceIndicator_aviary_indicatorId, 0 );

		a.recycle();

		if ( mResId > 0 ) {
			Drawable d = getContext().getResources().getDrawable( mResId );
			mResWidth = d.getIntrinsicWidth();
			mResHeight = d.getIntrinsicHeight();
		}

	}

	@Override
	protected void onLayout( boolean changed, int l, int t, int r, int b ) {
		super.onLayout( changed, l, t, r, b );
	}

	void resetView( int count ) {
		removeAllViews();

		if ( mResId != 0 && count > 0 ) {

			int h = getHeight();

			if ( mResWidth > 0 ) {
				float ratio = (float) mResHeight / h;
				if ( mResHeight > h ) {
					mResHeight = h;
					mResWidth = (int) ( mResWidth / ratio );
				}
			} else {
				mResWidth = LinearLayout.LayoutParams.WRAP_CONTENT;
				mResHeight = LinearLayout.LayoutParams.MATCH_PARENT;
			}

			for ( int i = 0; i < count; i++ ) {
				ImageView v = new ImageView( getContext() );

				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams( mResWidth, mResHeight );
				v.setImageResource( mResId );
				v.setSelected( false );
				v.setPadding( 2, 0, 2, 0 );
				v.setLayoutParams( params );
				addView( v );
			}
		}
	}

	public void setLevel( int mCurrentScreen, int mItemCount ) {

		if ( getChildCount() != mItemCount ) {
			resetView( mItemCount );
			mSelected = 0;
		}

		if ( mCurrentScreen >= 0 && mCurrentScreen < getChildCount() ) {
			getChildAt( mSelected ).setSelected( false );
			getChildAt( mCurrentScreen ).setSelected( true );
			mSelected = mCurrentScreen;
		}
	}

}
