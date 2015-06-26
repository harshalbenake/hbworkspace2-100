package com.aviary.android.feather.widget;

import it.sephiroth.android.library.widget.HListView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ViewFlipper;

import com.aviary.android.feather.R;

public class AviaryBottomBarViewFlipper extends ViewFlipper implements OnClickListener {

	public static interface OnViewChangingStatusListener {

		void OnOpenStart();

		void OnOpenEnd();

		void OnCloseStart();

		void OnCloseEnd();
	}

	public static interface OnBottomBarItemClickListener {
		void onBottomBarItemClick( int id );
	}

	private View mLogo;
	private OnViewChangingStatusListener mListener;
	private OnBottomBarItemClickListener mBottomClickListener;

	public AviaryBottomBarViewFlipper ( Context context ) {
		super( context );
	}

	public AviaryBottomBarViewFlipper ( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	@Override
	protected void onFinishInflate() {

		mLogo = findViewById( R.id.aviary_white_logo );
		mLogo.setOnClickListener( this );

		super.onFinishInflate();
	}

	public void setOnViewChangingStatusListener( OnViewChangingStatusListener listener ) {
		mListener = listener;
	}

	public void setOnBottomBarItemClickListener( OnBottomBarItemClickListener listener ) {
		mBottomClickListener = listener;
	}

	public boolean open() {

		if ( getDisplayedChild() == 1 ) {

			Animation inAnimation = getInAnimation();

			if ( null != inAnimation ) {

				if ( inAnimation.hasStarted() && !inAnimation.hasEnded() ) {
					return false;
				}

				inAnimation.setAnimationListener( new AnimationListener() {

					@Override
					public void onAnimationStart( Animation animation ) {
						getChildAt( 0 ).setVisibility( View.VISIBLE );
						if ( null != mListener ) mListener.OnOpenStart();
					}

					@Override
					public void onAnimationRepeat( Animation animation ) {}

					@Override
					public void onAnimationEnd( Animation animation ) {
						if ( null != mListener ) mListener.OnOpenEnd();
						getChildAt( 1 ).setVisibility( View.GONE );
					}
				} );
			}

			setDisplayedChild( 0 );
			return true;
		}
		return false;
	}

	public boolean close() {

		if ( getDisplayedChild() == 0 ) {

			Animation inAnimation = getInAnimation();

			if ( null != inAnimation ) {

				if ( inAnimation.hasStarted() && !inAnimation.hasEnded() ) {
					return false;
				}

				inAnimation.setAnimationListener( new AnimationListener() {

					@Override
					public void onAnimationStart( Animation animation ) {
						getChildAt( 1 ).setVisibility( View.VISIBLE );
						if ( null != mListener ) mListener.OnCloseStart();
					}

					@Override
					public void onAnimationRepeat( Animation animation ) {}

					@Override
					public void onAnimationEnd( Animation animation ) {
						if ( null != mListener ) mListener.OnCloseEnd();
						getChildAt( 0 ).setVisibility( View.GONE );
					}
				} );
			}

			setDisplayedChild( 1 );
			return true;
		}
		return false;
	}

	public boolean opened() {
		return getDisplayedChild() == 0;
	}

	/**
	 * Return the child used to populate the tools
	 * 
	 * @return
	 */
	public ViewGroup getContentPanel() {
		return (ViewGroup) getChildAt( 0 );
	}

	public HListView getToolsListView() {
		return (HListView) findViewById( R.id.aviary_tools_listview );
	}

	public void toggleLogoVisibility( boolean visible ) {
		if( visible )
			findViewById( R.id.aviary_white_logo ).setVisibility( View.VISIBLE );
		else
			findViewById( R.id.aviary_white_logo ).setVisibility( View.INVISIBLE );
	}

	@Override
	public void onClick( View v ) {
		if ( null != v ) {
			final int id = v.getId();

			if ( null != mBottomClickListener ) {
				mBottomClickListener.onBottomBarItemClick( id );
			}
		}
	}
}
