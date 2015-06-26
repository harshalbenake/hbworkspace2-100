package com.aviary.android.feather.graphics;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Draw a bitmap repeated horizontally and scaled vertically.
 * 
 * @author alessandro
 */
public class RepeatableHorizontalDrawable extends Drawable {

	private Paint mPaint = new Paint();
	private Rect mRect = new Rect();
	private Matrix mMatrix = new Matrix();
	private Bitmap mBitmap = null;
	private Shader mShader;

	/**
	 * Construct a new {@link RepeatableHorizontalDrawable} instance.
	 * 
	 * @param resources
	 *            the current Context {@link Resources} used to extract the resource
	 * @param resId
	 *            the {@link BitmapDrawable} resource used to draw
	 */
	public RepeatableHorizontalDrawable ( Resources resources, int resId ) {
		try {
			Bitmap bitmap = ( (BitmapDrawable) resources.getDrawable( resId ) ).getBitmap();
			init( bitmap );
		} catch ( Exception e ) {
		}

	}

	public static Drawable createFromView( View view ) {
		Drawable drawable = view.getBackground();

		if ( null != drawable ) {
			if ( drawable instanceof BitmapDrawable ) {
				Bitmap bitmap = ( (BitmapDrawable) drawable ).getBitmap();
				return new RepeatableHorizontalDrawable( bitmap );
			}
		}
		return drawable;
	}

	public RepeatableHorizontalDrawable ( Bitmap bitmap ) {
		init( bitmap );
	}

	private void init( Bitmap bitmap ) {
		mBitmap = bitmap;

		if ( mBitmap != null ) {
			mShader = new BitmapShader( mBitmap, TileMode.REPEAT, TileMode.CLAMP );
			mPaint.setShader( mShader );
		}
	}

	@Override
	public void draw( Canvas canvas ) {
		if ( null != mBitmap ) {
			copyBounds( mRect );
			canvas.drawPaint( mPaint );
		}
	}

	@Override
	protected void onBoundsChange( Rect bounds ) {
		super.onBoundsChange( bounds );
		if ( null != mBitmap ) {
			mMatrix.setScale( 1, (float) bounds.height() / mBitmap.getHeight() );
			mShader.setLocalMatrix( mMatrix );
		}
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha( int alpha ) {
		mPaint.setAlpha( alpha );
	}

	@Override
	public void setColorFilter( ColorFilter cf ) {
		mPaint.setColorFilter( cf );
	}
}
