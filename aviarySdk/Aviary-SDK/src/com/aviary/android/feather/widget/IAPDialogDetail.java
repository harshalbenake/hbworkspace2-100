package com.aviary.android.feather.widget;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.widget.CursorAdapter;
import android.text.Html;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aviary.android.feather.R;
import com.aviary.android.feather.cds.AviaryCds;
import com.aviary.android.feather.cds.AviaryCds.ContentType;
import com.aviary.android.feather.cds.AviaryCds.PackType;
import com.aviary.android.feather.cds.AviaryCdsDownloaderFactory;
import com.aviary.android.feather.cds.AviaryCdsDownloaderFactory.Downloader;
import com.aviary.android.feather.cds.AviaryCdsValidatorFactory;
import com.aviary.android.feather.cds.AviaryCdsValidatorFactory.Validator;
import com.aviary.android.feather.cds.CdsUtils;
import com.aviary.android.feather.cds.CdsUtils.PackOption;
import com.aviary.android.feather.cds.CdsUtils.Resolution;
import com.aviary.android.feather.cds.IAPWrapper;
import com.aviary.android.feather.cds.PacksColumns;
import com.aviary.android.feather.cds.PacksColumns.PackCursorWrapper;
import com.aviary.android.feather.cds.PacksContentColumns;
import com.aviary.android.feather.cds.PacksItemsColumns;
import com.aviary.android.feather.cds.billing.util.IabHelper;
import com.aviary.android.feather.cds.billing.util.IabResult;
import com.aviary.android.feather.common.AviaryIntent;
import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.common.utils.PackageManagerUtils;
import com.aviary.android.feather.common.utils.SystemUtils;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.graphics.CdsPreviewTransformer;
import com.aviary.android.feather.library.Constants;
import com.aviary.android.feather.library.services.BadgeService;
import com.aviary.android.feather.library.services.IAPService;
import com.aviary.android.feather.utils.PackIconCallable;
import com.aviary.android.feather.widget.AviaryWorkspace.OnPageChangeListener;
import com.aviary.android.feather.widget.CellLayout.CellInfo;
import com.aviary.android.feather.widget.IAPDialogMain.IAPUpdater;
import com.aviary.android.feather.widget.IAPDialogMain.PackOptionWithPrice;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class IAPDialogDetail extends LinearLayout implements OnPageChangeListener, OnClickListener {

	// listen to pack purchase status change
	ContentObserver mPackPurchasedContentObserver = new ContentObserver( new Handler() ) {
		@Override
		public void onChange( boolean selfChange ) {
			onChange( selfChange, null );
		}

		@Override
		public void onChange( boolean selfChange, Uri uri ) {
			logger.info( "** mPackPurchasedContentObserver::onChange **" );
			if ( !isValidContext() || null == mData || null == mPack || null == mPack.getContent() ) return;

			if ( null != uri ) {
				int purchased = Integer.parseInt( uri.getLastPathSegment() );
				long packId = Integer.parseInt( uri.getPathSegments().get( uri.getPathSegments().size() - 2 ) );

				logger.log( "purchased status changed(%d) for packId: %d", purchased, packId );

				if ( null != mPriceMap ) {
					mPriceMap.put( packId, new PackOptionWithPrice( purchased == 1 ? PackOption.OWNED : PackOption.ERROR ) );
				}
			}

			determinePackOption( mPack, true );
		};
	};

	ContentObserver mPackContentObserver = new ContentObserver( new Handler() ) {

		@Override
		public void onChange( boolean selfChange ) {
			onChange( selfChange, null );
		};

		@Override
		public void onChange( boolean selfChange, Uri uri ) {
			logger.info( "** mPackContentObserver::onChange **" );
			if ( !isValidContext() || null == mPack || null == mPack.getContent() ) return;
			updatePlugin( true );
		}
	};

	private IAPUpdater mData;
	private PacksColumns.PackCursorWrapper mPack;

	private IAPDialogMain mParent;
	private HashMap<Long, PackOptionWithPrice> mPriceMap;

	private int mMainLayoutResId = R.layout.aviary_iap_workspace_screen_stickers;
	private int mCellResId = R.layout.aviary_iap_cell_item_effects;

	private View mErrorView;
	private TextView mErrorText;
	private TextView mRetryButton;
	private View mLoader;
	private AviaryTextView mTitle, mDescription;
	private IAPBuyButton mButtonContainer;
	private AviaryWorkspace mWorkspace;
	private AviaryWorkspaceIndicator mWorkspaceIndicator;
	private ImageView mIcon;
	private View mHeadView;

	private IAPService mIapService;
	private Picasso mPicassoLibrary;

	private WorkspaceAdapter mWorkspaceAdapter;

	private View mBannerView;

	private boolean mDownloadOnDemand = true;
	private boolean mAttached;

	// workspace attributes
	int mRows = 1;
	int mCols = 1;
	int mItemsPerPage;

	private static Logger logger = LoggerFactory.getLogger( "iap-dialog-detail", LoggerType.ConsoleLoggerType );

	public IAPDialogDetail ( Context context, AttributeSet attrs ) {
		super( context, attrs );
		mDownloadOnDemand = SystemUtils.getApplicationTotalMemory() < Constants.APP_MEMORY_LARGE;
	}

	public IAPUpdater getData() {
		return mData;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		logger.info( "onAttachedFromWindow" );

		mAttached = true;

		mPicassoLibrary = Picasso.with( getContext() );

		mIcon = (ImageView) findViewById( R.id.aviary_icon );

		mButtonContainer = (IAPBuyButton) findViewById( R.id.aviary_buy_button );
		mHeadView = findViewById( R.id.aviary_head );
		mTitle = (AviaryTextView) findViewById( R.id.aviary_title );
		mDescription = (AviaryTextView) findViewById( R.id.aviary_description );
		mWorkspace = (AviaryWorkspace) findViewById( R.id.aviary_workspace );
		mWorkspaceIndicator = (AviaryWorkspaceIndicator) findViewById( R.id.aviary_workspace_indicator );
		mLoader = findViewById( R.id.aviary_progress );
		mBannerView = findViewById( R.id.aviary_banner_view );

		mErrorView = findViewById( R.id.aviary_error_message );

		if ( null != mErrorView ) {
			mErrorText = (TextView) mErrorView.findViewById( R.id.aviary_retry_text );
			mRetryButton = (TextView) mErrorView.findViewById( R.id.aviary_retry_button );

			if ( null != mRetryButton ) {
				if ( !isInEditMode() ) mRetryButton
						.setText( Html.fromHtml( String.format( getContext().getResources().getString( R.string.feather_try_again ) ) ) );
				mRetryButton.setOnClickListener( this );
			}
		}

		mButtonContainer.setOnClickListener( this );
		
		if( PackageManagerUtils.isStandalone( getContext() ) ) {
			mBannerView.setOnClickListener( this );
		} else {
			mBannerView.setVisibility( View.GONE );
		}

		mWorkspaceAdapter = new WorkspaceAdapter( getContext(), null, -1, null );
		mWorkspace.setAdapter( mWorkspaceAdapter );
		mWorkspace.setIndicator( mWorkspaceIndicator );
	}

	private void computeLayoutItems( Resources res, String packType ) {
		if ( AviaryCds.PACKTYPE_EFFECT.equals( packType ) || AviaryCds.PACKTYPE_FRAME.equals( packType ) ) {
			mMainLayoutResId = R.layout.aviary_iap_workspace_screen_effects;
			mCols = res.getInteger( R.integer.aviary_iap_dialog_cols_effects );
			mRows = res.getInteger( R.integer.aviary_iap_dialog_rows_effects );
			mCellResId = R.layout.aviary_iap_cell_item_effects;
		} else {
			mMainLayoutResId = R.layout.aviary_iap_workspace_screen_stickers;
			mCols = res.getInteger( R.integer.aviary_iap_dialog_cols_stickers );
			mRows = res.getInteger( R.integer.aviary_iap_dialog_rows_stickers );
			mCellResId = R.layout.aviary_iap_cell_item_stickers;
		}
		mItemsPerPage = mRows * mCols;
	}

	@Override
	protected void onDetachedFromWindow() {
		logger.info( "onDetachedFromWindow" );

		super.onDetachedFromWindow();

		// unregister content observer
		unregisterContentObservers();

		mButtonContainer.setOnClickListener( null );
		mRetryButton.setOnClickListener( null );
		mWorkspace.setTag( null );
		mWorkspaceAdapter.changeCursor( null );
		mWorkspace.setAdapter( null );
		mWorkspace.setOnPageChangeListener( null );
		mIapService = null;
		mAttached = false;
	}

	@Override
	public void onClick( View v ) {

		final int id = v.getId();

		if ( id == mBannerView.getId() ) {
			Intent intent = new Intent( Intent.ACTION_VIEW );
			intent.setComponent( AviaryIntent.getTutorialComponent( getContext() ) );
			intent.setData( Uri.parse( "aviary://launch-activity/iap_tutorial" ) );
			try {
				getContext().startActivity( intent );
			} catch ( ActivityNotFoundException e ) {
				e.printStackTrace();
			}
		}

		else if ( id == mRetryButton.getId() ) {
			logger.info( "retry" );
			update( getData(), mParent );

		} else if ( id == mButtonContainer.getId() ) {
			PackOptionWithPrice packOption = mButtonContainer.getPackOption();
			if ( null != packOption ) {
				switch ( packOption.option ) {
					case PURCHASE:
						if ( null != mPack && null != mPack.getContent() ) {
							mParent.launchPackPurchaseFlow( mPack.getId(), mPack.getIdentifier(), mPack.getPackType(), "DetailView", packOption.price );
						}
						break;

					case ERROR:
						if ( null != mPriceMap ) mPriceMap.remove( mPack.getId() );
						updatePlugin( false );
						break;

					case FREE:
					case RESTORE:
						IAPDialogMain.trackBeginPurchaseFlow( getContext(), mPack.getIdentifier(), mPack.getPackType(), "DetailView", packOption.price,
								packOption.option == PackOption.RESTORE, packOption.option == PackOption.FREE );

					case DOWNLOAD_ERROR:
						PackOptionWithPrice newOption;
						
						if( packOption.option == PackOption.FREE ) {
							mParent.sendReceipt( mPack.getIdentifier(), true, false );
						} else if( packOption.option == PackOption.RESTORE ){
							mParent.sendReceipt( mPack.getIdentifier(), false, true );
						}

						try {
							mParent.requestPackDownload( mPack.getId() );
							Pair<PackOption, String> pair = CdsUtils.getPackOptionDownloadStatus( getContext(), mPack.getId() );
							if( null != pair ) {
								newOption = new PackOptionWithPrice( pair.first );
							} else {
								newOption = new PackOptionWithPrice( PackOption.DOWNLOADING );
							}
							
						} catch( Throwable t ) {
							newOption = new PackOptionWithPrice( PackOption.DOWNLOAD_ERROR, null );
							
							StringBuilder sb = new StringBuilder();
							sb.append( getContext().getString( R.string.feather_download_start_failed ) );
							sb.append( "." );
							sb.append( "\n" );
							sb.append( "Cause: " );
							sb.append( t.getLocalizedMessage() );			
							
							new AlertDialog.Builder( getContext() )
								.setTitle( R.string.feather_iap_download_failed )
								.setMessage( sb.toString() )
								.setPositiveButton( android.R.string.cancel, null )
								.create()
								.show();
						}
						
						onPackOptionUpdated( newOption, mPack );
						break;

					default:
						break;
				}
			}
		}
	}

	// TODO: implement this better!
	void onDownloadStatusChanged( Uri uri ) {
		if ( !isValidContext() || null == mPack || null == mData ) return;
		logger.info( "** onDownloadStatusChanged: %s **", uri );
		determinePackOption( mPack, false );
	}

	void onPurchaseSuccess( long packId, String identifier, String packType ) {
		logger.info( "onPurchaseSuccess: %s - %s", identifier, packType );
		updatePlugin( false );
	}

	private void initWorkspace( PacksColumns.PackCursorWrapper pack ) {
		if ( null != pack && null != getContext() ) {

			Long oldTag = (Long) mWorkspace.getTag();
			if ( null != oldTag && oldTag == pack.getId() ) {
				logger.warn( "ok, don't reload the workspace, same tag found" );
				return;
			}

			Cursor cursor = getContext().getContentResolver().query(
					PackageManagerUtils.getCDSProviderContentUri( getContext(), "pack/" + pack.getId() + "/item/list" ),
					new String[] { PacksItemsColumns._ID + " as _id", PacksColumns.PACK_TYPE, PacksItemsColumns._ID, PacksItemsColumns.IDENTIFIER,
							PacksItemsColumns.DISPLAY_NAME }, null, null, null );

			mWorkspaceAdapter.setBaseDir( pack.getContent().getPreviewPath() );
			mWorkspaceAdapter.setFileExt( AviaryCds.getPreviewItemExt( mData.getPackType() ) );
			mWorkspaceAdapter.changeCursor( cursor );
			mWorkspace.setOnPageChangeListener( this );
			mWorkspace.setTag( Long.valueOf( pack.getId() ) );

			mLoader.setVisibility( View.GONE );

			if ( null == cursor || cursor.getCount() <= mItemsPerPage ) {
				mWorkspaceIndicator.setVisibility( View.INVISIBLE );
			} else {
				mWorkspaceIndicator.setVisibility( View.VISIBLE );
			}
		} else {
			logger.error( "invalid plugin" );
			mWorkspaceAdapter.changeCursor( null );
			mWorkspace.setTag( null );
			mWorkspace.setOnPageChangeListener( null );
			mWorkspaceIndicator.setVisibility( View.INVISIBLE );
		}
	}

	public void update( IAPUpdater updater, IAPDialogMain parent ) {
		if ( null == updater ) return;

		boolean forceUpdate = true;

		if ( updater.equals( mData ) ) {
			forceUpdate = false;
		}
		
		logger.info( "update: %s, forceUpdate: %b", updater, forceUpdate );

		mData = (IAPUpdater) updater.clone();
		mParent = parent;
		mPriceMap = mParent.getPriceMap( mData.getPackType() );
		
		mPack = null;

		if ( null != mParent.getController() ) {
			mIapService = mParent.getController().getService( IAPService.class );
		}
		
		registerContentObserver( mData.getPackId() );

		// initialize the display process
		if ( forceUpdate ) {
			processPlugin();
		} else {
			updatePlugin( true );
		}
	}

	private void registerContentObserver( long pack_id ) {
		logger.info( "registerContentObserver" );
		if ( null != getContext() ) {
			getContext().getContentResolver().registerContentObserver( PackageManagerUtils.getCDSProviderContentUri( getContext(), "pack/contentUpdated/" + pack_id ), false,
					mPackContentObserver );
			getContext().getContentResolver().registerContentObserver( PackageManagerUtils.getCDSProviderContentUri( getContext(), "pack/purchased/" + pack_id ), true,
					mPackPurchasedContentObserver );
		}
	}

	private void unregisterContentObservers() {
		logger.info( "unregisterContentObservers" );
		if ( null != getContext() ) {
			getContext().getContentResolver().unregisterContentObserver( mPackContentObserver );
			getContext().getContentResolver().unregisterContentObserver( mPackPurchasedContentObserver );
		}
	}

	/**
	 * Error downloading plugin informations
	 */
	private void onDownloadError() {
		logger.info( "onDownloadError" );

		mErrorView.setVisibility( View.VISIBLE );

		mLoader.setVisibility( View.GONE );
		mButtonContainer.setVisibility( View.GONE );
		mWorkspaceAdapter.changeCursor( null );
		mWorkspace.setTag( null );
		mTitle.setText( "" );

		if ( null != mErrorText ) {
			mErrorText.setText( R.string.feather_item_not_found );
		}
	}

	private void onDownloadPreviewError( String message ) {
		mErrorView.setVisibility( View.VISIBLE );

		mLoader.setVisibility( View.GONE );
		mWorkspaceAdapter.changeCursor( null );
		mWorkspace.setTag( null );

		if ( null != mErrorText ) {
			mErrorText.setText( R.string.feather_iap_failed_download_previews );
		}
	}

	private void updatePlugin( boolean update ) {
		if ( !isValidContext() || null == mData ) return;
		
		logger.info( "updatePlugin: " + update );

		if ( update || mPack == null || null == mPack.getContent() ) {
			mPack = CdsUtils.getPackFullInfoById( getContext(), mData.getPackId() );

			if ( null == mPack || null == mPack.getContent() ) {
				logger.error( "pack or content are null" );
				onDownloadError();
				return;
			}
		}

		mButtonContainer.setVisibility( View.VISIBLE );
		mWorkspaceIndicator.setVisibility( View.INVISIBLE );
		mTitle.setText( mPack.getContent().getDisplayName() );
		mTitle.setSelected( true );
		
		mDescription.setText( mPack.getContent().getDisplayDescription() != null ? mPack.getContent().getDisplayDescription() : "" );

		int delayTime = getResources().getInteger( android.R.integer.config_mediumAnimTime ) + 300;
		
		Handler handler = getHandler();
		
		if( null != handler ) {
			
			// download the pack icon, if necessary
			downloadPackIcon( mPack, mPack.getContent() );
			
			handler.postDelayed( new Runnable() {
				
				@Override
				public void run() {
					if( null != mPack && null != mPack.getContent() ) {
						// update price button
						determinePackOption( (PacksColumns.PackCursorWrapper) mPack.clone(), false );
						
						// download the pack previews
						downloadPackPreviews( mPack );
					}
					
				}
			}, delayTime );
		}
	}

	/**
	 * display and download informations
	 * about the selected pack
	 */
	private void processPlugin() {
		logger.info( "processPlugin" );

		if ( !isValidContext() || null == mData ) return;

		// invalidate current UI
		mIcon.setTag( null );
		mIcon.setImageBitmap( null );

		mButtonContainer.setPackOption( new PackOptionWithPrice( PackOption.PACK_OPTION_BEING_DETERMINED ), -1 );
		
		mErrorView.setVisibility( View.GONE );

		// invalidate the workspace
		mWorkspace.setTag( null );
		mWorkspaceAdapter.changeCursor( null );

		// load the pack informations

		mPack = CdsUtils.getPackFullInfoById( getContext(), mData.getPackId() );

		if ( null == mPack || null == mPack.getContent() ) {
			logger.error( "pack or content are null" );
			onDownloadError();
			return;
		}

		// mark pack as 'read'
		if ( null != mParent.getController() ) {
			BadgeService badgeService = mParent.getController().getService( BadgeService.class );
			if ( null != badgeService ) {
				badgeService.markAsRead( mPack.getIdentifier() );
			}
		}

		// update workspace
		computeLayoutItems( getContext().getResources(), mPack.getPackType() );
		mWorkspaceAdapter.setContext( getContext() );
		mWorkspaceAdapter.setResourceId( mMainLayoutResId );
		mWorkspaceAdapter.setBaseDir( null );

		// update plugin content
		updatePlugin( false );

		if ( null != mHeadView ) {
			mHeadView.requestFocus();
			mHeadView.requestFocusFromTouch();
		}
	}

	/**
	 * Update the price button
	 * 
	 * @param option
	 * @param pack
	 */
	private void onPackOptionUpdated( PackOptionWithPrice option, final PacksColumns.PackCursorWrapper pack ) {
		if ( !isValidContext() || null == pack || null == pack.getContent() ) return;
		if ( !pack.equals( mPack ) ) return;

		logger.log( "onPackOptionUpdated: %s, packId: %d", option, pack.getId() );
		mButtonContainer.setPackOption( option, pack.getId() );
	}


	private void determinePackOption( PacksColumns.PackCursorWrapper pack, boolean update ) {
		logger.info( "determinePackOption: " + update );
		
		if( null == pack ) {
			logger.error( "pack is null" );
			return;
		}

		Pair<PackOption, String> pair = CdsUtils.getPackOptionDownloadStatus( getContext(), pack.getId() );
		PackOptionWithPrice downloadStatus = null;

		if ( null != pair ) {
			downloadStatus = new PackOptionWithPrice( pair.first );
			onPackOptionUpdated( downloadStatus, pack );

			if ( downloadStatus.option != PackOption.DOWNLOAD_COMPLETE ) {
				return;
			}
		}

		if ( update ) {
			new DeterminePackOptionAsyncTask( update ).execute( pack );
		} else {
			PackOptionWithPrice newResult = mPriceMap.get( pack.getId() );
			if( null != newResult ) {
				if( null != downloadStatus ) {
					if ( downloadStatus.option == PackOption.DOWNLOAD_COMPLETE
							&& ( newResult.option == PackOption.RESTORE || newResult.option == PackOption.FREE ) ) {
						// special case ( still installing )
						newResult = downloadStatus;
					}
				}
				onPackOptionUpdated( newResult, pack );
			} else {
				new DeterminePackOptionAsyncTask( update ).execute( pack );
			}
		}

	}

	/**
	 * Fetch the current pack icon
	 * 
	 * @param plugin
	 */
	private void downloadPackIcon( PacksColumns.PackCursorWrapper pack, PacksContentColumns.ContentCursorWrapper plugin ) {
		logger.info( "downloadPackIcon" );

		if ( null != plugin ) {
			if ( null != mIcon ) {

				String iconPath = plugin.getIconPath();
				String tag = (String) mIcon.getTag();
				if ( null != tag ) {
					if ( tag.equals( plugin.getIconURL() ) ) {
						logger.warn( "icon already downloaded!" );
						return;
					}
				}
				
				mPicassoLibrary
					.load( iconPath )
					.error( R.drawable.aviary_ic_na )
					.resizeDimen( R.dimen.aviary_iap_detail_icon_maxsize, R.dimen.aviary_iap_detail_icon_maxsize, true )
					.transform( new PackIconCallable( getResources(), mData.getPackType(), iconPath ) )
					.into( mIcon );
					
			}
		}
	}

	private void downloadPackPreviews( PacksColumns.PackCursorWrapper pack ) {
		logger.info( "downloadPackPreviews" );

		if ( null == getContext() ) return;
		if ( null == pack || null == pack.getContent() ) return;

		final String previewPath = pack.getContent().getPreviewPath();

		if ( null != previewPath ) {
			File file = new File( previewPath );

			boolean preview_valid = false;
			Validator validator = AviaryCdsValidatorFactory.create( ContentType.PREVIEW, PackType.fromString( pack.getPackType() ) );

			try {
				preview_valid = validator.validate( getContext(), pack.getContent().getId(), file, false );
			} catch ( AssertionError e ) {
				e.printStackTrace();
				preview_valid = false;
			}

			if ( preview_valid ) {
				initWorkspace( mPack );
				return;
			}
		}

		RemotePreviewDownloader task = new RemotePreviewDownloader( mPack );
		task.execute( mPack.getId() );
	}

	class WorkspaceAdapter extends CursorAdapter {

		LayoutInflater mLayoutInflater;
		int mResId;
		String mBaseDir;
		String mFileExt;
		int mTargetDensity;
		int mInputDensity;

		int columnIndexType;
		int columnIndexDisplayName;
		int columnIndexIdentifier;

		public WorkspaceAdapter ( Context context, String baseDir, int resource, Cursor cursor ) {
			super( context, cursor, false );
			mResId = resource;
			mLayoutInflater = LayoutInflater.from( getContext() );
			mBaseDir = baseDir;
			mTargetDensity = context.getResources().getDisplayMetrics().densityDpi;

			Resolution resolution = CdsUtils.getResolution( context );
			switch ( resolution ) {
				case HIGH:
					mInputDensity = DisplayMetrics.DENSITY_XHIGH;
					break;

				case LOW:
					mInputDensity = DisplayMetrics.DENSITY_MEDIUM;
					break;
			}

			initCursor( cursor );
		}

		@Override
		public Cursor swapCursor( Cursor newCursor ) {
			initCursor( newCursor );
			recycleBitmaps();
			return super.swapCursor( newCursor );
		}

		private void recycleBitmaps() {
			logger.info( "recycleBitmaps" );

			int count = mWorkspace.getChildCount();

			for ( int i = 0; i < count; i++ ) {
				CellLayout view = (CellLayout) mWorkspace.getChildAt( i );
				int cells = view.getChildCount();
				for ( int k = 0; k < cells; k++ ) {
					ImageView imageView = (ImageView) view.getChildAt( k );
					if ( null != imageView ) {
						Drawable drawable = imageView.getDrawable();
						if ( null != drawable && ( drawable instanceof BitmapDrawable ) ) {
							imageView.setImageBitmap( null );

							Bitmap bitmap = ( (BitmapDrawable) drawable ).getBitmap();

							if ( null != bitmap && !bitmap.isRecycled() ) {
								bitmap.recycle();
							}
						}
					}
				}
			}
		}

		private void initCursor( Cursor cursor ) {
			if ( null != cursor ) {
				columnIndexDisplayName = cursor.getColumnIndex( PacksItemsColumns.DISPLAY_NAME );
				columnIndexIdentifier = cursor.getColumnIndex( PacksItemsColumns.IDENTIFIER );
				columnIndexType = cursor.getColumnIndex( PacksColumns.PACK_TYPE );
			}
		}

		public void setContext( Context context ) {
			mContext = context;
		}

		public void setResourceId( int resid ) {
			mResId = resid;
		}

		public void setBaseDir( String dir ) {
			mBaseDir = dir;
		}

		public String getBaseDir() {
			return mBaseDir;
		}

		public void setFileExt( String file_ext ) {
			mFileExt = file_ext;
		}

		@Override
		public int getCount() {
			return (int) Math.ceil( (double) super.getCount() / mItemsPerPage );
		}

		/**
		 * Gets the real num of items.
		 * 
		 * @return the real count
		 */
		public int getRealCount() {
			return super.getCount();
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup parent ) {
			View view = mLayoutInflater.inflate( mResId, parent, false );
			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor ) {

			int page = cursor.getPosition();
			int position = page * mItemsPerPage;

			CellLayout cell = (CellLayout) view;
			cell.setNumCols( mCols );
			cell.setNumRows( mRows );

			for ( int i = 0; i < mItemsPerPage; i++ ) {
				View toolView;
				CellInfo cellInfo = cell.findVacantCell();

				if ( cellInfo == null ) {
					toolView = cell.getChildAt( i );
				} else {
					toolView = mLayoutInflater.inflate( mCellResId, mWorkspace, false );
					CellLayout.LayoutParams lp = new CellLayout.LayoutParams( cellInfo.cellX, cellInfo.cellY, cellInfo.spanH, cellInfo.spanV );
					cell.addView( toolView, -1, lp );
				}

				final int index = position + i;
				final ImageView imageView = (ImageView) toolView.findViewById( R.id.aviary_image );

				int maxW = mWorkspace.getWidth() / mCols;
				int maxH = mWorkspace.getHeight() / mRows;

				if ( index < getRealCount() ) {
					loadImage( i * 60, position + i, imageView, mDownloadOnDemand, maxW, maxH );
				} else {
					// else...
					// imageView.setTag( null );
					if( null != imageView ) {
						imageView.setImageBitmap( null );
					}
				}
			}
			view.requestLayout();
		}

		public void loadImage( int delay, int position, final ImageView imageView, boolean onDemand, int maxW, int maxH ) {
			
			Cursor cursor = (Cursor) getItem( position );

			if ( null != cursor && !cursor.isAfterLast() && null != imageView ) {
				String identifier = cursor.getString( columnIndexIdentifier );
				String displayName = cursor.getString( columnIndexDisplayName );
				String type = cursor.getString( columnIndexType );

				File file = new File( getBaseDir(), identifier + ( mFileExt ) );
				final String path = file.getAbsolutePath();
				final int imageTag = path.hashCode();

				final Integer tag = (Integer) imageView.getTag();
				final boolean same = ( tag != null && tag.equals( imageTag ) );

				if ( onDemand ) {
					if ( !same ) {
						imageView.setTag( null );
						imageView.setImageBitmap( null );
					}
				} else {
					if ( !same ) {

						mPicassoLibrary.load( path )
							.fit()
							.centerInside()
							.withDelay( delay )
							// .skipMemoryCache() // don't use cache for previews
							.error( R.drawable.aviary_ic_na )
							.transform( new CdsPreviewTransformer( path, displayName, type ) ).into( imageView, new Callback() {

								@Override
								public void onSuccess() {
									logger.log( "onSuccess: %d", imageTag );
									imageView.setTag( Integer.valueOf( imageTag ) );
								}

								public void onError() {}
							} );
					}
				}
			}
		}
	}

	@Override
	public void onPageChanged( int which, int old ) {
		logger.info( "onPageChanged: " + old + " >> " + which );

		if ( !mDownloadOnDemand ) return;
		if ( null == getContext() ) return;

		if ( null != mWorkspace ) {
			WorkspaceAdapter adapter = (WorkspaceAdapter) mWorkspace.getAdapter();

			int index = which * mItemsPerPage;
			int endIndex = index + mItemsPerPage;
			int total = adapter.getRealCount();

			for ( int i = index; i < endIndex; i++ ) {
				CellLayout cellLayout = (CellLayout) mWorkspace.getScreenAt( which );
				if( null == cellLayout ) continue;
				
				ImageView toolView = (ImageView) cellLayout.getChildAt( i - index );

				int maxW = mWorkspace.getWidth() / mCols;
				int maxH = mWorkspace.getHeight() / mRows;

				if ( i < total ) {
					adapter.loadImage( i * 60, i, toolView, false, maxW, maxH );
				}
			}

			// if download on demand, then cleanup the old page bitmaps
			if ( mDownloadOnDemand && old != which ) {
				CellLayout cellLayout = (CellLayout) mWorkspace.getScreenAt( old );
				if ( null != cellLayout ) {
					for ( int i = 0; i < cellLayout.getChildCount(); i++ ) {
						View toolView = cellLayout.getChildAt( i );
						ImageView imageView = (ImageView) toolView.findViewById( R.id.aviary_image );
						if ( null != imageView ) {
							imageView.setImageBitmap( null );
							imageView.setTag( null );
						}
					}
				}
			}
		}
	}

	/**
	 * Download the pack previews
	 * @author alessandro
	 */
	class RemotePreviewDownloader extends AviaryAsyncTask<Long, Void, String> {

		private PacksColumns.PackCursorWrapper mCurrentPack;
		private Throwable mError;

		public RemotePreviewDownloader ( PacksColumns.PackCursorWrapper pack ) {
			mCurrentPack = (PacksColumns.PackCursorWrapper) pack.clone();
		}

		@Override
		protected void PostExecute( String result ) {
			if ( isCancelled() ) return;
			if ( !isValidContext() || null == mPack ) return;

			if ( !mPack.equals( mCurrentPack ) ) {
				logger.warn( "different pack!" );
				return;
			}
			logger.info( "RemotePreviewDownloader::PostExecute: %s", result );
			logger.error( "error: " + mError );

			if ( null != result ) {
				mLoader.setVisibility( View.GONE );
			}

			if ( null != mError && null != mError.getMessage() ) {
				onDownloadPreviewError( mError.getMessage() );
			}
		}

		@Override
		protected void PreExecute() {
			logger.info( "RemotePreviewDownloader::PreExecute" );
			if ( null == getContext() || mPack == null ) return;
			if ( !mPack.equals( mCurrentPack ) ) return;

			mLoader.setVisibility( View.VISIBLE );
			mErrorView.setVisibility( View.GONE );
		}

		@Override
		protected String doInBackground( Long... params ) {
			logger.info( "RemotePreviewDownloader::doInBackground" );
			Downloader downloader = AviaryCdsDownloaderFactory.create( ContentType.PREVIEW );
			String result = null;
			try {
				result = downloader.download( getContext(), params[0] );
			} catch ( IOException e ) {
				e.printStackTrace();
				mError = e;
			} catch ( AssertionError e ) {
				e.printStackTrace();
				mError = e;
			}
			return result;
		}

	}

	class DeterminePackOptionAsyncTask extends AviaryAsyncTask<PacksColumns.PackCursorWrapper, PackOptionWithPrice, PackOptionWithPrice> {
		IabResult mResult;
		PacksColumns.PackCursorWrapper mPack;
		boolean mForceUpdate;
		IabResult mIabResult;

		public DeterminePackOptionAsyncTask ( boolean update ) {
			mForceUpdate = update;
		}

		@Override
		protected void PreExecute() {}

		@Override
		protected PackOptionWithPrice doInBackground( PackCursorWrapper... params ) {
			if ( !isValidContext() ) return null;

			logger.info( "DeterminePackOptionAsyncTask::doInBackground" );

			PacksColumns.PackCursorWrapper pack = params[0];
			mPack = (PacksColumns.PackCursorWrapper) pack.clone();

			PackOptionWithPrice result = null;

			// 1. first check if it's in the download queue
			Pair<PackOption, String> pair = CdsUtils.getPackOptionDownloadStatus( getContext(), pack.getId() );
			if ( null != pair ) {
				result = new PackOptionWithPrice( pair.first, null );

				if ( result.option == PackOption.DOWNLOAD_COMPLETE ) {
					publishProgress( result );
				} else {
					return result;
				}
			}

			if ( null != result && result.option == PackOption.DOWNLOAD_COMPLETE ) {
				SystemUtils.trySleep( 1200 );
			}

			// 2. check the cache
			if ( !mForceUpdate ) {
				result = mPriceMap.get( pack.getId() );
				if ( null != result ) {
					logger.log( "from the cache: %s", result );
					return result;
				}
			}

			// 3. check pack option status ( free/restore/owned )
			result = new PackOptionWithPrice( CdsUtils.getPackOption( getContext(), pack ), null );
			logger.log( "CdsUtils.getPackOption: %s", result );

			if ( result.option == PackOption.PACK_OPTION_BEING_DETERMINED ) {
				publishProgress( result );
				// need to check the google play inventory

				IAPWrapper wrapper = null;

				if ( null != mIapService ) {
					wrapper = mIapService.available();
				}

				if ( null != wrapper ) {
					mIabResult = CdsUtils.waitForIAPSetupDone( wrapper );
				}

				if ( null != mIabResult && mIabResult.isSuccess() ) {
					result = IAPDialogMain.getFromInventory( wrapper, pack.getIdentifier() );
				} else {
					result = new PackOptionWithPrice( PackOption.ERROR );
				}
				logger.log( "getFromInventory: %s", result );
			}

			// 4. check again the download status, but only if not already owned
			if ( result.option != PackOption.OWNED ) {
				pair = CdsUtils.getPackOptionDownloadStatus( getContext(), pack.getId() );
				if ( null != pair ) {
					result = new PackOptionWithPrice( pair.first, null );
					return result;
				}
			}

			if ( PackOption.isDetermined( result.option ) ) {
				mPriceMap.put( pack.getId(), result );
			}

			return result;
		}

		@Override
		protected void ProgressUpdate( PackOptionWithPrice... values ) {
			if ( null != values ) {
				PackOptionWithPrice option = values[0];
				logger.info( "DeterminePackOptionAsyncTask::ProgressUpdate: %s - %s", mPack.getContent().getDisplayName(), option );
				onPackOptionUpdated( option, mPack );
			}
		}

		@Override
		protected void PostExecute( PackOptionWithPrice result ) {
			if ( !isValidContext() ) return;
			if ( null == mPack ) return;

			logger.info( "DeterminePackOptionAsyncTask::PostExecute: %s - %s", mPack.getContent().getDisplayName(), result );

			if ( null != result ) {
				onPackOptionUpdated( result, mPack );
			} else {
				onPackOptionUpdated( new PackOptionWithPrice( PackOption.ERROR, null ), mPack );
			}

			if ( null != mIabResult && mIabResult.isFailure() ) {
				logger.warn( mIabResult.getMessage() );
				if( mIabResult.getResponse() != IabHelper.IABHELPER_MISSING_SIGNATURE ) {
					Toast.makeText( getContext(), mIabResult.getMessage(), Toast.LENGTH_SHORT ).show();
				}
			}
		}
	}

	boolean isValidContext() {
		return mAttached && null != getContext();
	}
}
