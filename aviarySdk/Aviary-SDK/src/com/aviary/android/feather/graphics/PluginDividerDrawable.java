package com.aviary.android.feather.graphics;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.aviary.android.feather.R;
import com.aviary.android.feather.utils.TypefaceUtils;

public class PluginDividerDrawable extends Drawable {

	public static final String LOG_TAG = "Drawable";

	private float mTextSize = 10;

	private int mStrokeWidth;
	private int mStrokeColor;
	private int mFillColor;
	private float mTextDivider;

	private FontMetrics mMetrics;
	private String mLabel;
	private Paint mTextPaint;
	private Paint mTextStrokePaint;

	private Rect mBounds;
	private int mWidth;
	private int mHeight;

	public PluginDividerDrawable ( Context context, int styleid, final String string ) {

		Typeface font = Typeface.DEFAULT;

		Theme theme = context.getTheme();
		TypedArray array = theme.obtainStyledAttributes( null, R.styleable.AviaryPluginDividerDrawable, R.attr.aviaryEffectThumbDividerTextStyle, -1 );

		mFillColor = array.getColor( R.styleable.AviaryPluginDividerDrawable_android_textColor, 0 );
		mStrokeWidth = array.getDimensionPixelSize( R.styleable.AviaryPluginDividerDrawable_aviary_strokeWidth, 0 );
		mStrokeColor = array.getColor( R.styleable.AviaryPluginDividerDrawable_aviary_strokeColor, 0 );
		mTextDivider = array.getFloat( R.styleable.AviaryPluginDividerDrawable_aviary_textPerc, 0.9f );

		String fontname = array.getString( R.styleable.AviaryPluginDividerDrawable_aviary_typeface );

		try {
			font = TypefaceUtils.createFromAsset( context.getAssets(), fontname );
		} catch ( Throwable t ) {
			t.printStackTrace();
		}

		array.recycle();

		mLabel = string;

		mTextPaint = new Paint( Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.DEV_KERN_TEXT_FLAG );
		mTextPaint.setColor( mFillColor );
		mTextPaint.setStyle( Style.FILL );

		if ( null != font ) {
			mTextPaint.setTypeface( font );
		}

		mTextStrokePaint = new Paint( mTextPaint );
		mTextStrokePaint.setColor( mStrokeColor );
		mTextStrokePaint.setStyle( Style.STROKE );
		mTextStrokePaint.setStrokeWidth( mStrokeWidth );

		mBounds = new Rect();
		mMetrics = new FontMetrics();
	}

	public void setTitle( final String value ) {
		mLabel = value;
		onBoundsChange( getBounds() );
		invalidateSelf();
	}

	public final String getTitle() {
		return mLabel;
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSLUCENT;
	}

	@Override
	public void setAlpha( int alpha ) {}

	@Override
	public void setColorFilter( ColorFilter cf ) {}

	@Override
	public void clearColorFilter() {}

	@Override
	public int getIntrinsicHeight() {
		return super.getIntrinsicHeight();
	}

	@Override
	public int getIntrinsicWidth() {
		return super.getIntrinsicWidth();
	}

	@Override
	public int getMinimumHeight() {
		return super.getMinimumHeight();
	}

	@Override
	public int getMinimumWidth() {
		return super.getMinimumWidth();
	}

	@Override
	protected void onBoundsChange( Rect bounds ) {
		super.onBoundsChange( bounds );

		mWidth = bounds.width();
		mHeight = bounds.height();

		onTextBoundsChanged();
	}

	private int mTextY = 0;
	private int mMaxHeight;

	protected final void onTextBoundsChanged() {
		mTextSize = mWidth * mTextDivider;

		mTextPaint.setTextSize( mTextSize );
		mTextStrokePaint.setTextSize( mTextSize );
		mTextPaint.getTextBounds( mLabel, 0, mLabel.length(), mBounds );
		mTextPaint.getFontMetrics( mMetrics );

		mTextY = (int) ( ( ( mWidth / 2.0f ) + mTextSize / 2.0f ) - mMetrics.bottom / 2.0f );

		/*
		 * if ( mBounds.width() >= ( mHeight * 0.95 ) ) {
		 * if ( mLabel.length() > 4 ) {
		 * mLabel = mLabel.substring( 0, mLabel.length() - 4 ) + "..";
		 * onTextBoundsChanged();
		 * }
		 * }
		 */

		mMaxHeight = (int) ( (double) mHeight * 0.9 );
	}

	@Override
	public void draw( Canvas canvas ) {

		int saveCount = canvas.save( Canvas.MATRIX_SAVE_FLAG );
		canvas.rotate( -90 );
		canvas.translate( -mHeight + ( mHeight - mBounds.width() ) / 2, mTextY );

		if ( mBounds.width() > mMaxHeight ) {
			float diff = (float) mMaxHeight / mBounds.width();
			canvas.scale( diff, diff, mBounds.centerX(), mBounds.centerY() );
		}

		if ( mStrokeWidth > 0 ) {
			canvas.drawText( mLabel, 0, 0, mTextStrokePaint );
		}
		canvas.drawText( mLabel, 0, 0, mTextPaint );
		canvas.restoreToCount( saveCount );

	}

}
