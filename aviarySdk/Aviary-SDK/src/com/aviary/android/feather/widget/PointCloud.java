package com.aviary.android.feather.widget;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.FloatMath;
import android.util.Log;

@SuppressLint ( "FloatMath" )
public class PointCloud {

	enum WaveType {
		Circle, Line
	}

	private static final String TAG = "PointCloud";

	private static final float MIN_POINT_SIZE = 2.0f;
	private static final float MAX_POINT_SIZE = 4.0f;
	private static final int INNER_POINTS = 8;

	private ArrayList<Point> mPointCloud1 = new ArrayList<Point>();
	private ArrayList<Point> mPointCloud2 = new ArrayList<Point>();

	private Drawable mDrawable;
	private float mCenterX;
	private float mRotation = 0;
	private float mCenterY;
	private Paint mPaint;
	private float mScale = 1.0f;
	private float mOuterRadius;
	private static final float PI = (float) Math.PI;

	public static class WaveManager {

		private float radius = 50;
		private float width = 200.0f; // TODO: Make configurable
		private float alpha = 0.0f;
		private WaveType type = WaveType.Circle;

		public void setRadius( float r ) {
			radius = r;
		}

		public void setType( WaveType t ) {
			type = t;
		}

		public WaveType getType() {
			return type;
		}

		public float getRadius() {
			return radius;
		}

		public void setAlpha( float a ) {
			alpha = a;
		}

		public float getAlpha() {
			return alpha;
		}
	};

	static class Point {

		float x;
		float y;
		float radius;

		public Point ( float x2, float y2, float r ) {
			x = (float) x2;
			y = (float) y2;
			radius = r;
		}
	}

	public WaveManager waveManager = new WaveManager();

	public PointCloud ( Drawable drawable ) {
		mPaint = new Paint();
		mPaint.setFilterBitmap( true );
		mPaint.setColor( Color.rgb( 255, 255, 255 ) ); // TODO: make configurable
		mPaint.setAntiAlias( true );
		mPaint.setDither( true );

		mDrawable = drawable;

		if ( mDrawable != null ) {
			drawable.setBounds( 0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight() );
		}
	}

	public void setCenter( float x, float y ) {
		mCenterX = x;
		mCenterY = y;
	}

	public void setRotation( float angle ) {
		mRotation = angle;
	}

	public void makePointCloud( float innerRadius, float outerRadius, RectF rect ) {

		if ( innerRadius == 0 ) {
			Log.w( TAG, "Must specify an inner radius" );
			return;
		}

		mOuterRadius = outerRadius;

		// radial
		mPointCloud1.clear();

		final float pointAreaRadius = ( outerRadius - innerRadius );
		final float ds = ( 2.0f * PI * innerRadius / INNER_POINTS );
		final int bands = (int) Math.round( pointAreaRadius / ds );
		final float dr = pointAreaRadius / bands;
		float r = innerRadius;
		for ( int b = 0; b <= bands; b++, r += dr ) {
			float circumference = 2.0f * PI * r;
			final int pointsInBand = (int) ( circumference / ds );
			float eta = PI / 2.0f;
			float dEta = 2.0f * PI / pointsInBand;
			for ( int i = 0; i < pointsInBand; i++ ) {
				float x = r * FloatMath.cos( eta );
				float y = r * FloatMath.sin( eta );
				eta += dEta;
				mPointCloud1.add( new Point( x, y, r ) );
			}
		}

		// linear
		mPointCloud2.clear();
		r = innerRadius;
		final float rect_side = Math.max( rect.width(), rect.height() );
		float circumference = rect_side;

		for ( int b = 0; b <= bands; b++, r += dr ) {
			final float pointSize = interp( MAX_POINT_SIZE, MIN_POINT_SIZE, r / mOuterRadius );
			final int pointsInBand = (int) ( circumference / ( ds * ( pointSize / MIN_POINT_SIZE ) ) );

			for ( int i = 0; i <= pointsInBand; i++ ) {
				float x = r;
				float y = -circumference / 2 + ( circumference / pointsInBand * i );
				mPointCloud2.add( new Point( x, y, r ) );
				mPointCloud2.add( new Point( -x, y, r ) );
			}

		}

	}

	public void setScale( float scale ) {
		mScale = scale;
	}

	public float getScale() {
		return mScale;
	}

	private static float hypot( float x, float y ) {
		return FloatMath.sqrt( x * x + y * y );
	}

	private static float max( float a, float b ) {
		return a > b ? a : b;
	}

	public int getAlphaForPoint( Point point, boolean circle ) {

		// Compute contribution from Wave
		float radius = hypot( point.x, point.y );

		if ( !circle ) {
			radius = point.radius;
		}

		float distanceToWaveRing = ( radius - waveManager.radius );
		float waveAlpha = 0.0f;

		if ( distanceToWaveRing > 0.0f ) {
			// outside
			if ( distanceToWaveRing < waveManager.width * 0.5f ) {
				float cosf = FloatMath.cos( PI * 0.25f * distanceToWaveRing / ( waveManager.width * 0.5f ) );
				waveAlpha = waveManager.alpha * max( 0.0f, (float) Math.pow( cosf, 20.0f ) );
			}

		} else {
			// inside
			if ( distanceToWaveRing > -( waveManager.width * 0.5f ) ) {
				float cosf = FloatMath.cos( PI * 0.25f * distanceToWaveRing / ( waveManager.width * 0.5f ) );
				waveAlpha = waveManager.alpha * max( 0.0f, (float) Math.pow( cosf, 20.0f ) );
			}
		}

		return (int) ( waveAlpha * 255 );
	}

	private float interp( float min, float max, float f ) {
		return min + ( max - min ) * f;
	}

	public void draw( Canvas canvas ) {

		if ( !( waveManager.getAlpha() > 0.0f ) ) {
			return;
		}

		WaveType type = waveManager.getType();

		int saveCount = canvas.save( Canvas.MATRIX_SAVE_FLAG );
		canvas.scale( mScale, mScale, mCenterX, mCenterY );

		if ( type == WaveType.Line ) {
			canvas.rotate( mRotation, mCenterX, mCenterY );
			ArrayList<Point> points = mPointCloud2;
			for ( int i = 0; i < points.size(); i++ ) {
				Point point = points.get( i );
				final float pointSize = interp( MAX_POINT_SIZE, MIN_POINT_SIZE, point.radius / mOuterRadius );
				final float px = point.x + mCenterX;
				final float py = point.y + mCenterY;
				int alpha = getAlphaForPoint( point, type == WaveType.Circle );

				if ( alpha == 0 ) continue;

				if ( mDrawable != null ) {
					int count = canvas.save( Canvas.MATRIX_SAVE_FLAG );
					final float cx = mDrawable.getIntrinsicWidth() * 0.5f;
					final float cy = mDrawable.getIntrinsicHeight() * 0.5f;
					final float s = pointSize / MAX_POINT_SIZE;
					canvas.scale( s, s, px, py );
					canvas.translate( px - cx, py - cy );
					mDrawable.setAlpha( alpha );
					mDrawable.draw( canvas );
					canvas.restoreToCount( count );
				} else {
					mPaint.setAlpha( alpha );
					canvas.drawCircle( px, py, pointSize, mPaint );
				}
			}

		} else {

			ArrayList<Point> points = mPointCloud1;
			for ( int i = 0; i < points.size(); i++ ) {
				Point point = points.get( i );
				final float pointSize = interp( MAX_POINT_SIZE, MIN_POINT_SIZE, point.radius / mOuterRadius );
				final float px = point.x + mCenterX;
				final float py = point.y + mCenterY;
				int alpha = getAlphaForPoint( point, type == WaveType.Circle );

				if ( alpha == 0 ) continue;

				if ( mDrawable != null ) {
					int count = canvas.save( Canvas.MATRIX_SAVE_FLAG );
					final float cx = mDrawable.getIntrinsicWidth() * 0.5f;
					final float cy = mDrawable.getIntrinsicHeight() * 0.5f;
					final float s = pointSize / MAX_POINT_SIZE;
					canvas.scale( s, s, px, py );
					canvas.translate( px - cx, py - cy );
					mDrawable.setAlpha( alpha );
					mDrawable.draw( canvas );
					canvas.restoreToCount( count );
				} else {
					mPaint.setAlpha( alpha );
					canvas.drawCircle( px, py, pointSize, mPaint );
				}
			}
		}
		canvas.restoreToCount( saveCount );
	}

}
