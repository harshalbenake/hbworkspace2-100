package com.aviary.android.feather.widget;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.aviary.android.feather.R;
import com.aviary.android.feather.library.graphics.drawable.EditableDrawable;
import com.aviary.android.feather.library.graphics.drawable.FeatherDrawable;
import com.aviary.android.feather.library.services.DragControllerService.DragSource;
import com.aviary.android.feather.library.services.drag.DragView;
import com.aviary.android.feather.library.services.drag.DropTarget;

public class ImageViewDrawableOverlay extends ImageViewTouch implements DropTarget {

	public static interface OnDrawableEventListener {
		void onFocusChange( DrawableHighlightView newFocus, DrawableHighlightView oldFocus );

		void onDown( DrawableHighlightView view );

		void onMove( DrawableHighlightView view );

		void onClick( DrawableHighlightView view );
	};

	private List<DrawableHighlightView> mOverlayViews = new ArrayList<DrawableHighlightView>();

	private DrawableHighlightView mOverlayView;

	private OnDrawableEventListener mDrawableListener;

	private boolean mForceSingleSelection = true;

	private DropTargetListener mDropTargetListener;

	private Paint mDropPaint;

	private Rect mTempRect = new Rect();

	private boolean mScaleWithContent = false;

	private int mOverlayStyleId;

	public ImageViewDrawableOverlay ( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public ImageViewDrawableOverlay ( Context context, AttributeSet attrs, int defStyle ) {
		super( context, attrs, defStyle );
	}

	@Override
	protected void init( Context context, AttributeSet attrs, int defStyle ) {
		super.init( context, attrs, defStyle );
		mTouchSlop = ViewConfiguration.get( context ).getScaledDoubleTapSlop();
		mGestureDetector.setIsLongpressEnabled( false );

		Theme theme = context.getTheme();

		TypedArray array = theme.obtainStyledAttributes( attrs, R.styleable.AviaryImageViewDrawableOverlay, defStyle, 0 );
		mOverlayStyleId = array.getResourceId( R.styleable.AviaryImageViewDrawableOverlay_aviary_highlightStyle, -1 );

		array.recycle();
	}

	/**
	 * Return the overlay default style
	 * 
	 * @return
	 */
	public int getOverlayStyleId() {
		return mOverlayStyleId;
	}

	/**
	 * How overlay content will be scaled/moved
	 * when zomming/panning the base image
	 * 
	 * @param value
	 *            true if content will scale according to the image
	 */
	public void setScaleWithContent( boolean value ) {
		mScaleWithContent = value;
	}

	public boolean getScaleWithContent() {
		return mScaleWithContent;
	}

	/**
	 * If true, when the user tap outside the drawable overlay and
	 * there is only one active overlay selection is not changed.
	 * 
	 * @param value
	 *            the new force single selection
	 */
	public void setForceSingleSelection( boolean value ) {
		mForceSingleSelection = value;
	}

	public void setDropTargetListener( DropTargetListener listener ) {
		mDropTargetListener = listener;
	}

	public void setOnDrawableEventListener( OnDrawableEventListener listener ) {
		mDrawableListener = listener;
	}

	@Override
	public void setImageDrawable( android.graphics.drawable.Drawable drawable, Matrix initial_matrix, float min_zoom, float max_zoom ) {
		super.setImageDrawable( drawable, initial_matrix, min_zoom, max_zoom );
	}

	@Override
	protected void onLayoutChanged( int left, int top, int right, int bottom ) {
		super.onLayoutChanged( left, top, right, bottom );

		if ( getDrawable() != null ) {

			Iterator<DrawableHighlightView> iterator = mOverlayViews.iterator();
			while ( iterator.hasNext() ) {
				DrawableHighlightView view = iterator.next();
				view.getMatrix().set( getImageMatrix() );
				view.invalidate();
			}
		}
	}

	@Override
	protected void postTranslate( float deltaX, float deltaY ) {
		super.postTranslate( deltaX, deltaY );

		Iterator<DrawableHighlightView> iterator = mOverlayViews.iterator();
		while ( iterator.hasNext() ) {
			DrawableHighlightView view = iterator.next();
			if ( getScale() != 1 ) {
				float[] mvalues = new float[9];
				getImageMatrix().getValues( mvalues );
				final float scale = mvalues[Matrix.MSCALE_X];

				if ( !mScaleWithContent ) view.getCropRectF().offset( -deltaX / scale, -deltaY / scale );
			}

			view.getMatrix().set( getImageMatrix() );
			view.invalidate();
		}
	}

	@Override
	protected void postScale( float scale, float centerX, float centerY ) {

		if ( mOverlayViews.size() > 0 ) {
			Iterator<DrawableHighlightView> iterator = mOverlayViews.iterator();

			Matrix oldMatrix = new Matrix( getImageViewMatrix() );
			super.postScale( scale, centerX, centerY );

			while ( iterator.hasNext() ) {
				DrawableHighlightView view = iterator.next();

				if ( !mScaleWithContent ) {
					RectF cropRect = view.getCropRectF();
					RectF rect1 = view.getDisplayRect( oldMatrix, view.getCropRectF() );
					RectF rect2 = view.getDisplayRect( getImageViewMatrix(), view.getCropRectF() );

					float[] mvalues = new float[9];
					getImageViewMatrix().getValues( mvalues );
					final float currentScale = mvalues[Matrix.MSCALE_X];

					cropRect.offset( ( rect1.left - rect2.left ) / currentScale, ( rect1.top - rect2.top ) / currentScale );
					cropRect.right += -( rect2.width() - rect1.width() ) / currentScale;
					cropRect.bottom += -( rect2.height() - rect1.height() ) / currentScale;

					view.getMatrix().set( getImageMatrix() );
					view.getCropRectF().set( cropRect );
				} else {
					view.getMatrix().set( getImageMatrix() );
				}
				view.invalidate();
			}
		} else {
			super.postScale( scale, centerX, centerY );
		}
	}

	private void ensureVisible( DrawableHighlightView hv, float deltaX, float deltaY ) {
		RectF r = hv.getDrawRect();
		int panDeltaX1 = 0, panDeltaX2 = 0;
		int panDeltaY1 = 0, panDeltaY2 = 0;

		if ( deltaX > 0 ) panDeltaX1 = (int) Math.max( 0, getLeft() - r.left );
		if ( deltaX < 0 ) panDeltaX2 = (int) Math.min( 0, getRight() - r.right );

		if ( deltaY > 0 ) panDeltaY1 = (int) Math.max( 0, getTop() - r.top );

		if ( deltaY < 0 ) panDeltaY2 = (int) Math.min( 0, getBottom() - r.bottom );

		int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
		int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

		if ( panDeltaX != 0 || panDeltaY != 0 ) {
			panBy( panDeltaX, panDeltaY );
		}
	}

	@Override
	public boolean onSingleTapConfirmed( MotionEvent e ) {

		// iterate the items and post a single tap event to the selected item
		Iterator<DrawableHighlightView> iterator = mOverlayViews.iterator();
		while ( iterator.hasNext() ) {
			DrawableHighlightView view = iterator.next();
			if ( view.isSelected() ) {
				view.onSingleTapConfirmed( e.getX(), e.getY() );
				postInvalidate();
			}
		}

		return super.onSingleTapConfirmed( e );
	}

	@Override
	public boolean onDown( MotionEvent e ) {
		Log.i( LOG_TAG, "onDown" );

		mScrollStarted = false;
		mLastMotionScrollX = e.getX();
		mLastMotionScrollY = e.getY();

		// return the item being clicked
		DrawableHighlightView newSelection = checkSelection( e );
		DrawableHighlightView realNewSelection = newSelection;

		if ( newSelection == null && mOverlayViews.size() == 1 && mForceSingleSelection ) {
			// force a selection if none is selected, when force single selection is
			// turned on
			newSelection = mOverlayViews.get( 0 );
		}

		setSelectedHighlightView( newSelection );

		if ( realNewSelection != null && mScaleWithContent ) {
			RectF displayRect = realNewSelection.getDisplayRect( realNewSelection.getMatrix(), realNewSelection.getCropRectF() );
			boolean invalidSize = realNewSelection.getContent().validateSize( displayRect );

			Log.d( LOG_TAG, "invalidSize: " + invalidSize );

			if ( !invalidSize ) {
				Log.w( LOG_TAG, "drawable too small!!!" );

				float minW = realNewSelection.getContent().getMinWidth();
				float minH = realNewSelection.getContent().getMinHeight();

				Log.d( LOG_TAG, "minW: " + minW );
				Log.d( LOG_TAG, "minH: " + minH );

				float minSize = Math.min( minW, minH ) * 1.1f;

				Log.d( LOG_TAG, "minSize: " + minSize );

				float minRectSize = Math.min( displayRect.width(), displayRect.height() );

				Log.d( LOG_TAG, "minRectSize: " + minRectSize );

				float diff = minSize / minRectSize;

				Log.d( LOG_TAG, "diff: " + diff );

				Log.d( LOG_TAG, "min.size: " + minW + "x" + minH );
				Log.d( LOG_TAG, "cur.size: " + displayRect.width() + "x" + displayRect.height() );
				Log.d( LOG_TAG, "zooming to: " + ( getScale() * diff ) );

				zoomTo( getScale() * diff, displayRect.centerX(), displayRect.centerY(), DEFAULT_ANIMATION_DURATION * 1.5f );
				return true;
			}
		}

		if ( mOverlayView != null ) {
			int edge = mOverlayView.getHit( e.getX(), e.getY() );
			if ( edge != DrawableHighlightView.NONE ) {
				mOverlayView.setMode( ( edge == DrawableHighlightView.MOVE ) ? DrawableHighlightView.MOVE
						: ( edge == DrawableHighlightView.ROTATE ? DrawableHighlightView.ROTATE : DrawableHighlightView.GROW ) );
				postInvalidate();
				if ( mDrawableListener != null ) {
					mDrawableListener.onDown( mOverlayView );
				}
			}
		}

		return super.onDown( e );
	}

	@Override
	public boolean onUp( MotionEvent e ) {
		Log.i( LOG_TAG, "onUp" );

		if ( mOverlayView != null ) {
			mOverlayView.setMode( DrawableHighlightView.NONE );
			postInvalidate();
		}
		return super.onUp( e );
	}

	@Override
	public boolean onSingleTapUp( MotionEvent e ) {
		Log.i( LOG_TAG, "onSingleTapUp" );

		if ( mOverlayView != null ) {

			int edge = mOverlayView.getHit( e.getX(), e.getY() );
			if ( ( edge & DrawableHighlightView.MOVE ) == DrawableHighlightView.MOVE ) {
				if ( mDrawableListener != null ) {
					mDrawableListener.onClick( mOverlayView );
				}
				return true;
			}

			mOverlayView.setMode( DrawableHighlightView.NONE );
			postInvalidate();

			Log.d( LOG_TAG, "selected items: " + mOverlayViews.size() );

			if ( mOverlayViews.size() != 1 ) {
				setSelectedHighlightView( null );
			}
		}

		return super.onSingleTapUp( e );
	}

	boolean mScrollStarted;
	float mLastMotionScrollX, mLastMotionScrollY;

	@Override
	public boolean onScroll( MotionEvent e1, MotionEvent e2, float distanceX, float distanceY ) {
		Log.i( LOG_TAG, "onScroll" );

		float dx, dy;

		float x = e2.getX();
		float y = e2.getY();

		if ( !mScrollStarted ) {
			dx = 0;
			dy = 0;
			mScrollStarted = true;
		} else {
			dx = mLastMotionScrollX - x;
			dy = mLastMotionScrollY - y;
		}

		mLastMotionScrollX = x;
		mLastMotionScrollY = y;

		if ( mOverlayView != null && mOverlayView.getMode() != DrawableHighlightView.NONE ) {
			mOverlayView.onMouseMove( mOverlayView.getMode(), e2, -dx, -dy );
			postInvalidate();

			if ( mDrawableListener != null ) {
				mDrawableListener.onMove( mOverlayView );
			}

			if ( mOverlayView.getMode() == DrawableHighlightView.MOVE ) {
				if ( !mScaleWithContent ) {
					ensureVisible( mOverlayView, distanceX, distanceY );
				}
			}
			return true;
		} else {
			return super.onScroll( e1, e2, distanceX, distanceY );
		}
	}

	@Override
	public boolean onFling( MotionEvent e1, MotionEvent e2, float velocityX, float velocityY ) {
		Log.i( LOG_TAG, "onFling" );

		if ( mOverlayView != null && mOverlayView.getMode() != DrawableHighlightView.NONE ) return false;
		return super.onFling( e1, e2, velocityX, velocityY );
	}

	@Override
	public void onDraw( Canvas canvas ) {
		super.onDraw( canvas );

		boolean shouldInvalidateAfter = false;

		for ( int i = 0; i < mOverlayViews.size(); i++ ) {
			canvas.save( Canvas.MATRIX_SAVE_FLAG );

			DrawableHighlightView current = mOverlayViews.get( i );
			current.draw( canvas );

			// check if we should invalidate again the canvas
			if ( !shouldInvalidateAfter ) {
				FeatherDrawable content = current.getContent();
				if ( content instanceof EditableDrawable ) {
					if ( ( (EditableDrawable) content ).isEditing() ) {
						shouldInvalidateAfter = true;
					}
				}
			}

			canvas.restore();
		}

		if ( null != mDropPaint ) {
			getDrawingRect( mTempRect );
			canvas.drawRect( mTempRect, mDropPaint );
		}

		if ( shouldInvalidateAfter ) {
			postInvalidateDelayed( EditableDrawable.CURSOR_BLINK_TIME );
		}
	}

	public void clearOverlays() {
		Log.i( LOG_TAG, "clearOverlays" );
		setSelectedHighlightView( null );
		while ( mOverlayViews.size() > 0 ) {
			DrawableHighlightView hv = mOverlayViews.remove( 0 );
			hv.dispose();
		}
		mOverlayView = null;
	}

	public boolean addHighlightView( DrawableHighlightView hv ) {
		for ( int i = 0; i < mOverlayViews.size(); i++ ) {
			if ( mOverlayViews.get( i ).equals( hv ) ) return false;
		}
		mOverlayViews.add( hv );
		postInvalidate();

		if ( mOverlayViews.size() == 1 ) {
			setSelectedHighlightView( hv );
		}

		return true;
	}

	public int getHighlightCount() {
		return mOverlayViews.size();
	}

	public DrawableHighlightView getHighlightViewAt( int index ) {
		return mOverlayViews.get( index );
	}

	public boolean removeHightlightView( DrawableHighlightView view ) {
		Log.i( LOG_TAG, "removeHightlightView" );
		for ( int i = 0; i < mOverlayViews.size(); i++ ) {
			if ( mOverlayViews.get( i ).equals( view ) ) {
				DrawableHighlightView hv = mOverlayViews.remove( i );
				if ( hv.equals( mOverlayView ) ) {
					setSelectedHighlightView( null );
				}
				hv.dispose();
				return true;
			}
		}
		return false;
	}

	@Override
	protected void onZoomAnimationCompleted( float scale ) {
		Log.i( LOG_TAG, "onZoomAnimationCompleted: " + scale );
		super.onZoomAnimationCompleted( scale );

		if ( mOverlayView != null ) {
			mOverlayView.setMode( DrawableHighlightView.MOVE );
			postInvalidate();
		}
	}

	public DrawableHighlightView getSelectedHighlightView() {
		return mOverlayView;
	}

	public void commit( Canvas canvas ) {

		DrawableHighlightView hv;
		for ( int i = 0; i < getHighlightCount(); i++ ) {
			hv = getHighlightViewAt( i );
			FeatherDrawable content = hv.getContent();
			if ( content instanceof EditableDrawable ) {
				( (EditableDrawable) content ).endEdit();
			}

			Matrix rotateMatrix = hv.getCropRotationMatrix();
			Rect rect = hv.getCropRect();

			int saveCount = canvas.save( Canvas.MATRIX_SAVE_FLAG );
			canvas.concat( rotateMatrix );
			content.setBounds( rect );
			content.draw( canvas );
			canvas.restoreToCount( saveCount );
		}
	}

	private DrawableHighlightView checkSelection( MotionEvent e ) {
		Iterator<DrawableHighlightView> iterator = mOverlayViews.iterator();
		DrawableHighlightView selection = null;
		while ( iterator.hasNext() ) {
			DrawableHighlightView view = iterator.next();
			int edge = view.getHit( e.getX(), e.getY() );
			if ( edge != DrawableHighlightView.NONE ) {
				selection = view;
			}
		}
		return selection;
	}

	public void setSelectedHighlightView( DrawableHighlightView newView ) {

		final DrawableHighlightView oldView = mOverlayView;

		if ( mOverlayView != null && !mOverlayView.equals( newView ) ) {
			mOverlayView.setSelected( false );
		}

		if ( newView != null ) {
			newView.setSelected( true );
		}

		postInvalidate();

		mOverlayView = newView;

		if ( mDrawableListener != null ) {
			mDrawableListener.onFocusChange( newView, oldView );
		}
	}

	@Override
	public void onDrop( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo ) {
		if ( mDropTargetListener != null ) {
			mDropTargetListener.onDrop( source, x, y, xOffset, yOffset, dragView, dragInfo );
		}
	}

	@Override
	public void onDragEnter( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo ) {
		mDropPaint = new Paint();
		mDropPaint.setColor( 0xff33b5e5 );
		mDropPaint.setStrokeWidth( 2 );
		mDropPaint.setMaskFilter( new BlurMaskFilter( 4.0f, Blur.NORMAL ) );
		mDropPaint.setStyle( Paint.Style.STROKE );
		invalidate();
	}

	@Override
	public void onDragOver( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo ) {}

	@Override
	public void onDragExit( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo ) {
		mDropPaint = null;
		invalidate();
	}

	@Override
	public boolean acceptDrop( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo ) {
		if ( mDropTargetListener != null ) {
			return mDropTargetListener.acceptDrop( source, x, y, xOffset, yOffset, dragView, dragInfo );
		}
		return false;
	}

	@Override
	public Rect estimateDropLocation( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo, Rect recycle ) {
		return null;
	}
}
