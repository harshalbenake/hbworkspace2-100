package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aviary.android.feather.R;
import com.aviary.android.feather.graphics.PreviewFillColorDrawable;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.filters.TextFilter;
import com.aviary.android.feather.library.graphics.drawable.AviaryTextDrawable;
import com.aviary.android.feather.library.graphics.drawable.EditableDrawable;
import com.aviary.android.feather.library.graphics.drawable.FeatherDrawable;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.MatrixUtils;
import com.aviary.android.feather.library.utils.UIConfiguration;
import com.aviary.android.feather.widget.AviaryAdapterView;
import com.aviary.android.feather.widget.AviaryGallery;
import com.aviary.android.feather.widget.AviaryGallery.OnItemsScrollListener;
import com.aviary.android.feather.widget.DrawableHighlightView;
import com.aviary.android.feather.widget.ImageViewDrawableOverlay;
import com.aviary.android.feather.widget.ImageViewDrawableOverlay.OnDrawableEventListener;

public class TextPanel extends AbstractContentPanel implements OnDrawableEventListener, OnEditorActionListener, OnItemsScrollListener, OnKeyListener {

	abstract class MyTextWatcher implements TextWatcher {
		public DrawableHighlightView view;
	}

	// array of available colors
	private int[] mColors;
	// current selected color
	private int mColor = 0;

	private AviaryGallery mGallery;
	private int mSelectedPosition;

	// default text size in px
	private int mDefaultTextSize;

	// canvas used to draw the text into
	private Canvas mCanvas;

	private EditText mEditText;

	private ConfigService config;

	String mColorContentDescription;

	private final MyTextWatcher mEditTextWatcher = new MyTextWatcher() {

		@Override
		public void afterTextChanged( final Editable s ) {
			mLogger.info( "afterTextChanged" );
		}

		@Override
		public void beforeTextChanged( final CharSequence s, final int start, final int count, final int after ) {
			mLogger.info( "beforeTextChanged" );
		}

		@Override
		public void onTextChanged( final CharSequence s, final int start, final int before, final int count ) {
			if ( ( view != null ) && ( view.getContent() instanceof EditableDrawable ) ) {
				final EditableDrawable editable = (EditableDrawable) view.getContent();

				if ( !editable.isEditing() ) return;

				editable.setText( s.toString() );

				// if( view.forceUpdate() ){
				// mImageView.invalidate( view.getInvalidationRect() );
				// }
			}
		}
	};

	public TextPanel ( final IAviaryController context, ToolEntry entry ) {
		super( context, entry );
	}

	private void beginEdit( final DrawableHighlightView view ) {

		if ( null != view ) {
			view.setFocused( true );
			mImageView.postInvalidate();
		}

		mEditTextWatcher.view = null;
		mEditText.removeTextChangedListener( mEditTextWatcher );

		final EditableDrawable editable = (EditableDrawable) view.getContent();
		final String oldText = editable.isTextHint() ? "" : (String) editable.getText();
		mEditText.setText( oldText );
		mEditText.setSelection( mEditText.length() );
		mEditText.requestFocusFromTouch();

		InputMethodManager manager = (InputMethodManager) getContext().getBaseContext().getSystemService( Context.INPUT_METHOD_SERVICE );
		if ( null != manager ) {
			// manager.toggleSoftInput( InputMethodManager.SHOW_FORCED, 0 );
			manager.showSoftInput( mEditText, 0 );
		}

		mEditTextWatcher.view = view;
		mEditText.setOnEditorActionListener( this );
		mEditText.addTextChangedListener( mEditTextWatcher );
	}

	private void createAndConfigurePreview() {

		if ( ( mPreview != null ) && !mPreview.isRecycled() ) {
			mPreview.recycle();
			mPreview = null;
		}

		mPreview = BitmapUtils.copy( mBitmap, mBitmap.getConfig() );
		mCanvas = new Canvas( mPreview );
	}

	private void endEdit( final DrawableHighlightView view ) {
		if ( null != view ) {
			view.setFocused( false );
			if ( view.forceUpdate() ) {
				mImageView.invalidate( view.getInvalidationRect() );
			} else {
				mImageView.postInvalidate();
			}
		}
		mEditTextWatcher.view = null;
		mEditText.removeTextChangedListener( mEditTextWatcher );

		InputMethodManager manager = (InputMethodManager) getContext().getBaseContext().getSystemService( Context.INPUT_METHOD_SERVICE );
		if ( null != manager ) {
			if ( manager.isActive( mEditText ) ) {
				manager.hideSoftInputFromWindow( mEditText.getWindowToken(), 0 );
			}
		}
	}

	@Override
	protected View generateContentView( final LayoutInflater inflater ) {
		return inflater.inflate( R.layout.aviary_content_text, null );
	}

	@Override
	protected ViewGroup generateOptionView( final LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_crop, parent, false );
	}

	@Override
	public boolean getIsChanged() {
		return super.getIsChanged() || ( ( (ImageViewDrawableOverlay) mImageView ).getHighlightCount() > 0 );
	}

	private void onAddNewText() {
		final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;

		if ( image.getHighlightCount() > 0 ) onApplyCurrent( image.getHighlightViewAt( 0 ) );

		final AviaryTextDrawable text = new AviaryTextDrawable( "", mDefaultTextSize );
		text.setTextColor( mColor );
		text.setStrokeEnabled( config.getBoolean( R.integer.aviary_text_stroke_enabled ) );
		text.setCursorWidth( 2 );
		text.setMinTextSize( 14 );
		text.setTextHint( mEditText.getHint() );

		final DrawableHighlightView hv = new DrawableHighlightView( mImageView, image.getOverlayStyleId(), text );

		final Matrix mImageMatrix = mImageView.getImageViewMatrix();

		final int width = mImageView.getWidth();
		final int height = mImageView.getHeight();
		final int imageSize = Math.max( width, height );

		// width/height of the sticker
		int cropWidth = text.getIntrinsicWidth();
		int cropHeight = text.getIntrinsicHeight();

		final int cropSize = Math.max( cropWidth, cropHeight );

		if ( cropSize > imageSize ) {
			cropWidth = width / 2;
			cropHeight = height / 2;
		}

		final int x = ( width - cropWidth ) / 2;
		final int y = ( height - cropHeight ) / 2;

		final Matrix matrix = new Matrix( mImageMatrix );
		matrix.invert( matrix );

		final float[] pts = new float[] { x, y, x + cropWidth, y + cropHeight };
		MatrixUtils.mapPoints( matrix, pts );

		final RectF cropRect = new RectF( pts[0], pts[1], pts[2], pts[3] );
		final Rect imageRect = new Rect( 0, 0, width, height );

		hv.setup( getContext().getBaseContext(), mImageMatrix, imageRect, cropRect, false );

		image.addHighlightView( hv );

		// image.setSelectedHighlightView( hv );

		onClick( hv );
	}

	private MoaActionList onApplyCurrent() {
		final MoaActionList nullActions = MoaActionFactory.actionList();
		final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;
		if ( image.getHighlightCount() < 1 ) return nullActions;
		final DrawableHighlightView hv = ( (ImageViewDrawableOverlay) mImageView ).getHighlightViewAt( 0 );

		if ( hv != null ) {

			if ( hv.getContent() instanceof EditableDrawable ) {
				EditableDrawable editable = (EditableDrawable) hv.getContent();
				if ( editable != null ) {
					if ( editable.isTextHint() ) {
						setIsChanged( false );
						return nullActions;
					}
				}
			}
			return onApplyCurrent( hv );
		}
		return nullActions;
	}

	private MoaActionList onApplyCurrent( final DrawableHighlightView hv ) {

		MoaActionList actionList;

		if ( hv != null ) {
			setIsChanged( true );

			final RectF cropRect = hv.getCropRectF();
			final float scale = MatrixUtils.getScale( mImageView.getImageMatrix() )[0];

			mLogger.log( "cropRect: " + cropRect );

			final int w = mBitmap.getWidth();
			final int h = mBitmap.getHeight();

			final float left = cropRect.left;
			final float top = cropRect.top;
			final float right = cropRect.right;
			final float bottom = cropRect.bottom;

			// stop editing
			EditableDrawable editable = (EditableDrawable) hv.getContent();
			editable.endEdit();

			mImageView.invalidate();

			// generate the text filter
			TextFilter filter = (TextFilter) FilterLoaderFactory.get( Filters.TEXT );
			filter.setPreviewSize( w, h );
			filter.setText( editable.getText() );
			filter.setFillColor( mColor );
			filter.setStrokeColor( editable.getStrokeEnabled() ? editable.getTextStrokeColor() : 0 );
			filter.setRotation( hv.getRotation() );
			filter.setTopLeft( top, left );
			filter.setBottomRight( bottom, right );
			filter.setTextSize( editable.getTextSize() / scale );

			actionList = (MoaActionList) filter.getActions().clone();

			// now save the preview bitmap
			final Rect rect = new Rect( (int) cropRect.left, (int) cropRect.top, (int) cropRect.right, (int) cropRect.bottom );
			final int saveCount = mCanvas.save( Canvas.MATRIX_SAVE_FLAG );
			final Matrix rotateMatrix = hv.getCropRotationMatrix();
			mCanvas.concat( rotateMatrix );
			hv.getContent().setBounds( rect );
			hv.getContent().draw( mCanvas );
			mCanvas.restoreToCount( saveCount );

			mImageView.invalidate();
			onClearCurrent( hv );

		} else {
			actionList = MoaActionFactory.actionList();
		}

		onPreviewChanged( mPreview, false, false );
		return actionList;
	}

	private void onClearCurrent( final DrawableHighlightView hv ) {
		hv.setOnDeleteClickListener( null );
		( (ImageViewDrawableOverlay) mImageView ).removeHightlightView( hv );
	}

	@Override
	public void onClick( final DrawableHighlightView view ) {
		if ( view != null ) if ( view.getContent() instanceof EditableDrawable ) {

			if ( !view.isFocused() ) {
				beginEdit( view );
			}
		}
	}

	@Override
	public void onCreate( final Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		config = getContext().getService( ConfigService.class );

		mColors = config.getIntArray( R.array.aviary_text_fill_colors );
		mSelectedPosition = config.getInteger( R.integer.aviary_text_fill_color_index );

		mColor = mColors[mSelectedPosition];

		mColorContentDescription = config.getString( R.string.feather_acc_color );

		mGallery = (AviaryGallery) getOptionView().findViewById( R.id.aviary_gallery );
		mGallery.setDefaultPosition( mSelectedPosition );
		mGallery.setCallbackDuringFling( false );
		mGallery.setAutoSelectChild( true );
		mGallery.setAdapter( new GalleryAdapter( getContext().getBaseContext(), mColors ) );
		mGallery.setOnItemsScrollListener( this );

		mEditText = (EditText) getContentView().findViewById( R.id.aviary_text );
		mImageView = (ImageViewTouch) getContentView().findViewById( R.id.aviary_image );
		mImageView.setDisplayType( DisplayType.FIT_IF_BIGGER );
		mImageView.setDoubleTapEnabled( false );

		createAndConfigurePreview();

		mImageView.setImageBitmap( mPreview, null, -1, UIConfiguration.IMAGE_VIEW_MAX_ZOOM );
	}

	@Override
	public void onActivate() {
		super.onActivate();

		disableHapticIsNecessary( mGallery );

		mDefaultTextSize = config.getDimensionPixelSize( R.dimen.aviary_text_overlay_default_size );

		( (ImageViewDrawableOverlay) mImageView ).setOnDrawableEventListener( this );
		( (ImageViewDrawableOverlay) mImageView ).setScaleWithContent( true );
		( (ImageViewDrawableOverlay) mImageView ).setForceSingleSelection( false );

		mImageView.requestLayout();

		mEditText.setOnKeyListener( this );

		contentReady();

		// delay the creation of the text element
		getHandler().postDelayed( new Runnable() {

			@Override
			public void run() {
				onAddNewText();
			}
		}, 200 );
	}

	@Override
	public void onDeactivate() {
		( (ImageViewDrawableOverlay) mImageView ).setOnDrawableEventListener( null );
		mGallery.setOnItemsScrollListener( null );
		endEdit( null );
		super.onDeactivate();
	}

	@Override
	public void onDestroy() {
		mCanvas = null;
		( (ImageViewDrawableOverlay) mImageView ).clearOverlays();
		super.onDestroy();
	}

	@Override
	public void onScrollStarted( AviaryAdapterView<?> parent, View view, int position, long id ) {}

	@Override
	public void onScroll( AviaryAdapterView<?> parent, View view, int position, long id ) {
		if ( position >= 0 && position < mColors.length ) {
			final int color = mColors[position];
			mColor = color;
			updateCurrentHighlightColor();
		}
	}

	@Override
	public void onScrollFinished( AviaryAdapterView<?> parent, View view, int position, long id ) {
		if ( position >= 0 && position < mColors.length ) {
			final int color = mColors[position];
			mColor = color;
			updateCurrentHighlightColor();
		}
	}

	@Override
	public boolean onKey( View v, int keyCode, KeyEvent event ) {

		DrawableHighlightView hv = ( (ImageViewDrawableOverlay) mImageView ).getSelectedHighlightView();
		mLogger.log( "onKey: " + keyCode );

		if ( null != hv ) {

			if ( keyCode == KeyEvent.KEYCODE_DEL || keyCode == KeyEvent.KEYCODE_BACK ) {

				FeatherDrawable content = hv.getContent();
				if ( content instanceof EditableDrawable ) {
					EditableDrawable editable = (EditableDrawable) content;

					if ( editable.isTextHint() && editable.isEditing() ) {
						editable.setText( "" );
						if ( hv.forceUpdate() ) {
							mImageView.invalidate( hv.getInvalidationRect() );
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public void onDown( final DrawableHighlightView view ) {}

	@Override
	public void onFocusChange( final DrawableHighlightView newFocus, final DrawableHighlightView oldFocus ) {
		EditableDrawable text;

		if ( oldFocus != null ) if ( oldFocus.getContent() instanceof EditableDrawable ) {
			text = (EditableDrawable) oldFocus.getContent();
			if ( text.isEditing() ) {
				// text.endEdit();
				endEdit( oldFocus );
			}
		}

		if ( newFocus != null ) if ( newFocus.getContent() instanceof EditableDrawable ) {
			text = (EditableDrawable) newFocus.getContent();
			mColor = text.getTextColor();
		}
	}

	@Override
	protected void onGenerateResult() {
		MoaActionList actions = onApplyCurrent();
		super.onGenerateResult( actions );
	}

	@Override
	public void onMove( final DrawableHighlightView view ) {
		if ( view.getContent() instanceof EditableDrawable ) if ( ( (EditableDrawable) view.getContent() ).isEditing() ) {
			// ( (EditableDrawable) view.getContent() ).endEdit();
			endEdit( view );
		}
	}

	/**
	 * Update current highlight color.
	 */
	private void updateCurrentHighlightColor() {
		final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;

		DrawableHighlightView current = image.getSelectedHighlightView();
		if ( current == null ) {
			if ( image.getHighlightCount() > 0 ) {
				current = image.getHighlightViewAt( 0 );
			}
		}

		if ( current != null ) {
			if ( current.getContent() instanceof EditableDrawable ) {
				( (EditableDrawable) current.getContent() ).setTextColor( mColor );
				mImageView.postInvalidate();
			}
		}
	}

	@Override
	public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {

		mLogger.info( "onEditorAction: " + actionId + ", event: " + event );

		if ( mEditText != null ) {
			if ( mEditText.equals( v ) ) {
				if ( actionId == EditorInfo.IME_ACTION_DONE ) {
					final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;
					if ( image.getSelectedHighlightView() != null ) {
						DrawableHighlightView d = image.getSelectedHighlightView();
						if ( d.getContent() instanceof EditableDrawable ) {
							EditableDrawable text = (EditableDrawable) d.getContent();
							if ( text.isEditing() ) {
								// text.endEdit();
								endEdit( d );
							}
						}
					}
				}
			}
		}

		return false;
	}

	class GalleryAdapter extends BaseAdapter {

		private final int VALID_POSITION = 0;
		private final int INVALID_POSITION = 1;

		private int[] sizes;
		LayoutInflater mLayoutInflater;
		Resources mRes;

		public GalleryAdapter ( Context context, int[] values ) {
			mLayoutInflater = LayoutInflater.from( context );
			sizes = values;
			mRes = context.getResources();
		}

		@Override
		public int getCount() {
			return sizes.length;
		}

		@Override
		public Object getItem( int position ) {
			return sizes[position];
		}

		@Override
		public long getItemId( int position ) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType( int position ) {
			final boolean valid = position >= 0 && position < getCount();
			return valid ? VALID_POSITION : INVALID_POSITION;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {

			final int type = getItemViewType( position );

			PreviewFillColorDrawable drawable = null;
			int color = 0;

			if ( convertView == null ) {

				convertView = mLayoutInflater.inflate( R.layout.aviary_gallery_item_view, mGallery, false );

				if ( type == VALID_POSITION ) {
					drawable = new PreviewFillColorDrawable( getContext().getBaseContext() );
					ImageView image = (ImageView) convertView.findViewById( R.id.image );
					image.setImageDrawable( drawable );
					convertView.setTag( drawable );
				}
			} else {
				if ( type == VALID_POSITION ) {
					drawable = (PreviewFillColorDrawable) convertView.getTag();
				}
			}

			if ( drawable != null && type == VALID_POSITION ) {
				color = sizes[position];
				drawable.setColor( color );

				try {
					convertView.setContentDescription( mColorContentDescription + " " + Integer.toHexString( color ) );
				} catch ( Exception e ) {
				}
			}

			convertView.setSelected( mSelectedPosition == position );
			convertView.setId( position );
			return convertView;
		}
	}

}
