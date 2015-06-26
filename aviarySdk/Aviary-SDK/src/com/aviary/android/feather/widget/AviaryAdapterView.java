/*
 * Copyright (C) 2006 The Android Open Source Project Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of the
 * License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed
 * to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under the
 * License.
 */

package com.aviary.android.feather.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Adapter;
import android.widget.ListView;

public abstract class AviaryAdapterView<T extends Adapter> extends ViewGroup {

	public static final int ITEM_VIEW_TYPE_IGNORE = -1;
	public static final int ITEM_VIEW_TYPE_HEADER_OR_FOOTER = -2;

	int mFirstPosition = 0;
	int mSpecificTop;
	int mSyncPosition;
	long mSyncRowId = INVALID_ROW_ID;
	long mSyncHeight;
	boolean mNeedSync = false;
	int mSyncMode;
	private int mLayoutHeight;
	static final int SYNC_SELECTED_POSITION = 0;
	static final int SYNC_FIRST_POSITION = 1;
	static final int SYNC_MAX_DURATION_MILLIS = 100;
	boolean mInLayout = false;
	OnItemSelectedListener mOnItemSelectedListener;
	OnItemClickListener mOnItemClickListener;
	OnItemLongClickListener mOnItemLongClickListener;
	boolean mDataChanged;
	int mNextSelectedPosition = INVALID_POSITION;
	long mNextSelectedRowId = INVALID_ROW_ID;
	int mSelectedPosition = INVALID_POSITION;
	long mSelectedRowId = INVALID_ROW_ID;
	private View mEmptyView;
	int mItemCount;
	int mOldItemCount;
	public static final int INVALID_POSITION = -1;
	public static final long INVALID_ROW_ID = Long.MIN_VALUE;
	int mOldSelectedPosition = INVALID_POSITION;
	long mOldSelectedRowId = INVALID_ROW_ID;
	private boolean mDesiredFocusableState;
	private boolean mDesiredFocusableInTouchModeState;
	private SelectionNotifier mSelectionNotifier;
	boolean mBlockLayoutRequests = false;

	public AviaryAdapterView ( Context context ) {
		super( context );
	}

	public AviaryAdapterView ( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public AviaryAdapterView ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	public interface OnItemClickListener {

		void onItemClick( AviaryAdapterView<?> parent, View view, int position, long id );
	}

	public void setOnItemClickListener( OnItemClickListener listener ) {
		mOnItemClickListener = listener;
	}

	public final OnItemClickListener getOnItemClickListener() {
		return mOnItemClickListener;
	}

	public boolean performItemClick( View view, int position, long id ) {
		if ( mOnItemClickListener != null && null != view ) {
			playSoundEffect( SoundEffectConstants.CLICK );
			view.sendAccessibilityEvent( AccessibilityEvent.TYPE_VIEW_CLICKED );
			mOnItemClickListener.onItemClick( this, view, position, id );
			return true;
		}
		return false;
	}

	public interface OnItemLongClickListener {

		boolean onItemLongClick( AviaryAdapterView<?> parent, View view, int position, long id );
	}

	public void setOnItemLongClickListener( OnItemLongClickListener listener ) {
		if ( !isLongClickable() ) {
			setLongClickable( true );
		}
		mOnItemLongClickListener = listener;
	}

	public final OnItemLongClickListener getOnItemLongClickListener() {
		return mOnItemLongClickListener;
	}

	public interface OnItemSelectedListener {

		void onItemSelected( AviaryAdapterView<?> parent, View view, int position, long id );

		void onNothingSelected( AviaryAdapterView<?> parent );
	}

	public void setOnItemSelectedListener( OnItemSelectedListener listener ) {
		mOnItemSelectedListener = listener;
	}

	public final OnItemSelectedListener getOnItemSelectedListener() {
		return mOnItemSelectedListener;
	}

	public static class AdapterContextMenuInfo implements ContextMenu.ContextMenuInfo {

		public AdapterContextMenuInfo ( View targetView, int position, long id ) {
			this.targetView = targetView;
			this.position = position;
			this.id = id;
		}

		public View targetView;

		public int position;

		public long id;
	}

	public abstract T getAdapter();

	public abstract void setAdapter( T adapter );

	@Override
	public void addView( View child ) {
		throw new UnsupportedOperationException( "addView(View) is not supported in AviaryAdapterView" );
	}

	@Override
	public void addView( View child, int index ) {
		throw new UnsupportedOperationException( "addView(View, int) is not supported in AviaryAdapterView" );
	}

	@Override
	public void addView( View child, LayoutParams params ) {
		throw new UnsupportedOperationException( "addView(View, LayoutParams) " + "is not supported in AviaryAdapterView" );
	}

	@Override
	public void addView( View child, int index, LayoutParams params ) {
		throw new UnsupportedOperationException( "addView(View, int, LayoutParams) " + "is not supported in AviaryAdapterView" );
	}

	@Override
	public void removeView( View child ) {
		throw new UnsupportedOperationException( "removeView(View) is not supported in AviaryAdapterView" );
	}

	@Override
	public void removeViewAt( int index ) {
		throw new UnsupportedOperationException( "removeViewAt(int) is not supported in AviaryAdapterView" );
	}

	@Override
	public void removeAllViews() {
		throw new UnsupportedOperationException( "removeAllViews() is not supported in AviaryAdapterView" );
	}

	@Override
	protected void onLayout( boolean changed, int left, int top, int right, int bottom ) {
		mLayoutHeight = getHeight();
	}

	public int getSelectedItemPosition() {
		return mNextSelectedPosition;
	}

	public long getSelectedItemId() {
		return mNextSelectedRowId;
	}

	public abstract View getSelectedView();

	public Object getSelectedItem() {
		T adapter = getAdapter();
		int selection = getSelectedItemPosition();
		if ( adapter != null && adapter.getCount() > 0 && selection >= 0 ) {
			return adapter.getItem( selection );
		} else {
			return null;
		}
	}

	public int getCount() {
		return mItemCount;
	}

	public int getPositionForView( View view ) {
		View listItem = view;
		try {
			View v;
			while ( !( v = (View) listItem.getParent() ).equals( this ) ) {
				listItem = v;
			}
		} catch ( ClassCastException e ) {
			// We made it up to the window without find this list view
			return INVALID_POSITION;
		}

		// Search the children for the list item
		final int childCount = getChildCount();
		for ( int i = 0; i < childCount; i++ ) {
			if ( getChildAt( i ).equals( listItem ) ) {
				return mFirstPosition + i;
			}
		}

		// Child not found!
		return INVALID_POSITION;
	}

	public int getFirstVisiblePosition() {
		return mFirstPosition;
	}

	public int getLastVisiblePosition() {
		return mFirstPosition + getChildCount() - 1;
	}

	public abstract void setSelection( int position );

	public void setEmptyView( View emptyView ) {
		mEmptyView = emptyView;

		final T adapter = getAdapter();
		final boolean empty = ( ( adapter == null ) || adapter.isEmpty() );
		updateEmptyStatus( empty );
	}

	public View getEmptyView() {
		return mEmptyView;
	}

	boolean isInFilterMode() {
		return false;
	}

	@Override
	public void setFocusable( boolean focusable ) {
		final T adapter = getAdapter();
		final boolean empty = adapter == null || adapter.getCount() == 0;

		mDesiredFocusableState = focusable;
		if ( !focusable ) {
			mDesiredFocusableInTouchModeState = false;
		}

		super.setFocusable( focusable && ( !empty || isInFilterMode() ) );
	}

	@Override
	public void setFocusableInTouchMode( boolean focusable ) {
		final T adapter = getAdapter();
		final boolean empty = adapter == null || adapter.getCount() == 0;

		mDesiredFocusableInTouchModeState = focusable;
		if ( focusable ) {
			mDesiredFocusableState = true;
		}

		super.setFocusableInTouchMode( focusable && ( !empty || isInFilterMode() ) );
	}

	void checkFocus() {
		final T adapter = getAdapter();
		final boolean empty = adapter == null || adapter.getCount() == 0;
		final boolean focusable = !empty || isInFilterMode();
		// The order in which we set focusable in touch mode/focusable may matter
		// for the client, see View.setFocusableInTouchMode() comments for more
		// details
		super.setFocusableInTouchMode( focusable && mDesiredFocusableInTouchModeState );
		super.setFocusable( focusable && mDesiredFocusableState );
		if ( mEmptyView != null ) {
			updateEmptyStatus( ( adapter == null ) || adapter.isEmpty() );
		}
	}

	@SuppressLint ( "WrongCall" )
	private void updateEmptyStatus( boolean empty ) {
		if ( isInFilterMode() ) {
			empty = false;
		}

		if ( empty ) {
			if ( mEmptyView != null ) {
				mEmptyView.setVisibility( View.VISIBLE );
				setVisibility( View.GONE );
			} else {
				// If the caller just removed our empty view, make sure the list view is
				// visible
				setVisibility( View.VISIBLE );
			}

			// We are now GONE, so pending layouts will not be dispatched.
			// Force one here to make sure that the state of the list matches
			// the state of the adapter.
			if ( mDataChanged ) {
				this.onLayout( false, getLeft(), getTop(), getRight(), getBottom() );
			}
		} else {
			if ( mEmptyView != null ) mEmptyView.setVisibility( View.GONE );
			setVisibility( View.VISIBLE );
		}
	}

	public Object getItemAtPosition( int position ) {
		T adapter = getAdapter();
		return ( adapter == null || position < 0 ) ? null : adapter.getItem( position );
	}

	public long getItemIdAtPosition( int position ) {
		T adapter = getAdapter();
		return ( adapter == null || position < 0 ) ? INVALID_ROW_ID : adapter.getItemId( position );
	}

	@Override
	public void setOnClickListener( OnClickListener l ) {
		throw new RuntimeException( "Don't call setOnClickListener for an AviaryAdapterView. " + "You probably want setOnItemClickListener instead" );
	}

	@Override
	protected void dispatchSaveInstanceState( SparseArray<Parcelable> container ) {
		dispatchFreezeSelfOnly( container );
	}

	@Override
	protected void dispatchRestoreInstanceState( SparseArray<Parcelable> container ) {
		dispatchThawSelfOnly( container );
	}

	class AdapterDataSetObserver extends DataSetObserver {

		private Parcelable mInstanceState = null;

		@Override
		public void onChanged() {
			mDataChanged = true;
			mOldItemCount = mItemCount;
			mItemCount = getAdapter().getCount();

			// Detect the case where a cursor that was previously invalidated has
			// been repopulated with new data.
			if ( AviaryAdapterView.this.getAdapter().hasStableIds() && mInstanceState != null && mOldItemCount == 0 && mItemCount > 0 ) {
				AviaryAdapterView.this.onRestoreInstanceState( mInstanceState );
				mInstanceState = null;
			} else {
				rememberSyncState();
			}
			checkFocus();
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			mDataChanged = true;

			if ( AviaryAdapterView.this.getAdapter().hasStableIds() ) {
				// Remember the current state for the case where our hosting activity is
				// being
				// stopped and later restarted
				mInstanceState = AviaryAdapterView.this.onSaveInstanceState();
			}

			// Data is invalid so we should reset our state
			mOldItemCount = mItemCount;
			mItemCount = 0;
			mSelectedPosition = INVALID_POSITION;
			mSelectedRowId = INVALID_ROW_ID;
			mNextSelectedPosition = INVALID_POSITION;
			mNextSelectedRowId = INVALID_ROW_ID;
			mNeedSync = false;

			checkFocus();
			requestLayout();
		}

		public void clearSavedState() {
			mInstanceState = null;
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		removeCallbacks( mSelectionNotifier );
	}

	private class SelectionNotifier implements Runnable {

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

	void selectionChanged( int oldIndex, int newIndex ) {
		Log.i( VIEW_LOG_TAG, "selectionChanged: " + mSelectedPosition );
		if ( mOnItemSelectedListener != null ) {
			if ( mInLayout || mBlockLayoutRequests ) {
				// If we are in a layout traversal, defer notification
				// by posting. This ensures that the view tree is
				// in a consistent state and is able to accomodate
				// new layout or invalidate requests.
				if ( mSelectionNotifier == null ) {
					mSelectionNotifier = new SelectionNotifier();
				}
				post( mSelectionNotifier );
			} else {
				fireOnSelected();
			}
		}

		// we fire selection events here not in View
		if ( mSelectedPosition != ListView.INVALID_POSITION && isShown() && !isInTouchMode() ) {
			sendAccessibilityEvent( AccessibilityEvent.TYPE_VIEW_SELECTED );
		}
	}

	protected void fireOnSelected() {
		if ( mOnItemSelectedListener == null ) return;

		int selection = this.getSelectedItemPosition();
		if ( selection >= 0 ) {
			View v = getSelectedView();
			mOnItemSelectedListener.onItemSelected( this, v, selection, getAdapter().getItemId( selection ) );
		} else {
			mOnItemSelectedListener.onNothingSelected( this );
		}
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent( AccessibilityEvent event ) {
		View selectedView = getSelectedView();
		if ( selectedView != null && selectedView.getVisibility() == VISIBLE && selectedView.dispatchPopulateAccessibilityEvent( event ) ) {
			return true;
		}
		return false;
	}

	@SuppressWarnings ( "unused" )
	private boolean isScrollableForAccessibility() {
		T adapter = getAdapter();
		if ( adapter != null ) {
			final int itemCount = adapter.getCount();
			return itemCount > 0 && ( getFirstVisiblePosition() > 0 || getLastVisiblePosition() < itemCount - 1 );
		}
		return false;
	}

	@Override
	protected boolean canAnimate() {
		return super.canAnimate() && mItemCount > 0;
	}

	void handleDataChanged() {
		final int count = mItemCount;
		boolean found = false;

		if ( count > 0 ) {

			int newPos;

			// Find the row we are supposed to sync to
			if ( mNeedSync ) {
				// Update this first, since setNextSelectedPositionInt inspects
				// it
				mNeedSync = false;

				// See if we can find a position in the new data with the same
				// id as the old selection
				newPos = findSyncPosition();
				if ( newPos >= 0 ) {
					// Verify that new selection is selectable
					int selectablePos = lookForSelectablePosition( newPos, true );
					if ( selectablePos == newPos ) {
						// Same row id is selected
						setNextSelectedPositionInt( newPos );
						found = true;
					}
				}
			}
			if ( !found ) {
				// Try to use the same position if we can't find matching data
				newPos = getSelectedItemPosition();

				// Pin position to the available range
				if ( newPos >= count ) {
					newPos = count - 1;
				}
				if ( newPos < 0 ) {
					newPos = 0;
				}

				// Make sure we select something selectable -- first look down
				int selectablePos = lookForSelectablePosition( newPos, true );
				if ( selectablePos < 0 ) {
					// Looking down didn't work -- try looking up
					selectablePos = lookForSelectablePosition( newPos, false );
				}
				if ( selectablePos >= 0 ) {
					setNextSelectedPositionInt( selectablePos );
					checkSelectionChanged();
					found = true;
				}
			}
		}
		if ( !found ) {
			// Nothing is selected
			mSelectedPosition = INVALID_POSITION;
			mSelectedRowId = INVALID_ROW_ID;
			mNextSelectedPosition = INVALID_POSITION;
			mNextSelectedRowId = INVALID_ROW_ID;
			mNeedSync = false;
			checkSelectionChanged();
		}
	}

	void checkSelectionChanged() {
		Log.i( VIEW_LOG_TAG, "checkSelectionChanged " + ( ( mSelectedPosition != mOldSelectedPosition ) || ( mSelectedRowId != mOldSelectedRowId ) ) );
		if ( ( mSelectedPosition != mOldSelectedPosition ) || ( mSelectedRowId != mOldSelectedRowId ) ) {
			selectionChanged( mOldSelectedPosition, mSelectedPosition );
			mOldSelectedPosition = mSelectedPosition;
			mOldSelectedRowId = mSelectedRowId;
		}
	}

	int findSyncPosition() {
		int count = mItemCount;

		if ( count == 0 ) {
			return INVALID_POSITION;
		}

		long idToMatch = mSyncRowId;
		int seed = mSyncPosition;

		// If there isn't a selection don't hunt for it
		if ( idToMatch == INVALID_ROW_ID ) {
			return INVALID_POSITION;
		}

		// Pin seed to reasonable values
		seed = Math.max( 0, seed );
		seed = Math.min( count - 1, seed );

		long endTime = SystemClock.uptimeMillis() + SYNC_MAX_DURATION_MILLIS;

		long rowId;

		// first position scanned so far
		int first = seed;

		// last position scanned so far
		int last = seed;

		// True if we should move down on the next iteration
		boolean next = false;

		// True when we have looked at the first item in the data
		boolean hitFirst;

		// True when we have looked at the last item in the data
		boolean hitLast;

		// Get the item ID locally (instead of getItemIdAtPosition), so
		// we need the adapter
		T adapter = getAdapter();
		if ( adapter == null ) {
			return INVALID_POSITION;
		}

		while ( SystemClock.uptimeMillis() <= endTime ) {
			rowId = adapter.getItemId( seed );
			if ( rowId == idToMatch ) {
				// Found it!
				return seed;
			}

			hitLast = last == count - 1;
			hitFirst = first == 0;

			if ( hitLast && hitFirst ) {
				// Looked at everything
				break;
			}

			if ( hitFirst || ( next && !hitLast ) ) {
				// Either we hit the top, or we are trying to move down
				last++;
				seed = last;
				// Try going up next time
				next = false;
			} else if ( hitLast || ( !next && !hitFirst ) ) {
				// Either we hit the bottom, or we are trying to move up
				first--;
				seed = first;
				// Try going down next time
				next = true;
			}

		}

		return INVALID_POSITION;
	}

	int lookForSelectablePosition( int position, boolean lookDown ) {
		return position;
	}

	void setSelectedPositionInt( int position ) {
		mSelectedPosition = position;
		mSelectedRowId = getItemIdAtPosition( position );
	}

	void setNextSelectedPositionInt( int position ) {
		Log.i( VIEW_LOG_TAG, "setNextSelectedPositionInt: " + position );

		mNextSelectedPosition = position;
		mNextSelectedRowId = getItemIdAtPosition( position );
		// If we are trying to sync to the selection, update that too
		if ( mNeedSync && mSyncMode == SYNC_SELECTED_POSITION && position >= 0 ) {
			mSyncPosition = position;
			mSyncRowId = mNextSelectedRowId;
		}
	}

	void rememberSyncState() {
		if ( getChildCount() > 0 ) {
			mNeedSync = true;
			mSyncHeight = mLayoutHeight;
			if ( mSelectedPosition >= 0 ) {
				// Sync the selection state
				View v = getChildAt( mSelectedPosition - mFirstPosition );
				mSyncRowId = mNextSelectedRowId;
				mSyncPosition = mNextSelectedPosition;
				if ( v != null ) {
					mSpecificTop = v.getTop();
				}
				mSyncMode = SYNC_SELECTED_POSITION;
			} else {
				// Sync the based on the offset of the first view
				View v = getChildAt( 0 );
				T adapter = getAdapter();
				if ( mFirstPosition >= 0 && mFirstPosition < adapter.getCount() ) {
					mSyncRowId = adapter.getItemId( mFirstPosition );
				} else {
					mSyncRowId = NO_ID;
				}
				mSyncPosition = mFirstPosition;
				if ( v != null ) {
					mSpecificTop = v.getTop();
				}
				mSyncMode = SYNC_FIRST_POSITION;
			}
		}
	}
}
