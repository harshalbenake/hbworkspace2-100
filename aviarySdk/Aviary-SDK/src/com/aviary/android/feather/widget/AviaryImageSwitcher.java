package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ViewSwitcher;

public class AviaryImageSwitcher extends ViewSwitcher {

	protected boolean mSwitchEnabled = true;

	public AviaryImageSwitcher ( Context context ) {
		super( context );
	}

	public AviaryImageSwitcher ( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public void setImageBitmap( Bitmap bitmap, Matrix matrix ) {
		ImageViewTouch image = null;

		if ( mSwitchEnabled ) image = (ImageViewTouch) this.getNextView();
		else image = (ImageViewTouch) this.getChildAt( 0 );

		image.setImageBitmap( bitmap, matrix, ImageViewTouchBase.ZOOM_INVALID, ImageViewTouchBase.ZOOM_INVALID );

		if ( mSwitchEnabled ) showNext();
		else setDisplayedChild( 0 );
	}

	public void setImageDrawable( Drawable drawable, Matrix matrix ) {
		ImageViewTouch image = null;

		if ( mSwitchEnabled ) image = (ImageViewTouch) this.getNextView();
		else image = (ImageViewTouch) this.getChildAt( 0 );

		image.setImageDrawable( drawable, matrix, ImageViewTouchBase.ZOOM_INVALID, ImageViewTouchBase.ZOOM_INVALID );

		if ( mSwitchEnabled ) showNext();
		else setDisplayedChild( 0 );
	}

	public void setSwitchEnabled( boolean enable ) {
		mSwitchEnabled = enable;
	}
}
