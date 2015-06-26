package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import android.view.LayoutInflater;
import android.view.View;

import com.aviary.android.feather.effects.AbstractPanel.ContentPanel;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.services.IAviaryController;

abstract class AbstractContentPanel extends AbstractOptionPanel implements ContentPanel {

	protected OnContentReadyListener mContentReadyListener;
	protected View mDrawingPanel;
	protected ImageViewTouch mImageView;

	public AbstractContentPanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry );
	}

	@Override
	public final void setOnReadyListener( OnContentReadyListener listener ) {
		mContentReadyListener = listener;
	}

	@Override
	public final View getContentView( LayoutInflater inflater ) {
		mDrawingPanel = generateContentView( inflater );
		return mDrawingPanel;
	}

	@Override
	public final View getContentView() {
		return mDrawingPanel;
	}

	@Override
	protected void onDispose() {
		mContentReadyListener = null;
		super.onDispose();
	}

	@Override
	public void setEnabled( boolean value ) {
		super.setEnabled( value );
		getContentView().setEnabled( value );
	}

	/**
	 * Call this method when your tool is ready to display its overlay.
	 * After this call the main context will remove the main image
	 * and will replace it with the content of this panel
	 */
	protected void contentReady() {
		if ( mContentReadyListener != null && isActive() ) mContentReadyListener.onReady( this );
	}

	protected abstract View generateContentView( LayoutInflater inflater );

	@Override
	public boolean isRendering() {
		// assume that a content panel is always in rendering mode
		return true;
	}
}
