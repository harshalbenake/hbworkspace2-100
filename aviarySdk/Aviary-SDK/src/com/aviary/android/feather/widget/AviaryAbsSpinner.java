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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

public abstract class AviaryAbsSpinner extends AviaryAdapterView<Adapter> {

	Adapter mAdapter;

	int mHeightMeasureSpec;

	int mWidthMeasureSpec;

	boolean mBlockLayoutRequests;

	int mSelectionLeftPadding = 0;

	int mSelectionTopPadding = 0;

	int mSelectionRightPadding = 0;

	int mSelectionBottomPadding = 0;

	final Rect mSpinnerPadding = new Rect();

	int mPaddingLeft;

	int mPaddingRight;

	int mPaddingTop;

	int mPaddingBottom;

	protected final List<Queue<View>> mRecycleBin;

	private DataSetObserver mDataSetObserver;

	private Rect mTouchFrame;

	public AviaryAbsSpinner ( Context context ) {
		this( context, null );
	}

	public AviaryAbsSpinner ( Context context, AttributeSet attrs ) {
		this( context, attrs, 0 );
	}

	public AviaryAbsSpinner ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
		mRecycleBin = new ArrayList<Queue<View>>( 10 );
		initAbsSpinner();
	}

	@Override
	public void setPadding( int left, int top, int right, int bottom ) {
		super.setPadding( left, top, right, bottom );
		mPaddingLeft = left;
		mPaddingBottom = bottom;
		mPaddingTop = top;
		mPaddingRight = right;
	}

	private void initAbsSpinner() {
		setFocusable( true );
		setWillNotDraw( false );
	}

	int mDefaultPosition;

	/**
	 * Set the default selected position.
	 * This method has effects only if called before setting the new adapter
	 * 
	 * @param position
	 */
	public void setDefaultPosition( int position ) {
		mDefaultPosition = position;
	}

	@Override
	public void setAdapter( Adapter adapter ) {
		if ( null != mAdapter ) {
			mAdapter.unregisterDataSetObserver( mDataSetObserver );
			emptyRecycler();
			resetList();
		}

		mAdapter = adapter;

		mOldSelectedPosition = INVALID_POSITION;
		mOldSelectedRowId = INVALID_ROW_ID;

		if ( mAdapter != null ) {
			mOldItemCount = mItemCount;
			mItemCount = mAdapter.getCount();
			checkFocus();

			mDataSetObserver = new AdapterDataSetObserver();
			mAdapter.registerDataSetObserver( mDataSetObserver );

			int position;
			if ( mDefaultPosition >= 0 && mDefaultPosition < mItemCount ) {
				position = mDefaultPosition;
			} else {
				position = mItemCount > 0 ? 0 : INVALID_POSITION;
			}

			int total = mAdapter.getViewTypeCount();
			for ( int i = 0; i < total; i++ ) {
				mRecycleBin.add( new LinkedList<View>() );
			}

			setSelectedPositionInt( position );
			setNextSelectedPositionInt( position );

			if ( mItemCount == 0 ) {
				// Nothing selected
				checkSelectionChanged();
			}

		} else {
			checkFocus();
			resetList();
			// Nothing selected
			checkSelectionChanged();
		}

		requestLayout();
	}

	private void emptyRecycler() {
		emptySubRecycler();
		if ( null != mRecycleBin ) {
			mRecycleBin.clear();
		}
	}

	protected void emptySubRecycler() {
		if ( null != mRecycleBin ) {
			for ( int i = 0; i < mRecycleBin.size(); i++ ) {
				mRecycleBin.get( i ).clear();
			}
		}
	}

	void resetList() {
		Log.i( VIEW_LOG_TAG, "emptyList" );
		mDataChanged = false;
		mNeedSync = false;

		removeAllViewsInLayout();
		emptyRecycler();

		mOldSelectedPosition = INVALID_POSITION;
		mOldSelectedRowId = INVALID_ROW_ID;

		setSelectedPositionInt( INVALID_POSITION );
		setNextSelectedPositionInt( INVALID_POSITION );
		invalidate();
	}

	@Override
	protected void onMeasure( int widthMeasureSpec, int heightMeasureSpec ) {
		int widthMode = MeasureSpec.getMode( widthMeasureSpec );
		int widthSize;
		int heightSize;

		mSpinnerPadding.left = mPaddingLeft > mSelectionLeftPadding ? mPaddingLeft : mSelectionLeftPadding;
		mSpinnerPadding.top = mPaddingTop > mSelectionTopPadding ? mPaddingTop : mSelectionTopPadding;
		mSpinnerPadding.right = mPaddingRight > mSelectionRightPadding ? mPaddingRight : mSelectionRightPadding;
		mSpinnerPadding.bottom = mPaddingBottom > mSelectionBottomPadding ? mPaddingBottom : mSelectionBottomPadding;

		if ( mDataChanged ) {
			handleDataChanged();
		}

		int preferredHeight = 0;
		int preferredWidth = 0;
		boolean needsMeasuring = true;

		int selectedPosition = getSelectedItemPosition();
		if ( selectedPosition >= 0 && mAdapter != null && selectedPosition < mAdapter.getCount() ) {
			// Try looking in the recycler. (Maybe we were measured once already)

			int viewType = mAdapter.getItemViewType( selectedPosition );
			View view = mRecycleBin.get( viewType ).poll();
			if ( view == null ) {
				// Make a new one
				view = mAdapter.getView( selectedPosition, null, this );
			}

			if ( view != null ) {
				// Put in recycler for re-measuring and/or layout
				mRecycleBin.get( viewType ).offer( view );
			}

			if ( view != null ) {
				if ( view.getLayoutParams() == null ) {
					mBlockLayoutRequests = true;
					view.setLayoutParams( generateDefaultLayoutParams() );
					mBlockLayoutRequests = false;
				}
				measureChild( view, widthMeasureSpec, heightMeasureSpec );

				preferredHeight = getChildHeight( view ) + mSpinnerPadding.top + mSpinnerPadding.bottom;
				preferredWidth = getChildWidth( view ) + mSpinnerPadding.left + mSpinnerPadding.right;

				needsMeasuring = false;
			}
		}

		if ( needsMeasuring ) {
			// No views -- just use padding
			preferredHeight = mSpinnerPadding.top + mSpinnerPadding.bottom;
			if ( widthMode == MeasureSpec.UNSPECIFIED ) {
				preferredWidth = mSpinnerPadding.left + mSpinnerPadding.right;
			}
		}

		preferredHeight = Math.max( preferredHeight, getSuggestedMinimumHeight() );
		preferredWidth = Math.max( preferredWidth, getSuggestedMinimumWidth() );

		heightSize = resolveSize( preferredHeight, heightMeasureSpec );
		widthSize = resolveSize( preferredWidth, widthMeasureSpec );

		setMeasuredDimension( widthSize, heightSize );
		mHeightMeasureSpec = heightMeasureSpec;
		mWidthMeasureSpec = widthMeasureSpec;
	}

	int getChildHeight( View child ) {
		return child.getMeasuredHeight();
	}

	int getChildWidth( View child ) {
		return child.getMeasuredWidth();
	}

	@Override
	protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
		return new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
	}

	void recycleAllViews() {

		final int childCount = getChildCount();
		final int position = mFirstPosition;

		// All views go in recycler
		for ( int i = 0; i < childCount; i++ ) {
			View v = getChildAt( i );
			int index = position + i;
			int viewType = mAdapter.getItemViewType( index );
			mRecycleBin.get( viewType ).offer( v );

			// if ( position + i < 0 ) {
			// recycleBin2.put( index, v );
			// } else {
			// recycleBin.put( index, v );
			// }
		}
		// recycleBin2.clear();
	}

	public void setSelection( int position, boolean animate, boolean changed ) {
		// Animate only if requested position is already on screen somewhere
		boolean shouldAnimate = animate && mFirstPosition <= position && position <= mFirstPosition + getChildCount() - 1;
		setSelectionInt( position, shouldAnimate, changed );
	}

	@Override
	public void setSelection( int position ) {
		if ( mItemCount > 0 && ( position >= 0 && position < mItemCount ) ) {
			setNextSelectedPositionInt( position );
			requestLayout();
			postInvalidate();
		}
	}

	void setSelectionInt( int position, boolean animate, boolean changed ) {
		if ( position != mOldSelectedPosition ) {
			mBlockLayoutRequests = true;
			int delta = position - mSelectedPosition;
			setNextSelectedPositionInt( position );
			layout( delta, animate, changed );
			mBlockLayoutRequests = false;
		}
	}

	abstract void layout( int delta, boolean animate, boolean changed );

	@Override
	public View getSelectedView() {
		if ( mItemCount > 0 && mSelectedPosition >= 0 ) {
			return getChildAt( mSelectedPosition - mFirstPosition );
		} else {
			return null;
		}
	}

	@Override
	public void requestLayout() {
		if ( !mBlockLayoutRequests ) {
			super.requestLayout();
		}
	}

	@Override
	public Adapter getAdapter() {
		return mAdapter;
	}

	@Override
	public int getCount() {
		return mItemCount;
	}

	public int pointToPosition( int x, int y ) {
		Rect frame = mTouchFrame;
		if ( frame == null ) {
			mTouchFrame = new Rect();
			frame = mTouchFrame;
		}

		final int count = getChildCount();
		for ( int i = count - 1; i >= 0; i-- ) {
			View child = getChildAt( i );
			if ( child.getVisibility() == View.VISIBLE ) {
				child.getHitRect( frame );
				if ( frame.contains( x, y ) ) {
					return mFirstPosition + i;
				}
			}
		}
		return INVALID_POSITION;
	}

	static class SavedState extends BaseSavedState {

		long selectedId;
		int position;

		SavedState ( Parcelable superState ) {
			super( superState );
		}

		private SavedState ( Parcel in ) {
			super( in );
			selectedId = in.readLong();
			position = in.readInt();
		}

		@Override
		public void writeToParcel( Parcel out, int flags ) {
			super.writeToParcel( out, flags );
			out.writeLong( selectedId );
			out.writeInt( position );
		}

		@Override
		public String toString() {
			return "AviaryAbsSpinner.SavedState{" + Integer.toHexString( System.identityHashCode( this ) ) + " selectedId=" + selectedId + " position="
					+ position + "}";
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

	@Override
	public Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState( superState );
		ss.selectedId = getSelectedItemId();
		if ( ss.selectedId >= 0 ) {
			ss.position = getSelectedItemPosition();
		} else {
			ss.position = INVALID_POSITION;
		}
		return ss;
	}

	@Override
	public void onRestoreInstanceState( Parcelable state ) {
		SavedState ss = (SavedState) state;

		super.onRestoreInstanceState( ss.getSuperState() );

		if ( ss.selectedId >= 0 ) {
			mDataChanged = true;
			mNeedSync = true;
			mSyncRowId = ss.selectedId;
			mSyncPosition = ss.position;
			mSyncMode = SYNC_SELECTED_POSITION;
			requestLayout();
		}
	}
}
