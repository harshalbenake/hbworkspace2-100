package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.EdgeEffect;

import com.aviary.android.feather.R;

/**
 * Modification of the original {@link EdgeEffect} class which allows to
 * set a color filter for the edge
 * 
 * @author alessandro
 */
public class AviaryEdgeEffect {

	@SuppressWarnings ( "unused" )
	private static final String TAG = "EdgeEffect";

	// Time it will take the effect to fully recede in ms
	private static final int RECEDE_TIME = 1000;

	// Time it will take before a pulled glow begins receding in ms
	private static final int PULL_TIME = 167;

	// Time it will take in ms for a pulled glow to decay to partial strength before
	// release
	private static final int PULL_DECAY_TIME = 1000;

	private static final float MAX_ALPHA = 1.f;
	private static final float HELD_EDGE_SCALE_Y = 0.5f;

	private static final float MAX_GLOW_HEIGHT = 1.f;

	private static final float PULL_GLOW_BEGIN = 0.1f;
	private static final float PULL_EDGE_BEGIN = 0.6f;

	// Minimum velocity that will be absorbed
	private static final int MIN_VELOCITY = 100;

	private static final float EPSILON = 0.001f;

	private Drawable mEdge;
	private Drawable mGlow;
	private int mWidth;
	private int mX;
	private int mY;

	private int mEdgeMaxAlpha = 255;
	private int mGlowMaxAlpha = 255;
	private float mEdgeAlpha;
	private float mEdgeScaleY;
	private float mGlowAlpha;
	private float mGlowScaleY;

	private float mEdgeAlphaStart;
	private float mEdgeAlphaFinish;
	private float mEdgeScaleYStart;
	private float mEdgeScaleYFinish;
	private float mGlowAlphaStart;
	private float mGlowAlphaFinish;
	private float mGlowScaleYStart;
	private float mGlowScaleYFinish;

	private long mStartTime;
	private float mDuration;

	private final Interpolator mInterpolator;

	private static final int STATE_IDLE = 0;
	private static final int STATE_PULL = 1;
	private static final int STATE_ABSORB = 2;
	private static final int STATE_RECEDE = 3;
	private static final int STATE_PULL_DECAY = 4;

	// How much dragging should effect the height of the edge image.
	// Number determined by user testing.
	private static final int PULL_DISTANCE_EDGE_FACTOR = 7;

	// How much dragging should effect the height of the glow image.
	// Number determined by user testing.
	private static final int PULL_DISTANCE_GLOW_FACTOR = 7;
	private static final float PULL_DISTANCE_ALPHA_GLOW_FACTOR = 1.1f;

	private static final int VELOCITY_EDGE_FACTOR = 8;
	private static final int VELOCITY_GLOW_FACTOR = 16;

	private int mState = STATE_IDLE;

	private float mPullDistance;

	private final Rect mBounds = new Rect();

	private int mEdgeHeight;
	private int mGlowHeight;
	private final int mGlowWidth;
	private final int mMaxEffectHeight;

	public AviaryEdgeEffect ( Context context, int styleid ) {

		TypedArray array = context.obtainStyledAttributes( styleid, R.styleable.AviaryEdgeEffect );

		mEdge = array.getDrawable( R.styleable.AviaryEdgeEffect_aviary_edgeDrawable );
		mGlow = array.getDrawable( R.styleable.AviaryEdgeEffect_aviary_glowDrawable );
		int color = array.getColor( R.styleable.AviaryEdgeEffect_aviary_edgeColor, 0 );
		String blendMode = array.getString( R.styleable.AviaryEdgeEffect_aviary_blendMode );

		array.recycle();

		if ( null == mEdge ) {
			mEdge = context.getResources().getDrawable( R.drawable.aviary_overscroll_edge );
		}

		if ( null == mGlow ) {
			mGlow = context.getResources().getDrawable( R.drawable.aviary_overscroll_glow );
		}

		if ( color != 0 && null != blendMode ) {
			Mode mode = Mode.MULTIPLY;
			try {
				mode = Mode.valueOf( blendMode );
			} catch ( Throwable t ) {
			}

			setColorFilter( color, mode );
		}

		mEdgeHeight = mEdge.getIntrinsicHeight();
		mGlowHeight = mGlow.getIntrinsicHeight();
		mGlowWidth = mGlow.getIntrinsicWidth();

		mMaxEffectHeight = (int) ( Math.min( mGlowHeight * MAX_GLOW_HEIGHT * mGlowHeight / mGlowWidth * 0.6f, mGlowHeight * MAX_GLOW_HEIGHT ) + 0.5f );

		mInterpolator = new DecelerateInterpolator();
	}

	public void setColorFilter( int color, PorterDuff.Mode mode ) {
		if ( null != mEdge ) mEdge.setColorFilter( color, mode );
		if ( null != mGlow ) mGlow.setColorFilter( color, mode );
	}

	/**
	 * Set the size of this edge effect in pixels.
	 * 
	 * @param width
	 *            Effect width in pixels
	 * @param height
	 *            Effect height in pixels
	 */
	public void setSize( int width, int height ) {
		mWidth = width;
		mEdgeHeight = mGlowHeight = height;
	}

	public void setEdgeMaxAlpha( int value ) {
		mEdgeMaxAlpha = value;
	}

	public void setGlowMaxAlpha( int value ) {
		mGlowMaxAlpha = value;
	}

	/**
	 * Set the position of this edge effect in pixels. This position is
	 * only used by {@link #getBounds(boolean)}.
	 * 
	 * @param x
	 *            The position of the edge effect on the X axis
	 * @param y
	 *            The position of the edge effect on the Y axis
	 */
	void setPosition( int x, int y ) {
		mX = x;
		mY = y;
	}

	/**
	 * Reports if this EdgeEffect's animation is finished. If this method returns false
	 * after a call to {@link #draw(Canvas)} the host widget should schedule another
	 * drawing pass to continue the animation.
	 * 
	 * @return true if animation is finished, false if drawing should continue on the next
	 *         frame.
	 */
	public boolean isFinished() {
		return mState == STATE_IDLE;
	}

	/**
	 * Immediately finish the current animation.
	 * After this call {@link #isFinished()} will return true.
	 */
	public void finish() {
		mState = STATE_IDLE;
	}

	/**
	 * A view should call this when content is pulled away from an edge by the user.
	 * This will update the state of the current visual effect and its associated
	 * animation.
	 * The host view should always {@link android.view.View#invalidate()} after this
	 * and draw the results accordingly.
	 * 
	 * @param deltaDistance
	 *            Change in distance since the last call. Values may be 0 (no change) to
	 *            1.f (full length of the view) or negative values to express change
	 *            back toward the edge reached to initiate the effect.
	 */
	public void onPull( float deltaDistance ) {
		final long now = AnimationUtils.currentAnimationTimeMillis();
		if ( mState == STATE_PULL_DECAY && now - mStartTime < mDuration ) {
			return;
		}
		if ( mState != STATE_PULL ) {
			mGlowScaleY = PULL_GLOW_BEGIN;
		}
		mState = STATE_PULL;

		mStartTime = now;
		mDuration = PULL_TIME;

		mPullDistance += deltaDistance;
		float distance = Math.abs( mPullDistance );

		mEdgeAlpha = mEdgeAlphaStart = Math.max( PULL_EDGE_BEGIN, Math.min( distance, MAX_ALPHA ) );
		mEdgeScaleY = mEdgeScaleYStart = Math.max( HELD_EDGE_SCALE_Y, Math.min( distance * PULL_DISTANCE_EDGE_FACTOR, 1.f ) );

		mGlowAlpha = mGlowAlphaStart = Math.min( MAX_ALPHA, mGlowAlpha + ( Math.abs( deltaDistance ) * PULL_DISTANCE_ALPHA_GLOW_FACTOR ) );

		float glowChange = Math.abs( deltaDistance );
		if ( deltaDistance > 0 && mPullDistance < 0 ) {
			glowChange = -glowChange;
		}
		if ( mPullDistance == 0 ) {
			mGlowScaleY = 0;
		}

		// Do not allow glow to get larger than MAX_GLOW_HEIGHT.
		mGlowScaleY = mGlowScaleYStart = Math.min( MAX_GLOW_HEIGHT, Math.max( 0, mGlowScaleY + glowChange * PULL_DISTANCE_GLOW_FACTOR ) );

		mEdgeAlphaFinish = mEdgeAlpha;
		mEdgeScaleYFinish = mEdgeScaleY;
		mGlowAlphaFinish = mGlowAlpha;
		mGlowScaleYFinish = mGlowScaleY;
	}

	/**
	 * Call when the object is released after being pulled.
	 * This will begin the "decay" phase of the effect. After calling this method
	 * the host view should {@link android.view.View#invalidate()} and thereby
	 * draw the results accordingly.
	 */
	public void onRelease() {
		mPullDistance = 0;

		if ( mState != STATE_PULL && mState != STATE_PULL_DECAY ) {
			return;
		}

		mState = STATE_RECEDE;
		mEdgeAlphaStart = mEdgeAlpha;
		mEdgeScaleYStart = mEdgeScaleY;
		mGlowAlphaStart = mGlowAlpha;
		mGlowScaleYStart = mGlowScaleY;

		mEdgeAlphaFinish = 0.f;
		mEdgeScaleYFinish = 0.f;
		mGlowAlphaFinish = 0.f;
		mGlowScaleYFinish = 0.f;

		mStartTime = AnimationUtils.currentAnimationTimeMillis();
		mDuration = RECEDE_TIME;
	}

	/**
	 * Call when the effect absorbs an impact at the given velocity.
	 * Used when a fling reaches the scroll boundary.
	 * <p>
	 * When using a {@link android.widget.Scroller} or {@link android.widget.OverScroller}
	 * , the method <code>getCurrVelocity</code> will provide a reasonable approximation
	 * to use here.
	 * </p>
	 * 
	 * @param velocity
	 *            Velocity at impact in pixels per second.
	 */
	public void onAbsorb( int velocity ) {
		mState = STATE_ABSORB;
		velocity = Math.max( MIN_VELOCITY, Math.abs( velocity ) );

		mStartTime = AnimationUtils.currentAnimationTimeMillis();
		mDuration = 0.1f + ( velocity * 0.03f );

		// The edge should always be at least partially visible, regardless
		// of velocity.
		mEdgeAlphaStart = 0.f;
		mEdgeScaleY = mEdgeScaleYStart = 0.f;
		// The glow depends more on the velocity, and therefore starts out
		// nearly invisible.
		mGlowAlphaStart = 0.5f;
		mGlowScaleYStart = 0.f;

		// Factor the velocity by 8. Testing on device shows this works best to
		// reflect the strength of the user's scrolling.
		mEdgeAlphaFinish = Math.max( 0, Math.min( velocity * VELOCITY_EDGE_FACTOR, 1 ) );
		// Edge should never get larger than the size of its asset.
		mEdgeScaleYFinish = Math.max( HELD_EDGE_SCALE_Y, Math.min( velocity * VELOCITY_EDGE_FACTOR, 1.f ) );

		// Growth for the size of the glow should be quadratic to properly
		// respond
		// to a user's scrolling speed. The faster the scrolling speed, the more
		// intense the effect should be for both the size and the saturation.
		mGlowScaleYFinish = Math.min( 0.025f + ( velocity * ( velocity / 100 ) * 0.00015f ), 1.75f );
		// Alpha should change for the glow as well as size.
		mGlowAlphaFinish = Math.max( mGlowAlphaStart, Math.min( velocity * VELOCITY_GLOW_FACTOR * .00001f, MAX_ALPHA ) );
	}

	/**
	 * Draw into the provided canvas. Assumes that the canvas has been rotated
	 * accordingly and the size has been set. The effect will be drawn the full
	 * width of X=0 to X=width, beginning from Y=0 and extending to some factor <
	 * 1.f of height.
	 * 
	 * @param canvas
	 *            Canvas to draw into
	 * @return true if drawing should continue beyond this frame to continue the
	 *         animation
	 */
	public boolean draw( Canvas canvas ) {
		update();

		mGlow.setAlpha( (int) ( Math.max( 0, Math.min( mGlowAlpha, 1 ) ) * mGlowMaxAlpha ) );

		int glowBottom = (int) ( mGlowHeight * mGlowScaleY );

		mGlow.setBounds( 0, 0, mWidth, glowBottom );

		mGlow.draw( canvas );

		mEdge.setAlpha( (int) ( Math.max( 0, Math.min( mEdgeAlpha, 1 ) ) * mEdgeMaxAlpha ) );

		int edgeBottom = (int) ( mEdgeHeight * mEdgeScaleY );
		mEdge.setBounds( 0, 0, mWidth, edgeBottom );
		mEdge.draw( canvas );

		if ( mState == STATE_RECEDE && glowBottom == 0 && edgeBottom == 0 ) {
			mState = STATE_IDLE;
		}

		return mState != STATE_IDLE;
	}

	/**
	 * Returns the bounds of the edge effect.
	 * 
	 * @hide
	 */
	public Rect getBounds( boolean reverse ) {
		mBounds.set( 0, 0, mWidth, mMaxEffectHeight );
		mBounds.offset( mX, mY - ( reverse ? mMaxEffectHeight : 0 ) );

		return mBounds;
	}

	private void update() {
		final long time = AnimationUtils.currentAnimationTimeMillis();
		final float t = Math.min( ( time - mStartTime ) / mDuration, 1.f );

		final float interp = mInterpolator.getInterpolation( t );

		mEdgeAlpha = mEdgeAlphaStart + ( mEdgeAlphaFinish - mEdgeAlphaStart ) * interp;
		mEdgeScaleY = mEdgeScaleYStart + ( mEdgeScaleYFinish - mEdgeScaleYStart ) * interp;
		mGlowAlpha = mGlowAlphaStart + ( mGlowAlphaFinish - mGlowAlphaStart ) * interp;
		mGlowScaleY = mGlowScaleYStart + ( mGlowScaleYFinish - mGlowScaleYStart ) * interp;

		if ( t >= 1.f - EPSILON ) {
			switch ( mState ) {
				case STATE_ABSORB:
					mState = STATE_RECEDE;
					mStartTime = AnimationUtils.currentAnimationTimeMillis();
					mDuration = RECEDE_TIME;

					mEdgeAlphaStart = mEdgeAlpha;
					mEdgeScaleYStart = mEdgeScaleY;
					mGlowAlphaStart = mGlowAlpha;
					mGlowScaleYStart = mGlowScaleY;

					// After absorb, the glow and edge should fade to nothing.
					mEdgeAlphaFinish = 0.f;
					mEdgeScaleYFinish = 0.f;
					mGlowAlphaFinish = 0.f;
					mGlowScaleYFinish = 0.f;
					break;
				case STATE_PULL:
					mState = STATE_PULL_DECAY;
					mStartTime = AnimationUtils.currentAnimationTimeMillis();
					mDuration = PULL_DECAY_TIME;

					mEdgeAlphaStart = mEdgeAlpha;
					mEdgeScaleYStart = mEdgeScaleY;
					mGlowAlphaStart = mGlowAlpha;
					mGlowScaleYStart = mGlowScaleY;

					// After pull, the glow and edge should fade to nothing.
					mEdgeAlphaFinish = 0.f;
					mEdgeScaleYFinish = 0.f;
					mGlowAlphaFinish = 0.f;
					mGlowScaleYFinish = 0.f;
					break;
				case STATE_PULL_DECAY:
					// When receding, we want edge to decrease more slowly
					// than the glow.
					float factor = mGlowScaleYFinish != 0 ? 1 / ( mGlowScaleYFinish * mGlowScaleYFinish ) : Float.MAX_VALUE;
					mEdgeScaleY = mEdgeScaleYStart + ( mEdgeScaleYFinish - mEdgeScaleYStart ) * interp * factor;
					mState = STATE_RECEDE;
					break;
				case STATE_RECEDE:
					mState = STATE_IDLE;
					break;
			}
		}
	}
}
