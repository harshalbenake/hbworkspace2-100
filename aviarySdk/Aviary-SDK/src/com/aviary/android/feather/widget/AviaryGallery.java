package com.aviary.android.feather.widget;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ListView;

import com.aviary.android.feather.R;
import com.aviary.android.feather.widget.ScrollerRunnable.ScrollableView;

public class AviaryGallery extends AviaryAbsSpinner implements GestureDetector.OnGestureListener, ScrollableView, VibrationWidget {

	public interface OnItemsScrollListener {

		void onScrollStarted( AviaryAdapterView<?> parent, View view, int position, long id );

		void onScroll( AviaryAdapterView<?> parent, View view, int position, long id );

		void onScrollFinished( AviaryAdapterView<?> parent, View view, int position, long id );
	}

	@SuppressWarnings ( "unused" )
	private static final int SCROLL_TO_FLING_UNCERTAINTY_TIMEOUT = 250;

	// helper for this View vibration
	private VibrationHelper mVibratorHelper;

	// auto select child while scrolling
	private boolean mAutoSelectChild = false;

	private OnItemsScrollListener mItemsScrollListener = null;

	// center of this View
	private int mCenter = 0;

	// animation duration
	private int mAnimationDuration;

	// left and right edges
	private EdgeEffectCompat mEdgeGlowLeft;
	private EdgeEffectCompat mEdgeGlowRight;

	// vertical gravity for children
	private int mGravity = Gravity.CENTER_VERTICAL;

	private GestureDetector mGestureDetector;

	// remember the touch down position
	private int mDownTouchPosition;

	private boolean isDown;

	// scroller helper
	private ScrollerRunnable mScroller;

	private boolean mAutoScrollToCenter = true;

	private int mTouchSlop;

	ScrollCompletedSelectionNotifier mScrollCompletedNotifier;
	ScrollScrollSelectionNotifier mScrollScrollNotifier;

	private int mRealSelectedPosition;

	// Whether to continuously callback on the item selected listener during a fling.
	private boolean mShouldCallbackDuringFling = false;

	// Whether to callback when an item that is not selected is clicked.
	private boolean mShouldCallbackOnUnselectedItemClick = true;

	// If true, do not callback to item selected listener.
	private boolean mSuppressSelectionChanged = true;

	/**
	 * If true, we have received the "invoke" (center or enter buttons) key down. This is
	 * checked before we action on the "invoke"
	 * key up, and is subsequently cleared.
	 */
	private boolean mReceivedInvokeKeyDown;

	private AdapterContextMenuInfo mContextMenuInfo;

	private boolean mIsFirstScroll;

	@SuppressWarnings ( "unused" )
	private int mLastMotionValue;

	/**
	 * Instantiates a new gallery.
	 * 
	 * @param context
	 *            the context
	 */
	public AviaryGallery ( Context context ) {
		this( context, null );
	}

	/**
	 * Instantiates a new gallery.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 */
	public AviaryGallery ( Context context, AttributeSet attrs ) {
		this( context, attrs, R.attr.aviaryGalleryStyle );
	}

	/**
	 * Instantiates a new gallery.
	 * 
	 * @param context
	 *            the context
	 * @param attrs
	 *            the attrs
	 * @param defStyle
	 *            the def style
	 */
	public AviaryGallery ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );

		Theme theme = context.getTheme();
		TypedArray a = theme.obtainStyledAttributes( attrs, R.styleable.AviaryGallery, defStyle, 0 );

		mAnimationDuration = a.getInteger( R.styleable.AviaryGallery_android_animationDuration, 400 );

		a.recycle();

		mGestureDetector = new GestureDetector( context, this );
		mGestureDetector.setIsLongpressEnabled( false );

		ViewConfiguration configuration = ViewConfiguration.get( context );
		mTouchSlop = configuration.getScaledTouchSlop();

		mScroller = new ScrollerRunnable( this, mAnimationDuration, configuration.getScaledOverscrollDistance(), new DecelerateInterpolator( 1.0f ) );
		mVibratorHelper = new VibrationHelper( context, true );
	}

	/**
	 * if true then the center item will be always selected by default while scrolling
	 * otherwise the center item will be selected only when the movement has completed
	 * 
	 * @param value
	 */
	public void setAutoSelectChild( boolean value ) {
		mAutoSelectChild = value;
	}

	public boolean getAutoSelectChild() {
		return mAutoSelectChild;
	}

	@Override
	public void setVibrationEnabled( boolean value ) {
		mVibratorHelper.setEnabled( value );
	}

	@Override
	public boolean getVibrationEnabled() {
		return mVibratorHelper.enabled();
	}

	/**
	 * Sets the on items scroll listener.
	 * 
	 * @param value
	 *            the new on items scroll listener
	 */
	public void setOnItemsScrollListener( OnItemsScrollListener value ) {
		mItemsScrollListener = value;
	}

	@Override
	public void setOverScrollMode( int overScrollMode ) {
		super.setOverScrollMode( overScrollMode );

		if ( overScrollMode != OVER_SCROLL_NEVER ) {
			if ( mEdgeGlowLeft == null ) {
				mEdgeGlowLeft = new EdgeEffectCompat( getContext() );
				mEdgeGlowRight = new EdgeEffectCompat( getContext() );
			}
		} else {
			mEdgeGlowLeft = mEdgeGlowRight = null;
		}
	}

	@Override
	protected void dispatchDraw( Canvas canvas ) {
		super.dispatchDraw( canvas );

		if ( getChildCount() > 0 ) {
			drawEdges( canvas );
		}
	}

	private void drawEdges( Canvas canvas ) {

		if ( mEdgeGlowLeft != null ) {
			if ( !mEdgeGlowLeft.isFinished() ) {
				final int restoreCount = canvas.save();
				final int height = getHeight() - getPaddingTop() - getPaddingBottom();

				canvas.rotate( 270 );
				canvas.translate( -height + getPaddingTop(), 0 );
				mEdgeGlowLeft.setSize( height, getWidth() );
				if ( mEdgeGlowLeft.draw( canvas ) ) {
					postInvalidate();
				}
				canvas.restoreToCount( restoreCount );
			}
			if ( !mEdgeGlowRight.isFinished() ) {
				final int restoreCount = canvas.save();
				final int width = getWidth();
				final int height = getHeight() - getPaddingTop() - getPaddingBottom();

				canvas.rotate( 90 );
				canvas.translate( -getPaddingTop(), -width );
				mEdgeGlowRight.setSize( height, width );
				if ( mEdgeGlowRight.draw( canvas ) ) {
					postInvalidate();
				}
				canvas.restoreToCount( restoreCount );
			}
		}
	}

	/**
	 * Sets the auto scroll to center.
	 * 
	 * @param value
	 *            the new auto scroll to center
	 */
	public void setAutoScrollToCenter( boolean value ) {
		mAutoScrollToCenter = value;
	}

	/**
	 * Whether or not to callback on any {@link #getOnItemSelectedListener()} while the
	 * items are being flinged. If false, only the
	 * final selected item will cause the callback. If true, all items between the first
	 * and the final will cause callbacks.
	 * 
	 * @param shouldCallback
	 *            Whether or not to callback on the listener while the items are being
	 *            flinged.
	 */
	public void setCallbackDuringFling( boolean shouldCallback ) {
		mShouldCallbackDuringFling = shouldCallback;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		removeCallbacks( mScrollCompletedNotifier );
		removeCallbacks( mScrollScrollNotifier );
	}

	/**
	 * Whether or not to callback when an item that is not selected is clicked. If false,
	 * the item will become selected (and
	 * re-centered). If true, the {@link #getOnItemClickListener()} will get the callback.
	 * 
	 * @param shouldCallback
	 *            Whether or not to callback on the listener when a item that is not
	 *            selected is clicked.
	 * @hide
	 */
	public void setCallbackOnUnselectedItemClick( boolean shouldCallback ) {
		mShouldCallbackOnUnselectedItemClick = shouldCallback;
	}

	public void setAnimationDuration( int animationDurationMillis ) {
		mAnimationDuration = animationDurationMillis;
	}

	@Override
	protected int computeHorizontalScrollExtent() {
		return 1;
	}

	@Override
	protected int computeHorizontalScrollOffset() {
		return mSelectedPosition;
	}

	@Override
	protected int computeHorizontalScrollRange() {
		return mItemCount;
	}

	@Override
	protected boolean checkLayoutParams( ViewGroup.LayoutParams p ) {
		return p instanceof LayoutParams;
	}

	@Override
	protected ViewGroup.LayoutParams generateLayoutParams( ViewGroup.LayoutParams p ) {
		return new LayoutParams( p );
	}

	@Override
	public ViewGroup.LayoutParams generateLayoutParams( AttributeSet attrs ) {
		return new LayoutParams( getContext(), attrs );
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		/*
		 * AviaryGallery expects AviaryGallery.LayoutParams.
		 */
		return new AviaryGallery.LayoutParams( ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT );
	}

	@Override
	protected void onLayout( boolean changed, int l, int t, int r, int b ) {
		Log.i( VIEW_LOG_TAG, "onLayout: " + changed );
		super.onLayout( changed, l, t, r, b );

		mInLayout = true;
		layout( 0, false, changed );
		mInLayout = false;
	}

	@Override
	int getChildHeight( View child ) {
		return child.getMeasuredHeight();
	}

	@Override
	public void trackMotionScroll( int delta ) {
		if ( delta == 0 ) return;

		int deltaX = mScroller.getLastFlingX() - delta;

		// Pretend that each frame of a fling scroll is a touch scroll
		if ( delta > 0 ) {
			mDownTouchPosition = mFirstPosition;
			// Don't fling more than 1 screen
			// delta = Math.min( getWidth() - mPaddingLeft - mPaddingRight - 1, delta );
			delta = Math.min( getWidth() - 0 - 0 - 1, delta );
		} else {
			mDownTouchPosition = ( mFirstPosition + getChildCount() - 1 );
			// Don't fling more than 1 screen
			// delta = Math.max( -( getWidth() - mPaddingRight - mPaddingLeft - 1 ), delta
			// );
			delta = Math.max( -( getWidth() - 0 - 0 - 1 ), delta );
		}

		if ( getChildCount() == 0 ) {
			return;
		}

		boolean toLeft = deltaX < 0;

		int limitedDeltaX = deltaX;

		if ( !mScroller.isFinished() ) {
			int realDeltaX = getLimitedMotionScrollAmount( toLeft, deltaX );
			if ( realDeltaX != deltaX ) {
				if ( !toLeft ) { // <<<
					if ( null != mEdgeGlowLeft ) mEdgeGlowLeft.onAbsorb( (int) mScroller.getCurrVelocity() );
				} else { // >>>
					if ( null != mEdgeGlowRight ) mEdgeGlowRight.onAbsorb( (int) mScroller.getCurrVelocity() );
				}
				limitedDeltaX = realDeltaX;
			}
		}

		if ( limitedDeltaX != deltaX ) {
			mScroller.endFling( false );
			if ( limitedDeltaX == 0 ) onFinishedMovement();
		}

		offsetChildrenLeftAndRight( limitedDeltaX );
		detachOffScreenChildren( toLeft );

		if ( toLeft ) {
			// If moved left, there will be empty space on the right
			fillToGalleryRight();
		} else {
			// Similarly, empty space on the left
			fillToGalleryLeft();
		}

		setSelectionToCenterChild();
		onScrollChanged( 0, 0, 0, 0 ); // dummy values, View's implementation does not use
										// these.
		invalidate();
	}

	int getLimitedMotionScrollAmount( boolean motionToLeft, int deltaX ) {
		int extremeItemPosition = motionToLeft ? mItemCount - 1 : 0;
		View extremeChild = getChildAt( extremeItemPosition - mFirstPosition );

		if ( extremeChild == null ) {
			return deltaX;
		}

		int extremeChildCenter = getCenterOfView( extremeChild ) + ( motionToLeft ? extremeChild.getWidth() / 2 : -extremeChild.getWidth() / 2 );
		int galleryCenter = getCenterOfGallery();

		if ( motionToLeft ) {
			if ( extremeChildCenter <= galleryCenter ) {
				// The extreme child is past his boundary point!
				return 0;
			}
		} else {
			if ( extremeChildCenter >= galleryCenter ) {
				// The extreme child is past his boundary point!
				return 0;
			}
		}

		int centerDifference = galleryCenter - extremeChildCenter;

		return motionToLeft ? Math.max( centerDifference, deltaX ) : Math.min( centerDifference, deltaX );
	}

	/**
	 * Gets the over scroll delta.
	 * 
	 * @param motionToLeft
	 *            the motion to left
	 * @param deltaX
	 *            the delta x
	 * @return the over scroll delta
	 */
	int getOverScrollDelta( int deltaX ) {
		boolean motionToLeft = deltaX > 0;
		int extremeItemPosition = motionToLeft ? mItemCount - 1 : 0;

		View extremeChild = getChildAt( extremeItemPosition - mFirstPosition );

		if ( extremeChild == null ) {
			return deltaX;
		}

		int extremeChildCenter = getCenterOfView( extremeChild );
		int galleryCenter = getCenterOfGallery();

		if ( motionToLeft ) {
			if ( extremeChildCenter - deltaX < galleryCenter ) {
				return extremeChildCenter - galleryCenter;
			}
		} else {
			if ( extremeChildCenter - deltaX > galleryCenter ) {
				return extremeChildCenter - galleryCenter;
			}
		}
		return deltaX;
	}

	@Override
	protected void onOverScrolled( int scrollX, int scrollY, boolean clampedX, boolean clampedY ) {}

	/**
	 * Offset the horizontal location of all children of this view by the specified number
	 * of pixels.
	 * 
	 * @param offset
	 *            the number of pixels to offset
	 */
	private void offsetChildrenLeftAndRight( int offset ) {
		for ( int i = getChildCount() - 1; i >= 0; i-- ) {
			getChildAt( i ).offsetLeftAndRight( offset );
		}
	}

	private int getCenterOfGallery() {
		return mCenter;
	}

	@Override
	protected void onSizeChanged( int w, int h, int oldw, int oldh ) {
		super.onSizeChanged( w, h, oldw, oldh );
		// mCenter = ( w - mPaddingLeft - mPaddingRight ) / 2 + mPaddingLeft;
		mCenter = w / 2;

	}

	/**
	 * Gets the center of view.
	 * 
	 * @param view
	 *            the view
	 * @return The center of the given view.
	 */
	private static int getCenterOfView( View view ) {
		return view.getLeft() + view.getWidth() / 2;
	}

	/**
	 * Detaches children that are off the screen (i.e.: AviaryGallery bounds).
	 * 
	 * @param toLeft
	 *            Whether to detach children to the left of the AviaryGallery, or to the
	 *            right.
	 */
	private void detachOffScreenChildren( boolean toLeft ) {
		int numChildren = getChildCount();
		int firstPosition = mFirstPosition;
		int start = 0;
		int count = 0;

		if ( toLeft ) {
			final int galleryLeft = 0; // mPaddingLeft;
			for ( int i = 0; i < numChildren; i++ ) {
				int n = i;
				final View child = getChildAt( n );
				if ( child.getRight() >= galleryLeft ) {
					break;
				} else {
					start = n;
					count++;

					int viewType = mAdapter.getItemViewType( firstPosition + n );
					mRecycleBin.get( viewType ).add( child );
				}
			}
			start = 0;

		} else {
			final int galleryRight = getWidth(); // - mPaddingRight;
			for ( int i = numChildren - 1; i >= 0; i-- ) {
				int n = i;
				final View child = getChildAt( n );
				if ( child.getLeft() <= galleryRight ) {
					break;
				} else {
					start = n;
					count++;

					int viewType = mAdapter.getItemViewType( firstPosition + n );
					mRecycleBin.get( viewType ).add( child );
				}
			}
		}

		detachViewsFromParent( start, count );

		if ( toLeft ) {
			mFirstPosition += count;
		}
	}

	/**
	 * Scrolls the items so that the selected item is in its 'slot' (its center is the
	 * gallery's center).
	 */
	@Override
	public void scrollIntoSlots() {
		if ( getChildCount() == 0 ) return;

		if ( mAutoScrollToCenter ) {

			View view = getChildAt( mSelectedPosition - mFirstPosition );
			if ( null == view ) return;

			int selectedCenter = getCenterOfView( view );
			int targetCenter = getCenterOfGallery();
			int scrollAmount = targetCenter - selectedCenter;

			if ( scrollAmount != 0 ) {
				mScroller.startUsingDistance( 0, -scrollAmount );
			} else {
				onFinishedMovement();
			}
		} else {
			onFinishedMovement();
		}
	}

	/**
	 * Scrolls the items so that the selected item is in its 'slot' (its center is the
	 * gallery's center).
	 * 
	 * @return true, if is over scrolled
	 */
	private boolean isOverScrolled() {
		if ( getChildCount() < 2 ) return false;

		if ( mSelectedPosition == 0 || mSelectedPosition == mItemCount - 1 ) {

			View view = getChildAt( mSelectedPosition - mFirstPosition );
			if ( null == view ) return false;

			int selectedCenter0 = getCenterOfView( view );
			int targetCenter = getCenterOfGallery();

			if ( mSelectedPosition == 0 && selectedCenter0 > targetCenter ) return true;
			if ( ( mSelectedPosition == mItemCount - 1 ) && selectedCenter0 < targetCenter ) return true;
		}
		return false;
	}

	private void onFinishedMovement() {
		Log.i( VIEW_LOG_TAG, "onFinishedMovement" );

		if ( isDown ) return;

		if ( mSuppressSelectionChanged ) {
			mSuppressSelectionChanged = false;
			selectionChanged( mSelectedPosition, mSelectedPosition );
		}

		scrollCompleted();

		if ( !mAutoSelectChild ) {

			View centerChild = getSelectedView();

			if ( mRealSelectedPosition != mSelectedPosition ) {

				View old = getChildAt( mRealSelectedPosition - mFirstPosition );
				if ( null != old ) {
					old.setSelected( false );
				}
			}

			mRealSelectedPosition = mSelectedPosition;

			if ( null != centerChild ) {
				centerChild.setSelected( true );
			}
		}

		invalidate();
	}

	/**
	 * Looks for the child that is closest to the center and sets it as the selected
	 * child.
	 */
	private void setSelectionToCenterChild() {
		final View view = getChildAt( mSelectedPosition - mFirstPosition );

		if ( null == view ) return;

		int galleryCenter = getCenterOfGallery();

		// Common case where the current selected position is correct
		if ( view.getLeft() <= galleryCenter && view.getRight() >= galleryCenter ) {
			return;
		}

		int closestEdgeDistance = Integer.MAX_VALUE;
		int newSelectedChildIndex = 0;
		for ( int i = getChildCount() - 1; i >= 0; i-- ) {

			View child = getChildAt( i );

			if ( child.getLeft() <= galleryCenter && child.getRight() >= galleryCenter ) {
				newSelectedChildIndex = i;
				break;
			}

			int childClosestEdgeDistance = Math.min( Math.abs( child.getLeft() - galleryCenter ), Math.abs( child.getRight() - galleryCenter ) );
			if ( childClosestEdgeDistance < closestEdgeDistance ) {
				closestEdgeDistance = childClosestEdgeDistance;
				newSelectedChildIndex = i;
			}
		}

		int newPos = mFirstPosition + newSelectedChildIndex;

		if ( newPos != mSelectedPosition ) {

			newPos = Math.min( Math.max( newPos, 0 ), mItemCount - 1 );
			setSelectedPositionInt( newPos, true );
			setNextSelectedPositionInt( newPos );
			checkSelectionChanged();
		}
	}

	/**
	 * Creates and positions all views for this AviaryGallery.
	 * <p>
	 * We layout rarely, most of the time {@link #trackMotionScroll(int)} takes care of
	 * repositioning, adding, and removing children.
	 * 
	 * @param delta
	 *            Change in the selected position. +1 means the selection is moving to the
	 *            right, so views are scrolling to the
	 *            left. -1 means the selection is moving to the left.
	 * @param animate
	 *            the animate
	 */
	@Override
	void layout( int delta, boolean animate, boolean changed ) {

		if ( !changed && delta == 0 ) {
			layoutChildren();
			return;
		}

		int childrenLeft = 0; // mSpinnerPadding.left;
		int childrenWidth = getRight() - getLeft();// - mSpinnerPadding.left -
													// mSpinnerPadding.right;

		Log.i( VIEW_LOG_TAG, "layout. delta: " + delta + ", animate: " + animate + ", changed: " + changed + ", inLayout: " + mInLayout );

		if ( mDataChanged ) {
			handleDataChanged();
		}

		// Handle an empty gallery by removing all views.
		if ( mItemCount == 0 ) {
			resetList();
			return;
		}

		// Update to the new selected position.
		Log.d( VIEW_LOG_TAG, "mNextSelectedPosition: " + mNextSelectedPosition );
		if ( mNextSelectedPosition >= 0 ) {
			mRealSelectedPosition = mNextSelectedPosition;
			setSelectedPositionInt( mNextSelectedPosition, animate );
		}

		// All views go in recycler while we are in layout
		mFirstPosition = mSelectedPosition;

		if ( getChildCount() == 0 || changed || delta != 0 ) {
			recycleAllViews();
			emptySubRecycler();
			removeAllViewsInLayout();

			// Make selected view and center it
			View sel = makeAndAddView( mSelectedPosition, 0, 0, true );

			// Put the selected child in the center
			int selectedOffset = childrenLeft + ( childrenWidth / 2 ) - ( sel.getWidth() / 2 );
			sel.offsetLeftAndRight( selectedOffset );

			fillToGalleryRight();
			fillToGalleryLeft();
		}

		// checkSelectionChanged();

		mDataChanged = false;
		mNeedSync = false;

		mSuppressSelectionChanged = false;
		setNextSelectedPositionInt( mSelectedPosition );
		checkSelectionChanged();
		mSuppressSelectionChanged = true;

		Log.d( VIEW_LOG_TAG, "layout complete" );
		postInvalidate();
	}

	private void fillToGalleryLeft() {
		int galleryLeft = 0; // mPaddingLeft;

		// Set state for initial iteration
		View prevIterationView = getChildAt( 0 );
		int curPosition;
		int curRightEdge;

		if ( prevIterationView != null ) {
			curPosition = mFirstPosition - 1;
			curRightEdge = prevIterationView.getLeft();// - itemSpacing;
		} else {
			// No children available!
			curPosition = 0;
			curRightEdge = getRight() - getLeft();// - mPaddingRight;
		}

		while ( curRightEdge > galleryLeft /* && curPosition >= 0 */) {
			prevIterationView = makeAndAddView( curPosition, curPosition - mSelectedPosition, curRightEdge, false );

			// Remember some state
			mFirstPosition = curPosition;

			// Set state for next iteration
			curRightEdge = prevIterationView.getLeft();// - itemSpacing;
			curPosition--;
		}
	}

	private void fillToGalleryRight() {
		int galleryRight = getRight() - getLeft();// - mPaddingRight;
		int numChildren = getChildCount();

		// Set state for initial iteration
		View prevIterationView = getChildAt( numChildren - 1 );
		int curPosition;
		int curLeftEdge;

		if ( prevIterationView != null ) {
			curPosition = mFirstPosition + numChildren;
			curLeftEdge = prevIterationView.getRight(); // + itemSpacing;
		} else {
			mFirstPosition = curPosition = mItemCount - 1;
			curLeftEdge = 0; // mPaddingLeft;
		}

		while ( curLeftEdge < galleryRight /* && curPosition < numItems */) {
			prevIterationView = makeAndAddView( curPosition, curPosition - mSelectedPosition, curLeftEdge, true );

			// Set state for next iteration
			curLeftEdge = prevIterationView.getRight();// + itemSpacing;
			curPosition++;
		}
	}

	private View makeAndAddView( int position, int offset, int x, boolean fromLeft ) {

		View child = null;
		int viewType = mAdapter.getItemViewType( position );

		if ( !mDataChanged ) {
			child = mRecycleBin.get( viewType ).poll();

			if ( child != null ) {
				child = mAdapter.getView( position, child, this );
				setUpChild( child, offset, x, fromLeft );
			}
		}

		if ( null == child ) {
			child = mAdapter.getView( position, null, this );
			setUpChild( child, offset, x, fromLeft );
		}

		// auto select children is false
		if ( !mAutoSelectChild && null != child ) {
			child.setSelected( position == mRealSelectedPosition );
		}

		return child;
	}

	public void invalidateViews() {
		int count = getChildCount();
		for ( int i = 0; i < count; i++ ) {
			View child = getChildAt( i );
			mAdapter.getView( mFirstPosition + i, child, this );

			// auto select children is false
			if ( !mAutoSelectChild ) {
				child.setSelected( ( mFirstPosition + i ) == mRealSelectedPosition );
			}
		}
	}

	/**
	 * Helper for makeAndAddView to set the position of a view and fill out its layout
	 * parameters.
	 * 
	 * @param child
	 *            The view to position
	 * @param offset
	 *            Offset from the selected position
	 * @param x
	 *            X-coordinate indicating where this view should be placed. This will
	 *            either be the left or right edge of the view,
	 *            depending on the fromLeft parameter
	 * @param fromLeft
	 *            Are we positioning views based on the left edge? (i.e., building from
	 *            left to right)?
	 */
	private void setUpChild( View child, int offset, int x, boolean fromLeft ) {

		AviaryGallery.LayoutParams lp = (AviaryGallery.LayoutParams) child.getLayoutParams();

		if ( lp == null ) {
			lp = (AviaryGallery.LayoutParams) generateDefaultLayoutParams();
		}

		addViewInLayout( child, fromLeft ? -1 : 0, lp );

		if ( mAutoSelectChild ) child.setSelected( offset == 0 );

		// Get measure specs
		int childHeightSpec = ViewGroup.getChildMeasureSpec( mHeightMeasureSpec, mPaddingTop + mPaddingBottom, lp.height );
		int childWidthSpec = ViewGroup.getChildMeasureSpec( mWidthMeasureSpec, 0 /*
																				 * mSpinnerPadding
																				 * .left +
																				 * mSpinnerPadding
																				 * .right
																				 */, lp.width );

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

	public void layoutChildren() {
		for ( int i = 0; i < getChildCount(); i++ ) {
			View child = getChildAt( i );
			forceChildLayout( child, child.getLayoutParams() );
			child.layout( child.getLeft(), child.getTop(), child.getRight(), child.getBottom() );
		}
	}

	public void forceChildLayout( View child, LayoutParams params ) {
		int childHeightSpec = ViewGroup.getChildMeasureSpec( mHeightMeasureSpec, getPaddingTop() + getPaddingBottom(), params.height );
		int childWidthSpec = ViewGroup.getChildMeasureSpec( mWidthMeasureSpec, getPaddingLeft() + getPaddingRight(), params.width );
		child.measure( childWidthSpec, childHeightSpec );
	}

	/**
	 * Figure out vertical placement based on mGravity.
	 * 
	 * @param child
	 *            Child to place
	 * @param duringLayout
	 *            the during layout
	 * @return Where the top of the child should be
	 */
	private int calculateTop( View child, boolean duringLayout ) {
		int myHeight = duringLayout ? getMeasuredHeight() : getHeight();
		int childHeight = duringLayout ? child.getMeasuredHeight() : child.getHeight();

		int childTop = 0;

		switch ( mGravity ) {
			case Gravity.TOP:
				childTop = mSpinnerPadding.top;
				break;
			case Gravity.CENTER_VERTICAL:
				int availableSpace = myHeight - mSpinnerPadding.bottom - mSpinnerPadding.top - childHeight;
				childTop = mSpinnerPadding.top + ( availableSpace / 2 );
				break;
			case Gravity.BOTTOM:
				childTop = myHeight - mSpinnerPadding.bottom - childHeight;
				break;
		}
		return childTop;
	}

	@Override
	public boolean onTouchEvent( MotionEvent event ) {

		// Give everything to the gesture detector
		boolean retValue = mGestureDetector.onTouchEvent( event );

		int action = event.getAction();
		if ( action == MotionEvent.ACTION_UP ) {
			onUp();
		} else if ( action == MotionEvent.ACTION_CANCEL ) {
			onCancel();
		}

		return retValue;
	}

	@Override
	public boolean onSingleTapUp( MotionEvent e ) {

		if ( mDownTouchPosition >= 0 && mDownTouchPosition < mItemCount ) {

			if ( !scrollToChild( mDownTouchPosition - mFirstPosition ) ) {

				if ( mShouldCallbackOnUnselectedItemClick || mDownTouchPosition == mSelectedPosition ) {
					performItemClick( getChildAt( mDownTouchPosition - mFirstPosition ), mDownTouchPosition, mAdapter.getItemId( mDownTouchPosition ) );
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void onLongPress( MotionEvent e ) {}

	@Override
	public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {

		if ( !mShouldCallbackDuringFling ) {
			mSuppressSelectionChanged = true;
		}

		int initialVelocity = (int) -velocityX / 2;
		int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;

		int delta = initialVelocity < 0 ? -2 : 2;
		int clampedDelta = getOverScrollDelta( delta );

		if ( delta == clampedDelta ) {
			mScroller.startUsingVelocity( initialX, initialVelocity );
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {

		getParent().requestDisallowInterceptTouchEvent( true );

		mSuppressSelectionChanged = false;

		if ( mIsFirstScroll ) {

			if ( mItemsScrollListener != null ) {
				int selection = this.getSelectedItemPosition();
				if ( selection >= 0 ) {
					View v = getSelectedView();
					mItemsScrollListener.onScrollStarted( this, v, selection, getAdapter().getItemId( selection ) );
				}
			}

			if ( distanceX > 0 ) distanceX -= mTouchSlop;
			else distanceX += mTouchSlop;
		}

		int delta = (int) distanceX;
		int clampedDelta = getOverScrollDelta( delta );
		trackMotionScroll( clampedDelta );

		if ( null != mEdgeGlowLeft ) {
			// edges scroll
			if ( delta != clampedDelta ) {
				final boolean toLeft = delta > 0;
				float max_width;

				View view = getChildAt( 0 );
				if ( null != view ) {
					max_width = view.getWidth() * 2;
				} else {
					max_width = getWidth() / 2;
				}

				if ( !toLeft ) {
					mEdgeGlowLeft.onPull( -delta / max_width );
					if ( !mEdgeGlowRight.isFinished() ) {
						mEdgeGlowRight.onRelease();
					}
				} else {
					mEdgeGlowRight.onPull( delta / max_width );

					if ( !mEdgeGlowLeft.isFinished() ) {
						mEdgeGlowLeft.onRelease();
					}
				}
			}

			if ( !mEdgeGlowRight.isFinished() || !mEdgeGlowLeft.isFinished() ) {
				postInvalidate();
			}
		}

		mIsFirstScroll = false;
		return true;
	}

	@Override
	public boolean onDown( MotionEvent e ) {

		isDown = true;
		mScroller.stop( false );
		mDownTouchPosition = pointToPosition( (int) e.getX(), (int) e.getY() );

		if ( mDownTouchPosition >= 0 && mDownTouchPosition < mItemCount ) {
			View view = getChildAt( mDownTouchPosition - mFirstPosition );
			if ( null != view ) {
				// view.setPressed( true );
			}
		}
		mIsFirstScroll = true;
		return true;
	}

	void onUp() {
		isDown = false;
		if ( mScroller.isFinished() ) {
			scrollIntoSlots();
		} else {
			if ( isOverScrolled() ) {
				scrollIntoSlots();
			}
		}

		if ( mEdgeGlowLeft != null ) {
			mEdgeGlowLeft.onRelease();
			mEdgeGlowRight.onRelease();
		}
		dispatchUnpress();
	}

	void onCancel() {
		onUp();
	}

	@Override
	public void onShowPress( MotionEvent e ) {}

	private void dispatchPress( View child ) {
		if ( child != null ) {
			// child.setPressed( true );
		}
		setPressed( true );
	}

	private void dispatchUnpress() {
		for ( int i = getChildCount() - 1; i >= 0; i-- ) {
			// getChildAt( i ).setPressed( false );
		}
		setPressed( false );
	}

	@Override
	public void dispatchSetSelected( boolean selected ) {
		Log.i( VIEW_LOG_TAG, "dispatchSetSelected" );
	}

	@Override
	protected void dispatchSetPressed( boolean pressed ) {
		View view = getChildAt( mSelectedPosition - mFirstPosition );
		if ( view != null ) {
			// view.setPressed( pressed );
		}
	}

	@Override
	protected ContextMenuInfo getContextMenuInfo() {
		return mContextMenuInfo;
	}

	@Override
	public boolean onKeyDown( int keyCode, KeyEvent event ) {
		switch ( keyCode ) {

			case KeyEvent.KEYCODE_DPAD_LEFT:
				if ( movePrevious() ) {
					playSoundEffect( SoundEffectConstants.NAVIGATION_LEFT );
				}
				return true;

			case KeyEvent.KEYCODE_DPAD_RIGHT:
				if ( moveNext() ) {
					playSoundEffect( SoundEffectConstants.NAVIGATION_RIGHT );
				}
				return true;

			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER:
				mReceivedInvokeKeyDown = true;
		}

		return false;
	}

	@Override
	public boolean dispatchKeyEvent( KeyEvent event ) {
		boolean handled = event.dispatch( this, null, null );
		return handled;
	}

	@Override
	public boolean onKeyUp( int keyCode, KeyEvent event ) {
		switch ( keyCode ) {
			case KeyEvent.KEYCODE_DPAD_CENTER:
			case KeyEvent.KEYCODE_ENTER: {

				if ( mReceivedInvokeKeyDown ) {
					if ( mItemCount > 0 ) {

						int selectedIndex = mSelectedPosition - mFirstPosition;
						View view = getChildAt( selectedIndex );
						if ( null != view ) {
							dispatchPress( view );
							postDelayed( new Runnable() {

								@Override
								public void run() {
									dispatchUnpress();
								}
							}, ViewConfiguration.getPressedStateDuration() );

							performItemClick( view, mSelectedPosition, mAdapter.getItemId( mSelectedPosition ) );
						}
					}
				}
				mReceivedInvokeKeyDown = false;
				return true;
			}
		}
		return false;
	}

	public boolean movePrevious() {
		if ( mItemCount > 0 && mSelectedPosition > 0 ) {
			scrollToChild( mSelectedPosition - mFirstPosition - 1 );
			return true;
		} else {
			return false;
		}
	}

	public boolean moveNext() {
		if ( mItemCount > 0 && mSelectedPosition < mItemCount - 1 ) {
			scrollToChild( mSelectedPosition - mFirstPosition + 1 );
			return true;
		} else {
			return false;
		}
	}

	private boolean scrollToChild( int childPosition ) {
		View child = getChildAt( childPosition );
		if ( child != null ) {
			if ( mItemsScrollListener != null ) {
				int selection = this.getSelectedItemPosition();
				if ( selection >= 0 ) {
					View v = getSelectedView();
					mItemsScrollListener.onScrollStarted( this, v, selection, getAdapter().getItemId( selection ) );
				}
			}
			int distance = getCenterOfGallery() - getCenterOfView( child );
			mScroller.startUsingDistance( 0, -distance );
			return distance != 0;
		}
		return false;
	}

	void setSelectedPositionInt( int position, boolean animate ) {
		Log.i( VIEW_LOG_TAG, "setSelectedPositionInt: " + position + " from " + mSelectedPosition );
		mOldSelectedPosition = mSelectedPosition;
		super.setSelectedPositionInt( position );
		// updateSelectedItemMetadata( animate, false );
		// checkSelectionChanged();
	}

	private void fireVibration() {
		mVibratorHelper.vibrate( 10 );
	}

	/**
	 * Describes how the child views are aligned.
	 * 
	 * @param gravity
	 *            the new gravity
	 * @attr ref android.R.styleable#Gallery_gravity
	 */
	public void setGravity( int gravity ) {
		if ( mGravity != gravity ) {
			mGravity = gravity;
			requestLayout();
		}
	}

	/*
	 * @Override protected int getChildDrawingOrder( int childCount, int i ) { int
	 * selectedIndex = mSelectedPosition -
	 * mFirstPosition; // Just to be safe if ( selectedIndex < 0 ) return i; if ( i ==
	 * childCount - 1 ) { // Draw the selected child
	 * last return selectedIndex; } else if ( i >= selectedIndex ) { // Move the children
	 * after the selected child earlier one return
	 * i + 1; } else { // Keep the children before the selected child the same return i; }
	 * }
	 */

	@Override
	protected void onFocusChanged( boolean gainFocus, int direction, Rect previouslyFocusedRect ) {
		super.onFocusChanged( gainFocus, direction, previouslyFocusedRect );

		View current = getChildAt( mSelectedPosition - mFirstPosition );

		if ( gainFocus && current != null ) {
			// current.requestFocus( direction );

			if ( mAutoSelectChild ) current.setSelected( true );
		}
	}

	private class ScrollCompletedSelectionNotifier implements Runnable {
		@Override
		public void run() {
			if ( mDataChanged ) {
				if ( getAdapter() != null ) {
					post( this );
				}
			} else {
				fireOnScrollCompleted();
			}
		}
	}

	private class ScrollScrollSelectionNotifier implements Runnable {

		@Override
		public void run() {
			if ( mDataChanged ) {
				if ( getAdapter() != null ) {
					post( this );
				}
			} else {
				fireOnSelected();
			}
		}
	}

	void scrollCompleted() {
		if ( mItemsScrollListener != null ) {
			if ( mInLayout || mBlockLayoutRequests ) {
				// If we are in a layout traversal, defer notification
				// by posting. This ensures that the view tree is
				// in a consistent state and is able to accomodate
				// new layout or invalidate requests.
				if ( mScrollCompletedNotifier == null ) {
					mScrollCompletedNotifier = new ScrollCompletedSelectionNotifier();
				}
				post( mScrollCompletedNotifier );
			} else {
				fireOnScrollCompleted();
			}
		}
	}

	private void fireOnScrollCompleted() {
		Log.i( VIEW_LOG_TAG, "fireOnScrollCompleted" );

		if ( mItemsScrollListener == null ) return;

		int selection = this.getSelectedItemPosition();
		if ( selection >= 0 && selection < mItemCount ) {
			View v = getSelectedView();
			mItemsScrollListener.onScrollFinished( this, v, selection, getAdapter().getItemId( selection ) );
		}
	}

	/**
	 * We completely override the super method because we dont
	 * want to use the AdapterView scroll listener
	 */
	@Override
	void selectionChanged( int oldIndex, int newIndex ) {
		Log.i( VIEW_LOG_TAG, "selectionChanged: " + mSelectedPosition + "(" + oldIndex + "/" + newIndex + "), inLayout: " + mInLayout + ", suppress selection: "
				+ mSuppressSelectionChanged );

		if ( mAutoSelectChild && oldIndex != newIndex ) {
			View oldSelectedChild = getChildAt( oldIndex - mFirstPosition );
			View newSelectedChild = getChildAt( newIndex - mFirstPosition );

			if ( null != newSelectedChild ) {
				newSelectedChild.setSelected( true );
			}

			if ( null != oldSelectedChild && oldSelectedChild != newSelectedChild ) {
				oldSelectedChild.setSelected( false );
			}
		}

		if ( !mSuppressSelectionChanged ) {

			if ( mItemsScrollListener != null ) {
				if ( mInLayout || mBlockLayoutRequests ) {
					// if ( mScrollScrollNotifier == null ) {
					// mScrollScrollNotifier = new ScrollScrollSelectionNotifier();
					// }
					// post( mScrollScrollNotifier );
				} else {
					fireOnSelected();
				}
			}

			// we fire selection events here not in View
			if ( mSelectedPosition != ListView.INVALID_POSITION && isShown() && !isInTouchMode() ) {
				sendAccessibilityEvent( AccessibilityEvent.TYPE_VIEW_SELECTED );
			}
		}

		if ( oldIndex != newIndex && !mInLayout ) {
			// selection changed, fire the vibration
			fireVibration();
		}
	}

	@Override
	protected void fireOnSelected() {
		Log.i( VIEW_LOG_TAG, "fireOnSelected: current: " + mSelectedPosition + ", next: " + getSelectedItemPosition() + ", inLayout: " + mInLayout );

		if ( mItemsScrollListener == null ) return;

		View newSelectedChild = getSelectedView();
		int position = getSelectedItemPosition();
		mItemsScrollListener.onScroll( this, newSelectedChild, position, getAdapter().getItemId( position ) );
	}

	@Override
	public int getMinX() {
		return 0;
	}

	@Override
	public int getMaxX() {
		return Integer.MAX_VALUE;
	}

	/**
	 * Return the current item at the center of the Gallery ( not the one currently
	 * selected )
	 * 
	 * @return
	 */
	@Override
	public int getSelectedItemPosition() {
		return super.getSelectedItemPosition();
	}

	/**
	 * Return the current selected item position.
	 * If the {@link #getAutoSelectChild()} is false, then the position is different from
	 * the {@link #getSelectedItemPosition()}.
	 * 
	 * @return
	 */
	public int getCurrentSelectedItemPosition() {
		if ( !mAutoSelectChild ) return mRealSelectedPosition;
		return mSelectedPosition;
	}

}
