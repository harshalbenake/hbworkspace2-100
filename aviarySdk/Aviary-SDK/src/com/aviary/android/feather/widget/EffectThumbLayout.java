package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.easing.Easing;
import it.sephiroth.android.library.imagezoom.easing.Linear;
import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Checkable;
import android.widget.RelativeLayout;

import com.aviary.android.feather.R;

public class EffectThumbLayout extends RelativeLayout implements Checkable {

	private boolean mChecked;
	private boolean mOpened;
	private int mThumbAnimationDuration;
	private View mHiddenView;
	private View mImageView;

	public EffectThumbLayout ( Context context, AttributeSet attrs ) {
		super( context, attrs );
		init( context, attrs, R.attr.aviaryEffectThumbLayoutStyle );
		mOpened = false;
		mChecked = false;
	}

	private void init( Context context, AttributeSet attrs, int defStyle ) {
		Log.i( VIEW_LOG_TAG, "init" );
		Theme theme = context.getTheme();
		TypedArray array = theme.obtainStyledAttributes( attrs, R.styleable.AviaryEffectThumbLayout, defStyle, 0 );
		mThumbAnimationDuration = array.getInteger( R.styleable.AviaryEffectThumbLayout_aviary_animationDuration, 100 );
		array.recycle();
	}

	public boolean isOpened() {
		return mOpened;
	}

	@Override
	public boolean isChecked() {
		return mChecked;
	}

	@Override
	public void setChecked( boolean checked ) {
		boolean animate = isChecked() != checked;
		mChecked = checked;

		if ( null != getParent() && animate ) {
			if ( checked ) {
				open();
			} else {
				close();
			}
		} else {
			mOpened = checked;
		}
	}

	@Override
	public void toggle() {
		setChecked( !isChecked() );
	}

	@Override
	public void setSelected( boolean selected ) {
		boolean animate = isSelected() != selected;
		super.setSelected( selected );

		if ( null != getParent() && animate ) {
			if ( selected ) {
				open();
			} else {
				close();
			}
		} else {
			mOpened = selected;
		}

	}

	void open() {
		animateView( mThumbAnimationDuration, false );
	}

	void close() {
		animateView( mThumbAnimationDuration, true );
	}

	void setIsOpened( boolean value ) {
		mOpened = value;

		if ( null != mHiddenView ) {
			postSetIsOpened( mOpened );
		}

	}

	protected void postSetIsOpened( final boolean opened ) {

		if ( null == mHiddenView ) return;

		if ( mHiddenView.getHeight() < 1 ) {
			if ( null != getHandler() ) {
				getHandler().postDelayed( new Runnable() {

					@Override
					public void run() {
						postSetIsOpened( opened );
					}
				}, 10 );
			}
			return;
		}

		mHiddenView.setVisibility( mOpened ? View.VISIBLE : View.INVISIBLE );

		boolean shouldApplyLayoutParams = false;
		int targetBottomMargin = opened ? mHiddenView.getHeight() + ( mHiddenView.getPaddingTop() + mHiddenView.getPaddingBottom() ) : 0;

		LayoutParams params = (LayoutParams) mImageView.getLayoutParams();

		if ( params.bottomMargin != targetBottomMargin ) {
			shouldApplyLayoutParams = true;
			params.bottomMargin = targetBottomMargin;
		}

		if ( shouldApplyLayoutParams ) {
			mImageView.setLayoutParams( params );
			requestLayout();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mHiddenView = null;
		mImageView = null;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mOpened = isChecked();
		mHiddenView = findViewById( R.id.aviary_hidden );
		mImageView = findViewById( R.id.aviary_image );
		setIsOpened( mOpened );
	}

	@Override
	protected void onRestoreInstanceState( Parcelable state ) {
		super.onRestoreInstanceState( state );
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		return super.onSaveInstanceState();
	}

	protected void postAnimateView( final int durationMs, final boolean isClosing ) {
		if ( null != getHandler() ) {
			getHandler().post( new Runnable() {

				@Override
				public void run() {
					animateView( durationMs, isClosing );
				}
			} );
		}
	}

	protected void animateView( final int durationMs, final boolean isClosing ) {

		if ( !isClosing ) {
			mHiddenView.setVisibility( View.VISIBLE );
		}

		final boolean is_valid = mHiddenView != null && mImageView != null;

		if ( !is_valid ) return;

		if ( mHiddenView.getHeight() == 0 ) {
			postAnimateView( durationMs, isClosing );
		}

		final long startTime = System.currentTimeMillis();
		final float startHeight = 0;
		final float endHeight = isClosing ? 0 : ( mHiddenView.getHeight() + mHiddenView.getPaddingTop() + mHiddenView.getPaddingBottom() );

		final Easing easing = new Linear();

		if ( null != mHiddenView && null != getParent() && null != getHandler() ) {

			getHandler().post( new Runnable() {

				@Override
				public void run() {

					if ( null != mHiddenView ) {
						long now = System.currentTimeMillis();

						float currentMs = Math.min( durationMs, now - startTime );
						float newHeight = (float) easing.easeOut( currentMs, startHeight, endHeight, durationMs );

						int height = isClosing ? (int) ( endHeight - newHeight ) : (int) newHeight;

						LayoutParams params = (LayoutParams) mImageView.getLayoutParams();
						params.bottomMargin = height;

						mImageView.setLayoutParams( params );

						if ( currentMs < durationMs ) {
							if ( null != getHandler() ) {
								getHandler().post( this );
								invalidate();
							}
						} else {
							mOpened = !isClosing;
							if ( null != getParent() ) {
								if ( !mOpened ) mHiddenView.setVisibility( View.INVISIBLE );
								else mHiddenView.setVisibility( View.VISIBLE );
							}
							postInvalidate();
						}
					}
				}
			} );
		}
	}

}
