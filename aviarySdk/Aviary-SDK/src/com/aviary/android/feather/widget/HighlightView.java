package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.easing.Easing;
import it.sephiroth.android.library.imagezoom.easing.Quad;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.ReflectionUtils;
import com.aviary.android.feather.common.utils.ReflectionUtils.ReflectionException;
import com.aviary.android.feather.library.graphics.RectD;

public class HighlightView {

	@SuppressWarnings ( "unused" )
	private static final String LOG_TAG = "hv";

	static final int GROW_NONE = 1 << 0;
	static final int GROW_LEFT_EDGE = 1 << 1;
	static final int GROW_RIGHT_EDGE = 1 << 2;
	static final int GROW_TOP_EDGE = 1 << 3;
	static final int GROW_BOTTOM_EDGE = 1 << 4;
	static final int MOVE = 1 << 5;
	private boolean mHidden;

	private int mParentWidth, mParentHeight;

	private static Handler mHandler = new Handler();

	enum Mode {
		None, Move, Grow
	}

	private int mMinSize = 20;
	private Mode mMode;
	private Rect mDrawRect = new Rect();
	private RectD mImageRect;
	private RectD mCropRect;
	private Matrix mMatrix;
	private boolean mMaintainAspectRatio = false;
	private double mInitialAspectRatio;
	private Drawable mResizeDrawable;

	private final Paint mOutlinePaint = new Paint();
	private final Paint mOutlinePaint2 = new Paint();
	private final Paint mOutlineFill = new Paint();
	private Paint mLinesPaintShadow = new Paint();

	private int mStrokeColor;
	private int mStrokeColorPressed;
	private int mOutsideFillColor;
	private int mOutsideFillColorPressed;
	private int mStrokeWidth, mStrokeWidth2;
	private int mInternalStrokeColor, mInternalStrokeColorPressed;

	private int dWidth, dHeight;

	final int grid_rows = 3;
	final int grid_cols = 3;

	public HighlightView ( View context, int styleId ) {

		if ( styleId > 0 ) {
			TypedArray appearance = context.getContext().obtainStyledAttributes( styleId, R.styleable.AviaryCropHighlightView );

			mStrokeWidth = appearance.getDimensionPixelSize( R.styleable.AviaryCropHighlightView_aviary_strokeWidth, 2 );
			mStrokeColor = appearance.getColor( R.styleable.AviaryCropHighlightView_aviary_strokeColor, Color.WHITE );
			mStrokeColorPressed = appearance.getColor( R.styleable.AviaryCropHighlightView_aviary_strokeColor2, Color.WHITE );

			mOutsideFillColor = appearance.getColor( R.styleable.AviaryCropHighlightView_aviary_color1, 0x99000000 );
			mOutsideFillColorPressed = appearance.getColor( R.styleable.AviaryCropHighlightView_aviary_color2, 0x99000000 );

			mStrokeWidth2 = appearance.getDimensionPixelSize( R.styleable.AviaryCropHighlightView_aviary_strokeWidth2, 1 );
			mInternalStrokeColor = appearance.getColor( R.styleable.AviaryCropHighlightView_aviary_strokeColor3, Color.WHITE );
			mInternalStrokeColorPressed = appearance.getColor( R.styleable.AviaryCropHighlightView_aviary_strokeColor4, Color.WHITE );

			mResizeDrawable = appearance.getDrawable( R.styleable.AviaryCropHighlightView_android_src );

			appearance.recycle();
		} else {
			mStrokeWidth = 2;
			mStrokeWidth2 = 1;
			mStrokeColor = Color.WHITE;
			mStrokeColorPressed = Color.WHITE;
			mOutsideFillColor = 0;
			mOutsideFillColorPressed = 0;
			mInternalStrokeColor = 0;
			mInternalStrokeColorPressed = 0;
			mResizeDrawable = null;
		}

		if ( null != mResizeDrawable ) {
			double w = mResizeDrawable.getIntrinsicWidth();
			double h = mResizeDrawable.getIntrinsicHeight();

			dWidth = (int) Math.ceil( w / 2.0 );
			dHeight = (int) Math.ceil( h / 2.0 );
		}

		// calculate the initial drawing rectangle
		context.getDrawingRect( mViewDrawingRect );
		mParentWidth = context.getWidth();
		mParentHeight = context.getHeight();
	}

	public void dispose() {}

	public void setMinSize( int value ) {
		mMinSize = value;
	}

	public void setHidden( boolean hidden ) {
		mHidden = hidden;
	}

	private Rect mViewDrawingRect = new Rect();

	private Path mPath = new Path();
	private Path mLinesPath = new Path();
	private Path mInversePath = new Path();
	private RectD tmpRect2 = new RectD();
	private Rect tmpRect4 = new Rect();

	private RectF tmpDrawRect2F = new RectF();
	private RectF tmpDrawRectF = new RectF();
	private RectF tmpDisplayRectF = new RectF();
	private Rect tmpRectMotion = new Rect();
	private RectD tmpRectMotionD = new RectD();
	private RectF tempLayoutRectF = new RectF();

	protected void draw( Canvas canvas ) {
		if ( mHidden ) return;

		mPath.reset();
		mInversePath.reset();
		mLinesPath.reset();

		tmpDrawRectF.set( mDrawRect );
		tmpDrawRect2F.set( mViewDrawingRect );

		mInversePath.addRect( tmpDrawRect2F, Path.Direction.CW );
		mInversePath.addRect( tmpDrawRectF, Path.Direction.CCW );

		tmpDrawRectF.set( mDrawRect );
		mPath.addRect( tmpDrawRectF, Path.Direction.CW );

		tmpDrawRect2F.set( mDrawRect );
		mLinesPath.addRect( tmpDrawRect2F, Path.Direction.CW );

		float colStep = (float) mDrawRect.height() / grid_cols;
		float rowStep = (float) mDrawRect.width() / grid_rows;

		for ( int i = 1; i < grid_cols; i++ ) {
			mLinesPath.moveTo( (int) mDrawRect.left, (int) ( mDrawRect.top + colStep * i ) );
			mLinesPath.lineTo( (int) mDrawRect.right, (int) ( mDrawRect.top + colStep * i ) );
		}

		for ( int i = 1; i < grid_rows; i++ ) {
			mLinesPath.moveTo( (int) ( mDrawRect.left + rowStep * i ), (int) mDrawRect.top );
			mLinesPath.lineTo( (int) ( mDrawRect.left + rowStep * i ), (int) mDrawRect.bottom );
		}

		// canvas.restore();
		canvas.drawPath( mInversePath, mOutlineFill );
		canvas.drawPath( mLinesPath, mOutlinePaint2 );
		canvas.drawPath( mPath, mOutlinePaint );

		if ( true /* || mMode == Mode.Grow */) {
			int left = mDrawRect.left + 1;
			int right = mDrawRect.right + 1;
			int top = mDrawRect.top + 4;
			int bottom = mDrawRect.bottom + 3;
			if ( mResizeDrawable != null ) {

				mResizeDrawable.setBounds( left - dWidth, top - dHeight, left + dWidth, top + dHeight );
				mResizeDrawable.draw( canvas );
				mResizeDrawable.setBounds( right - dWidth, top - dHeight, right + dWidth, top + dHeight );
				mResizeDrawable.draw( canvas );
				mResizeDrawable.setBounds( left - dWidth, bottom - dHeight, left + dWidth, bottom + dHeight );
				mResizeDrawable.draw( canvas );
				mResizeDrawable.setBounds( right - dWidth, bottom - dHeight, right + dWidth, bottom + dHeight );
				mResizeDrawable.draw( canvas );
			}
		}
	}

	public void setMode( Mode mode ) {
		if ( mode != mMode ) {
			mMode = mode;
			mOutlinePaint.setColor( mMode == Mode.None ? mStrokeColor : mStrokeColorPressed );
			mOutlinePaint2.setColor( mMode == Mode.None ? mInternalStrokeColor : mInternalStrokeColorPressed );
			mLinesPaintShadow.setAlpha( mMode == Mode.None ? 102 : 0 );
			mOutlineFill.setColor( mMode == Mode.None ? mOutsideFillColor : mOutsideFillColorPressed );
		}
	}

	final float hysteresis = 30F;

	public int getHit( float x, float y ) {
		Rect r = new Rect();
		computeLayout( false, r );
		int retval = GROW_NONE;
		boolean verticalCheck = ( y >= r.top - hysteresis ) && ( y < r.bottom + hysteresis );
		boolean horizCheck = ( x >= r.left - hysteresis ) && ( x < r.right + hysteresis );
		if ( ( Math.abs( r.left - x ) < hysteresis ) && verticalCheck ) retval |= GROW_LEFT_EDGE;
		if ( ( Math.abs( r.right - x ) < hysteresis ) && verticalCheck ) retval |= GROW_RIGHT_EDGE;
		if ( ( Math.abs( r.top - y ) < hysteresis ) && horizCheck ) retval |= GROW_TOP_EDGE;
		if ( ( Math.abs( r.bottom - y ) < hysteresis ) && horizCheck ) retval |= GROW_BOTTOM_EDGE;
		if ( retval == GROW_NONE && r.contains( (int) x, (int) y ) ) retval = MOVE;
		return retval;
	}

	boolean isLeftEdge( int edge ) {
		return ( GROW_LEFT_EDGE & edge ) == GROW_LEFT_EDGE;
	}

	boolean isRightEdge( int edge ) {
		return ( GROW_RIGHT_EDGE & edge ) == GROW_RIGHT_EDGE;
	}

	boolean isTopEdge( int edge ) {
		return ( GROW_TOP_EDGE & edge ) == GROW_TOP_EDGE;
	}

	boolean isBottomEdge( int edge ) {
		return ( GROW_BOTTOM_EDGE & edge ) == GROW_BOTTOM_EDGE;
	}

	void handleMotion( int edge, float dx, float dy ) {
		if ( mRunning ) return;
		computeLayout( false, tmpRect4 );
		if ( edge == GROW_NONE ) {
			return;
		} else if ( edge == MOVE ) {
			moveBy( dx * ( mCropRect.width() / tmpRect4.width() ), dy * ( mCropRect.height() / tmpRect4.height() ) );
		} else {
			if ( ( ( GROW_LEFT_EDGE | GROW_RIGHT_EDGE ) & edge ) == 0 ) dx = 0;
			if ( ( ( GROW_TOP_EDGE | GROW_BOTTOM_EDGE ) & edge ) == 0 ) dy = 0;

			// Convert to image space before sending to growBy().
			double xDelta = Math.round( dx * ( mCropRect.width() / tmpRect4.width() ) );
			double yDelta = Math.round( dy * ( mCropRect.height() / tmpRect4.height() ) );
			if ( mMaintainAspectRatio ) {
				growWithConstantAspectSize( edge, xDelta, yDelta );
			} else {
				growWithoutConstantAspectSize( edge, xDelta, yDelta );
			}
		}
	}

	double calculateDy( double dx, double dy ) {
		double ndy = dy;
		if ( dx != 0 ) {
			ndy = ( dx / mInitialAspectRatio );
			if ( dy != 0 ) {
				if ( dy > 0 ) {
					ndy = Math.abs( ndy );
				} else {
					ndy = Math.abs( ndy ) * -1;
				}
			}
			dy = ndy;
		}
		return ndy;
	}

	double calculateDx( double dy, double dx ) {
		double ndx = dx;
		if ( dy != 0 ) {
			ndx = ( dy * mInitialAspectRatio );
			if ( dx != 0 ) {
				if ( dx > 0 ) {
					ndx = Math.abs( ndx );
				} else {
					ndx = Math.abs( ndx ) * -1;
				}
			}
			dx = ndx;
		}
		return ndx;
	}

	void growWithConstantAspectSize( int edge, double dx, double dy ) {

		final boolean left = isLeftEdge( edge );
		final boolean right = isRightEdge( edge );
		final boolean top = isTopEdge( edge );
		final boolean bottom = isBottomEdge( edge );
		final boolean horizontal = left || right;
		final boolean vertical = top || bottom;
		final boolean singleSide = !( horizontal && vertical );

		// check minimum size and outset the rectangle as needed
		final double widthCap = (double) mMinSize / getScale();
		double ndx, ndy;

		tmpRectMotionD.set( mCropRect );

		if ( singleSide ) {
			if ( horizontal ) {
				// horizontal only
				ndx = dx;
				ndy = calculateDy( ndx, 0 );

				if ( left ) {
					tmpRectMotionD.left += ndx;
					tmpRectMotionD.inset( 0, ( ndy / 2 ) );
				} else {
					tmpRectMotionD.right += ndx;
					tmpRectMotionD.inset( 0, ( -ndy / 2 ) );
				}

			} else {
				// vertical only
				ndy = dy;
				ndx = calculateDx( ndy, 0 );
				if ( top ) {
					tmpRectMotionD.top += ndy;
					tmpRectMotionD.inset( ( ndx / 2 ), 0 );
				} else if ( bottom ) {
					tmpRectMotionD.bottom += ndy;
					tmpRectMotionD.inset( ( -ndx / 2 ), 0 );
				}
			}

		} else {
			// both horizontal & vertical
			ndx = dx;
			ndy = calculateDy( dx, 0 );

			if ( left && top ) {
				tmpRectMotionD.left += ndx;
				tmpRectMotionD.top += ndy;
			} else if ( left && bottom ) {
				tmpRectMotionD.left += ndx;
				tmpRectMotionD.bottom -= ndy;
			} else if ( right && top ) {
				tmpRectMotionD.right += ndx;
				tmpRectMotionD.top -= ndy;
			} else if ( right && bottom ) {
				tmpRectMotionD.right += ndx;
				tmpRectMotionD.bottom += ndy;
			}
		}

		if ( tmpRectMotionD.width() >= widthCap && tmpRectMotionD.height() >= widthCap && mImageRect.contains( tmpRectMotionD ) ) {
			mCropRect.set( tmpRectMotionD );
		}

		computeLayout( true, mDrawRect );
	}

	void growWithoutConstantAspectSize( int edge, double dx, double dy ) {

		final boolean left = isLeftEdge( edge );
		final boolean right = isRightEdge( edge );
		final boolean top = isTopEdge( edge );
		final boolean bottom = isBottomEdge( edge );
		final boolean horizontal = left || right;
		final boolean vertical = top || bottom;

		// check minimum size and outset the rectangle as needed
		final double widthCap = (double) mMinSize / getScale();

		tmpRectMotionD.set( mCropRect );

		double ndy = dy;
		double ndx = dx;

		if ( horizontal ) {

			if ( left ) {
				tmpRectMotionD.left += ndx;
				if ( !vertical ) tmpRectMotionD.inset( 0, ( ndy / 2 ) );
			} else if ( right ) {
				tmpRectMotionD.right += ndx;
				if ( !vertical ) tmpRectMotionD.inset( 0, ( -ndy / 2 ) );
			}
		}

		if ( vertical ) {

			if ( top ) {
				tmpRectMotionD.top += ndy;
				if ( !horizontal ) tmpRectMotionD.inset( ( ndx / 2 ), 0 );
			} else if ( bottom ) {
				tmpRectMotionD.bottom += ndy;
				if ( !horizontal ) tmpRectMotionD.inset( ( -ndx / 2 ), 0 );
			}
		}

		if ( tmpRectMotionD.width() >= widthCap && tmpRectMotionD.height() >= widthCap && mImageRect.contains( tmpRectMotionD ) ) {
			mCropRect.set( tmpRectMotionD );
		}

		computeLayout( true, mDrawRect );
	}

	void moveBy( double dx, double dy ) {
		moveBy( (float) dx, (float) dy );
	}

	void moveBy( float dx, float dy ) {
		tmpRectMotion.set( mDrawRect );
		mCropRect.offset( dx, dy );
		mCropRect.offset( Math.max( 0, mImageRect.left - mCropRect.left ), Math.max( 0, mImageRect.top - mCropRect.top ) );
		mCropRect.offset( Math.min( 0, mImageRect.right - mCropRect.right ), Math.min( 0, mImageRect.bottom - mCropRect.bottom ) );

		computeLayout( false, mDrawRect );

		tmpRectMotion.union( mDrawRect );
		tmpRectMotion.inset( -dWidth * 2, -dHeight * 2 );
	}

	public Rect getInvalidateRect() {
		return tmpRectMotion;
	}

	protected float getScale() {
		float values[] = new float[9];
		mMatrix.getValues( values );
		return values[Matrix.MSCALE_X];
	}

	private void adjustCropRect( RectD r ) {

		if ( r.left < mImageRect.left ) {
			r.offset( mImageRect.left - r.left, 0.0 );
		} else if ( r.right > mImageRect.right ) {
			r.offset( -( r.right - mImageRect.right ), 0 );
		}

		if ( r.top < mImageRect.top ) {
			r.offset( 0F, mImageRect.top - r.top );
		} else if ( r.bottom > mImageRect.bottom ) {
			r.offset( 0F, -( r.bottom - mImageRect.bottom ) );
		}

		double diffx = -1, diffy = -1;

		if ( r.width() > mImageRect.width() ) {

			if ( r.left < mImageRect.left ) {
				diffx = mImageRect.left - r.left;
				r.left += diffx;
			} else if ( r.right > mImageRect.right ) {
				diffx = ( r.right - mImageRect.right );
				r.right += -diffx;
			}

		} else if ( r.height() > mImageRect.height() ) {
			if ( r.top < mImageRect.top ) {
				// top
				diffy = mImageRect.top - r.top;
				r.top += diffy;

			} else if ( r.bottom > mImageRect.bottom ) {
				// bottom
				diffy = ( r.bottom - mImageRect.bottom );
				r.bottom += -diffy;
			}
		}

		if ( mMaintainAspectRatio ) {
			if ( diffy != -1 ) {
				diffx = diffy * mInitialAspectRatio;
				r.inset( ( diffx / 2.0 ), 0 );
			} else if ( diffx != -1 ) {
				diffy = diffx / mInitialAspectRatio;
				r.inset( 0, ( diffy / 2.0 ) );
			}
		}

		r.sort();
	}

	private RectD adjustRealCropRect( Matrix matrix, RectD rect, RectD outsideRect ) {

		boolean adjusted = false;

		tempLayoutRectF.set( (float) rect.left, (float) rect.top, (float) rect.right, (float) rect.bottom );
		matrix.mapRect( tempLayoutRectF );

		float[] mvalues = new float[9];
		matrix.getValues( mvalues );
		final float scale = mvalues[Matrix.MSCALE_X];

		if ( tempLayoutRectF.left < outsideRect.left ) {
			adjusted = true;
			rect.offset( ( outsideRect.left - tempLayoutRectF.left ) / scale, 0 );
		} else if ( tempLayoutRectF.right > outsideRect.right ) {
			adjusted = true;
			rect.offset( -( tempLayoutRectF.right - outsideRect.right ) / scale, 0 );
		}

		if ( tempLayoutRectF.top < outsideRect.top ) {
			adjusted = true;
			rect.offset( 0, ( outsideRect.top - tempLayoutRectF.top ) / scale );
		} else if ( tempLayoutRectF.bottom > outsideRect.bottom ) {
			adjusted = true;
			rect.offset( 0, -( tempLayoutRectF.bottom - outsideRect.bottom ) / scale );
		}

		tempLayoutRectF.set( (float) rect.left, (float) rect.top, (float) rect.right, (float) rect.bottom );
		matrix.mapRect( tempLayoutRectF );

		if ( tempLayoutRectF.width() > outsideRect.width() ) {
			adjusted = true;
			if ( tempLayoutRectF.left < outsideRect.left ) rect.left += ( outsideRect.left - tempLayoutRectF.left ) / scale;
			if ( tempLayoutRectF.right > outsideRect.right ) rect.right += -( tempLayoutRectF.right - outsideRect.right ) / scale;
		}

		if ( tempLayoutRectF.height() > outsideRect.height() ) {
			adjusted = true;
			if ( tempLayoutRectF.top < outsideRect.top ) rect.top += ( outsideRect.top - tempLayoutRectF.top ) / scale;
			if ( tempLayoutRectF.bottom > outsideRect.bottom ) rect.bottom += -( tempLayoutRectF.bottom - outsideRect.bottom ) / scale;
		}

		if ( mMaintainAspectRatio && adjusted ) {
			if ( mInitialAspectRatio >= 1 ) { // width > height
				final double dy = rect.width() / mInitialAspectRatio;
				rect.bottom = ( rect.top + dy );
			} else { // height >= width
				final double dx = rect.height() * mInitialAspectRatio;
				rect.right = ( rect.left + dx );
			}
		}

		rect.sort();
		return rect;
	}

	private void computeLayout( boolean adjust, Rect outRect ) {
		if ( adjust ) {
			adjustCropRect( mCropRect );
			tmpRect2.set( 0, 0, mParentWidth, mParentHeight );
			mCropRect = adjustRealCropRect( mMatrix, mCropRect, tmpRect2 );
		}
		getDisplayRect( mMatrix, mCropRect, outRect );
	}

	public void getDisplayRect( Matrix m, RectD supportRect, Rect outRect ) {
		tmpDisplayRectF.set( (float) supportRect.left, (float) supportRect.top, (float) supportRect.right, (float) supportRect.bottom );
		m.mapRect( tmpDisplayRectF );
		outRect.set( Math.round( tmpDisplayRectF.left ), Math.round( tmpDisplayRectF.top ), Math.round( tmpDisplayRectF.right ),
				Math.round( tmpDisplayRectF.bottom ) );
	}

	public void invalidate() {
		if ( !mRunning ) {
			computeLayout( true, mDrawRect );
		}
	}

	protected volatile boolean mRunning = false;
	protected int animationDurationMs = 300;
	protected Easing mEasing = new Quad();

	public boolean isRunning() {
		return mRunning;
	}

	public void animateTo( final View parent, Matrix m, RectD imageRect, RectD cropRect, final boolean maintainAspectRatio ) {

		if ( !mRunning ) {
			mRunning = true;
			setMode( Mode.None );
			parent.postInvalidate();

			mMatrix = new Matrix( m );
			mCropRect = cropRect;
			mImageRect = new RectD( imageRect );
			mMaintainAspectRatio = false;

			double ratio = (double) mCropRect.width() / (double) mCropRect.height();
			mInitialAspectRatio = ratio;

			final Rect oldRect = mDrawRect;
			final Rect newRect = new Rect();
			computeLayout( false, newRect );

			final float[] topLeft = { oldRect.left, oldRect.top };
			final float[] bottomRight = { oldRect.right, oldRect.bottom };

			final double pt1 = newRect.left - oldRect.left;
			final double pt2 = newRect.right - oldRect.right;
			final double pt3 = newRect.top - oldRect.top;
			final double pt4 = newRect.bottom - oldRect.bottom;

			final long startTime = System.currentTimeMillis();

			mHandler.post( new Runnable() {

				@Override
				public void run() {
					long now = System.currentTimeMillis();
					double currentMs = Math.min( animationDurationMs, now - startTime );

					double value1 = mEasing.easeOut( currentMs, 0, pt1, animationDurationMs );
					double value2 = mEasing.easeOut( currentMs, 0, pt2, animationDurationMs );
					double value3 = mEasing.easeOut( currentMs, 0, pt3, animationDurationMs );
					double value4 = mEasing.easeOut( currentMs, 0, pt4, animationDurationMs );

					mDrawRect.left = (int) ( topLeft[0] + value1 );
					mDrawRect.right = (int) ( bottomRight[0] + value2 );
					mDrawRect.top = (int) ( topLeft[1] + value3 );
					mDrawRect.bottom = (int) ( bottomRight[1] + value4 );

					if ( currentMs < animationDurationMs ) {
						if ( null != parent ) {
							parent.invalidate();
							mHandler.post( this );
						}
					} else {
						mMaintainAspectRatio = maintainAspectRatio;
						mRunning = false;
						invalidate();

						if ( null != parent ) {
							parent.postInvalidate();
						}
					}
				}

			} );

		}
	}

	public void setup( Matrix m, RectD imageRect, RectD cropRect, boolean maintainAspectRatio ) {

		mMatrix = new Matrix( m );
		mCropRect = cropRect;
		mImageRect = new RectD( imageRect );
		mMaintainAspectRatio = maintainAspectRatio;

		double ratio = (double) mCropRect.width() / (double) mCropRect.height();
		// mInitialAspectRatio = Math.round( ratio * 1000.0 ) / 1000.0;
		mInitialAspectRatio = ratio;

		computeLayout( true, mDrawRect );

		mOutlinePaint.setStrokeWidth( mStrokeWidth );
		mOutlinePaint.setStyle( Paint.Style.STROKE );
		mOutlinePaint.setAntiAlias( false );
		try {
			ReflectionUtils.invokeMethod( mOutlinePaint, "setHinting", new Class<?>[] { int.class }, 0 );
		} catch ( ReflectionException e ) {
		}

		mOutlinePaint2.setStrokeWidth( mStrokeWidth2 );
		mOutlinePaint2.setStyle( Paint.Style.STROKE );
		mOutlinePaint2.setAntiAlias( false );
		mOutlinePaint2.setColor( mInternalStrokeColor );
		try {
			ReflectionUtils.invokeMethod( mOutlinePaint2, "setHinting", new Class<?>[] { int.class }, 0 );
		} catch ( ReflectionException e ) {
		}

		mOutlineFill.setColor( mOutsideFillColor );
		mOutlineFill.setStyle( Paint.Style.FILL );
		mOutlineFill.setAntiAlias( false );
		mOutlineFill.setDither( true );
		try {
			ReflectionUtils.invokeMethod( mOutlineFill, "setHinting", new Class<?>[] { int.class }, 0 );
		} catch ( ReflectionException e ) {
		}

		mLinesPaintShadow.setStrokeWidth( mStrokeWidth2 );
		mLinesPaintShadow.setAntiAlias( true );
		mLinesPaintShadow.setColor( Color.BLACK );
		mLinesPaintShadow.setStyle( Paint.Style.STROKE );
		mLinesPaintShadow.setMaskFilter( new BlurMaskFilter( 2, Blur.NORMAL ) );

		setMode( Mode.None );
	}

	public void setAspectRatio( double value ) {
		mInitialAspectRatio = value;
	}

	public void setMaintainAspectRatio( boolean value ) {
		mMaintainAspectRatio = value;
	}

	public void update( Matrix imageMatrix, Rect imageRect ) {
		mMatrix = new Matrix( imageMatrix );
		mImageRect = new RectD( imageRect );
		computeLayout( true, mDrawRect );
	}

	public Matrix getMatrix() {
		return mMatrix;
	}

	public Rect getDrawRect() {
		return mDrawRect;
	}

	public RectD getCropRectD() {
		return mCropRect;
	}

	public Rect getCropRect() {
		return new Rect( (int) mCropRect.left, (int) mCropRect.top, (int) mCropRect.right, (int) mCropRect.bottom );
	}

	public void onSizeChanged( CropImageView cropImageView, int w, int h, int oldw, int oldh ) {
		cropImageView.getDrawingRect( mViewDrawingRect );
		mParentWidth = w;
		mParentHeight = h;
	}

}
