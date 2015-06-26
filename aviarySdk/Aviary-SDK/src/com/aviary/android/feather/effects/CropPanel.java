package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;

import java.util.HashSet;

import org.json.JSONException;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.ScreenUtils;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.headless.filters.impl.CropFilter;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.headless.moa.MoaPointParameter;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.graphics.RectD;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.widget.AviaryAdapterView;
import com.aviary.android.feather.widget.AviaryAdapterView.OnItemClickListener;
import com.aviary.android.feather.widget.AviaryGallery;
import com.aviary.android.feather.widget.AviaryHighlightImageButton;
import com.aviary.android.feather.widget.CropImageView;
import com.aviary.android.feather.widget.HighlightView;

public class CropPanel extends AbstractContentPanel implements com.aviary.android.feather.widget.AviaryGallery.OnItemsScrollListener, OnItemClickListener {

	AviaryGallery mGallery;
	String[] mCropNames, mCropValues;
	int mSelectedPosition = 0;
	boolean mIsPortrait = true;
	final static int noImage = 0;
	HashSet<Integer> mNonInvertOptions = new HashSet<Integer>();

	// whether to use inversion and photo size detection
	boolean mStrictPolicy = false;

	// Whether or not the proportions are inverted
	boolean isChecked = false;

	// gallery has scrolled
	@SuppressWarnings ( "unused" )
	private boolean mHasScrolled;

	public CropPanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry );
	}

	private void invertRatios( String[] names, String[] values ) {

		for ( int i = 0; i < names.length; i++ ) {

			if ( names[i].contains( ":" ) ) {
				String temp = names[i];
				String[] splitted = temp.split( "[:]" );
				String mNewOptionName = splitted[1] + ":" + splitted[0];
				names[i] = mNewOptionName;
			}

			if ( values[i].contains( ":" ) ) {
				String temp = values[i];
				String[] splitted = temp.split( "[:]" );
				String mNewOptionValue = splitted[1] + ":" + splitted[0];
				values[i] = mNewOptionValue;
			}
		}
	}

	private void populateInvertOptions( HashSet<Integer> options, String[] cropValues ) {
		for ( int i = 0; i < cropValues.length; i++ ) {
			String value = cropValues[i];
			String[] values = value.split( ":" );
			int x = Integer.parseInt( values[0] );
			int y = Integer.parseInt( values[1] );

			if ( x == y ) {
				options.add( i );
			}
		}
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		ConfigService config = getContext().getService( ConfigService.class );
		mFilter = FilterLoaderFactory.get( Filters.CROP );

		mCropNames = config.getStringArray( R.array.aviary_crop_labels );
		mCropValues = config.getStringArray( R.array.aviary_crop_values );
		mStrictPolicy = config.getBoolean( R.integer.aviary_crop_invert_policy );

		if ( !mStrictPolicy ) {
			if ( bitmap.getHeight() > bitmap.getWidth() ) {
				mIsPortrait = true;
			} else {
				mIsPortrait = false;
			}

			// configure options that will not invert
			populateInvertOptions( mNonInvertOptions, mCropValues );

			if ( mIsPortrait ) {
				invertRatios( mCropNames, mCropValues );
			}
		}

		mSelectedPosition = config.getInteger( R.integer.aviary_crop_selected_index );

		mImageView = (CropImageView) getContentView().findViewById( R.id.aviary_crop_image );
		mImageView.setDoubleTapEnabled( false );
		mImageView.setScaleEnabled( false );
		mImageView.setScrollEnabled( false );
		mImageView.setDisplayType( DisplayType.FIT_IF_BIGGER );

		mGallery = (AviaryGallery) getOptionView().findViewById( R.id.aviary_gallery );

		mGallery.setDefaultPosition( mSelectedPosition );
		mGallery.setCallbackDuringFling( false );
		mGallery.setAutoSelectChild( false );

		mGallery.setAdapter( new GalleryAdapter( getContext().getBaseContext(), mCropNames ) );
		mGallery.setSelection( mSelectedPosition, false, true );
	}

	@Override
	public void onActivate() {
		super.onActivate();

		disableHapticIsNecessary( mGallery );

		mGallery.setOnItemsScrollListener( this );
		mGallery.setOnItemClickListener( this );

		int position = mGallery.getSelectedItemPosition();
		final double ratio = calculateAspectRatio( position, false );

		createCropView( ratio, ratio != 0 );
		contentReady();
		setIsChanged( true );
	}

	private double calculateAspectRatio( int position, boolean inverted ) {

		String value = mCropValues[position];
		String[] values = value.split( ":" );

		if ( values.length == 2 ) {
			int aspectx = Integer.parseInt( inverted ? values[1] : values[0] );
			int aspecty = Integer.parseInt( inverted ? values[0] : values[1] );

			if ( aspectx == -1 ) {
				aspectx = inverted ? mBitmap.getHeight() : mBitmap.getWidth();
			}

			if ( aspecty == -1 ) {
				aspecty = inverted ? mBitmap.getWidth() : mBitmap.getHeight();
			}

			double ratio = 0;

			if ( aspectx != 0 && aspecty != 0 ) {
				ratio = (double) aspectx / (double) aspecty;
			}
			return ratio;
		}
		return 0;
	}

	@Override
	public void onDestroy() {
		mImageView.clear();
		( (CropImageView) mImageView ).setOnHighlightSingleTapUpConfirmedListener( null );
		super.onDestroy();
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();
		mGallery.setOnItemsScrollListener( null );
		mGallery.setOnItemClickListener( null );
	}

	private void createCropView( double aspectRatio, boolean isFixed ) {
		( (CropImageView) mImageView ).setImageBitmap( mBitmap, aspectRatio, isFixed );
	}

	private void setCustomRatio( double aspectRatio, boolean isFixed ) {
		( (CropImageView) mImageView ).setAspectRatio( aspectRatio, isFixed );
	}

	@Override
	protected void onGenerateResult() {
		RectD crop_rect = new RectD( ( (CropImageView) mImageView ).getHighlightView().getCropRectD() );
		GenerateResultTask task = new GenerateResultTask( crop_rect );
		task.execute( mBitmap );
	}

	class GenerateResultTask extends AviaryAsyncTask<Bitmap, Void, Bitmap> {

		RectD mCropRect;
		MoaActionList mActionList;

		public GenerateResultTask ( RectD rect ) {
			mCropRect = rect;
		}

		@Override
		protected void PreExecute() {
			onProgressStart();
		}

		@Override
		protected Bitmap doInBackground( Bitmap... arg0 ) {

			final Bitmap bitmap = arg0[0];

			double bitmapWidth = bitmap.getWidth();
			double bitmapHeight = bitmap.getHeight();

			MoaPointParameter topLeft = new MoaPointParameter();
			topLeft.setValue( (int) mCropRect.left, (int) mCropRect.top );

			MoaPointParameter size = new MoaPointParameter();
			size.setValue( (int) mCropRect.width(), (int) mCropRect.height() );

			MoaPointParameter previewSize = new MoaPointParameter( bitmapWidth, bitmapHeight );

			( (CropFilter) mFilter ).setTopLeft( topLeft );
			( (CropFilter) mFilter ).setSize( size );
			( (CropFilter) mFilter ).setPreviewSize( previewSize );

			mActionList = (MoaActionList) ( (CropFilter) mFilter ).getActions().clone();

			try {
				return ( (CropFilter) mFilter ).execute( arg0[0], null, 1, 1 );
			} catch ( JSONException e ) {
				e.printStackTrace();
			}
			return arg0[0];
		}

		@Override
		protected void PostExecute( Bitmap result ) {
			onProgressEnd();

			( (CropImageView) mImageView ).setImageBitmap( result, ( (CropImageView) mImageView ).getAspectRatio(),
					( (CropImageView) mImageView ).getAspectRatioIsFixed() );
			( (CropImageView) mImageView ).setHighlightView( null );

			onComplete( result, mActionList );
		}
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		View view = inflater.inflate( R.layout.aviary_content_crop, null );

		if ( ScreenUtils.isTablet( getContext().getBaseContext() ) ) {
			// try {
			// mLogger.log( "device is tablet, set the layerType to software" );
			// ReflectionUtils.invokeMethod( view, "setLayerType", new Class[] {
			// int.class, Paint.class }, 1, null );
			// } catch ( ReflectionException e ) {
			// e.printStackTrace();
			// }
		}

		return view;
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_crop, parent, false );
	}

	class GalleryAdapter extends BaseAdapter {

		final int INVALID_POSITION = 0;
		final int VALID_POSITION = 1;
		final int VALID_POSITION_CUSTOM = 2;

		private String[] mValues;
		LayoutInflater mLayoutInflater;
		Resources mRes;

		public GalleryAdapter ( Context context, String[] values ) {
			mLayoutInflater = LayoutInflater.from( context );
			mValues = values;
			mRes = context.getResources();
		}

		@Override
		public int getCount() {
			return mValues.length;
		}

		@Override
		public Object getItem( int position ) {
			return mValues[position];
		}

		@Override
		public long getItemId( int position ) {
			return 0;
		}

		@Override
		public int getViewTypeCount() {
			return 3;
		}

		@Override
		public int getItemViewType( int position ) {
			final boolean valid = position >= 0 && position < getCount();
			return valid ? ( mNonInvertOptions.contains( position ) ? VALID_POSITION_CUSTOM : VALID_POSITION ) : INVALID_POSITION;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {

			final int type = getItemViewType( position );

			AviaryHighlightImageButton image = null;
			TextView text = null;

			// PreviewFillColorDrawable drawable = null;

			if ( convertView == null ) {
				if ( type == VALID_POSITION ) {
					convertView = mLayoutInflater.inflate( R.layout.aviary_gallery_crop_item_view, mGallery, false );
				} else if ( type == VALID_POSITION_CUSTOM ) {
					convertView = mLayoutInflater.inflate( R.layout.aviary_gallery_crop_item_view_custom, mGallery, false );
				} else {
					convertView = mLayoutInflater.inflate( R.layout.aviary_gallery_item_view, mGallery, false );
					convertView.findViewById( R.id.image ).setVisibility( View.GONE );
				}
			}

			if ( type != INVALID_POSITION ) {

				String label = (String) getItem( position );
				text = (TextView) convertView.findViewById( R.id.text );
				text.setText( label );

				if ( type == VALID_POSITION ) {

					image = (AviaryHighlightImageButton) convertView.findViewById( R.id.image );

					int targetVisibility;

					if ( !mStrictPolicy && !mNonInvertOptions.contains( position ) ) {
						targetVisibility = View.VISIBLE;
					} else {
						targetVisibility = View.GONE;
					}

					if ( null != image ) {
						image.setVisibility( targetVisibility );
						image.setChecked( isChecked );
					}
				}
				// drawable.setColor( color );
			}

			// convertView.setSelected( mSelectedPosition == position );
			convertView.setId( position );
			return convertView;
		}
	}

	@Override
	public void onItemClick( AviaryAdapterView<?> parent, View view, int position, long id ) {

		if ( !mStrictPolicy && !mNonInvertOptions.contains( position ) ) {
			isChecked = !isChecked;

			CropImageView cview = (CropImageView) mImageView;
			double currentAspectRatio = cview.getAspectRatio();

			HighlightView hv = cview.getHighlightView();
			if ( !cview.getAspectRatioIsFixed() && hv != null ) {
				currentAspectRatio = (double) hv.getDrawRect().width() / (double) hv.getDrawRect().height();
			}

			double invertedAspectRatio = 1 / currentAspectRatio;

			cview.setAspectRatio( invertedAspectRatio, cview.getAspectRatioIsFixed() );
			invertRatios( mCropNames, mCropValues );
			mGallery.invalidateViews();
		}
	}

	@Override
	public void onScrollStarted( AviaryAdapterView<?> parent, View view, int position, long id ) {
		mHasScrolled = false;
	}

	@Override
	public void onScroll( AviaryAdapterView<?> parent, View view, int position, long id ) {
		mHasScrolled = true;
	}

	@Override
	public void onScrollFinished( AviaryAdapterView<?> parent, View view, int position, long id ) {
		mSelectedPosition = position;

		double ratio = calculateAspectRatio( position, false );
		setCustomRatio( ratio, ratio != 0 );
	}
}
