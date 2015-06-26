package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnDrawableChangeListener;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.OnLayoutChangeListener;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint.FontMetrics;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.PackageManagerUtils;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.filters.MemeFilter;
import com.aviary.android.feather.library.graphics.drawable.AviaryMemeTextDrawable;
import com.aviary.android.feather.library.graphics.drawable.EditableDrawable;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.MatrixUtils;
import com.aviary.android.feather.utils.TypefaceUtils;
import com.aviary.android.feather.widget.AviaryButton;
import com.aviary.android.feather.widget.DrawableHighlightView;
import com.aviary.android.feather.widget.ImageViewDrawableOverlay;
import com.aviary.android.feather.widget.ImageViewDrawableOverlay.OnDrawableEventListener;

public class MemePanel extends AbstractContentPanel implements OnEditorActionListener, OnClickListener, OnDrawableEventListener, OnLayoutChangeListener {

	AviaryButton editTopButton, editBottomButton;
	EditText editTopText, editBottomText;
	InputMethodManager mInputManager;
	Canvas mCanvas;
	DrawableHighlightView topHv, bottomHv;
	Typeface mTypeface;
	String mTypefaceSourceDir;
	String fontName;
	View clearButtonTop, clearButtonBottom;
	int paddingTop = 16;
	int paddingBottom = 16;
	int mTextColor = 0xFFFFFFFF;
	int mTextStrokeColor = 0xFF000000;
	boolean mTextStrokeEnabled = true;

	Handler mInputHandler = new Handler();
	ResultReceiver mInputReceiver = new ResultReceiver( mInputHandler );

	public MemePanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry );

		ConfigService config = context.getService( ConfigService.class );
		if ( config != null ) {
			fontName = config.getString( R.string.aviary_meme_font );
			mTextColor = config.getColor( R.color.aviary_meme_text_color );
			mTextStrokeColor = config.getColor( R.color.aviary_meme_stroke_color );
			mTextStrokeEnabled = config.getBoolean( R.integer.aviary_meme_stroke_enabled );
		}
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		editTopButton = (AviaryButton) getOptionView().findViewById( R.id.aviary_button1 );
		editBottomButton = (AviaryButton) getOptionView().findViewById( R.id.aviary_button2 );

		mImageView = (ImageViewTouch) getContentView().findViewById( R.id.aviary_image );
		editTopText = (EditText) getContentView().findViewById( R.id.aviary_invisible_text_1 );
		editBottomText = (EditText) getContentView().findViewById( R.id.aviary_invisible_text_2 );

		clearButtonTop = getOptionView().findViewById( R.id.aviary_clear_button1 );
		clearButtonBottom = getOptionView().findViewById( R.id.aviary_clear_button2 );

		mImageView.setDisplayType( DisplayType.FIT_TO_SCREEN );
		mImageView.setDoubleTapEnabled( false );
		mImageView.setScaleEnabled( false );
		mImageView.setScrollEnabled( false );

		createAndConfigurePreview();

		mImageView.setOnDrawableChangedListener( new OnDrawableChangeListener() {

			@Override
			public void onDrawableChanged( Drawable drawable ) {

				mLogger.info( "onDrawableChanged" );

				final Matrix mImageMatrix = mImageView.getImageViewMatrix();
				float[] matrixValues = getMatrixValues( mImageMatrix );
				final int height = (int) ( mBitmap.getHeight() * matrixValues[Matrix.MSCALE_Y] );
				View view = getContentView().findViewById( R.id.aviary_meme_dumb );
				LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) view.getLayoutParams();
				p.height = height - 50;
				view.setLayoutParams( p );
				view.requestLayout();
			}
		} );

		mImageView.setImageBitmap( mPreview, null, ImageViewTouchBase.ZOOM_INVALID, ImageViewTouchBase.ZOOM_INVALID );

	}

	@Override
	public void onActivate() {
		super.onActivate();

		createTypeFace();

		( (ImageViewDrawableOverlay) mImageView ).setOnDrawableEventListener( this );
		( (ImageViewDrawableOverlay) mImageView ).setOnLayoutChangeListener( this );

		mInputManager = (InputMethodManager) getContext().getBaseContext().getSystemService( Context.INPUT_METHOD_SERVICE );
		editTopButton.setOnClickListener( this );
		editBottomButton.setOnClickListener( this );

		editTopText.setVisibility( View.VISIBLE );
		editBottomText.setVisibility( View.VISIBLE );
		editTopText.getBackground().setAlpha( 0 );
		editBottomText.getBackground().setAlpha( 0 );

		clearButtonTop.setOnClickListener( this );
		clearButtonBottom.setOnClickListener( this );

		getContentView().setVisibility( View.VISIBLE );
		contentReady();
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();

		endEditView( topHv );
		endEditView( bottomHv );

		( (ImageViewDrawableOverlay) mImageView ).setOnDrawableEventListener( null );
		( (ImageViewDrawableOverlay) mImageView ).setOnLayoutChangeListener( null );

		editTopButton.setOnClickListener( null );
		editBottomButton.setOnClickListener( null );
		clearButtonTop.setOnClickListener( null );
		clearButtonBottom.setOnClickListener( null );

		if ( mInputManager.isActive( editTopText ) ) mInputManager.hideSoftInputFromWindow( editTopText.getWindowToken(), 0 );
		if ( mInputManager.isActive( editBottomText ) ) mInputManager.hideSoftInputFromWindow( editBottomText.getWindowToken(), 0 );

	}

	@Override
	public void onDestroy() {
		mCanvas = null;
		mInputManager = null;
		( (ImageViewDrawableOverlay) mImageView ).clearOverlays();
		super.onDestroy();
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.aviary_content_meme, null );
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_meme, parent, false );
	}

	@Override
	protected void onGenerateResult() {
		MemeFilter filter = (MemeFilter) FilterLoaderFactory.get( Filters.MEME );
		final float scale = MatrixUtils.getScale( mImageView.getImageMatrix() )[0];

		filter.setPaddingTop( (double) paddingTop / scale );
		filter.setPaddingBottom( (double) paddingBottom / scale );

		if ( null != mTypeface ) {
			filter.setAssetFontName( fontName );
		}

		if ( null != mTypefaceSourceDir ) {
			filter.setFontSourceDir( mTypefaceSourceDir );
		}

		if ( null != topHv ) {
			flattenText( topHv, filter );
		}

		if ( null != bottomHv ) {
			flattenText( bottomHv, filter );
		}

		MoaActionList actionList = (MoaActionList) filter.getActions().clone();
		super.onGenerateResult( actionList );
	}

	@Override
	public boolean onEditorAction( TextView v, int actionId, KeyEvent event ) {

		mLogger.info( "onEditorAction", v, actionId, event );

		if ( v != null ) {
			if ( actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_UNSPECIFIED ) {
				final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;
				if ( image.getSelectedHighlightView() != null ) {
					DrawableHighlightView d = image.getSelectedHighlightView();
					if ( d.getContent() instanceof EditableDrawable ) {
						endEditView( d );
					}
				}
			}
		}

		return false;
	}

	private void flattenText( final DrawableHighlightView hv, final MemeFilter filter ) {

		if ( hv != null ) {
			hv.setHidden( true );
			// final Matrix mImageMatrix = mImageView.getImageViewMatrix();
			// float[] matrixValues = getMatrixValues( mImageMatrix );

			final float scale = MatrixUtils.getScale( mImageView.getImageMatrix() )[0];

			final int width = (int) ( mBitmap.getWidth() );
			final int height = (int) ( mBitmap.getHeight() );

			final RectF cropRect = hv.getCropRectF();
			final Rect rect = new Rect( (int) cropRect.left, (int) cropRect.top, (int) cropRect.right, (int) cropRect.bottom );
			final AviaryMemeTextDrawable editable = (AviaryMemeTextDrawable) hv.getContent();

			final int saveCount = mCanvas.save( Canvas.MATRIX_SAVE_FLAG );

			// force end edit and hide the blinking cursor
			editable.endEdit();
			editable.invalidateSelf();

			editable.setContentSize( width, height );
			editable.setBounds( rect.left, rect.top, rect.right, rect.bottom );
			editable.draw( mCanvas );

			filter.setPreviewSize( width, height );
			filter.setFillColor( editable.getTextColor() );
			filter.setStrokeColor( editable.getStrokeEnabled() ? editable.getTextStrokeColor() : 0 );
			filter.setTextSize( editable.getDefaultTextSize() / scale );

			if ( topHv == hv ) {
				filter.setTopText( (String) editable.getText() );
			} else if ( hv == bottomHv ) {
				filter.setBottomText( (String) editable.getText() );
			}

			mCanvas.restoreToCount( saveCount );
			mImageView.invalidate();
		}

		onPreviewChanged( mPreview, false, false );
	}

	private void createAndConfigurePreview() {

		if ( ( mPreview != null ) && !mPreview.isRecycled() ) {
			mPreview.recycle();
			mPreview = null;
		}

		mPreview = BitmapUtils.copy( mBitmap, mBitmap.getConfig() );
		mCanvas = new Canvas( mPreview );
	}

	@Override
	public void onClick( View v ) {

		mLogger.info( "onClick" );

		if ( null == v ) return;

		final int id = v.getId();

		if ( id == editTopButton.getId() ) {
			if ( null == topHv ) {
				onAddTopText();
			}
			onTopClick( topHv );

		} else if ( id == editBottomButton.getId() ) {
			if ( null == bottomHv ) {
				onAddBottomText();
			}
			onTopClick( bottomHv );
		} else if ( id == clearButtonTop.getId() ) {
			clearEditView( topHv );
			endEditView( topHv );
		} else if ( id == clearButtonBottom.getId() ) {
			clearEditView( bottomHv );
			endEditView( bottomHv );
		}
	}

	public void onTopClick( final DrawableHighlightView view ) {

		mLogger.info( "onTopClick", view );

		if ( view != null ) {
			if ( view.getContent() instanceof EditableDrawable ) {
				beginEditView( view );
			}
		}
	}

	public static float[] getMatrixValues( Matrix m ) {
		float[] values = new float[9];
		m.getValues( values );
		return values;
	}

	private void onAddTopText() {

		final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;

		RectF rect = image.getBitmapRect();

		int textSize = (int) ( Math.min( rect.width(), rect.height() ) / 7.0 );

		final AviaryMemeTextDrawable text = new AviaryMemeTextDrawable( "", textSize, mTypeface, true );
		text.setTextColor( mTextColor );
		text.setStrokeEnabled( mTextStrokeEnabled );
		text.setStrokeColor( mTextStrokeColor );
		text.setContentSize( rect.width(), rect.height() );

		topHv = new DrawableHighlightView( image, image.getOverlayStyleId(), text );
		final Matrix mImageMatrix = image.getImageViewMatrix();

		int cropHeight = text.getIntrinsicHeight();

		final Matrix matrix = new Matrix( mImageMatrix );
		matrix.invert( matrix );

		final float[] pts = new float[] { rect.left, rect.top + paddingTop, rect.right, rect.top + cropHeight + paddingTop };
		MatrixUtils.mapPoints( matrix, pts );

		final RectF cropRect = new RectF( pts[0], pts[1], pts[2], pts[3] );

		topHv.setAlignModeV( DrawableHighlightView.AlignModeV.Top );
		topHv.setup( getContext().getBaseContext(), mImageMatrix, null, cropRect, false );

		image.addHighlightView( topHv );
	}

	private void onAddBottomText() {
		final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;

		RectF rect = image.getBitmapRect();
		int textSize = (int) ( Math.min( rect.width(), rect.height() ) / 7.0 );

		final AviaryMemeTextDrawable text = new AviaryMemeTextDrawable( "", textSize, mTypeface, false );
		text.setTextColor( mTextColor );
		text.setStrokeEnabled( mTextStrokeEnabled );
		text.setStrokeColor( mTextStrokeColor );
		text.setContentSize( rect.width(), rect.height() );

		FontMetrics metrics = new FontMetrics();
		text.getFontMetrics( metrics );

		bottomHv = new DrawableHighlightView( image, image.getOverlayStyleId(), text );
		final Matrix mImageMatrix = image.getImageViewMatrix();

		int cropHeight = text.getIntrinsicHeight();

		final Matrix matrix = new Matrix( mImageMatrix );
		matrix.invert( matrix );

		final float[] pts = new float[] { rect.left, rect.bottom - cropHeight - paddingBottom, rect.left + rect.width(), rect.bottom - paddingBottom };
		MatrixUtils.mapPoints( matrix, pts );

		final RectF cropRect = new RectF( pts[0], pts[1], pts[2], pts[3] );

		mLogger.log( "bitmap rect: " + rect );

		bottomHv.setAlignModeV( DrawableHighlightView.AlignModeV.Bottom );
		bottomHv.setup( getContext().getBaseContext(), mImageMatrix, null, cropRect, false );
		image.addHighlightView( bottomHv );
	}

	abstract class MyTextWatcher implements TextWatcher {

		public DrawableHighlightView view;
	}

	private final MyTextWatcher mEditTextWatcher = new MyTextWatcher() {

		@Override
		public void afterTextChanged( final Editable s ) {}

		@Override
		public void beforeTextChanged( final CharSequence s, final int start, final int count, final int after ) {}

		@Override
		public void onTextChanged( final CharSequence s, final int start, final int before, final int count ) {

			mLogger.info( "onTextChanged", view );

			if ( ( view != null ) && ( view.getContent() instanceof EditableDrawable ) ) {
				final EditableDrawable editable = (EditableDrawable) view.getContent();

				if ( !editable.isEditing() ) return;

				editable.setText( s.toString() );

				if ( view.equals( topHv ) ) {
					editTopButton.setText( s );
					clearButtonTop.setVisibility( s != null && s.length() > 0 ? View.VISIBLE : View.INVISIBLE );
				} else if ( view.equals( bottomHv ) ) {
					editBottomButton.setText( s );
					clearButtonBottom.setVisibility( s != null && s.length() > 0 ? View.VISIBLE : View.INVISIBLE );
				}

				if ( view.forceUpdate() ) {
					mImageView.invalidate( view.getInvalidationRect() );
				}

				checkIsChanged();
				// setIsChanged( true );
			}
		}
	};

	@Override
	public void onFocusChange( DrawableHighlightView newFocus, DrawableHighlightView oldFocus ) {

		mLogger.info( "onFocusChange", newFocus, oldFocus );

		if ( oldFocus != null ) {
			if ( newFocus == null ) {
				endEditView( oldFocus );
			}
		}
	}

	private void endEditView( DrawableHighlightView hv ) {
		if ( null == hv ) return;

		EditableDrawable text = (EditableDrawable) hv.getContent();

		if ( text.isEditing() ) {
			text.endEdit();
			endEditText( hv );
		}

		CharSequence value = text.getText();
		if ( hv.equals( topHv ) ) {
			editTopButton.setText( value );
			clearButtonTop.setVisibility( value != null && value.length() > 0 ? View.VISIBLE : View.INVISIBLE );
		} else if ( hv.equals( bottomHv ) ) {
			editBottomButton.setText( value );
			clearButtonBottom.setVisibility( value != null && value.length() > 0 ? View.VISIBLE : View.INVISIBLE );
		}
	}

	private void beginEditView( DrawableHighlightView hv ) {
		mLogger.info( "beginEditView" );

		if ( null == hv ) return;

		final EditableDrawable text = (EditableDrawable) hv.getContent();

		if ( hv == topHv ) {
			endEditView( bottomHv );
		} else {
			endEditView( topHv );
		}

		if ( !text.isEditing() ) {
			text.beginEdit();
		}

		beginEditText( hv );
	}

	private void clearEditView( DrawableHighlightView hv ) {
		if ( null != hv ) {
			final AviaryMemeTextDrawable text = (AviaryMemeTextDrawable) hv.getContent();
			text.setText( "" );
			text.invalidateSelf();
			if ( hv.forceUpdate() ) {
				mImageView.invalidate( hv.getInvalidationRect() );
			}

			checkIsChanged();
		}
	}

	private void checkIsChanged() {
		if ( checkIsChanged( topHv ) ) {
			setIsChanged( true );
		} else if ( checkIsChanged( bottomHv ) ) {
			setIsChanged( true );
		} else {
			setIsChanged( false );
		}

		mLogger.log( "isChanged?: " + getIsChanged() );
	}

	private boolean checkIsChanged( DrawableHighlightView view ) {
		if ( null != view ) {
			EditableDrawable editable = (EditableDrawable) view.getContent();
			if ( null != editable ) {
				CharSequence text = editable.getText();
				if ( null != text && text.length() > 0 ) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onDown( DrawableHighlightView view ) {

	}

	@Override
	public void onMove( DrawableHighlightView view ) {}

	@Override
	public void onClick( DrawableHighlightView view ) {
		if ( view != null ) {
			if ( view.getContent() instanceof EditableDrawable ) {
				beginEditView( view );
			}
		}

	}

	private void beginEditText( final DrawableHighlightView view ) {
		mLogger.info( "beginEditText", view );

		EditText editText = null;

		if ( view == topHv ) {
			editText = editTopText;
		} else if ( view == bottomHv ) {
			editText = editBottomText;
		}

		if ( editText != null ) {
			mEditTextWatcher.view = null;
			editText.removeTextChangedListener( mEditTextWatcher );

			final EditableDrawable editable = (EditableDrawable) view.getContent();
			final String oldText = (String) editable.getText();
			editText.setText( oldText );
			editText.setSelection( editText.length() );
			editText.setImeOptions( EditorInfo.IME_ACTION_DONE );
			editText.requestFocusFromTouch();

			if ( !mInputManager.showSoftInput( editText, 0, mInputReceiver ) ) {
				mInputManager.toggleSoftInput( InputMethodManager.SHOW_FORCED, 0 ); // TODO:
																					// verify
			}

			mEditTextWatcher.view = view;
			editText.setOnEditorActionListener( this );
			editText.addTextChangedListener( mEditTextWatcher );

			( (ImageViewDrawableOverlay) mImageView ).setSelectedHighlightView( view );
			( (EditableDrawable) view.getContent() ).setText( ( (EditableDrawable) view.getContent() ).getText() );

			if ( view.forceUpdate() ) {
				mImageView.invalidate( view.getInvalidationRect() );
			}

			checkIsChanged();
		}
	}

	private void endEditText( final DrawableHighlightView view ) {

		mLogger.info( "endEditText", view );

		mEditTextWatcher.view = null;
		EditText editText = null;

		if ( view == topHv ) editText = editTopText;
		else if ( view == bottomHv ) editText = editBottomText;

		if ( editText != null ) {
			editText.removeTextChangedListener( mEditTextWatcher );
			editText.setOnEditorActionListener( null );

			if ( mInputManager.isActive( editText ) ) {
				mInputManager.hideSoftInputFromWindow( editText.getWindowToken(), 0 );
			}
			editText.clearFocus();
		}
	}

	private void createTypeFace() {
		try {
			mTypeface = TypefaceUtils.createFromAsset( getContext().getBaseContext().getAssets(), fontName );

			ApplicationInfo info = PackageManagerUtils.getApplicationInfo( getContext().getBaseContext() );
			if ( null != info ) {
				mTypefaceSourceDir = info.sourceDir;
			}

		} catch ( Exception e ) {
			mTypeface = Typeface.DEFAULT;
		}
	}

	@Override
	public void onLayoutChanged( boolean changed, int left, int top, int right, int bottom ) {
		if ( changed ) {
			final Matrix mImageMatrix = mImageView.getImageViewMatrix();
			float[] matrixValues = getMatrixValues( mImageMatrix );
			final float w = mBitmap.getWidth();
			final float h = mBitmap.getHeight();
			final float scale = matrixValues[Matrix.MSCALE_X];

			if ( topHv != null ) {
				AviaryMemeTextDrawable text = (AviaryMemeTextDrawable) topHv.getContent();
				text.setContentSize( w * scale, h * scale );
			}

			if ( bottomHv != null ) {
				AviaryMemeTextDrawable text = (AviaryMemeTextDrawable) bottomHv.getContent();
				text.setContentSize( w * scale, h * scale );
			}
		}
	}
}
