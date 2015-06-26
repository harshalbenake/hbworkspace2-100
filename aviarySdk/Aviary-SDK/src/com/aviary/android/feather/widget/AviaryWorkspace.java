/*
 * Copyright (C) 2008 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aviary.android.feather.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.Adapter;
import android.widget.LinearLayout;
import android.widget.Scroller;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.log.LoggerFactory;

public class AviaryWorkspace extends ViewGroup {

	private static final int INVALID_SCREEN = -1;
	public static final int OVER_SCROLL_NEVER = 0;
	public static final int OVER_SCROLL_ALWAYS = 1;
	public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 2;

	private static final int SNAP_VELOCITY = 600;

	private int mPreviousScreen = INVALID_SCREEN;

	private int mDefaultScreen = 0;

	private int mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom;

	private int mCurrentScreen = INVALID_SCREEN;

	private int mNextScreen = INVALID_SCREEN;

	private int mOldSelectedPosition = INVALID_SCREEN;

	private Scroller mScroller;

	private VelocityTracker mVelocityTracker;

	private float mLastMotionX;

	private float mLastMotionX2;

	private float mLastMotionY;

	private final static int TOUCH_STATE_REST = 0;

	private final static int TOUCH_STATE_SCROLLING = 1;

	private int mTouchState = TOUCH_STATE_REST;

	private boolean mAllowLongPress = true;

	private int mTouchSlop;

	private int mMaximumVelocity;

	private static final int INVALID_POINTER = -1;

	private int mActivePointerId = INVALID_POINTER;

	private AviaryWorkspaceIndicator mIndicator;

	private static final float NANOTIME_DIV = 1000000000.0f;

	private static final float SMOOTHING_SPEED = 0.75f;

	private static final float SMOOTHING_CONSTANT = (float) ( 0.016 / Math.log( SMOOTHING_SPEED ) );

	private static final float BASELINE_FLING_VELOCITY = 2500.f;

	private static final float FLING_VELOCITY_INFLUENCE = 0.4f;

	private static final String LOG_TAG = "Workspace";

	private static final boolean LOG_ENABLED = LoggerFactory.LOG_ENABLED;

	private float mSmoothingTime;

	private float mTouchX;

	private Interpolator mScrollInterpolator;

	protected Adapter mAdapter;

	protected DataSetObserver mObserver;

	protected boolean mDataChanged;

	protected int mFirstPosition;

	protected int mItemCount = 0;

	protected int mItemTypeCount = 1;

	private List<Queue<View>> mRecycleBin;

	private int mHeightMeasureSpec;

	private int mWidthMeasureSpec;

	private EdgeEffectCompat mEdgeGlowLeft;

	private EdgeEffectCompat mEdgeGlowRight;

	private int mOverScrollMode;

	private boolean mAllowChildSelection = true;

	private boolean mCacheEnabled = false;

	public interface OnPageChangeListener {
		void onPageChanged( int which, int old );
	}

	private OnPageChangeListener mOnPageChangeListener;

	public void setOnPageChangeListener( OnPageChangeListener listener ) {
		mOnPageChangeListener = listener;
	}

	public AviaryWorkspace ( Context context, AttributeSet attrs ) {
		this( context, attrs, 0 );
		initWorkspace( context, attrs, 0 );
	}

	public AviaryWorkspace ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		initWorkspace( context, attrs, defStyle );
	}

	private void initWorkspace( Context context, AttributeSet attrs, int defStyle ) {

		Theme theme = context.getTheme();

		TypedArray a = theme.obtainStyledAttributes( attrs, R.styleable.AviaryWorkspace, defStyle, 0 );
		mDefaultScreen = a.getInt( R.styleable.AviaryWorkspace_aviary_defaultScreen, 0 );
		int overscrollMode = a.getInt( R.styleable.AviaryWorkspace_aviary_overscroll, 0 );
		a.recycle();

		setHapticFeedbackEnabled( false );

		mScrollInterpolator = new DecelerateInterpolator( 1.0f );
		mScroller = new Scroller( context, mScrollInterpolator );
		mPreviousScreen = INVALID_SCREEN;

		final ViewConfiguration configuration = ViewConfiguration.get( getContext() );
		mTouchSlop = configuration.getScaledTouchSlop();
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

		mPaddingTop = getPaddingTop();
		mPaddingBottom = getPaddingBottom();
		mPaddingLeft = getPaddingLeft();
		mPaddingRight = getPaddingRight();

		setOverScroll( overscrollMode );
	}

	public void setOverScroll( int mode ) {
		if ( mode != OVER_SCROLL_NEVER ) {
			if ( mEdgeGlowLeft == null ) {
				mEdgeGlowLeft = new EdgeEffectCompat( getContext() );
				mEdgeGlowRight = new EdgeEffectCompat( getContext() );
			}
		} else {
			mEdgeGlowLeft = null;
			mEdgeGlowRight = null;
		}
		mOverScrollMode = mode;
	}

	public int getOverScroll() {
		return mOverScrollMode;
	}

	public void setAllowChildSelection( boolean value ) {
		mAllowChildSelection = value;
	}

	public Adapter getAdapter() {
		return mAdapter;
	}

	public void setAdapter( Adapter adapter ) {

		if ( mAdapter != null ) {
			mAdapter.unregisterDataSetObserver( mObserver );
			mAdapter = null;
		}

		mAdapter = adapter;
		resetList();

		if ( mAdapter != null ) {
			mObserver = new WorkspaceDataSetObserver();
			mAdapter.registerDataSetObserver( mObserver );
		}

		reloadAdapter();
	}

	private void reloadAdapter() {
		if ( null != mAdapter ) {
			mItemTypeCount = mAdapter.getViewTypeCount();
			mItemCount = mAdapter.getCount();

			mRecycleBin = Collections.synchronizedList( new ArrayList<Queue<View>>() );
			for ( int i = 0; i < mItemTypeCount; i++ ) {
				mRecycleBin.add( new LinkedList<View>() );
			}
		} else {
			mItemCount = 0;
		}

		mDataChanged = true;
		requestLayout();

	}

	@Override
	public void addView( View child, int index, LayoutParams params ) {
		if ( !( child instanceof CellLayout ) ) {
			throw new IllegalArgumentException( "A Workspace can only have CellLayout children." );
		}
		super.addView( child, index, params );
	}

	@Override
	public void addView( View child ) {
		if ( !( child instanceof CellLayout ) ) {
			throw new IllegalArgumentException( "A Workspace can only have CellLayout children." );
		}
		super.addView( child );
	}

	@Override
	public void addView( View child, int index ) {
		if ( !( child instanceof CellLayout ) ) {
			throw new IllegalArgumentException( "A Workspace can only have CellLayout children." );
		}
		super.addView( child, index );
	}

	@Override
	public void addView( View child, int width, int height ) {
		if ( !( child instanceof CellLayout ) ) {
			throw new IllegalArgumentException( "A Workspace can only have CellLayout children." );
		}
		super.addView( child, width, height );
	}

	@Override
	public void addView( View child, LayoutParams params ) {
		if ( !( child instanceof CellLayout ) ) {
			throw new IllegalArgumentException( "A Workspace can only have CellLayout children." );
		}
		super.addView( child, params );
	}

	boolean isDefaultScreenShowing() {
		return mCurrentScreen == mDefaultScreen;
	}

	public int getCurrentScreen() {
		return mCurrentScreen;
	}

	public int getTotalPages() {
		return mItemCount;
	}

	void setCurrentScreen( int currentScreen ) {
		if ( LOG_ENABLED ) {
			Log.i( LOG_TAG, "setCurrentScreen: " + currentScreen );
		}

		if ( !mScroller.isFinished() ) mScroller.abortAnimation();
		mCurrentScreen = Math.max( 0, Math.min( currentScreen, mItemCount - 1 ) );
		if ( mIndicator != null ) mIndicator.setLevel( mCurrentScreen, mItemCount );
		scrollTo( mCurrentScreen * getWidth(), 0 );
		invalidate();
	}

	@Override
	public void scrollTo( int x, int y ) {
		super.scrollTo( x, y );
		mTouchX = x;
		mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
	}

	@Override
	public void computeScroll() {

		if ( mScroller.computeScrollOffset() ) {
			mTouchX = mScroller.getCurrX();
			float mScrollX = mTouchX;
			mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
			float mScrollY = mScroller.getCurrY();
			scrollTo( (int) mScrollX, (int) mScrollY );
			postInvalidate();
		} else if ( mNextScreen != INVALID_SCREEN ) {
			int which = Math.max( 0, Math.min( mNextScreen, mItemCount - 1 ) );
			onFinishedAnimation( which );
		} else if ( mTouchState == TOUCH_STATE_SCROLLING ) {
			final float now = System.nanoTime() / NANOTIME_DIV;
			final float e = (float) Math.exp( ( now - mSmoothingTime ) / SMOOTHING_CONSTANT );
			final float dx = mTouchX - getScrollX();
			float mScrollX = getScrollX() + ( dx * e );
			scrollTo( (int) mScrollX, 0 );
			mSmoothingTime = now;

			if ( dx > 1.f || dx < -1.f ) {
				postInvalidate();
			}
		}
	}

	private View mOldSelectedChild;

	private void onFinishedAnimation( int newScreen ) {

		final int previousScreen = mCurrentScreen;

		final boolean toLeft = newScreen > mCurrentScreen;
		final boolean toRight = newScreen < mCurrentScreen;
		final boolean changed = newScreen != mCurrentScreen;

		mCurrentScreen = newScreen;
		if ( mIndicator != null ) mIndicator.setLevel( mCurrentScreen, mItemCount );
		setNextSelectedPositionInt( INVALID_SCREEN );

		fillToGalleryRight();
		fillToGalleryLeft();

		if ( toLeft ) {
			detachOffScreenChildren( true );
		} else if ( toRight ) {
			detachOffScreenChildren( false );
		}

		if ( changed || mItemCount == 1 || true ) {

			View child = getChildAt( mCurrentScreen - mFirstPosition );

			if ( null != child ) {
				if ( mAllowChildSelection ) {

					if ( null != mOldSelectedChild ) {
						mOldSelectedChild.setSelected( false );
						mOldSelectedChild = null;
					}

					child.setSelected( true );
					mOldSelectedChild = child;
				}
				child.requestFocus();
			}
		}

		clearChildrenCache();

		if ( mOnPageChangeListener != null && newScreen != mPreviousScreen ) {
			post( new Runnable() {

				@Override
				public void run() {

					if ( null != mOnPageChangeListener ) mOnPageChangeListener.onPageChanged( mCurrentScreen, previousScreen );
				}
			} );
		}

		postUpdateIndicator( mCurrentScreen, mItemCount );

		mPreviousScreen = newScreen;
	}

	private void detachOffScreenChildren( boolean toLeft ) {
		int numChildren = getChildCount();
		int start = 0;
		int count = 0;
		int viewType;

		if ( toLeft ) {
			final int galleryLeft = mPaddingLeft + getScreenScrollPositionX( mCurrentScreen - 1 );
			for ( int i = 0; i < numChildren; i++ ) {
				final View child = getChildAt( i );
				if ( child.getRight() > galleryLeft ) {
					break;
				} else {
					count++;
					viewType = mAdapter.getItemViewType( i + mFirstPosition );
					mRecycleBin.get( viewType ).offer( child );
				}
			}
		} else {
			final int galleryRight = getTotalWidth() + getScreenScrollPositionX( mCurrentScreen + 1 );
			for ( int i = numChildren - 1; i >= 0; i-- ) {
				final View child = getChildAt( i );
				if ( child.getLeft() < galleryRight ) {
					break;
				} else {
					start = i;
					count++;
					viewType = mAdapter.getItemViewType( i + mFirstPosition );
					mRecycleBin.get( viewType ).offer( child );
				}
			}
		}

		detachViewsFromParent( start, count );

		if ( toLeft && count > 0 ) {
			mFirstPosition += count;
		}

	}

	private void drawEdges( Canvas canvas ) {

		if ( mEdgeGlowLeft != null ) {

			final int width = getWidth();
			final int height = getHeight() - getPaddingTop() - getPaddingBottom();

			if ( !mEdgeGlowLeft.isFinished() ) {
				final int restoreCount = canvas.save();

				canvas.rotate( 270 );
				canvas.translate( -height + getPaddingTop(), 0 );
				mEdgeGlowLeft.setSize( height, width );
				if ( mEdgeGlowLeft.draw( canvas ) ) {
					postInvalidate();
				}
				canvas.restoreToCount( restoreCount );
			}

			if ( !mEdgeGlowRight.isFinished() ) {
				final int restoreCount = canvas.save();

				canvas.rotate( 90 );
				canvas.translate( -getPaddingTop(), -( mTouchX + width ) );
				mEdgeGlowRight.setSize( height, width );

				if ( mEdgeGlowRight.draw( canvas ) ) {
					postInvalidate();
				}
				canvas.restoreToCount( restoreCount );
			}
		}
	}

	@Override
	protected void dispatchDraw( Canvas canvas ) {
		boolean restore = false;
		int restoreCount = 0;

		if ( mItemCount < 1 ) return;
		if ( mCurrentScreen < 0 ) return;

		boolean fastDraw = mTouchState != TOUCH_STATE_SCROLLING && mNextScreen == INVALID_SCREEN;
		// If we are not scrolling or flinging, draw only the current screen
		if ( fastDraw ) {
			try {
				drawChild( canvas, getChildAt( mCurrentScreen - mFirstPosition ), getDrawingTime() );
			} catch ( RuntimeException e ) {
				e.printStackTrace();
			}
		} else {
			final long drawingTime = getDrawingTime();
			final float scrollPos = (float) getScrollX() / getTotalWidth();
			final int leftScreen = (int) scrollPos;
			final int rightScreen = leftScreen + 1;
			if ( leftScreen >= 0 ) {
				try {
					drawChild( canvas, getChildAt( leftScreen - mFirstPosition ), drawingTime );
				} catch ( RuntimeException e ) {
					e.printStackTrace();
				}
			}
			if ( scrollPos != leftScreen && rightScreen < mItemCount ) {
				try {
					drawChild( canvas, getChildAt( rightScreen - mFirstPosition ), drawingTime );
				} catch ( RuntimeException e ) {
					e.printStackTrace();
				}
			}
		}

		// let's draw the edges only if we have more than 1 page
		if ( mEdgeGlowLeft != null && mItemCount > 1 ) {
			drawEdges( canvas );
		}

		if ( restore ) {
			canvas.restoreToCount( restoreCount );
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		super.onMeasure( widthMeasureSpec, heightMeasureSpec );

		mWidthMeasureSpec = widthMeasureSpec;
		mHeightMeasureSpec = heightMeasureSpec;

		final int widthMode = MeasureSpec.getMode( widthMeasureSpec );
		final int heightMode = MeasureSpec.getMode( heightMeasureSpec );

		if ( widthMode != MeasureSpec.EXACTLY ) {
			// throw new IllegalStateException(
			// "Workspace can only be used in EXACTLY mode." );
		}

		if ( heightMode != MeasureSpec.EXACTLY ) {
			// throw new IllegalStateException(
			// "Workspace can only be used in EXACTLY mode." );
		}
	}

	private void handleDataChanged() {
		if ( mItemCount > 0 ) {
			setNextSelectedPositionInt( 0 );
		} else {
			mCurrentScreen = INVALID_SCREEN;
			mPreviousScreen = INVALID_SCREEN;
			setNextSelectedPositionInt( INVALID_SCREEN );
		}
	}

	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {

		if ( changed || mDataChanged ) {
			handleDataChanged();
		}

		if ( mItemCount < 1 ) {
			return;
		}

		final int width = right - left;

		if ( mDataChanged ) {
			if ( mCurrentScreen > INVALID_SCREEN ) scrollTo( mCurrentScreen * width, 0 );
			else scrollTo( 0, 0 );
		}

		if ( mNextScreen > INVALID_SCREEN ) {
			setSelectedPositionInt( mNextScreen );
		}

		if ( mDataChanged ) {
			mFirstPosition = mDefaultScreen;
			int childrenLeft = mPaddingLeft;
			int childrenWidth = ( getRight() - getLeft() ) - ( mPaddingLeft + mPaddingRight );
			View sel = makeAndAddView( mCurrentScreen, 0, 0, true );
			int selectedOffset = childrenLeft + ( childrenWidth / 2 ) - ( sel.getWidth() / 2 );
			sel.offsetLeftAndRight( selectedOffset );
			fillToGalleryRight();
			fillToGalleryLeft();
			checkSelectionChanged();
		}

		mDataChanged = false;
		setNextSelectedPositionInt( mCurrentScreen );

		if ( changed && !mDataChanged ) {
			layoutChildren();
		}
	}

	void checkSelectionChanged() {
		if ( ( mCurrentScreen != mOldSelectedPosition ) ) {
			// selectionChanged();
			mOldSelectedPosition = mCurrentScreen;
		}
	}

	private View makeAndAddView( int position, int offset, int x, boolean fromLeft ) {

		View child;

		if ( !mDataChanged ) {
			int viewType = mAdapter.getItemViewType( position );
			child = mRecycleBin.get( viewType ).poll();
			if ( child != null ) {
				child = mAdapter.getView( position, child, this );
				setUpChild( child, offset, x, fromLeft );
				return child;
			}
		}

		// Nothing found in the recycler -- ask the adapter for a view
		child = mAdapter.getView( position, null, this );

		// Position the view
		setUpChild( child, offset, x, fromLeft );
		return child;
	}

	private void setUpChild( View child, int offset, int x, boolean fromLeft ) {

		// Respect layout params that are already in the view. Otherwise
		// make some up...
		LayoutParams lp = child.getLayoutParams();
		if ( lp == null ) {
			lp = (LayoutParams) generateDefaultLayoutParams();
		}

		addViewInLayout( child, fromLeft ? -1 : 0, lp );

		if ( mAllowChildSelection ) {
			// final boolean wantfocus = offset == 0;
			// child.setSelected( wantfocus );
			// if( wantfocus ){
			// child.requestFocus();
			// }
		}

		// Get measure specs
		int childHeightSpec = ViewGroup.getChildMeasureSpec( mHeightMeasureSpec, mPaddingTop + mPaddingBottom, lp.height );
		int childWidthSpec = ViewGroup.getChildMeasureSpec( mWidthMeasureSpec, mPaddingLeft + mPaddingRight, lp.width );

		// Measure child
		child.measure( childWidthSpec, childHeightSpec );

		int childLeft;
		int childRight;

		// Position vertically based on gravity setting
		int childTop = calculateTop( child, true );
		int childBottom = childTop + child.getMeasuredHeight();

		int width = child.getMeasuredWidth();
		if ( fromLeft ) {
			childLeft = x;
			childRight = childLeft + width;
		} else {
			childLeft = x - width;
			childRight = x;
		}

		child.layout( childLeft, childTop, childRight, childBottom );

	}

	private void layoutChildren() {

		int total = getTotalPages();
		int x = mPaddingLeft;

		for ( int i = 0; i < total; i++ ) {
			View child = getScreenAt( i );

			if ( null == child ) continue;

			LayoutParams lp = child.getLayoutParams();

			// Get measure specs
			int childHeightSpec = ViewGroup.getChildMeasureSpec( mHeightMeasureSpec, mPaddingTop + mPaddingBottom, lp.height );
			int childWidthSpec = ViewGroup.getChildMeasureSpec( mWidthMeasureSpec, mPaddingLeft + mPaddingRight, lp.width );

			// Measure child
			child.measure( childWidthSpec, childHeightSpec );

			int childLeft;
			int childRight;

			// Position vertically based on gravity setting
			int childTop = calculateTop( child, true );
			int childBottom = childTop + child.getMeasuredHeight();

			int width = child.getMeasuredWidth();
			childLeft = x;
			childRight = childLeft + width;

			child.layout( childLeft, childTop, childRight, childBottom );
			x = childRight;
		}
	}

	private int calculateTop( View child, boolean duringLayout ) {
		return mPaddingTop;
	}

	private int getTotalWidth() {
		return getWidth();
	}

	private int getScreenScrollPositionX( int screen ) {
		return ( screen * getTotalWidth() );
	}

	private void fillToGalleryRight() {
		int itemSpacing = 0;
		int galleryRight = getScreenScrollPositionX( mCurrentScreen + 2 );
		int numChildren = getChildCount();
		int numItems = mItemCount;

		// Set state for initial iteration
		View prevIterationView = getChildAt( numChildren - 1 );
		int curPosition;
		int curLeftEdge;

		if ( prevIterationView != null ) {
			curPosition = mFirstPosition + numChildren;
			curLeftEdge = prevIterationView.getRight() + itemSpacing;
		} else {
			mFirstPosition = curPosition = mItemCount - 1;
			curLeftEdge = mPaddingLeft;
		}

		while ( curLeftEdge < galleryRight && curPosition < numItems ) {
			prevIterationView = makeAndAddView( curPosition, curPosition - mCurrentScreen, curLeftEdge, true );

			// Set state for next iteration
			curLeftEdge = prevIterationView.getRight() + itemSpacing;
			curPosition++;
		}
	}

	private void fillToGalleryLeft() {
		int itemSpacing = 0;
		int galleryLeft = getScreenScrollPositionX( mCurrentScreen - 1 );

		// Set state for initial iteration
		View prevIterationView = getChildAt( 0 );
		int curPosition;
		int curRightEdge;

		if ( prevIterationView != null ) {
			curPosition = mFirstPosition - 1;
			curRightEdge = prevIterationView.getLeft() - itemSpacing;
		} else {
			// No children available!
			curPosition = 0;
			curRightEdge = getRight() - getLeft() - mPaddingRight;
		}

		while ( curRightEdge > galleryLeft && curPosition >= 0 ) {
			prevIterationView = makeAndAddView( curPosition, curPosition - mCurrentScreen, curRightEdge, false );

			// Remember some state
			mFirstPosition = curPosition;

			// Set state for next iteration
			curRightEdge = prevIterationView.getLeft() - itemSpacing;
			curPosition--;
		}
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new LinearLayout.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
	}

	void resetList() {

		while ( getChildCount() > 0 ) {
			View view = getChildAt( 0 );
			detachViewFromParent( view );
			removeDetachedView( view, false );
		}

		emptyRecycler();

		mOldSelectedPosition = INVALID_SCREEN;
		setSelectedPositionInt( INVALID_SCREEN );
		setNextSelectedPositionInt( INVALID_SCREEN );
		mPreviousScreen = INVALID_SCREEN;
		postInvalidate();
	}

	private void emptyRecycler() {
		if ( null != mRecycleBin ) {
			while ( mRecycleBin.size() > 0 ) {
				Queue<View> recycler = mRecycleBin.remove( 0 );
				recycler.clear();
			}
			mRecycleBin.clear();
		}
	}

	private void setNextSelectedPositionInt( int screen ) {
		mNextScreen = screen;
	}

	private void setSelectedPositionInt( int screen ) {
		mCurrentScreen = screen;
	}

	@Override
	public boolean requestChildRectangleOnScreen( View child, Rect rectangle, boolean immediate ) {
		int screen = indexOfChild( child ) + mFirstPosition;

		if ( screen != mCurrentScreen || !mScroller.isFinished() ) {
			snapToScreen( screen );
			return true;
		}
		return false;
	}

	@Override
	protected boolean onRequestFocusInDescendants( int direction, Rect previouslyFocusedRect ) {

		if ( mItemCount < 1 ) return false;

		if ( isEnabled() ) {
			int focusableScreen;
			if ( mNextScreen != INVALID_SCREEN ) {
				focusableScreen = mNextScreen;
			} else {
				focusableScreen = mCurrentScreen;
			}

			if ( focusableScreen != INVALID_SCREEN ) {
				View child = getChildAt( focusableScreen );
				if ( null != child ) child.requestFocus( direction, previouslyFocusedRect );
			}
		}
		return false;
	}

	@Override
	public boolean dispatchUnhandledMove( View focused, int direction ) {

		if ( direction == View.FOCUS_LEFT ) {
			if ( getCurrentScreen() > 0 ) {
				snapToScreen( getCurrentScreen() - 1 );
				return true;
			}
		} else if ( direction == View.FOCUS_RIGHT ) {
			if ( getCurrentScreen() < mItemCount - 1 ) {
				snapToScreen( getCurrentScreen() + 1 );
				return true;
			}
		}
		return super.dispatchUnhandledMove( focused, direction );
	}

	@Override
	public void setEnabled( boolean enabled ) {
		super.setEnabled( enabled );

		for ( int i = 0; i < getChildCount(); i++ ) {
			getChildAt( i ).setEnabled( enabled );
		}

	}

	@Override
	public void addFocusables( ArrayList<View> views, int direction, int focusableMode ) {

		if ( isEnabled() ) {
			View child = getChildAt( mCurrentScreen );
			if ( null != child ) {
				child.addFocusables( views, direction );
			}

			if ( direction == View.FOCUS_LEFT ) {
				if ( mCurrentScreen > 0 ) {
					child = getChildAt( mCurrentScreen - 1 );
					if ( null != child ) {
						child.addFocusables( views, direction );
					}
				}
			} else if ( direction == View.FOCUS_RIGHT ) {
				if ( mCurrentScreen < mItemCount - 1 ) {
					child = getChildAt( mCurrentScreen + 1 );
					if ( null != child ) {
						child.addFocusables( views, direction );
					}
				}
			}
		}
	}

	@Override
	public boolean onInterceptTouchEvent( MotionEvent ev ) {

		final int action = ev.getAction();

		if ( !isEnabled() ) {
			return false; // We don't want the events. Let them fall through to the all
							// apps view.
		}

		if ( ( action == MotionEvent.ACTION_MOVE ) && ( mTouchState != TOUCH_STATE_REST ) ) {
			return true;
		}

		acquireVelocityTrackerAndAddMovement( ev );

		switch ( action & MotionEvent.ACTION_MASK ) {
			case MotionEvent.ACTION_MOVE: {

				/*
				 * Locally do absolute value. mLastMotionX is set to the y value of the
				 * down event.
				 */
				final int pointerIndex = ev.findPointerIndex( mActivePointerId );

				if ( pointerIndex < 0 ) {
					// invalid pointer
					return true;
				}

				final float x = ev.getX( pointerIndex );
				final float y = ev.getY( pointerIndex );
				final int xDiff = (int) Math.abs( x - mLastMotionX );
				final int yDiff = (int) Math.abs( y - mLastMotionY );

				final int touchSlop = mTouchSlop;
				boolean xMoved = xDiff > touchSlop;
				boolean yMoved = yDiff > touchSlop;
				mLastMotionX2 = x;

				if ( xMoved || yMoved ) {

					if ( xMoved ) {
						// Scroll if the user moved far enough along the X axis
						mTouchState = TOUCH_STATE_SCROLLING;
						mLastMotionX = x;
						mTouchX = getScrollX();
						mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
						enableChildrenCache( mCurrentScreen - 1, mCurrentScreen + 1 );
					}

				}
				break;
			}

			case MotionEvent.ACTION_DOWN: {
				final float x = ev.getX();
				final float y = ev.getY();
				// Remember location of down touch
				mLastMotionX = x;
				mLastMotionX2 = x;
				mLastMotionY = y;
				mActivePointerId = ev.getPointerId( 0 );
				mAllowLongPress = true;

				mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
				break;
			}

			case MotionEvent.ACTION_CANCEL:
			case MotionEvent.ACTION_UP:
				// Release the drag
				clearChildrenCache();
				mTouchState = TOUCH_STATE_REST;
				mActivePointerId = INVALID_POINTER;
				mAllowLongPress = false;
				releaseVelocityTracker();
				break;

			case MotionEvent.ACTION_POINTER_UP:
				onSecondaryPointerUp( ev );
				break;
		}

		/*
		 * The only time we want to intercept motion events is if we are in the drag mode.
		 */
		return mTouchState != TOUCH_STATE_REST;
	}

	private void onSecondaryPointerUp( MotionEvent ev ) {
		final int pointerIndex = ( ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK ) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		final int pointerId = ev.getPointerId( pointerIndex );
		if ( pointerId == mActivePointerId ) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			// TODO: Make this decision more intelligent.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionX = ev.getX( newPointerIndex );
			mLastMotionX2 = ev.getX( newPointerIndex );
			mLastMotionY = ev.getY( newPointerIndex );
			mActivePointerId = ev.getPointerId( newPointerIndex );
			if ( mVelocityTracker != null ) {
				mVelocityTracker.clear();
			}
		}
	}

	@Override
	public void focusableViewAvailable( View focused ) {
		View current = getChildAt( mCurrentScreen );
		View v = focused;
		while ( true ) {
			if ( v == current ) {
				super.focusableViewAvailable( focused );
				return;
			}
			if ( v == this ) {
				return;
			}
			ViewParent parent = v.getParent();
			if ( parent instanceof View ) {
				v = (View) v.getParent();
			} else {
				return;
			}
		}
	}

	public void enableChildrenCache( int fromScreen, int toScreen ) {

		if ( !mCacheEnabled ) return;

		if ( fromScreen > toScreen ) {
			final int temp = fromScreen;
			fromScreen = toScreen;
			toScreen = temp;
		}

		final int count = getChildCount();

		fromScreen = Math.max( fromScreen, 0 );
		toScreen = Math.min( toScreen, count - 1 );

		for ( int i = fromScreen; i <= toScreen; i++ ) {
			final CellLayout layout = (CellLayout) getChildAt( i );
			layout.setChildrenDrawnWithCacheEnabled( true );
			layout.setChildrenDrawingCacheEnabled( true );
		}
	}

	public void clearChildrenCache() {

		if ( !mCacheEnabled ) return;

		final int count = getChildCount();
		for ( int i = 0; i < count; i++ ) {
			final CellLayout layout = (CellLayout) getChildAt( i );
			layout.setChildrenDrawnWithCacheEnabled( false );
			layout.setChildrenDrawingCacheEnabled( false );
		}
	}

	public void setCacheEnabled( boolean value ) {
		mCacheEnabled = value;
	}

	@Override
	public boolean onTouchEvent( MotionEvent ev ) {

		final int action = ev.getAction();

		if ( !isEnabled() ) {
			if ( !mScroller.isFinished() ) {
				mScroller.abortAnimation();
			}
			snapToScreen( mCurrentScreen );
			return false; // We don't want the events. Let them fall through to the all
							// apps view.
		}

		acquireVelocityTrackerAndAddMovement( ev );

		switch ( action & MotionEvent.ACTION_MASK ) {
			case MotionEvent.ACTION_DOWN:
				/*
				 * If being flinged and user touches, stop the fling. isFinished will be
				 * false if being flinged.
				 */

				if ( !mScroller.isFinished() ) {
					mScroller.abortAnimation();
				}

				// Remember where the motion event started
				mLastMotionX = ev.getX();
				mLastMotionX2 = ev.getX();
				mActivePointerId = ev.getPointerId( 0 );
				if ( mTouchState == TOUCH_STATE_SCROLLING ) {
					enableChildrenCache( mCurrentScreen - 1, mCurrentScreen + 1 );
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if ( mTouchState == TOUCH_STATE_SCROLLING ) {
					// Scroll to follow the motion event
					final int pointerIndex = ev.findPointerIndex( mActivePointerId );
					final float x = ev.getX( pointerIndex );
					final float deltaX = mLastMotionX - x;
					final float deltaX2 = mLastMotionX2 - x;
					final int mode = mOverScrollMode;

					mLastMotionX = x;

					if ( deltaX < 0 ) {
						mTouchX += deltaX;
						mSmoothingTime = System.nanoTime() / NANOTIME_DIV;

						if ( mTouchX < 0 && mode != OVER_SCROLL_NEVER ) {
							mTouchX = mLastMotionX = 0;
							// mLastMotionX = x;

							if ( mEdgeGlowLeft != null && deltaX2 < 0 ) {
								mEdgeGlowLeft.onPull( (float) deltaX / getWidth() );
								if ( !mEdgeGlowRight.isFinished() ) {
									mEdgeGlowRight.onRelease();
								}
							}
						}

						invalidate();

					} else if ( deltaX > 0 ) {
						final int totalWidth = getScreenScrollPositionX( mItemCount - 1 );
						final float availableToScroll = getScreenScrollPositionX( mItemCount ) - mTouchX;
						mSmoothingTime = System.nanoTime() / NANOTIME_DIV;

						mTouchX += Math.min( availableToScroll, deltaX );

						if ( availableToScroll <= getWidth() && mode != OVER_SCROLL_NEVER ) {
							mTouchX = mLastMotionX = totalWidth;
							// mLastMotionX = x;

							if ( mEdgeGlowLeft != null && deltaX2 > 0 ) {
								mEdgeGlowRight.onPull( (float) deltaX / getWidth() );
								if ( !mEdgeGlowLeft.isFinished() ) {
									mEdgeGlowLeft.onRelease();
								}
							}
						}
						invalidate();

					} else {
						awakenScrollBars();
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				if ( mTouchState == TOUCH_STATE_SCROLLING ) {
					final VelocityTracker velocityTracker = mVelocityTracker;
					velocityTracker.computeCurrentVelocity( 1000, mMaximumVelocity );
					final int velocityX = (int) velocityTracker.getXVelocity( mActivePointerId );

					final int screenWidth = getWidth();
					final int whichScreen = ( getScrollX() + ( screenWidth / 2 ) ) / screenWidth;
					final float scrolledPos = (float) getScrollX() / screenWidth;

					if ( velocityX > SNAP_VELOCITY && mCurrentScreen > 0 ) {
						// Fling hard enough to move left.
						// Don't fling across more than one screen at a time.
						final int bound = scrolledPos < whichScreen ? mCurrentScreen - 1 : mCurrentScreen;
						snapToScreen( Math.min( whichScreen, bound ), velocityX, true );
					} else if ( velocityX < -SNAP_VELOCITY && mCurrentScreen < mItemCount - 1 ) {
						// Fling hard enough to move right
						// Don't fling across more than one screen at a time.
						final int bound = scrolledPos > whichScreen ? mCurrentScreen + 1 : mCurrentScreen;
						snapToScreen( Math.max( whichScreen, bound ), velocityX, true );
					} else {
						snapToScreen( whichScreen, 0, true );
					}

					if ( mEdgeGlowLeft != null ) {
						mEdgeGlowLeft.onRelease();
						mEdgeGlowRight.onRelease();
					}
				}
				mTouchState = TOUCH_STATE_REST;
				mActivePointerId = INVALID_POINTER;
				releaseVelocityTracker();
				break;
			case MotionEvent.ACTION_CANCEL:
				if ( mTouchState == TOUCH_STATE_SCROLLING ) {
					final int screenWidth = getWidth();
					final int whichScreen = ( getScrollX() + ( screenWidth / 2 ) ) / screenWidth;
					snapToScreen( whichScreen, 0, true );
				}
				mTouchState = TOUCH_STATE_REST;
				mActivePointerId = INVALID_POINTER;
				releaseVelocityTracker();

				if ( mEdgeGlowLeft != null ) {
					mEdgeGlowLeft.onRelease();
					mEdgeGlowRight.onRelease();
				}

				break;
			case MotionEvent.ACTION_POINTER_UP:
				onSecondaryPointerUp( ev );
				break;
		}

		return true;
	}

	private void acquireVelocityTrackerAndAddMovement( MotionEvent ev ) {
		if ( mVelocityTracker == null ) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement( ev );
	}

	private void releaseVelocityTracker() {
		if ( mVelocityTracker != null ) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	void snapToScreen( int whichScreen ) {
		snapToScreen( whichScreen, 0, false );
	}

	private void snapToScreen( int whichScreen, int velocity, boolean settle ) {

		whichScreen = Math.max( 0, Math.min( whichScreen, mItemCount - 1 ) );

		enableChildrenCache( mCurrentScreen, whichScreen );
		setNextSelectedPositionInt( whichScreen );

		View focusedChild = getFocusedChild();
		if ( focusedChild != null && whichScreen != mCurrentScreen && focusedChild == getChildAt( mCurrentScreen ) ) {
			focusedChild.clearFocus();
		}

		final int screenDelta = Math.max( 1, Math.abs( whichScreen - mCurrentScreen ) );
		final int newX = whichScreen * getWidth();
		final int delta = newX - getScrollX();
		int duration = ( screenDelta + 1 ) * 100;

		if ( !mScroller.isFinished() ) {
			mScroller.abortAnimation();
		}

		velocity = Math.abs( velocity );
		if ( velocity > 0 ) {
			duration += ( duration / ( velocity / BASELINE_FLING_VELOCITY ) ) * FLING_VELOCITY_INFLUENCE;
		} else {
			duration += 100;
		}

		mScroller.startScroll( getScrollX(), 0, delta, 0, duration );

		int mode = getOverScroll();

		if ( delta != 0 && ( mode == OVER_SCROLL_IF_CONTENT_SCROLLS ) ) {
			edgeReached( whichScreen, delta, velocity );
		}

		invalidate();
	}

	private void postUpdateIndicator( final int screen, final int count ) {
		getHandler().post( new Runnable() {

			@Override
			public void run() {
				if ( mIndicator != null ) mIndicator.setLevel( screen, count );
			}
		} );
	}

	void edgeReached( int whichscreen, int delta, int vel ) {

		if ( whichscreen == 0 || whichscreen == ( mItemCount - 1 ) ) {
			if ( delta < 0 ) {
				mEdgeGlowLeft.onAbsorb( vel );
			} else {
				mEdgeGlowRight.onAbsorb( vel );
			}
		}
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		final SavedState state = new SavedState( super.onSaveInstanceState() );
		state.currentScreen = mCurrentScreen;
		return state;
	}

	@Override
	protected void onRestoreInstanceState( Parcelable state ) {
		SavedState savedState = (SavedState) state;
		super.onRestoreInstanceState( savedState.getSuperState() );
		if ( savedState.currentScreen != -1 ) {
			mCurrentScreen = savedState.currentScreen;
		}
	}

	public void scrollLeft() {
		if ( mScroller.isFinished() ) {
			if ( mCurrentScreen > 0 ) snapToScreen( mCurrentScreen - 1 );
		} else {
			if ( mNextScreen > 0 ) snapToScreen( mNextScreen - 1 );
		}
	}

	public void scrollRight() {
		if ( mScroller.isFinished() ) {
			if ( mCurrentScreen < mItemCount - 1 ) snapToScreen( mCurrentScreen + 1 );
		} else {
			if ( mNextScreen < mItemCount - 1 ) snapToScreen( mNextScreen + 1 );
		}
	}

	public int getScreenForView( View v ) {
		int result = -1;
		if ( v != null ) {
			ViewParent vp = v.getParent();
			int count = mItemCount;
			for ( int i = 0; i < count; i++ ) {
				if ( vp == getChildAt( i ) ) {
					return i;
				}
			}
		}
		return result;
	}

	public View getViewForTag( Object tag ) {
		int screenCount = mItemCount;
		for ( int screen = 0; screen < screenCount; screen++ ) {
			CellLayout currentScreen = ( (CellLayout) getChildAt( screen ) );
			int count = currentScreen.getChildCount();
			for ( int i = 0; i < count; i++ ) {
				View child = currentScreen.getChildAt( i );
				if ( child.getTag() == tag ) {
					return child;
				}
			}
		}
		return null;
	}

	public boolean allowLongPress() {
		return mAllowLongPress;
	}

	public void setAllowLongPress( boolean allowLongPress ) {
		mAllowLongPress = allowLongPress;
	}

	void moveToDefaultScreen( boolean animate ) {
		if ( animate ) {
			snapToScreen( mDefaultScreen );
		} else {
			setCurrentScreen( mDefaultScreen );
		}
		getChildAt( mDefaultScreen ).requestFocus();
	}

	public void setIndicator( AviaryWorkspaceIndicator indicator ) {
		mIndicator = indicator;
		mIndicator.setLevel( mCurrentScreen, mItemCount );
	}

	public static class SavedState extends BaseSavedState {

		int currentScreen = -1;

		SavedState ( Parcelable superState ) {
			super( superState );
		}

		private SavedState ( Parcel in ) {
			super( in );
			currentScreen = in.readInt();
		}

		@Override
		public void writeToParcel( Parcel out, int flags ) {
			super.writeToParcel( out, flags );
			out.writeInt( currentScreen );
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {

			@Override
			public SavedState createFromParcel( Parcel in ) {
				return new SavedState( in );
			}

			@Override
			public SavedState[] newArray( int size ) {
				return new SavedState[size];
			}
		};
	}

	class WorkspaceDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			Log.i( LOG_TAG, "onChanged" );
			super.onChanged();

			resetList();
			reloadAdapter();
		}

		@Override
		public void onInvalidated() {
			Log.i( LOG_TAG, "onInvalidated" );
			super.onInvalidated();

			resetList();
			reloadAdapter();
		}
	}

	class RecycleBin {

		protected View[][] array;

		protected int start[];

		protected int end[];

		protected int maxSize;

		protected boolean full[];

		public RecycleBin ( int typeCount, int size ) {
			maxSize = size;
			array = new View[typeCount][size];
			start = new int[typeCount];
			end = new int[typeCount];
			full = new boolean[typeCount];
		}

		public boolean isEmpty( int type ) {
			return ( ( start[type] == end[type] ) && !full[type] );
		}

		public void add( int type, View o ) {
			if ( !full[type] ) array[type][start[type] = ( ++start[type] % array[type].length )] = o;
			if ( start[type] == end[type] ) full[type] = true;
		}

		public View remove( int type ) {
			if ( full[type] ) {
				full[type] = false;
			} else if ( isEmpty( type ) ) return null;
			return array[type][end[type] = ( ++end[type] % array[type].length )];
		}

		void clear() {
			int typeCount = array.length;

			for ( int i = 0; i < typeCount; i++ ) {
				while ( !isEmpty( i ) ) {
					final View view = remove( i );
					if ( view != null ) {
						removeDetachedView( view, true );
					}
				}
			}

			array = new View[typeCount][maxSize];
			start = new int[typeCount];
			end = new int[typeCount];
			full = new boolean[typeCount];
		}
	}

	public View getScreenAt( int screen ) {
		return getChildAt( screen - mFirstPosition );
	}
}
