package com.aviary.android.feather.graphics;

import java.util.concurrent.Callable;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.aviary.android.feather.cds.AviaryCds;
import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.squareup.picasso.Transformation;

public class CdsPreviewTransformer implements Transformation, Callable<Bitmap> {

	static Logger logger = LoggerFactory.getLogger( CdsPreviewTransformer.class.getSimpleName(), LoggerType.ConsoleLoggerType );

	final String mPath;
	final String mDisplayName;
	final String mType;
	int mTargetDensity;
	int mInputDensity;
	int mMaxW = -1;
	int mMaxH = -1;

	public CdsPreviewTransformer ( String path, String displayName, String type ) {
		mDisplayName = displayName;
		mType = type;
		mPath = path;
	}

	public CdsPreviewTransformer ( String path, String displayName, String type, int inputDensity, int targetDensity, int maxW, int maxH ) {
		this( path, displayName, type );
		mTargetDensity = targetDensity;
		mInputDensity = inputDensity;
		mMaxW = maxW;
		mMaxH = maxH;
	}

	@Override
	public Bitmap transform( Bitmap bitmap ) {
		// ok, first resize to the max size, if necessary
		if ( null != bitmap && ( mMaxW > 0 && mMaxH > 0 ) ) {
			logger.log( "need to resize to: %dx%d", mMaxW, mMaxH );
			Bitmap resized = BitmapUtils.resizeBitmap( bitmap, mMaxW, mMaxH );
			if ( !resized.equals( bitmap ) ) {
				bitmap.recycle();
				bitmap = resized;
			}
		}

		if ( AviaryCds.PACKTYPE_EFFECT.equals( mType ) ) {
			if ( !bitmap.isMutable() ) {
				Bitmap newBitmap = bitmap.copy( bitmap.getConfig(), true );
				if ( newBitmap != bitmap ) {
					bitmap.recycle();
					bitmap = newBitmap;
				}
			}

			if ( null != bitmap && bitmap.isMutable() ) {
				Canvas canvas = new Canvas( bitmap );
				Paint paint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG );
				paint.setColor( Color.WHITE );
				paint.setTextSize( bitmap.getHeight() / 10 );

				Rect bounds = new Rect();
				paint.getTextBounds( mDisplayName, 0, mDisplayName.length(), bounds );
				canvas.drawText( mDisplayName, ( bitmap.getWidth() - bounds.width() ) / 2, bitmap.getHeight() - paint.descent() - 2, paint );
			}
		}
		logger.log( "final bitmap.size: %dx%d", bitmap.getWidth(), bitmap.getHeight() );
		return bitmap;
	}

	@Override
	public String key() {
		return mPath;
	}

	@Override
	public Bitmap call() throws Exception {
		Bitmap bitmap = decode();
		return transform( bitmap );
	}

	private Bitmap decode() {
		Bitmap bitmap = null;
		Options options = new Options();
		options.inPreferredConfig = Bitmap.Config.RGB_565;
		options.inInputShareable = true;
		options.inPurgeable = true;
		options.inTargetDensity = mTargetDensity;
		options.inDensity = mInputDensity;
		bitmap = BitmapFactory.decodeFile( mPath, options );

		logger.log( "input.density: %d, target.density: %d", mInputDensity, mTargetDensity );
		logger.log( "options.size: %dx%d", options.outWidth, options.outHeight );

		return bitmap;
	}
}
