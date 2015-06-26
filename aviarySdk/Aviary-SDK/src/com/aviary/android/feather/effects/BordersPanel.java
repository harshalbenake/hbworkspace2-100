package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;
import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.widget.AdapterView.OnItemClickListener;
import it.sephiroth.android.library.widget.AdapterView.OnItemSelectedListener;
import it.sephiroth.android.library.widget.HListView;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewSwitcher.ViewFactory;

import com.aviary.android.feather.AviaryMainController.FeatherContext;
import com.aviary.android.feather.R;
import com.aviary.android.feather.cds.AviaryCds.PackType;
import com.aviary.android.feather.cds.PacksItemsColumns;
import com.aviary.android.feather.cds.TrayColumns;
import com.aviary.android.feather.common.AviaryIntent;
import com.aviary.android.feather.common.utils.IOUtils;
import com.aviary.android.feather.common.utils.PackageManagerUtils;
import com.aviary.android.feather.common.utils.SystemUtils;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.graphics.PluginDividerDrawable;
import com.aviary.android.feather.headless.filters.INativeFilter;
import com.aviary.android.feather.headless.filters.NativeFilterProxy;
import com.aviary.android.feather.headless.moa.Moa;
import com.aviary.android.feather.headless.moa.MoaAction;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.headless.moa.MoaResult;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.BorderFilter;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.BadgeService;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.services.LocalDataService;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.utils.PackIconCallable;
import com.aviary.android.feather.widget.AviaryImageSwitcher;
import com.aviary.android.feather.widget.IAPDialogMain;
import com.aviary.android.feather.widget.IAPDialogMain.IAPUpdater;
import com.aviary.android.feather.widget.IAPDialogMain.OnCloseListener;
import com.squareup.picasso.Generator;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.Picasso;

public class BordersPanel extends AbstractOptionPanel implements ViewFactory, OnItemSelectedListener, OnItemClickListener,
		OnLoadCompleteListener<Cursor> {

	private final PackType mPackType;

	protected HListView mHList;

	protected View mLoader;

	protected volatile Boolean mIsRendering = false;

	private volatile boolean mIsAnimating;

	private RenderTask mCurrentTask;

	protected ConfigService mConfigService;

	protected BadgeService mBadgeService;

	protected MoaActionList mActions = null;

	/** default width of each effect thumbnail */
	private int mCellWidth = 80;

	protected int mThumbSize;

	private Picasso mPicassoLibrary;
	
	private LruCache mCache;

	/** thumbnail for effects */
	protected Bitmap mThumbBitmap;

	/** current selected position */
	protected int mSelectedPosition = -1;

	/* the first valid position of the list */
	protected int mListFirstValidPosition = 0;

	private boolean mFirstTime = true;

	/** options used to decode cached images */
	private static BitmapFactory.Options mThumbnailOptions;

	protected boolean mEnableFastPreview = false;

	protected TrayColumns.TrayCursorWrapper mRenderedEffect;

	protected CursorAdapter mAdapter;
	protected CursorLoader mCursorLoader;
	protected ContentObserver mContentObserver;

	protected IAPDialogMain mIapDialog;
	
	private static final int MAX_MEM_CACHE_SIZE = 6 * IOUtils.MEGABYTE; // 6MB

	/**
	 * Content resolver has loaded
	 */
	@Override
	public void onLoadComplete( Loader<Cursor> loader, Cursor cursor ) {
		mLogger.info( "onLoadComplete" );

		int index = 0;
		int firstValidIndex = -1;

		if ( null != cursor ) {
			index = cursor.getPosition();
			while ( cursor.moveToNext() ) {
				int type = cursor.getInt( TrayColumns.TYPE_COLUMN_INDEX );
				if ( type == TrayColumns.TYPE_CONTENT ) {
					firstValidIndex = cursor.getPosition();
					break;
				}
			}
			cursor.moveToPosition( index );
		}

		mAdapter.changeCursor( cursor );

		onEffectListUpdated( cursor, firstValidIndex );
	}

	public BordersPanel ( IAviaryController context, ToolEntry entry ) {
		this( context, entry, PackType.FRAME );
	}

	protected BordersPanel ( IAviaryController context, ToolEntry entry, PackType type ) {
		super( context, entry );
		mPackType = type;
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		mPicassoLibrary = Picasso.with( getContext().getBaseContext() );
		
		double[] mem = new double[3];
		SystemUtils.getRuntimeMemoryInfo( mem );
		
		final double total = Math.max( mem[0], 2 ); // at least 2MB
		int max_size = (int) ( IOUtils.MEGABYTE * total );
		
		mLogger.log( "max size for cache: " + max_size );
		
		max_size = Math.min( max_size, MAX_MEM_CACHE_SIZE );
		mCache = new LruCache( max_size );

		mThumbnailOptions = new Options();
		mThumbnailOptions.inPreferredConfig = Config.RGB_565;

		mConfigService = getContext().getService( ConfigService.class );
		mBadgeService = getContext().getService( BadgeService.class );

		LocalDataService dataService = getContext().getService( LocalDataService.class );

		mEnableFastPreview = dataService.getFastPreviewEnabled();

		mHList = (HListView) getOptionView().findViewById( R.id.aviary_list );
		mLoader = getOptionView().findViewById( R.id.aviary_loader );

		mPreview = BitmapUtils.copy( mBitmap, Bitmap.Config.ARGB_8888 );
	}

	@Override
	public void onBitmapReplaced( Bitmap bitmap ) {
		super.onBitmapReplaced( bitmap );

		if ( isActive() ) {
			mLogger.error( "TODO: BordersPanel check this" );
			mHList.setSelection( mListFirstValidPosition );
			// mHList.setSelectedPosition( mListFirstValidPosition, false );
		}
	}

	@Override
	public void onActivate() {
		super.onActivate();

		mCellWidth = mConfigService.getDimensionPixelSize( R.dimen.aviary_frame_item_width );
		mThumbSize = mConfigService.getDimensionPixelSize( R.dimen.aviary_frame_item_image_width );

		mThumbBitmap = generateThumbnail( mBitmap, mThumbSize, mThumbSize );

		mHList.setOnItemClickListener( this );
		onPostActivate();
	}

	@Override
	public boolean isRendering() {
		return mIsRendering;
	}

	protected final PackType getPluginType() {
		return mPackType;
	}

	protected void onPostActivate() {
		updateInstalledPacks( true );
	}

	@Override
	public void onDestroy() {
		mConfigService = null;
		mBadgeService = null;
		
		try {
			mCache.clear();
		} catch( Exception e ) {}
		
		super.onDestroy();
	}

	@Override
	public void onDeactivate() {
		onProgressEnd();
		mHList.setOnItemClickListener( null );
		mHList.setAdapter( null );

		removeIapDialog();

		Context context = getContext().getBaseContext();
		context.getContentResolver().unregisterContentObserver( mContentObserver );

		if ( null != mCursorLoader ) {
			mLogger.info( "disposing cursorloader..." );
			mCursorLoader.unregisterListener( this );
			mCursorLoader.stopLoading();
			mCursorLoader.abandon();
			mCursorLoader.reset();
		}

		if ( null != mAdapter ) {
			Cursor cursor = mAdapter.getCursor();
			IOUtils.closeSilently( cursor );
		}

		mAdapter = null;
		mCursorLoader = null;

		super.onDeactivate();
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig, Configuration oldConfig ) {
		if ( mIapDialog != null ) {
			mIapDialog.onConfigurationChanged( newConfig );
		}
		super.onConfigurationChanged( newConfig, oldConfig );
	}

	@Override
	protected void onDispose() {

		mHList.setAdapter( null );

		if ( mThumbBitmap != null && !mThumbBitmap.isRecycled() ) {
			mThumbBitmap.recycle();
		}
		mThumbBitmap = null;

		super.onDispose();
	}

	@Override
	protected void onGenerateResult() {
		mLogger.info( "onGenerateResult. isRendering: " + mIsRendering );
		if ( mIsRendering ) {
			GenerateResultTask task = new GenerateResultTask();
			task.execute();
		} else {
			onComplete( mPreview, mActions );
		}
	}

	@Override
	protected void onComplete( Bitmap bitmap, MoaActionList actions ) {

		if ( null != mRenderedEffect ) {
			Tracker.recordTag( mRenderedEffect.getIdentifier() + ": applied" );

			mTrackingAttributes.put( "Effect", mRenderedEffect.getIdentifier() );
			mTrackingAttributes.put( "Pack", mRenderedEffect.getPackageName() );

			HashMap<String, String> attrs = new HashMap<String, String>();
			attrs.put( "Effects", mRenderedEffect.getIdentifier() );
			Tracker.recordTag( mRenderedEffect.getPackageName() + ": applied", attrs );
		}

		super.onComplete( bitmap, actions );
	}

	@Override
	public boolean onBackPressed() {
		if ( backHandled() ) return true;
		return super.onBackPressed();
	}

	@Override
	public void onCancelled() {
		killCurrentTask();
		mIsRendering = false;
		super.onCancelled();
	}

	@Override
	public boolean getIsChanged() {
		return super.getIsChanged() || mIsRendering == true;
	}

	@Override
	public View makeView() {
		ImageViewTouch view = new ImageViewTouch( getContext().getBaseContext(), null );
		view.setBackgroundColor( 0x00000000 );
		view.setDoubleTapEnabled( false );
		view.setScaleEnabled( false );
		view.setScrollEnabled( false );
		view.setDisplayType( DisplayType.FIT_IF_BIGGER );
		view.setLayoutParams( new AviaryImageSwitcher.LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
		return view;
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_frames, parent, false );
	}

	protected Bitmap generateThumbnail( Bitmap input, final int width, final int height ) {
		return ThumbnailUtils.extractThumbnail( input, width, height );
	}

	/**
	 * Update the installed plugins
	 */
	protected void updateInstalledPacks( boolean firstTime ) {

		mLoader.setVisibility( View.VISIBLE );
		mHList.setVisibility( View.INVISIBLE );

		mAdapter = createListAdapter( getContext().getBaseContext(), null );
		mHList.setAdapter( mAdapter );

		Context context = getContext().getBaseContext();

		if ( null == mCursorLoader ) {
			
			final String uri = String.format( Locale.US, "packTray/%d/%d/%d/%s", 3, 0, 1, mPackType.toCdsString()  );
			mLogger.log( "uri: %s", uri );
			
			Uri baseUri = PackageManagerUtils.getCDSProviderContentUri( context, uri );
			mCursorLoader = new CursorLoader( context, baseUri, null, null, null, null );
			mCursorLoader.registerListener( 1, this );

			mContentObserver = new ContentObserver( new Handler() ) {
				@Override
				public void onChange( boolean selfChange ) {
					mLogger.info( "mContentObserver::onChange" );
					super.onChange( selfChange );

					if ( isActive() && null != mCursorLoader && mCursorLoader.isStarted() ) {
						mCursorLoader.onContentChanged();
					}
				}
			};
			context.getContentResolver().registerContentObserver( PackageManagerUtils.getCDSProviderContentUri( context, "packTray/" + mPackType.toCdsString() ), false,
					mContentObserver );
		}

		mCursorLoader.startLoading();
	}

	/**
	 * Creates and returns the default adapter for the frames listview
	 * 
	 * @param context
	 * @param result
	 * @return
	 */
	protected CursorAdapter createListAdapter( Context context, Cursor cursor ) {

		return new ListAdapter( context, R.layout.aviary_frame_item, R.layout.aviary_frame_item_more, R.layout.aviary_frame_item_external,
				R.layout.aviary_frame_item_divider, cursor );
	}

	/**
	 * @param result
	 *            containing all the {@link EffectPack} items ( external, internal,
	 *            dividers... )
	 * @param errors
	 *            contains all the error items
	 * @param firstValidIndex
	 *            the index of the first valid element
	 */
	private void onEffectListUpdated( Cursor cursor, int firstValidIndex ) {
		mLogger.info( "onEffectListUpdated: first valid index:" + firstValidIndex );

		long iapPackId = -1;

		// check if the incoming options bundle has some instructions
		if ( hasOptions() ) {
			Bundle options = getOptions();
			if ( options.containsKey( AviaryIntent.OptionBundle.SHOW_IAP_DIALOG ) ) {
				iapPackId = options.getLong( AviaryIntent.OptionBundle.SHOW_IAP_DIALOG );
			}
			// ok, we display the IAP dialog only the first time
			options.remove( AviaryIntent.OptionBundle.SHOW_IAP_DIALOG );
		}

		mListFirstValidPosition = firstValidIndex > 0 ? firstValidIndex : 0;

		if ( mFirstTime ) {
			mLoader.setVisibility( View.INVISIBLE );
			Animation animation = new AlphaAnimation( 0, 1 );
			animation.setFillAfter( true );
			animation.setDuration( getContext().getBaseContext().getResources().getInteger( android.R.integer.config_longAnimTime ) );

			if ( mListFirstValidPosition > 0 ) {
				mHList.setSelectionFromLeft( mListFirstValidPosition - 1, mCellWidth / 2 );
			}

			mHList.setVisibility( View.VISIBLE );
			mHList.startAnimation( animation );
		}

		mFirstTime = false;

		// display the iap dialog
		if ( iapPackId > -1 ) {
			displayIAPDialog( new IAPUpdater.Builder().setPackId( iapPackId ).setPackType( mPackType ).build() );
		}
	}

	// ///////////////
	// IAP - Dialog //
	// ///////////////

	private final void displayIAPDialog( IAPUpdater data ) {
		if ( null != mIapDialog ) {
			if ( mIapDialog.isValid() ) {
				mIapDialog.update( data );
				setApplyEnabled( false );
				return;
			} else {
				mIapDialog.dismiss( false );
				mIapDialog = null;
			}
		}

		IAPDialogMain dialog = IAPDialogMain.create( (FeatherContext) getContext().getBaseContext(), data );
		if ( dialog != null ) {
			dialog.setOnCloseListener( new OnCloseListener() {
				@Override
				public void onClose() {
					removeIapDialog();
				}
			} );
		}
		mIapDialog = dialog;
		setApplyEnabled( false );

		// TODO: add "Store: Opened" tracking event
	}

	private boolean removeIapDialog() {
		setApplyEnabled( true );
		if ( null != mIapDialog ) {
			mIapDialog.dismiss( true );
			mIapDialog = null;
			return true;
		}
		return false;
	}

	private void renderEffect( TrayColumns.TrayCursorWrapper item, int position ) {
		mLogger.info( "renderEffect: " + position );

		killCurrentTask();
		mCurrentTask = createRenderTask( position );
		mCurrentTask.execute( item );
	}

	protected RenderTask createRenderTask( int position ) {
		return new RenderTask( position );
	}

	boolean killCurrentTask() {
		if ( mCurrentTask != null ) {
			onProgressEnd();
			return mCurrentTask.cancel( true );
		}
		return false;
	}

	protected INativeFilter loadNativeFilter( final TrayColumns.TrayCursorWrapper item, int position, boolean hires ) throws JSONException {

		BorderFilter filter = (BorderFilter) FilterLoaderFactory.get( Filters.BORDERS );
		if ( null != item && position > -1 ) {
			Cursor cursor = getContext().getBaseContext().getContentResolver()
					.query( PackageManagerUtils.getCDSProviderContentUri( getContext().getBaseContext(), "pack/content/item/" + item.getId() ), null, null, null, null );
			double frameWidth = 0;
			try {
				if ( null != cursor ) {
					if ( cursor.moveToFirst() ) {
						byte[] options = cursor.getBlob( cursor.getColumnIndex( PacksItemsColumns.OPTIONS ) );
						JSONObject object = new JSONObject( new String( options ) );
						frameWidth = object.getDouble( "width" );
					}
				}
			} finally {
				IOUtils.closeSilently( cursor );
			}

			filter.setHiRes( hires );
			filter.setSize( frameWidth );
			filter.setIdentifier( item.getIdentifier() );
			filter.setSourceDir( item.getPath() );
		}

		return filter;
	}

	boolean backHandled() {
		if ( mIsAnimating ) return true;
		if ( null != mIapDialog ) {
			if ( mIapDialog.onBackPressed() ) return true;
			removeIapDialog();
			return true;
		}
		killCurrentTask();
		return false;
	}

	static class ViewHolder {
		protected TextView text;
		protected ImageView image;
		protected int type;
		protected long id;
		protected String identifier;
	}

	static class ViewHolderExternal extends ViewHolder {
		protected View badgeIcon;
		protected View externalIcon;
	}

	class ListAdapter extends CursorAdapter {

		static final int TYPE_INVALID = -1;
		static final int TYPE_LEFT_GETMORE = TrayColumns.TYPE_LEFT_GETMORE;
		static final int TYPE_RIGHT_GETMORE = TrayColumns.TYPE_RIGHT_GETMORE;
		static final int TYPE_NORMAL = TrayColumns.TYPE_CONTENT;
		static final int TYPE_EXTERNAL = TrayColumns.TYPE_PACK_EXTERNAL;
		static final int TYPE_DIVIDER = TrayColumns.TYPE_PACK_INTERNAL;
		static final int TYPE_LEFT_DIVIDER = TrayColumns.TYPE_LEFT_DIVIDER;
		static final int TYPE_RIGHT_DIVIDER = TrayColumns.TYPE_RIGHT_DIVIDER;

		Object mLock = new Object();
		LayoutInflater mInflater;
		int mDefaultResId;
		int mMoreResId;
		int mExternalResId;
		int mDividerResId;
		int mCount = -1;
		BitmapDrawable mExternalFolderIcon;

		int mIdColumnIndex;
		int mPackageNameColumnIndex;
		int mIdentifierColumnIndex;
		int mTypeColumnIndex;
		int mDisplayNameColumnIndex;
		int mPathColumnIndex;

		public ListAdapter ( Context context, int defaultResId, int moreResId, int externalResId, int dividerResId, Cursor cursor ) {
			super( context, cursor, 0 );
			initColumns( cursor );

			mInflater = LayoutInflater.from( context );

			mDefaultResId = defaultResId;
			mMoreResId = moreResId;
			mExternalResId = externalResId;
			mDividerResId = dividerResId;
			mExternalFolderIcon = getExternalBackgroundDrawable( context );
		}

		private void initColumns( Cursor cursor ) {
			if ( null != cursor ) {
				mIdColumnIndex = cursor.getColumnIndex( TrayColumns._ID );
				mPackageNameColumnIndex = cursor.getColumnIndex( TrayColumns.PACKAGE_NAME );
				mIdentifierColumnIndex = cursor.getColumnIndex( TrayColumns.IDENTIFIER );
				mTypeColumnIndex = cursor.getColumnIndex( TrayColumns.TYPE );
				mDisplayNameColumnIndex = cursor.getColumnIndex( TrayColumns.DISPLAY_NAME );
				mPathColumnIndex = cursor.getColumnIndex( TrayColumns.PATH );

				mLogger.log( "mIdColumnIndex: " + mIdColumnIndex );
				mLogger.log( "mPackageNameColumnIndex: " + mPackageNameColumnIndex );
				mLogger.log( "mIdentifierColumnIndex: " + mIdentifierColumnIndex );
				mLogger.log( "mTypeColumnIndex: " + mTypeColumnIndex );
				mLogger.log( "mDisplayNameColumnIndex: " + mDisplayNameColumnIndex );
				mLogger.log( "mPathColumnIndex: " + mPathColumnIndex );
			}
		}

		@Override
		public Cursor swapCursor( Cursor newCursor ) {
			mLogger.info( "swapCursor" );
			initColumns( newCursor );
			return super.swapCursor( newCursor );
		}

		@Override
		protected void onContentChanged() {
			super.onContentChanged();
			mLogger.error( "onContentChanged!!!!" );
		}

		protected BitmapDrawable getExternalBackgroundDrawable( Context context ) {
			return (BitmapDrawable) context.getResources().getDrawable( R.drawable.aviary_frames_pack_background );
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public int getViewTypeCount() {
			return 7;
		}

		@Override
		public int getItemViewType( int position ) {
			Cursor cursor = (Cursor) getItem( position );
			if ( null != cursor ) {
				return cursor.getInt( mTypeColumnIndex );
			}
			return TYPE_INVALID;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {
			if ( !mDataValid ) {
				throw new IllegalStateException( "this should only be called when the cursor is valid" );
			}

			View v;
			if ( convertView == null ) {
				v = newView( mContext, mCursor, parent, position );
			} else {
				v = convertView;
			}
			bindView( v, mContext, mCursor, position );
			return v;
		}

		private View newView( Context context, Cursor cursor, ViewGroup parent, int position ) {

			final int type = getItemViewType( position );

			View view;
			int layoutWidth;
			ViewHolder holder;

			switch ( type ) {
				case TYPE_LEFT_GETMORE:
					view = mInflater.inflate( mMoreResId, parent, false );
					layoutWidth = mCellWidth;
					break;

				case TYPE_RIGHT_GETMORE:
					view = mInflater.inflate( mMoreResId, parent, false );
					layoutWidth = mCellWidth;

					if ( parent.getChildCount() > 0 && mHList.getFirstVisiblePosition() == 0 ) {
						View lastView = parent.getChildAt( parent.getChildCount() - 1 );

						if ( lastView.getRight() < parent.getWidth() ) {
							view.setVisibility( View.INVISIBLE );
							layoutWidth = 1;
						}
					}

					break;

				case TYPE_DIVIDER:
					view = mInflater.inflate( mDividerResId, parent, false );
					layoutWidth = LayoutParams.WRAP_CONTENT;
					break;

				case TYPE_EXTERNAL:
					view = mInflater.inflate( mExternalResId, parent, false );
					layoutWidth = mCellWidth;
					break;

				case TYPE_LEFT_DIVIDER:
					view = mInflater.inflate( R.layout.aviary_thumb_divider_right, parent, false );
					layoutWidth = LayoutParams.WRAP_CONTENT;
					break;

				case TYPE_RIGHT_DIVIDER:
					view = mInflater.inflate( R.layout.aviary_thumb_divider_left, parent, false );
					layoutWidth = LayoutParams.WRAP_CONTENT;

					if ( parent.getChildCount() > 0 && mHList.getFirstVisiblePosition() == 0 ) {
						View lastView = parent.getChildAt( parent.getChildCount() - 1 );

						if ( lastView.getRight() < parent.getWidth() ) {
							view.setVisibility( View.INVISIBLE );
							layoutWidth = 1;
						}
					}
					break;

				case TYPE_NORMAL:
				default:
					view = mInflater.inflate( mDefaultResId, parent, false );
					layoutWidth = mCellWidth;
					break;
			}

			view.setLayoutParams( new LayoutParams( layoutWidth, LayoutParams.MATCH_PARENT ) );

			if ( type == TYPE_EXTERNAL ) {
				holder = new ViewHolderExternal();
				( (ViewHolderExternal) holder ).badgeIcon = view.findViewById( R.id.aviary_badge );
				( (ViewHolderExternal) holder ).externalIcon = view.findViewById( R.id.aviary_image2 );
			} else {
				holder = new ViewHolder();
			}

			holder.type = type;
			holder.image = (ImageView) view.findViewById( R.id.aviary_image );
			holder.text = (TextView) view.findViewById( R.id.aviary_text );

			if ( type != TYPE_DIVIDER && holder.image != null ) {
				LayoutParams params = holder.image.getLayoutParams();
				params.height = mThumbSize;
				params.width = mThumbSize;
				holder.image.setLayoutParams( params );
			}

			view.setTag( holder );
			return view;
		}

		void bindView( View view, Context context, Cursor cursor, int position ) {
			final ViewHolder holder = (ViewHolder) view.getTag();
			String displayName;
			String identifier;
			String path;
			Generator executor;
			long id = -1;

			if ( !cursor.isAfterLast() && !cursor.isBeforeFirst() ) {
				id = cursor.getLong( mIdColumnIndex );
			}

			if ( holder.type == TYPE_NORMAL ) {
				displayName = cursor.getString( mDisplayNameColumnIndex );
				identifier = cursor.getString( mIdentifierColumnIndex );
				path = cursor.getString( mPathColumnIndex );

				holder.text.setText( displayName );
				holder.identifier = identifier;

				if ( holder.id != id ) {
					final String file;
					
					if( mPackType == PackType.EFFECT ) {
						file = path + "/" + identifier + ".json";
					} else {
						file = path + "/" + identifier + "-small.png";
					}
					
					executor = createContentCallable( position, position, identifier, path );
					mPicassoLibrary
						.load( Uri.parse( "custom.resource://" + file ) )
						.fade( 200 )
						.error( R.drawable.aviary_ic_na )
						.withCache( mCache )
						.withGenerator( (Generator)executor )
						.into( holder.image );
					
				}
			} else if ( holder.type == TYPE_EXTERNAL ) {
				identifier = cursor.getString( mIdentifierColumnIndex );
				displayName = cursor.getString( mDisplayNameColumnIndex );
				String icon = cursor.getString( mPathColumnIndex );

				holder.text.setText( displayName );
				holder.identifier = identifier;

				if ( mBadgeService.getIsActive( identifier ) ) {
					( (ViewHolderExternal) holder ).badgeIcon.setVisibility( View.VISIBLE );
					( (ViewHolderExternal) holder ).externalIcon.setVisibility( View.GONE );
				} else {
					( (ViewHolderExternal) holder ).badgeIcon.setVisibility( View.GONE );
					( (ViewHolderExternal) holder ).externalIcon.setVisibility( View.VISIBLE );
				}

				if ( holder.id != id ) {
					mPicassoLibrary
						.load( icon )
						.transform( new PackIconCallable( getContext().getBaseContext().getResources(), mPackType, icon ) )
						.noFade()
						.error( R.drawable.aviary_ic_na )
						.into( holder.image );
				}

			} else if ( holder.type == TYPE_DIVIDER ) {
				Drawable drawable = holder.image.getDrawable();
				displayName = cursor.getString( mDisplayNameColumnIndex );

				if ( drawable instanceof PluginDividerDrawable ) {
					( (PluginDividerDrawable) drawable ).setTitle( displayName );
				} else {
					PluginDividerDrawable d = new PluginDividerDrawable( getContext().getBaseContext(), R.attr.aviaryEffectThumbDividerTextStyle, displayName );
					holder.image.setImageDrawable( d );
				}
			}

			holder.id = id;
		}

		@Override
		public View newView( Context arg0, Cursor arg1, ViewGroup arg2 ) {
			return null;
		}

		@Override
		public void bindView( View arg0, Context arg1, Cursor arg2 ) {}

		protected Generator createContentCallable( long id, int position, String identifier, String path ) {
			if ( null != identifier ) {
				return new BorderThumbnailCallable(mThumbBitmap);
			}
			return null;
		}
	}

	// ////////////////////////
	// OnItemClickedListener //
	// ////////////////////////

	@Override
	public void onItemClick( it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id ) {
		mLogger.error( "onItemClick: " + position );

		int checkedItems = mHList.getCheckedItemCount();
		mLogger.log( "checked items: " + checkedItems );

		if ( isActive() ) {
			ViewHolder holder = (ViewHolder) view.getTag();

			if ( null != holder ) {

				if ( holder.type == ListAdapter.TYPE_LEFT_GETMORE || holder.type == ListAdapter.TYPE_RIGHT_GETMORE ) {

					if ( holder.type == ListAdapter.TYPE_LEFT_GETMORE ) {
						Tracker.recordTag( "(" + mPackType.toCdsString() + ") LeftSupplyShop: Clicked" );
					} else if ( holder.type == ListAdapter.TYPE_RIGHT_GETMORE ) {
						Tracker.recordTag( "(" + mPackType.toCdsString() + ") RightSupplyShop: Clicked" );
					}

					displayIAPDialog( new IAPUpdater.Builder().setPackType( mPackType ).build() );

				} else if ( holder.type == ListAdapter.TYPE_EXTERNAL ) {

					ViewHolderExternal holder_ext = (ViewHolderExternal) holder;
					if ( null != holder_ext ) {
						holder_ext.badgeIcon.setVisibility( View.GONE );
						holder_ext.externalIcon.setVisibility( View.VISIBLE );
					}

					displayIAPDialog( new IAPUpdater.Builder().setPackId( holder.id ).setPackType( mPackType ).build() );

					if ( position > 0 ) {

						if ( mHList.getChildCount() > 0 ) {
							int left = view.getLeft();
							int right = view.getRight();
							int center = ( ( right - left ) / 2 + left );
							final int delta = mHList.getWidth() / 2 - center;

							mLogger.log( "delta: " + delta );

							mHList.postDelayed( new Runnable() {

								@Override
								public void run() {
									mHList.smoothScrollBy( -delta, 500 );
								}
							}, 300 );
						}
					}

				} else {
					removeIapDialog();

					// type normal
					mHList.clearChoices();
					if ( checkedItems > 0 ) {
						mHList.setItemChecked( position, true );

						Cursor cursor = (Cursor) mAdapter.getItem( position );
						if ( null != cursor ) {
							TrayColumns.TrayCursorWrapper item = TrayColumns.TrayCursorWrapper.create( cursor );
							if ( null != item ) {
								renderEffect( item, position );
							}
						}
					} else {
						renderEffect( null, -1 );
					}
				}
			}
		}

		// mHList.setItemChecked( position, true );
	}

	// /////////////////////////
	// OnItemSelectedListener //
	// /////////////////////////

	@Override
	public void onItemSelected( it.sephiroth.android.library.widget.AdapterView<?> arg0, View arg1, int arg2, long arg3 ) {
		mLogger.error( "onItemSelected: TODO" );
	}

	@Override
	public void onNothingSelected( it.sephiroth.android.library.widget.AdapterView<?> parent ) {
		mLogger.info( "onNothingSelected" );
	}

	static class BorderThumbnailCallable implements Generator {

		Bitmap mBitmap;

		public BorderThumbnailCallable (final Bitmap bitmap) {
			mBitmap = bitmap;
		}

		@Override
		public Bitmap decode(Uri uri) throws java.io.IOException {
			Log.d( "BordersPanel", "load thumbnail: " + uri );

			Bitmap bitmap = BitmapFactory.decodeFile( uri.getPath() );
			
			if( null == bitmap ) {
				throw new IOException( "null bitmap" );
			}

			MoaActionList actions = actionsForRoundedThumbnail( true, null );
			MoaResult mResult;
			
			try {
				mResult = NativeFilterProxy.prepareActions( actions, bitmap, null, 1, 1 );
				mResult.execute();
			} catch( Exception e ) {
				throw new IOException( e );
			}

			if ( mResult.outputBitmap != bitmap ) {
				bitmap.recycle();
			}

			bitmap = mResult.outputBitmap;
			return bitmap;
		}

		MoaActionList actionsForRoundedThumbnail( final boolean isValid, INativeFilter filter ) {

			MoaActionList actions = MoaActionFactory.actionList();
			if ( null != filter ) {
				actions.addAll( filter.getActions() );
			}

			MoaAction action = MoaActionFactory.action( "ext-roundedborders" );
			action.setValue( "padding", 0 );
			action.setValue( "roundPx", 0 );
			action.setValue( "strokeColor", 0xff000000 );
			action.setValue( "strokeWeight", 1 );

			if ( !isValid ) {
				action.setValue( "overlaycolor", 0x99000000 );
			}

			actions.add( action );
			return actions;
		}
	}

	protected CharSequence[] getOptionalEffectsValues() {
		return new CharSequence[] { "original" };
	}

	protected CharSequence[] getOptionalEffectsLabels() {
		if ( null != mConfigService ) {
			return new CharSequence[] { mConfigService.getString( R.string.feather_original ) };
		} else {
			return new CharSequence[] { "Original" };
		}
	}

	class EffectPackError {

		CharSequence mPackageName;
		CharSequence mLabel;
		int mError;
		String mErrorMessage;

		public EffectPackError ( CharSequence packagename, CharSequence label, int error, String errorString ) {
			mPackageName = packagename;
			mLabel = label;
			mError = error;
			mErrorMessage = errorString;
		}
	}

	static class EffectPack {

		static enum EffectPackType {
			INTERNAL, EXTERNAL, PACK_DIVIDER, LEFT_DIVIDER, RIGHT_DIVIDER, GET_MORE
		};

		CharSequence mPackageName;
		List<Pair<String, String>> mValues;
		List<Long> mIds;
		CharSequence mTitle;
		int mStatus;
		String mError;
		int size = 0;
		int index = 0;
		EffectPackType mType;

		public EffectPack ( EffectPackType type ) {
			mType = type;
			size = 1;
		}

		public EffectPack ( EffectPackType type, final String label ) {
			this( type );
			mTitle = label;
		}

		public EffectPack ( EffectPackType type, CharSequence packageName, CharSequence pakageTitle, List<Pair<String, String>> values, List<Long> ids,
				int status, String errorMsg ) {
			this( type );
			mPackageName = packageName;
			mStatus = status;
			mTitle = pakageTitle;
			mValues = values;
			mIds = ids;
			mError = errorMsg;

			if ( null != values ) {
				size = values.size();
			} else {
				size = 1;
			}
		}

		public int getCount() {
			return size;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex( int value ) {
			index = value;
		}

		public CharSequence getItemAt( int position ) {
			return mValues.get( position - index ).first;
		}

		public long getItemIdAt( int position ) {
			if ( null != mIds ) return mIds.get( position - index );
			return -1;
		}

		public CharSequence getLabelAt( int position ) {
			return mValues.get( position - index ).second;
		}

		public int getItemIndex( int position ) {
			return position - index;
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
		}
	}

	/**
	 * Render the selected effect
	 */
	protected class RenderTask extends AviaryAsyncTask<TrayColumns.TrayCursorWrapper, Bitmap, Bitmap> implements OnCancelListener {

		int mPosition;
		String mError;
		MoaResult mMoaMainExecutor;

		/**
		 * Instantiates a new render task.
		 * 
		 * @param tag
		 */
		public RenderTask ( final int position ) {
			mPosition = position;
		}

		@Override
		protected void PreExecute() {
			onProgressStart();
		}

		private INativeFilter initFilter( TrayColumns.TrayCursorWrapper item, int position ) {
			final INativeFilter filter;

			try {
				filter = loadNativeFilter( item, position, true );
			} catch ( Throwable t ) {
				t.printStackTrace();
				return null;
			}

			mActions = (MoaActionList) filter.getActions().clone();

			if ( filter instanceof BorderFilter ) ( (BorderFilter) filter ).setHiRes( false );

			try {
				mMoaMainExecutor = filter.prepare( mBitmap, mPreview, 1, 1 );
			} catch ( JSONException e ) {
				e.printStackTrace();
				mMoaMainExecutor = null;
				return null;
			}
			return filter;
		}

		protected MoaResult initPreview( INativeFilter filter ) {
			return null;
		}

		public void doFullPreviewInBackground() {
			mMoaMainExecutor.execute();
		}

		@Override
		public Bitmap doInBackground( final TrayColumns.TrayCursorWrapper... params ) {

			if ( isCancelled() ) return null;

			final TrayColumns.TrayCursorWrapper item = params[0];
			mRenderedEffect = item;

			initFilter( item, mPosition );

			mIsRendering = true;

			if ( isCancelled() ) return null;

			// rendering the full preview
			try {
				doFullPreviewInBackground();
			} catch ( Exception exception ) {
				mError = exception.getMessage();
				exception.printStackTrace();
				return null;
			}

			if ( !isCancelled() ) {
				return mMoaMainExecutor.outputBitmap;
			} else {
				return null;
			}
		}

		@Override
		public void PostExecute( final Bitmap result ) {

			if ( !isActive() ) return;

			mPreview = result;

			if ( result == null || mMoaMainExecutor == null || mMoaMainExecutor.active == 0 ) {

				onRestoreOriginalBitmap();

				if ( mError != null ) {
					onGenericError( mError, android.R.string.ok, null );
				}

				setIsChanged( false );
				mActions = null;

			} else {
				onApplyNewBitmap( result );

				if ( null != mRenderedEffect && null != mRenderedEffect ) {
					HashMap<String, String> attrs = new HashMap<String, String>();
					attrs.put( "Pack", mRenderedEffect.getPackageName() );
					attrs.put( "Effect", mRenderedEffect.getIdentifier() );
					Tracker.recordTag( "EffectPreview: selected", attrs );
				}
			}

			onProgressEnd();

			mIsRendering = false;
			mCurrentTask = null;
		}

		protected void onApplyNewBitmap( final Bitmap result ) {
			if ( SystemUtils.isHoneyComb() ) {
				Moa.notifyPixelsChanged( result );
			}
			onPreviewChanged( result, false, true );
			setIsChanged( true );
		}

		protected void onRestoreOriginalBitmap() {
			// restore the original bitmap...

			onPreviewChanged( mBitmap, true, true );
			setIsChanged( false );
		}

		@Override
		public void onCancelled() {
			super.onCancelled();

			if ( mMoaMainExecutor != null ) {
				mMoaMainExecutor.cancel();
			}
			mIsRendering = false;
		}

		@Override
		public void onCancel( DialogInterface dialog ) {
			cancel( true );
		}
	}

	/**
	 * Used to generate the Bitmap result. If user clicks on the "Apply" button when an
	 * effect is still rendering, then starts this
	 * task.
	 */
	class GenerateResultTask extends AviaryAsyncTask<Void, Void, Void> {

		ProgressDialog mProgress = new ProgressDialog( getContext().getBaseContext() );

		@Override
		protected void PreExecute() {
			mProgress.setTitle( getContext().getBaseContext().getString( R.string.feather_loading_title ) );
			mProgress.setMessage( getContext().getBaseContext().getString( R.string.feather_effect_loading_message ) );
			mProgress.setIndeterminate( true );
			mProgress.setCancelable( false );
			mProgress.show();
		}

		@Override
		protected Void doInBackground( Void... params ) {

			mLogger.info( "GenerateResultTask::doInBackground", mIsRendering );

			while ( mIsRendering ) {
				mLogger.log( "waiting...." );
			}

			return null;
		}

		@Override
		protected void PostExecute( Void result ) {
			if ( getContext().getBaseActivity().isFinishing() ) return;
			if ( mProgress.isShowing() ) mProgress.dismiss();

			onComplete( mPreview, mActions );
		}
	}

}
