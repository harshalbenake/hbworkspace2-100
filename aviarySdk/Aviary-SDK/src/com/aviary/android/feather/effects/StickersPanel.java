package com.aviary.android.feather.effects;

import it.sephiroth.android.library.imagezoom.ImageViewTouchBase.DisplayType;
import it.sephiroth.android.library.widget.AdapterView.OnItemClickListener;
import it.sephiroth.android.library.widget.AdapterView.OnItemLongClickListener;
import it.sephiroth.android.library.widget.HListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import junit.framework.Assert;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.Loader.OnLoadCompleteListener;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.aviary.android.feather.AviaryMainController.FeatherContext;
import com.aviary.android.feather.R;
import com.aviary.android.feather.cds.AviaryCds;
import com.aviary.android.feather.cds.AviaryCds.PackType;
import com.aviary.android.feather.cds.AviaryCds.Size;
import com.aviary.android.feather.cds.CdsUtils;
import com.aviary.android.feather.cds.PacksItemsColumns;
import com.aviary.android.feather.cds.TrayColumns;
import com.aviary.android.feather.common.AviaryIntent;
import com.aviary.android.feather.common.utils.IOUtils;
import com.aviary.android.feather.common.utils.PackageManagerUtils;
import com.aviary.android.feather.effects.BordersPanel.ViewHolder;
import com.aviary.android.feather.effects.BordersPanel.ViewHolderExternal;
import com.aviary.android.feather.effects.SimpleStatusMachine.OnStatusChangeListener;
import com.aviary.android.feather.headless.moa.MoaActionFactory;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.StickerFilter;
import com.aviary.android.feather.library.graphics.drawable.FeatherDrawable;
import com.aviary.android.feather.library.graphics.drawable.StickerDrawable;
import com.aviary.android.feather.library.services.BadgeService;
import com.aviary.android.feather.library.services.ConfigService;
import com.aviary.android.feather.library.services.DragControllerService;
import com.aviary.android.feather.library.services.DragControllerService.DragListener;
import com.aviary.android.feather.library.services.DragControllerService.DragSource;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.services.drag.DragView;
import com.aviary.android.feather.library.services.drag.DropTarget;
import com.aviary.android.feather.library.services.drag.DropTarget.DropTargetListener;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.library.utils.MatrixUtils;
import com.aviary.android.feather.library.utils.UIConfiguration;
import com.aviary.android.feather.utils.PackIconCallable;
import com.aviary.android.feather.widget.DrawableHighlightView;
import com.aviary.android.feather.widget.DrawableHighlightView.OnDeleteClickListener;
import com.aviary.android.feather.widget.IAPDialogMain;
import com.aviary.android.feather.widget.IAPDialogMain.IAPUpdater;
import com.aviary.android.feather.widget.IAPDialogMain.OnCloseListener;
import com.aviary.android.feather.widget.ImageViewDrawableOverlay;
import com.squareup.picasso.Picasso;

public class StickersPanel extends AbstractContentPanel implements OnStatusChangeListener, OnItemClickListener, DragListener, DragSource, DropTargetListener,
		OnLoadCompleteListener<Cursor> {

	// TODO: implements tracking

	private static final int STATUS_NULL = SimpleStatusMachine.INVALID_STATUS;
	private static final int STATUS_PACKS = 1;
	private static final int STATUS_STICKERS = 2;

	/** panel's status */
	private SimpleStatusMachine mStatus;

	/** horizontal listview for stickers packs */
	private HListView mListPacks;

	/** horizontal listview for stickers items */
	private HListView mListStickers;

	/** view flipper for switching between lists */
	private ViewFlipper mViewFlipper;

	private Picasso mPicassoLib;

	/** canvas used to draw stickers */
	private Canvas mCanvas;

	private int mPackCellWidth;
	private int mStickerCellWidth;

	/** installed plugins */
	private List<String> mInstalledPackages;

	/** required services */
	private ConfigService mConfigService;
	private DragControllerService mDragControllerService;
	private BadgeService mBadgeService;

	/** iap dialog for inline previews */
	private IAPDialogMain mIapDialog;

	private MoaActionList mActionList;
	private StickerFilter mCurrentFilter;

	private int mPackThumbSize;
	private int mStickerThumbSize;

	private boolean mFirstTimeRenderer = true;

	protected CursorAdapter mAdapterPacks;
	protected CursorAdapter mAdapterStickers;
	protected CursorLoader mCursorLoaderPacks;
	protected ContentObserver mContentObserver;

	// for status_sticker
	private StickerPackInfo mPackInfo;
	
	@Override
	public void onLoadComplete( Loader<Cursor> loader, Cursor cursor ) {
		mLogger.info( "onLoadComplete: " + cursor + ", currentStatus: " + mStatus.getCurrentStatus() );
		
		int firstValidIndex = -1;
		int newStatus = STATUS_PACKS;
		
		int index = 0;
		int cursorSize = 0;

		if ( null != cursor ) {
			index = cursor.getPosition();
			while ( cursor.moveToNext() ) {
				int type = cursor.getInt( TrayColumns.TYPE_COLUMN_INDEX );
				if ( type == TrayColumns.TYPE_PACK_INTERNAL ) {
					firstValidIndex = cursor.getPosition();
					break;
				}
			}
			cursorSize = cursor.getCount();
			cursor.moveToPosition( index );
		}
		
		if( firstValidIndex == 0 && cursorSize == 1 && null != cursor && mStatus.getCurrentStatus() != STATUS_STICKERS ) {
			// we have only 1 installed pack and nothing else, so just
			// display its content
			
			index = cursor.getPosition();
			
			if( cursor.moveToFirst() ) {
				int id_index = cursor.getColumnIndex( TrayColumns._ID );
				int identifier_index = cursor.getColumnIndex( TrayColumns.IDENTIFIER );
				int type_index = cursor.getColumnIndex( TrayColumns.TYPE );
				
				if( id_index > -1 && identifier_index > -1 && type_index > -1 ) {
					int packType = cursor.getInt( type_index );
					
					if( packType == StickerPacksAdapter.TYPE_DIVIDER ) {
						mLogger.log( "one pack only, show it" );
						mPackInfo = new StickerPackInfo( cursor.getLong( id_index ), cursor.getString( identifier_index ) );
						newStatus = STATUS_STICKERS;
					}
				}
			}
			cursor.moveToPosition( index );
		}
		
		mStatus.setStatus( newStatus );
		mAdapterPacks.changeCursor( cursor );
		onStickersPackListUpdated( cursor, firstValidIndex );

		// check optional messaging
		long iapPackageId = -1;
		if ( hasOptions() ) {
			Bundle options = getOptions();
			if ( options.containsKey( AviaryIntent.OptionBundle.SHOW_IAP_DIALOG ) ) {
				iapPackageId = options.getLong( AviaryIntent.OptionBundle.SHOW_IAP_DIALOG );
			}
			// ok, we display the IAP dialog only the first time
			options.remove( AviaryIntent.OptionBundle.SHOW_IAP_DIALOG );
			
			// display the iap dialog
			if ( iapPackageId > -1 ) {
				IAPUpdater iapData = new IAPUpdater.Builder().setPackId( iapPackageId ).setPackType( PackType.STICKER ).build();
				displayIAPDialog( iapData );
			}		
		}

	}

	public StickersPanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry );
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		mStatus = new SimpleStatusMachine();

		// init layout components
		mListPacks = (HListView) getOptionView().findViewById( R.id.aviary_list_packs );
		mListStickers = (HListView) getOptionView().findViewById( R.id.aviary_list_stickers );
		mViewFlipper = (ViewFlipper) getOptionView().findViewById( R.id.aviary_flipper );
		mImageView = (ImageViewDrawableOverlay) getContentView().findViewById( R.id.aviary_overlay );

		// init services
		mConfigService = getContext().getService( ConfigService.class );
		mBadgeService = getContext().getService( BadgeService.class );

		// setup the main imageview
		( (ImageViewDrawableOverlay) mImageView ).setDisplayType( DisplayType.FIT_IF_BIGGER );
		( (ImageViewDrawableOverlay) mImageView ).setForceSingleSelection( false );
		( (ImageViewDrawableOverlay) mImageView ).setDropTargetListener( this );
		( (ImageViewDrawableOverlay) mImageView ).setScaleWithContent( true );

		// create the default action list
		mActionList = MoaActionFactory.actionList();

		mPicassoLib = Picasso.with( getContext().getBaseContext() );

		// create the preview for the main imageview
		createAndConfigurePreview();

		DragControllerService dragger = getContext().getService( DragControllerService.class );
		dragger.addDropTarget( (DropTarget) mImageView );
		dragger.setMoveTarget( mImageView );
		dragger.setDragListener( this );

		setDragController( dragger );
	}

	@Override
	public void onActivate() {
		super.onActivate();

		mImageView.setImageBitmap( mPreview, null, -1, UIConfiguration.IMAGE_VIEW_MAX_ZOOM );

		mPackCellWidth = mConfigService.getDimensionPixelSize( R.dimen.aviary_sticker_pack_width );
		mPackThumbSize = mConfigService.getDimensionPixelSize( R.dimen.aviary_sticker_pack_image_width );
		mStickerCellWidth = mConfigService.getDimensionPixelSize( R.dimen.aviary_sticker_single_item_width );
		mStickerThumbSize = mConfigService.getDimensionPixelSize( R.dimen.aviary_sticker_single_item_image_width );

		mInstalledPackages = Collections.synchronizedList( new ArrayList<String>() );

		// register to status change
		mStatus.setOnStatusChangeListener( this );

		updateInstalledPacks( true );

		getContentView().setVisibility( View.VISIBLE );
		contentReady();
	}

	@Override
	public boolean onBackPressed() {
		mLogger.info( "onBackPressed" );

		if ( null != mIapDialog ) {
			if ( mIapDialog.onBackPressed() ) return true;
			removeIapDialog();
			return true;
		}

		// we're in the packs status
		if ( mStatus.getCurrentStatus() == STATUS_PACKS ) {
			if ( stickersOnScreen() ) {
				askToLeaveWithoutApply();
				return true;
			}
			return false;
		}

		// we're in the stickers status
		if ( mStatus.getCurrentStatus() == STATUS_STICKERS ) {
			
			int packsCount = 0;
			if( null != mAdapterPacks ) {
				packsCount = mAdapterPacks.getCount();
			}
			
			mLogger.log( "packsCount: %d", packsCount );
			
			if( packsCount > 1 ) {
				mStatus.setStatus( STATUS_PACKS );
				return true;
			} else {
				if ( stickersOnScreen() ) {
					askToLeaveWithoutApply();
					return true;
				}
				return false;
			}
		}
		return super.onBackPressed();
	}

	@Override
	public boolean onCancel() {

		mLogger.info( "onCancel" );

		// if there's an active sticker on screen
		// then ask if we really want to exit this panel
		// and discard changes
		if ( stickersOnScreen() ) {
			askToLeaveWithoutApply();
			return true;
		}

		return super.onCancel();
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();

		// disable the drag controller
		if ( null != getDragController() ) {
			getDragController().deactivate();
			getDragController().removeDropTarget( (DropTarget) mImageView );
			getDragController().setDragListener( null );
		}
		setDragController( null );

		if ( null != mAdapterPacks ) {
			mAdapterPacks.changeCursor( null );
		}

		if ( null != mAdapterStickers ) {
			mAdapterStickers.changeCursor( null );
		}

		mListPacks.setAdapter( null );
		mListStickers.setAdapter( null );

		// mPluginService.removeOnUpdateListener( this );
		mStatus.setOnStatusChangeListener( null );

		mListPacks.setOnItemClickListener( null );
		mListStickers.setOnItemClickListener( null );
		mListStickers.setOnItemLongClickListener( null );

		removeIapDialog();

		Context context = getContext().getBaseContext();
		
		if( null != mContentObserver ) {
			context.getContentResolver().unregisterContentObserver( mContentObserver );
		}

		if ( null != mCursorLoaderPacks ) {
			mLogger.info( "stop load cursorloader..." );
			mCursorLoaderPacks.unregisterListener( this );
			mCursorLoaderPacks.stopLoading();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		( (ImageViewDrawableOverlay) mImageView ).clearOverlays();
		mCurrentFilter = null;
		mActionList = null;
		mBadgeService = null;

		if ( null != mCursorLoaderPacks ) {
			mLogger.info( "disposing cursorloader..." );
			mCursorLoaderPacks.abandon();
			mCursorLoaderPacks.reset();
		}

		if ( null != mAdapterPacks ) {
			IOUtils.closeSilently( mAdapterPacks.getCursor() );
		}

		if ( null != mAdapterStickers ) {
			IOUtils.closeSilently( mAdapterStickers.getCursor() );
		}

		mAdapterPacks = null;
		mAdapterStickers = null;
		mCursorLoaderPacks = null;
	}

	@Override
	protected void onDispose() {
		super.onDispose();

		if ( null != mInstalledPackages ) {
			mInstalledPackages.clear();
		}

		mCanvas = null;
	}

	@Override
	protected void onGenerateResult() {
		onApplyCurrent();
		super.onGenerateResult( mActionList );
	}

	@Override
	public void onConfigurationChanged( Configuration newConfig, Configuration oldConfig ) {
		mLogger.info( "onConfigurationChanged: " + newConfig );

		if ( mIapDialog != null ) {
			mIapDialog.onConfigurationChanged( newConfig );
		}
		super.onConfigurationChanged( newConfig, oldConfig );
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.aviary_content_stickers, null );
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_stickers, null );
	}

	// /////////////////////////
	// OnStatusChangeListener //
	// /////////////////////////
	@Override
	public void OnStatusChanged( int oldStatus, int newStatus ) {
		mLogger.info( "OnStatusChange: " + oldStatus + " >> " + newStatus );

		switch ( newStatus ) {
			case STATUS_PACKS:

				// deactivate listeners for the stickers list
				mListStickers.setOnItemClickListener( null );
				mListStickers.setOnItemLongClickListener( null );
				
				if( mViewFlipper.getDisplayedChild() != 1 ) {
					mViewFlipper.setDisplayedChild( 1 );
				}

				if ( oldStatus == STATUS_NULL ) {
					// NULL
				} else if ( oldStatus == STATUS_STICKERS ) {
					restoreToolbarTitle();

					if ( getDragController() != null ) {
						getDragController().deactivate();
					}

					if ( null != mAdapterStickers ) {
						mAdapterStickers.changeCursor( null );
					}
				}
				break;

			case STATUS_STICKERS:
				loadStickers();
				
				if( mViewFlipper.getDisplayedChild() != 2 ) {
					mViewFlipper.setDisplayedChild( 2 );
				}

				if ( getDragController() != null ) {
					getDragController().activate();
				}
				break;

			default:
				mLogger.error( "unmanaged status change: " + oldStatus + " >> " + newStatus );
				break;
		}
	}

	@Override
	public void OnStatusUpdated( int status ) {
	}

	// //////////////////////
	// OnItemClickListener //
	// //////////////////////

	@Override
	public void onItemClick( it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id ) {
		mLogger.info( "onItemClick: " + position );

		if ( !isActive() ) return;

		if ( mStatus.getCurrentStatus() == STATUS_PACKS ) {

			ViewHolder holder = (ViewHolder) view.getTag();
			if ( null != holder ) {

				// get more
				if ( holder.type == StickerPacksAdapter.TYPE_LEFT_GETMORE || holder.type == StickerPacksAdapter.TYPE_RIGHT_GETMORE ) {

					if ( holder.type == StickerPacksAdapter.TYPE_LEFT_GETMORE ) {
						Tracker.recordTag( "(Stickers) LeftSupplyShop: Clicked" );
					} else if ( holder.type == StickerPacksAdapter.TYPE_RIGHT_GETMORE ) {
						Tracker.recordTag( "(Stickers) RightSupplyShop: Clicked" );
					}

					IAPUpdater iapData = new IAPUpdater.Builder().setPackType( PackType.STICKER ).build();
					displayIAPDialog( iapData );

					// external
				} else if ( holder.type == StickerPacksAdapter.TYPE_EXTERNAL ) {

					ViewHolderExternal holder_ext = (ViewHolderExternal) holder;
					if ( null != holder_ext ) {
						holder_ext.badgeIcon.setVisibility( View.GONE );
						holder_ext.externalIcon.setVisibility( View.VISIBLE );
					}

					IAPUpdater iapData = new IAPUpdater.Builder().setPackType( PackType.STICKER ).setPackId( holder.id ).build();
					displayIAPDialog( iapData );

					if ( position > 0 ) {

						if ( mListPacks.getChildCount() > 0 ) {
							int left = view.getLeft();
							int right = view.getRight();
							int center = ( ( right - left ) / 2 + left );
							final int delta = mListPacks.getWidth() / 2 - center;

							mListPacks.postDelayed( new Runnable() {

								@Override
								public void run() {
									mListPacks.smoothScrollBy( -delta, 500 );
								}
							}, 300 );
						}
					}
				} else if ( holder.type == StickerPacksAdapter.TYPE_DIVIDER ) {
					removeIapDialog();
					mPackInfo = new StickerPackInfo( holder.id, holder.identifier );
					mStatus.setStatus( STATUS_STICKERS );
				}
			}
		}
	}

	// ////////////////////////
	// Drag and Drop methods //
	// ////////////////////////

	/**
	 * Starts the drag and drop operation
	 * 
	 * @param parent
	 *            - the parent list
	 * @param view
	 *            - the current view clicked
	 * @param position
	 *            - the position in the list
	 * @param id
	 *            - the item id
	 * @param nativeClick
	 *            - it's a native click
	 * @return
	 */
	private boolean startDrag( it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id, boolean animate ) {

		mLogger.info( "startDrag" );

		if ( android.os.Build.VERSION.SDK_INT < 9 ) return false;

		if ( parent == null || view == null || parent.getAdapter() == null ) {
			return false;
		}

		if ( mStatus.getCurrentStatus() != STATUS_STICKERS ) return false;

		if ( null != view ) {
			View image = view.findViewById( R.id.image );
			if ( null != image ) {

				if ( null == parent.getAdapter() ) return false;
				StickersAdapter adapter = (StickersAdapter) parent.getAdapter();

				if ( null == adapter ) return false;

				final String identifier = adapter.getItemIdentifier( position );
				final String contentPath = adapter.getContentPath();

				if ( null == identifier || null == contentPath ) return false;

				final String iconPath = contentPath + "/" + AviaryCds.getPackItemFilename( identifier, PackType.STICKER, Size.Small );

				Bitmap bitmap;
				try {
					bitmap = new StickerThumbnailCallable( iconPath, mStickerThumbSize ).call();
					int offsetx = Math.abs( image.getWidth() - bitmap.getWidth() ) / 2;
					int offsety = Math.abs( image.getHeight() - bitmap.getHeight() ) / 2;
					return getDragController().startDrag( image, bitmap, offsetx, offsety, StickersPanel.this, new StickerDragInfo( contentPath, identifier ),
							DragControllerService.DRAG_ACTION_MOVE, animate );
				} catch ( Exception e ) {
					e.printStackTrace();
				}
				return getDragController().startDrag( image, StickersPanel.this, new StickerDragInfo( contentPath, identifier ),
						DragControllerService.DRAG_ACTION_MOVE, animate );
			}
		}
		return false;
	}

	@Override
	public void setDragController( DragControllerService controller ) {
		mDragControllerService = controller;
	}

	@Override
	public DragControllerService getDragController() {
		return mDragControllerService;
	}

	public void onDropCompleted( View arg0, boolean arg1 ) {}

	@Override
	public boolean onDragEnd() {
		return false;
	}

	@Override
	public void onDragStart( DragSource arg0, Object arg1, int arg2 ) {}

	@Override
	public boolean acceptDrop( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo ) {
		return source == this;
	}

	@Override
	public void onDrop( DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo ) {

		mLogger.info( "onDrop. source=" + source + ", dragInfo=" + dragInfo );

		if ( !isActive() ) return;

		if ( dragInfo != null && dragInfo instanceof StickerDragInfo ) {
			StickerDragInfo info = (StickerDragInfo) dragInfo;

			onApplyCurrent();

			float scaleFactor = dragView.getScaleFactor();

			float w = dragView.getWidth();
			float h = dragView.getHeight();

			int width = (int) ( w / scaleFactor );
			int height = (int) ( h / scaleFactor );

			int targetX = (int) ( x - xOffset );
			int targetY = (int) ( y - yOffset );

			RectF rect = new RectF( targetX, targetY, targetX + width, targetY + height );

			addSticker( info.contentPath, info.identifier, rect );
		}
	}

	// /////////////////////////
	// Stickers panel methods //
	// /////////////////////////

	/**
	 * Ask to leave without apply changes.
	 */
	void askToLeaveWithoutApply() {
		new AlertDialog.Builder( getContext().getBaseContext() ).setTitle( R.string.feather_attention ).setMessage( R.string.feather_tool_leave_question )
				.setPositiveButton( android.R.string.yes, new DialogInterface.OnClickListener() {

					@Override
					public void onClick( DialogInterface dialog, int which ) {
						getContext().cancel();
					}
				} ).setNegativeButton( android.R.string.no, null ).show();
	}

	/**
	 * Initialize the preview bitmap and canvas.
	 */
	private void createAndConfigurePreview() {

		if ( mPreview != null && !mPreview.isRecycled() ) {
			mPreview.recycle();
			mPreview = null;
		}

		mPreview = BitmapUtils.copy( mBitmap, Bitmap.Config.ARGB_8888 );
		mCanvas = new Canvas( mPreview );
	}

	protected void updateInstalledPacks( boolean firstTime ) {
		mLogger.info( "updateInstalledPacks: " + firstTime );

		// display the loader
		if ( mViewFlipper.getDisplayedChild() != 0 ) {
			mViewFlipper.setDisplayedChild( 0 );
		}

		mAdapterPacks = createPacksAdapter( getContext().getBaseContext(), null );
		mListPacks.setAdapter( mAdapterPacks );
		Context context = getContext().getBaseContext();

		if ( null == mCursorLoaderPacks ) {
			
			final String uri = String.format( Locale.US, "packTray/%d/%d/%d/%s", 3, 0, 0, AviaryCds.PACKTYPE_STICKER );
			
			Uri baseUri = PackageManagerUtils.getCDSProviderContentUri( context, uri );
			mCursorLoaderPacks = new CursorLoader( context, baseUri, null, null, null, null );
			mCursorLoaderPacks.registerListener( 1, this );

			mContentObserver = new ContentObserver( new Handler() ) {
				@Override
				public void onChange( boolean selfChange ) {
					mLogger.info( "mContentObserver::onChange" );
					super.onChange( selfChange );

					if ( isActive() && null != mCursorLoaderPacks && mCursorLoaderPacks.isStarted() ) {
						mCursorLoaderPacks.onContentChanged();
					}
				}
			};
			context.getContentResolver().registerContentObserver( PackageManagerUtils.getCDSProviderContentUri( context, "packTray/" + AviaryCds.PACKTYPE_STICKER ), false,
					mContentObserver );
		}

		mCursorLoaderPacks.startLoading();
		mListPacks.setOnItemClickListener( this );
	}

	private StickerPacksAdapter createPacksAdapter( Context context, Cursor cursor ) {
		return new StickerPacksAdapter( context, R.layout.aviary_sticker_item, R.layout.aviary_frame_item_external, R.layout.aviary_sticker_item_more, cursor );
	}

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

	/**
	 * Loads the list of available stickers for the current selected pack
	 */
	protected void loadStickers() {
		mLogger.info( "loadStickers" );

		final Context context = getContext().getBaseContext();

		if ( null == mPackInfo ) return;

		// retrieve the pack content path
		final String packContentPath = CdsUtils.getPackContentPath( context, mPackInfo.packId );

		// acquire the items cursor
		Cursor cursor = context.getContentResolver().query(
				PackageManagerUtils.getCDSProviderContentUri( context, "pack/" + mPackInfo.packId + "/item/list" ),
				new String[] { PacksItemsColumns._ID + " as _id", PacksItemsColumns._ID, PacksItemsColumns.PACK_ID, PacksItemsColumns.IDENTIFIER,
						PacksItemsColumns.DISPLAY_NAME }, null, null, null );

		if ( null == mAdapterStickers ) {
			mAdapterStickers = new StickersAdapter( context, R.layout.aviary_sticker_item_single, cursor );
			( (StickersAdapter) mAdapterStickers ).setContentPath( packContentPath );
			mListStickers.setAdapter( mAdapterStickers );
		} else {
			( (StickersAdapter) mAdapterStickers ).setContentPath( packContentPath );
			mAdapterStickers.changeCursor( cursor );
		}

		mListStickers.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick( it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id ) {
				mLogger.info( "onItemClick: " + position );
				StickersAdapter adapter = ( (StickersAdapter) parent.getAdapter() );

				final Cursor cursor = (Cursor) adapter.getItem( position );
				final String sticker = cursor.getString( cursor.getColumnIndex( PacksItemsColumns.IDENTIFIER ) );
				removeIapDialog();
				addSticker( adapter.getContentPath(), sticker, null );
			}
		} );

		mListStickers.setOnItemLongClickListener( new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick( it.sephiroth.android.library.widget.AdapterView<?> parent, View view, int position, long id ) {
				return startDrag( parent, view, position, id, false );
			}
		} );
	}

	private void addSticker( String contentPath, String identifier, RectF position ) {
		mLogger.info( "addSticker: %s - %s", contentPath, identifier );

		onApplyCurrent();

		Assert.assertNotNull( mPackInfo );
		Assert.assertNotNull( contentPath );

		File file = new File( contentPath, AviaryCds.getPackItemFilename( identifier, PackType.STICKER, Size.Medium ) );
		mLogger.log( "file: " + file.getAbsolutePath() );

		if ( file.exists() ) {
			StickerDrawable drawable = new StickerDrawable( getContext().getBaseContext().getResources(), file.getAbsolutePath(), identifier,
					mPackInfo.packIdentifier );
			drawable.setAntiAlias( true );

			mCurrentFilter = new StickerFilter( contentPath, identifier );
			mCurrentFilter.setSize( drawable.getBitmapWidth(), drawable.getBitmapHeight() );

			Tracker.recordTag( identifier + ": Selected" );

			addSticker( drawable, position );
		} else {
			mLogger.warn( "file does not exists" );
			Toast.makeText( getContext().getBaseContext(), "Error loading the selected sticker", Toast.LENGTH_SHORT ).show();
		}
	}

	private void addSticker( FeatherDrawable drawable, RectF positionRect ) {
		mLogger.info( "addSticker: " + drawable + ", position: " + positionRect );

		setIsChanged( true );

		DrawableHighlightView hv = new DrawableHighlightView( mImageView, ( (ImageViewDrawableOverlay) mImageView ).getOverlayStyleId(), drawable );

		hv.setOnDeleteClickListener( new OnDeleteClickListener() {

			@Override
			public void onDeleteClick() {
				onClearCurrent( true );
			}
		} );

		Matrix mImageMatrix = mImageView.getImageViewMatrix();

		int cropWidth, cropHeight;
		int x, y;

		final int width = mImageView.getWidth();
		final int height = mImageView.getHeight();

		// width/height of the sticker
		if ( positionRect != null ) {
			cropWidth = (int) positionRect.width();
			cropHeight = (int) positionRect.height();
		} else {
			cropWidth = (int) drawable.getCurrentWidth();
			cropHeight = (int) drawable.getCurrentHeight();
		}

		final int cropSize = Math.max( cropWidth, cropHeight );
		final int screenSize = Math.min( mImageView.getWidth(), mImageView.getHeight() );

		if ( cropSize > screenSize ) {
			float ratio;
			float widthRatio = (float) mImageView.getWidth() / cropWidth;
			float heightRatio = (float) mImageView.getHeight() / cropHeight;

			if ( widthRatio < heightRatio ) {
				ratio = widthRatio;
			} else {
				ratio = heightRatio;
			}

			cropWidth = (int) ( (float) cropWidth * ( ratio / 2 ) );
			cropHeight = (int) ( (float) cropHeight * ( ratio / 2 ) );

			if ( positionRect == null ) {
				int w = mImageView.getWidth();
				int h = mImageView.getHeight();
				positionRect = new RectF( w / 2 - cropWidth / 2, h / 2 - cropHeight / 2, w / 2 + cropWidth / 2, h / 2 + cropHeight / 2 );
			}

			positionRect.inset( ( positionRect.width() - cropWidth ) / 2, ( positionRect.height() - cropHeight ) / 2 );
		}

		if ( positionRect != null ) {
			x = (int) positionRect.left;
			y = (int) positionRect.top;
		} else {
			x = ( width - cropWidth ) / 2;
			y = ( height - cropHeight ) / 2;
		}

		Matrix matrix = new Matrix( mImageMatrix );
		matrix.invert( matrix );

		float[] pts = new float[] { x, y, x + cropWidth, y + cropHeight };
		MatrixUtils.mapPoints( matrix, pts );

		RectF cropRect = new RectF( pts[0], pts[1], pts[2], pts[3] );
		Rect imageRect = new Rect( 0, 0, width, height );

		// hv.setRotateAndScale( rotateAndResize );
		hv.setup( getContext().getBaseContext(), mImageMatrix, imageRect, cropRect, false );

		( (ImageViewDrawableOverlay) mImageView ).addHighlightView( hv );
		( (ImageViewDrawableOverlay) mImageView ).setSelectedHighlightView( hv );
	}

	private void onApplyCurrent() {
		mLogger.info( "onApplyCurrent" );

		if ( !stickersOnScreen() ) return;

		final DrawableHighlightView hv = ( (ImageViewDrawableOverlay) mImageView ).getHighlightViewAt( 0 );

		if ( hv != null ) {

			final StickerDrawable stickerDrawable = ( (StickerDrawable) hv.getContent() );

			RectF cropRect = hv.getCropRectF();
			Rect rect = new Rect( (int) cropRect.left, (int) cropRect.top, (int) cropRect.right, (int) cropRect.bottom );

			Matrix rotateMatrix = hv.getCropRotationMatrix();
			Matrix matrix = new Matrix( mImageView.getImageMatrix() );
			if ( !matrix.invert( matrix ) ) {
			}

			int saveCount = mCanvas.save( Canvas.MATRIX_SAVE_FLAG );
			mCanvas.concat( rotateMatrix );

			stickerDrawable.setDropShadow( false );
			hv.getContent().setBounds( rect );
			hv.getContent().draw( mCanvas );
			mCanvas.restoreToCount( saveCount );
			mImageView.invalidate();

			if ( mCurrentFilter != null ) {
				final int w = mBitmap.getWidth();
				final int h = mBitmap.getHeight();

				mCurrentFilter.setTopLeft( cropRect.left / w, cropRect.top / h );
				mCurrentFilter.setBottomRight( cropRect.right / w, cropRect.bottom / h );
				mCurrentFilter.setRotation( Math.toRadians( hv.getRotation() ) );

				int dw = stickerDrawable.getBitmapWidth();
				int dh = stickerDrawable.getBitmapHeight();
				float scalew = cropRect.width() / dw;
				float scaleh = cropRect.height() / dh;

				mCurrentFilter.setCenter( cropRect.centerX() / w, cropRect.centerY() / h );
				mCurrentFilter.setScale( scalew, scaleh );

				mActionList.add( mCurrentFilter.getActions().get( 0 ) );

				Tracker.recordTag( stickerDrawable.getPackIdentifier() + ": Applied" );

				mCurrentFilter = null;
			}
		}

		onClearCurrent( false );
		onPreviewChanged( mPreview, false, false );
	}

	/**
	 * Remove the current sticker.
	 * 
	 * @param removed
	 *            - true if the current sticker is being removed, otherwise it was
	 *            flattened
	 */
	private void onClearCurrent( boolean removed ) {
		mLogger.info( "onClearCurrent. removed=" + removed );

		if ( stickersOnScreen() ) {
			final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;
			final DrawableHighlightView hv = image.getHighlightViewAt( 0 );
			onClearCurrent( hv, removed );
		}
	}

	/**
	 * Removes the current active sticker.
	 * 
	 * @param hv
	 *            - the {@link DrawableHighlightView} of the active sticker
	 * @param removed
	 *            - current sticker is removed
	 */
	private void onClearCurrent( DrawableHighlightView hv, boolean removed ) {
		mLogger.info( "onClearCurrent. hv=" + hv + ", removed=" + removed );

		if ( mCurrentFilter != null ) {
			mCurrentFilter = null;
		}

		if ( null != hv ) {
			FeatherDrawable content = hv.getContent();

			if ( removed ) {
				if ( content instanceof StickerDrawable ) {
					String name = ( (StickerDrawable) content ).getIdentifier();
					String packname = ( (StickerDrawable) content ).getPackIdentifier();

					Tracker.recordTag( name + ": Cancelled" );
					Tracker.recordTag( packname + ": Cancelled" );
				}
			}
		}

		hv.setOnDeleteClickListener( null );
		( (ImageViewDrawableOverlay) mImageView ).removeHightlightView( hv );
		( (ImageViewDrawableOverlay) mImageView ).invalidate();
	}

	/**
	 * Return true if there's at least one active sticker on screen.
	 * 
	 * @return true, if successful
	 */
	private boolean stickersOnScreen() {
		final ImageViewDrawableOverlay image = (ImageViewDrawableOverlay) mImageView;
		return image.getHighlightCount() > 0;
	}

	private void onStickersPackListUpdated( Cursor cursor, int firstIndex ) {
		mLogger.info( "onStickersPackListUpdated: " + cursor + ", firstIndex: " + firstIndex );

		int mListFirstValidPosition = firstIndex > 0 ? firstIndex : 0;

		if ( mFirstTimeRenderer ) {
			if ( mListFirstValidPosition > 0 ) {
				mListPacks.setSelectionFromLeft( mListFirstValidPosition - 1, mPackCellWidth / 2 );
			}
		}
		mFirstTimeRenderer = false;
	}

	/**
	 * Sticker pack listview adapter class
	 * 
	 * @author alessandro
	 */
	class StickerPacksAdapter extends CursorAdapter {

		static final int TYPE_INVALID = -1;
		static final int TYPE_LEFT_GETMORE = 5;
		static final int TYPE_RIGHT_GETMORE = 6;
		static final int TYPE_NORMAL = TrayColumns.TYPE_CONTENT;
		static final int TYPE_EXTERNAL = TrayColumns.TYPE_PACK_EXTERNAL;
		static final int TYPE_DIVIDER = TrayColumns.TYPE_PACK_INTERNAL;
		static final int TYPE_LEFT_DIVIDER = TrayColumns.TYPE_LEFT_DIVIDER;
		static final int TYPE_RIGHT_DIVIDER = TrayColumns.TYPE_RIGHT_DIVIDER;

		private int mLayoutResId;
		private int mExternalLayoutResId;
		private int mMoreResId;
		private LayoutInflater mInflater;

		int mIdColumnIndex;
		int mPackageNameColumnIndex;
		int mIdentifierColumnIndex;
		int mTypeColumnIndex;
		int mDisplayNameColumnIndex;
		int mPathColumnIndex;

		public StickerPacksAdapter ( Context context, int mainResId, int externalResId, int moreResId, Cursor cursor ) {
			super( context, cursor, 0 );
			initColumns( cursor );
			mLayoutResId = mainResId;
			mExternalLayoutResId = externalResId;
			mMoreResId = moreResId;

			mInflater = LayoutInflater.from( context );
		}

		@Override
		public Cursor swapCursor( Cursor newCursor ) {
			mLogger.info( "swapCursor" );
			initColumns( newCursor );
			return super.swapCursor( newCursor );
		}

		private void initColumns( Cursor cursor ) {
			if ( null != cursor ) {
				mIdColumnIndex = cursor.getColumnIndex( TrayColumns._ID );
				mPackageNameColumnIndex = cursor.getColumnIndex( TrayColumns.PACKAGE_NAME );
				mIdentifierColumnIndex = cursor.getColumnIndex( TrayColumns.IDENTIFIER );
				mTypeColumnIndex = cursor.getColumnIndex( TrayColumns.TYPE );
				mDisplayNameColumnIndex = cursor.getColumnIndex( TrayColumns.DISPLAY_NAME );
				mPathColumnIndex = cursor.getColumnIndex( TrayColumns.PATH );
			}
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
			View view;
			ViewHolder holder;

			int layoutWidth = mPackCellWidth;
			final int type = getItemViewType( position );

			switch ( type ) {
				case TYPE_LEFT_GETMORE:
					view = mInflater.inflate( mMoreResId, parent, false );
					layoutWidth = mPackCellWidth;
					break;

				case TYPE_RIGHT_GETMORE:
					view = mInflater.inflate( mMoreResId, parent, false );
					layoutWidth = mPackCellWidth;

					if ( mPackCellWidth * cursor.getCount() < parent.getWidth() * 2 ) {
						view.setVisibility( View.INVISIBLE );
						layoutWidth = 1;
					} else {
						if ( parent.getChildCount() > 0 && mListPacks.getFirstVisiblePosition() == 0 ) {
							View lastView = parent.getChildAt( parent.getChildCount() - 1 );

							if ( lastView.getRight() < parent.getWidth() ) {
								view.setVisibility( View.INVISIBLE );
								layoutWidth = 1;
							}
						}
					}

					break;

				case TYPE_DIVIDER:
					view = mInflater.inflate( mLayoutResId, parent, false );
					layoutWidth = mPackCellWidth;
					break;

				case TYPE_EXTERNAL:
					view = mInflater.inflate( mExternalLayoutResId, parent, false );
					layoutWidth = mPackCellWidth;
					break;

				case TYPE_LEFT_DIVIDER:
					view = mInflater.inflate( R.layout.aviary_thumb_divider_right, parent, false );
					layoutWidth = LayoutParams.WRAP_CONTENT;
					break;

				case TYPE_RIGHT_DIVIDER:
					view = mInflater.inflate( R.layout.aviary_thumb_divider_left, parent, false );
					layoutWidth = LayoutParams.WRAP_CONTENT;

					if ( mPackCellWidth * cursor.getCount() < parent.getWidth() * 2 ) {
						view.setVisibility( View.INVISIBLE );
						layoutWidth = 1;
					} else {

						if ( parent.getChildCount() > 0 && mListPacks.getFirstVisiblePosition() == 0 ) {
							View lastView = parent.getChildAt( parent.getChildCount() - 1 );

							if ( lastView.getRight() < parent.getWidth() ) {
								view.setVisibility( View.INVISIBLE );
								layoutWidth = 1;
							}
						}
					}
					break;

				case TYPE_NORMAL:
				default:
					mLogger.error( "TYPE_NORMAL" );
					view = null;
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

			if ( holder.image != null ) {
				LayoutParams params = holder.image.getLayoutParams();
				params.height = mPackThumbSize;
				params.width = mPackThumbSize;
				holder.image.setLayoutParams( params );
			}

			view.setTag( holder );
			return view;
		}

		void bindView( View view, Context context, Cursor cursor, int position ) {
			final ViewHolder holder = (ViewHolder) view.getTag();
			String displayName;
			String identifier;
			long id = -1;

			if ( !cursor.isAfterLast() && !cursor.isBeforeFirst() ) {
				id = cursor.getLong( mIdColumnIndex );
			}

			if ( holder.type == TYPE_NORMAL ) {

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
					mPicassoLib
						.load( icon )
						.resize( mPackThumbSize, mPackThumbSize, true )
						.transform( new PackIconCallable( getContext().getBaseContext().getResources(), PackType.STICKER, icon ) )
						.noFade()
						.error( R.drawable.aviary_ic_na )
						.into( holder.image );
				}

			} else if ( holder.type == TYPE_DIVIDER ) {
				displayName = cursor.getString( mDisplayNameColumnIndex );
				identifier = cursor.getString( mIdentifierColumnIndex );
				String icon = cursor.getString( mPathColumnIndex );

				holder.text.setText( displayName );
				holder.identifier = identifier;

				if ( holder.id != id ) {
					
					mPicassoLib
					.load( new File( icon ) )
					.fit()
					.transform( new PackIconCallable( getContext().getBaseContext().getResources(), PackType.STICKER, icon ) )
					.noFade()
					.error( R.drawable.aviary_ic_na )
					.into( holder.image );					
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

	}

	/**
	 * Sticker pack element
	 * 
	 * @author alessandro
	 */
	static class StickerEffectPack {

		static enum StickerEffectPackType {
			GET_MORE_FIRST, GET_MORE_LAST, EXTERNAL, INTERNAL, LEFT_DIVIDER, RIGHT_DIVIDER
		}

		CharSequence mPackageName;
		CharSequence mTitle;
		int mPluginStatus;
		StickerEffectPackType mType;

		public StickerEffectPack ( StickerEffectPackType type ) {
			mType = type;
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
		}
	}

	//
	// Stickers list adapter
	//

	class StickersAdapter extends CursorAdapter {

		LayoutInflater mLayoutInflater;
		int mStickerResourceId;
		String mContentPath;
		int idColumnIndex, identifierColumnIndex, packIdColumnIndex;

		public StickersAdapter ( Context context, int resId, Cursor cursor ) {
			super( context, cursor, 0 );
			mStickerResourceId = resId;
			mLayoutInflater = LayoutInflater.from( context );
			initCursor( cursor );
		}

		public void setContentPath( String path ) {
			mContentPath = path;
		}

		public String getContentPath() {
			return mContentPath;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public Cursor swapCursor( Cursor newCursor ) {
			initCursor( newCursor );
			return super.swapCursor( newCursor );
		}

		private void initCursor( Cursor cursor ) {
			if ( null != cursor ) {
				idColumnIndex = cursor.getColumnIndex( PacksItemsColumns._ID );
				identifierColumnIndex = cursor.getColumnIndex( PacksItemsColumns.IDENTIFIER );
				packIdColumnIndex = cursor.getColumnIndex( PacksItemsColumns.PACK_ID );
			}
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup parent ) {
			View view = mLayoutInflater.inflate( mStickerResourceId, null );
			LayoutParams params = new LayoutParams( mStickerCellWidth, LayoutParams.MATCH_PARENT );
			view.setLayoutParams( params );
			return view;
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor ) {
			ImageView image = (ImageView) view.findViewById( R.id.image );

			String identifier = cursor.getString( identifierColumnIndex );

			final String iconPath = mContentPath + "/" + AviaryCds.getPackItemFilename( identifier, PackType.STICKER, Size.Small );
			
			mPicassoLib
				.load( iconPath )
				.skipMemoryCache()
				.resize( mStickerThumbSize, mStickerThumbSize, true )
				.noFade()
				.into( image );
			
		}

		public String getItemIdentifier( int position ) {
			Cursor cursor = (Cursor) getItem( position );
			return cursor.getString( identifierColumnIndex );
		}
	}

	/**
	 * Downloads and renders the sticker thumbnail
	 * 
	 * @author alessandro
	 */
	static class StickerThumbnailCallable implements Callable<Bitmap> {
		int mFinalSize;
		String mUrl;

		public StickerThumbnailCallable ( String path, int maxSize ) {
			mUrl = path;
			mFinalSize = maxSize;
		}

		@Override
		public Bitmap call() throws Exception {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.ARGB_8888;
			Bitmap bitmap = BitmapFactory.decodeFile( mUrl, options );

			if ( mFinalSize > 0 && null != bitmap ) {
				Bitmap result = BitmapUtils.resizeBitmap( bitmap, mFinalSize, mFinalSize );
				if ( result != bitmap ) {
					bitmap.recycle();
					bitmap = result;
				}
			}
			return bitmap;
		}
	}

	static class StickerPackInfo {
		long packId;
		String packIdentifier;

		StickerPackInfo ( long packId, String packIdentifier ) {
			this.packId = packId;
			this.packIdentifier = packIdentifier;
		}
	}

	static class StickerDragInfo {
		String contentPath;
		String identifier;

		StickerDragInfo ( String contentPath, String identifier ) {
			this.contentPath = contentPath;
			this.identifier = identifier;
		}
	}
}
