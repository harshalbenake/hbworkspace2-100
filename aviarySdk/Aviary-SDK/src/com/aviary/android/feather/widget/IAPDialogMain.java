package com.aviary.android.feather.widget;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Assert;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.aviary.android.feather.AviaryMainController;
import com.aviary.android.feather.AviaryMainController.FeatherContext;
import com.aviary.android.feather.FeatherActivity;
import com.aviary.android.feather.R;
import com.aviary.android.feather.cds.AviaryCds.PackType;
import com.aviary.android.feather.cds.CdsUtils;
import com.aviary.android.feather.cds.CdsUtils.PackOption;
import com.aviary.android.feather.cds.IAPWrapper;
import com.aviary.android.feather.cds.PacksColumns;
import com.aviary.android.feather.cds.PacksContentColumns;
import com.aviary.android.feather.cds.billing.util.IabException;
import com.aviary.android.feather.cds.billing.util.IabHelper;
import com.aviary.android.feather.cds.billing.util.IabResult;
import com.aviary.android.feather.cds.billing.util.Inventory;
import com.aviary.android.feather.cds.billing.util.Purchase;
import com.aviary.android.feather.cds.billing.util.SkuDetails;
import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.common.utils.IOUtils;
import com.aviary.android.feather.common.utils.PackageManagerUtils;
import com.aviary.android.feather.library.services.IAPService;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.receipt.Receipt;
import com.aviary.android.feather.receipt.ReceiptManager;
import com.aviary.android.feather.widget.IAPDialogList.onPackSelectedListener;

@SuppressLint ( "MissingSuperCall" )
public class IAPDialogMain implements onPackSelectedListener {

	public interface OnCloseListener {
		void onClose();
	}

	public static class IAPUpdater implements Cloneable {
		private long packId = -1;
		private PackType packType = null;

		public long getPackId() {
			return packId;
		}

		public PackType getPackType() {
			return packType;
		}

		@Override
		public Object clone() {
			IAPUpdater cloned = new IAPUpdater();
			cloned.packId = packId;
			cloned.packType = packType;
			return cloned;
		}
		
		@Override
		public boolean equals( Object o ) {
			if( o instanceof IAPUpdater ) {
				IAPUpdater other = (IAPUpdater) o;
				return other.packId == packId && other.packType == packType;
			}
			return super.equals( o );
		}
		
		@Override
		public String toString() {
			return "IAPUpdater{packType: " + packType + ", packId: " + packId + "}";
		}

		public static class Builder {

			IAPUpdater result;

			public Builder () {
				result = new IAPUpdater();
			}

			public Builder setPackId( long id ) {
				result.packId = id;
				return this;
			}

			public Builder setPackType( PackType type ) {
				result.packType = type;
				return this;
			}

			public IAPUpdater build() {
				return result;
			}
		}
	}

	static Logger logger = LoggerFactory.getLogger( "IAPDialogMain", LoggerType.ConsoleLoggerType );

	/** price map for packId/priceOption. contains ONLY price/restore/free status */
	private HashMap<Long, PackOptionWithPrice> privatePriceMap = new HashMap<Long, PackOptionWithPrice>();

	OnCloseListener mCloseListener;
	ViewGroup mView;
	IAPUpdater mData;
	ViewAnimator mViewAnimator;
	AviaryMainController mController;
	IAPService mIAPService;

	/**
	 * Download status content observer
	 */
	ContentObserver mDownloadStatusObserver = new ContentObserver( new Handler() ) {

		@Override
		public void onChange( boolean selfChange ) {
			onChange( selfChange, null );
		}

		@Override
		public void onChange( boolean selfChange, Uri uri ) {
			logger.info( "** downloadStatusObserver::onChange **" );
			if ( null == getContext() ) return;

			onDownloadStatusChanged( uri );
		}
	};

	public static IAPDialogMain create( FeatherContext context, IAPUpdater data ) {
		logger.info( "create" );

		ViewGroup container = context.activatePopupContainer();
		ViewGroup dialog = (ViewGroup) container.findViewById( R.id.aviary_main_iap_dialog_container );
		IAPDialogMain instance = null;

		if ( dialog == null ) {
			dialog = addToParent( container, -1 );
			instance = new IAPDialogMain( dialog );
			instance.update( data, true );
		} else {
			instance = (IAPDialogMain) dialog.getTag();
			instance.update( data, false );
		}
		return instance;
	}

	public IAPDialogMain ( ViewGroup view ) {
		mView = view;
		mView.setTag( this );
		onAttachedToWindow();
	}

	private void initController() {
		if ( null == mController ) {
			if ( mView.getContext() instanceof FeatherActivity ) {
				mController = ( (FeatherActivity) mView.getContext() ).getMainController();
				mIAPService = mController.getService( IAPService.class );
			}
		} else {
			logger.log( "controller: " + mController );
		}
	}

	public AviaryMainController getController() {
		return mController;
	}

	public boolean onBackPressed() {
		if ( mViewAnimator.getDisplayedChild() == 0 ) {
			return false;
		} else {
			IAPDialogList view = (IAPDialogList) mViewAnimator.getChildAt( 0 );
			if ( null != view && view.getData() != null ) {
				displayChild( 0, false );
				return true;
			}
		}
		return false;
	}

	public HashMap<Long, PackOptionWithPrice> getPriceMap( PackType packType ) {
		return privatePriceMap;
	}

	private void onDownloadStatusChanged( Uri uri ) {
		IAPDialogList listView = (IAPDialogList) mViewAnimator.getChildAt( 0 );
		IAPDialogDetail detailView = (IAPDialogDetail) mViewAnimator.getChildAt( 1 );

		if ( listView.getData() != null ) {
			listView.onDownloadStatusChanged( uri );
		}

		if ( null != detailView && detailView.getData() != null ) {
			detailView.onDownloadStatusChanged( uri );
		}
	}
	
	/**
	 * Purchase completed successfully
	 * @param purchase
	 * @param packId
	 * @param identifier
	 * @param packType
	 */
	private void onPurchaseSuccess( final Purchase purchase, final long packId, final String identifier, final String packType, final String price ) {
		logger.info( "onPurchaseSuccess: %s - %s (%s)", identifier, packType, purchase );
		
		PackOptionWithPrice newOption;
		
		sendReceipt( purchase, price );

		try {
			if( requestPackDownload( packId ) ) {
				newOption = new PackOptionWithPrice( PackOption.DOWNLOADING, null );
			} else {
				newOption = new PackOptionWithPrice( PackOption.DOWNLOAD_ERROR, null );
			}
		} catch( Throwable t ) {
			
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
			return;
		}
		
		HashMap<Long, PackOptionWithPrice> pricemap = getPriceMap( PackType.fromString( packType ) );
		if( null != pricemap ) {
			pricemap.remove( packId );
			if( PackOption.isDetermined( newOption.option )) {
				pricemap.put( packId, newOption );
				logger.log( "set PackOption.DOWNLOADING to %d", packId );
			}
		}

		IAPDialogList listView = getListView();
		IAPDialogDetail detailView = getDetailView();
		
		if( null != listView && listView.getData() != null ) {
			listView.onPurchaseSuccess( packId, identifier, packType );
		}
		
		if( null != detailView && detailView.getData() != null ) {
			detailView.onPurchaseSuccess( packId, identifier, packType );
		}
	}

	public void update( IAPUpdater updater ) {
		update( updater, false );
	}

	public void update( IAPUpdater updater, boolean firstTime ) {
		logger.info( "update" );

		if ( null == updater || !isValid() ) return;
		mData = updater;

		initController();

		if ( updater.packId < 0 && updater.packType == null ) {
			logger.error( "invalid updater instance" );
			return;
		}

		int currentChild = mViewAnimator.getDisplayedChild();
		int targetChild = updater.getPackId() < 0 ? 0 : 1;

		logger.log( "currentChild: " + currentChild );
		logger.log( "firstTime: " + firstTime );
		logger.log( "target child: " + targetChild );

		displayChild( targetChild, firstTime );

		if ( targetChild == 0 ) {
			IAPDialogList view = (IAPDialogList) mViewAnimator.getChildAt( targetChild );
			
			IAPUpdater viewData = view.getData();
			if( null != updater && !updater.equals( viewData ) ) {
				view.update( updater, this );
			}
			view.setOnPackSelectedListener( this );
		} else {
			IAPDialogDetail view = (IAPDialogDetail) mViewAnimator.getChildAt( targetChild );
			IAPUpdater viewData = view.getData();
			if( null != updater && !updater.equals( viewData ) ) {
				view.update( updater, this );
			}
		}
	}
	
	void displayChild( int targetChild, boolean firstTime ) {
		int currentChild = mViewAnimator.getDisplayedChild();
		
		if ( firstTime ) {
			if ( targetChild == 0 ) {
				mViewAnimator.setAnimateFirstView( true );
			}
		}

		// if ( targetChild == 1 && mViewAnimator.getChildCount() < 2 ) {
		//	 LayoutInflater.from( getContext() ).inflate( R.layout.aviary_iap_dialog_detail, mViewAnimator );
		// }
		
		if( targetChild == 0 ) {
			mViewAnimator.setInAnimation( getContext(), R.anim.aviary_slide_in_left );
			mViewAnimator.setOutAnimation( getContext(), R.anim.aviary_slide_out_right );			
		} else {
			mViewAnimator.setInAnimation( getContext(), R.anim.aviary_slide_in_right );
			mViewAnimator.setOutAnimation( getContext(), R.anim.aviary_slide_out_left );			
		}
		
		if ( currentChild != targetChild ) {
			mViewAnimator.setDisplayedChild( targetChild );
		}
		
		mViewAnimator.getInAnimation().setAnimationListener( new AnimationListener() {
			
			@Override
			public void onAnimationStart( Animation animation ) {
				logger.info( "onAnimationStart" );
			}
			
			@Override
			public void onAnimationRepeat( Animation animation ) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onAnimationEnd( Animation animation ) {
				logger.info( "onAnimationEnd" );
				
			}
		} );
		
	}

	IAPDialogList getListView() {
		return (IAPDialogList) mViewAnimator.getChildAt( 0 );
	}

	IAPDialogDetail getDetailView() {
		if ( mViewAnimator.getChildCount() > 0 ) return (IAPDialogDetail) mViewAnimator.getChildAt( 1 );
		return null;
	}

	public IAPUpdater getData() {
		return mData;
	}

	/**
	 * launch the IAP purchase flow
	 * 
	 * @param pack
	 * @param content
	 */
	void launchPackPurchaseFlow( final long packId, final String identifier, final String packType, final String whereFrom, final String price ) {
		logger.info( "launchPackPurchaseFlow" );

		if ( null == getContext() || null == mIAPService ) return;
		
		if( !mIAPService.isSetupDone() ) {
			Toast.makeText( getContext(), "There was a problem connecting to the billing service. Please try again.", Toast.LENGTH_SHORT ).show();
			mIAPService.startSetup( null );
			return;
		}

		IabHelper.OnIabPurchaseFinishedListener purchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
			public void onIabPurchaseFinished( IabResult result, Purchase purchase ) {
				logger.log( "Purchase finished: " + result + ", purchase: " + purchase );

				if ( null == getContext() ) {
					logger.error( "context is null" );
					return;
				}

				if ( result.isFailure() ) {
					final int response = result.getResponse();
					switch ( response ) {
						case IabHelper.IABHELPER_USER_CANCELLED:
							// no need to display a message
							break;
						default:
							Toast.makeText( getContext(), result.getMessage(), Toast.LENGTH_SHORT ).show();
							break;
					}
				} else {
					onPurchaseSuccess( purchase, packId, identifier, packType, price );
				}
				trackEndPurchaseFlow( getContext(), packId, identifier, packType, whereFrom, price, result.isSuccess() );

			}
		};

		if ( null != mIAPService ) {
			mIAPService.launchPurchaseFlow( identifier, purchaseFinishedListener, null );
			trackBeginPurchaseFlow( getContext(), identifier, packType, whereFrom, price, false, false );
		} else {
			logger.error( "iapservice is null" );
		}
	}

	/**
	 * start tracking the purchase flow
	 * 
	 * @param packIdentifier
	 * @param packType
	 * @param whereFrom
	 * @param price
	 * @param isRestore
	 * @param isFree
	 */
	static void trackBeginPurchaseFlow( Context context, String packIdentifier, String packType, String whereFrom, String price, boolean isRestore, boolean isFree ) {
		HashMap<String, String> attributes = new HashMap<String, String>();
		attributes.put( "Pack", packIdentifier );
		attributes.put( "ContentType", packType );
		attributes.put( "WhereFrom", "Supply Shop - " + whereFrom );
		attributes.put( "isRestore", isRestore ? "true" : "false" );
		attributes.put( "isFree", isFree ? "true" : "false" );
		if ( null != price ) {
			attributes.put( "Price", price );
		}
		Tracker.recordTag( "(" + packIdentifier + ")" + whereFrom + ":" + "InstalledClicked", attributes );
	}

	/**
	 * Tracks the purchase flow completed
	 * 
	 * @param context
	 * @param packId
	 * @param packIdentifier
	 * @param packType
	 * @param whereFrom
	 * @param price
	 */
	static void trackEndPurchaseFlow( Context context, long packId, String packIdentifier, String packType, String whereFrom, String price, boolean success ) {
		
		if( LoggerFactory.LOG_ENABLED ) {
			logger.info( "trackEndPurchaseFlow: " + packIdentifier + ", " + packType + ", " + whereFrom + ", " + price + ", " + success );
		}
		
		HashMap<String, String> attributes = new HashMap<String, String>();
		attributes.put( "Pack", packIdentifier );
		attributes.put( "ContentType", packType );
		attributes.put( "WhereFrom", "Supply Shop - " + whereFrom );
		attributes.put( "success", success ? "true" : "false" );
		if ( null != price ) {
			attributes.put( "Price", price );
		}
		Tracker.recordTag( "Content: PurchaseCompleted", attributes );
	}
	
	void sendReceipt( final String identifier, boolean free, boolean isRestore ) {
		// empty
		
		Receipt.Builder builder = new Receipt.Builder( free )
			.withProductId( identifier )
			.isNewPurchase( !isRestore )
			.withPurchaseTime( System.currentTimeMillis() );
		
		try {
			Receipt receipt = builder.build();
			ReceiptManager.getInstance( getContext() ).sendTicket( receipt );
		} catch( AssertionError e ) {
			e.printStackTrace();
		}
	}

	void sendReceipt( final Purchase purchase, String price ) {
		// empty
		
		Receipt.Builder builder = new Receipt.Builder( false )
			.withProductId( purchase.getSku() )
			.withPurchaseTime( purchase.getPurchaseTime() )
			.withOrderId( purchase.getOrderId() )
			.withPrice( price )
			.isNewPurchase( true )
			.withToken( purchase.getToken() );
		
		try {
			Receipt receipt = builder.build();
			ReceiptManager.getInstance( getContext() ).sendTicket( receipt );
		} catch( AssertionError e ) {
			e.printStackTrace();
		}
	}

	boolean requestPackDownload( long packId ) throws AssertionError, IOException {
		return requestPackDownload( packId, true );
	}
	
	private boolean requestPackDownload( long packId, boolean notify ) throws AssertionError, IOException {
		PacksContentColumns.ContentCursorWrapper content = CdsUtils.getPackContentById( getContext(), packId, new String[] { PacksContentColumns._ID,
				PacksContentColumns.PACK_ID } );
		if ( null == content ) return false;
		return requestPackDownload( packId, content.getId(), notify );
	}

	private boolean requestPackDownload( final long packId, final long contentId, boolean notify ) throws AssertionError, IOException {
		logger.info( "requestPackDownload" );

		final Context context = getContext();
		
		Assert.assertNotNull( "Invalid Context", context );
		Assert.assertNotNull( "Invalid in-app billing service", mIAPService );

		Uri uri = PackageManagerUtils.getCDSProviderContentUri( context, "pack/id/" + packId + "/requestDownload/1" );
		logger.log( "updating: " + uri );

		int result = context.getContentResolver().update( uri, new ContentValues(), null, null );
		logger.log( "result: " + result );
		
		Assert.assertTrue( "Failed to update the database, please try again later", result != 0 );

		// finally request to download the item
		String requestResult = CdsUtils.requestPackDownload( getContext(), packId, notify );
		logger.log( "requestResult: %s", requestResult );
		return true;
	}

	private static ViewGroup addToParent( ViewGroup parent, int index ) {
		ViewGroup view = (ViewGroup) LayoutInflater.from( parent.getContext() ).inflate( R.layout.aviary_iap_dialog_container, parent, false );
		view.setFocusable( true );
		if ( index > -1 ) {
			parent.addView( view, index );
		} else {
			parent.addView( view );
		}
		return view;
	}

	private Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			handleHide();
		}
	};

	public void onConfigurationChanged( Configuration newConfig ) {
		logger.info( "onConfigurationChanged" );

		if ( !isValid() ) return;

		ViewGroup parent = (ViewGroup) mView.getParent();
		if ( null != parent ) {
			int index = parent.indexOfChild( mView );
			removeFromParent();

			mView = addToParent( parent, index );

			ViewGroup animator = (ViewGroup) mView.findViewById( R.id.aviary_main_iap_dialog );
			if ( null != animator ) {
				animator.setLayoutAnimation( null );
			}

			onAttachedToWindow();
			update( (IAPUpdater) mData.clone() );
		} else {
			logger.error( "parent is null" );
		}
	}

	protected void onAttachedToWindow() {
		logger.info( "onAttachedToWindow" );
		mViewAnimator = (ViewAnimator) mView.findViewById( R.id.aviary_view_animator );
		getContext().getContentResolver().registerContentObserver( PackageManagerUtils.getCDSProviderContentUri( getContext(), "download/statusChanged/" ), true,
				mDownloadStatusObserver );
	}

	protected void onDetachedFromWindow() {
		logger.info( "onDetachedFromWindow" );
		setOnCloseListener( null );
		getContext().getContentResolver().unregisterContentObserver( mDownloadStatusObserver );
	}

	private boolean removeFromParent() {
		logger.info( "removeFromParent" );
		if ( null != mView ) {
			ViewGroup parent = (ViewGroup) mView.getParent();
			if ( null != parent ) {
				parent.removeView( mView );
				onDetachedFromWindow();
				return true;
			}
		}
		return false;
	}

	public void dismiss( boolean animate ) {
		logger.info( "dismiss, animate: " + animate );

		getListView().setOnPackSelectedListener( null );

		if ( animate ) {
			hide();
		} else {
			removeFromParent();
		}
	}

	protected void hide() {
		logger.info( "hide" );
		if ( !isValid() ) return;
		mView.post( mHideRunnable );
	}

	public boolean isValid() {
		return mView != null && mView.getWindowToken() != null;
	}

	public Context getContext() {
		if ( null != mView ) {
			return mView.getContext();
		}
		return null;
	}

	private void handleHide() {
		logger.info( "handleHide" );

		if ( !isValid() ) return;

		Animation animation = AnimationUtils.loadAnimation( mView.getContext(), R.anim.aviary_iap_close_animation );
		AnimationListener listener = new AnimationListener() {

			@Override
			public void onAnimationStart( Animation animation ) {}

			@Override
			public void onAnimationRepeat( Animation animation ) {}

			@Override
			public void onAnimationEnd( Animation animation ) {
				removeFromParent();
			}
		};
		animation.setAnimationListener( listener );
		mView.startAnimation( animation );
	}

	public void setOnCloseListener( OnCloseListener listener ) {
		mCloseListener = listener;
	}

	@Override
	public void onPackSelected( long packid, PackType packType ) {
		logger.info( "onPackSelected: " + packid );
		update( new IAPUpdater.Builder().setPackId( packid ).setPackType( packType ).build() );

		try {
			trackPackSelected( packid );
		} catch ( Throwable t ) {
			t.printStackTrace();
		}
	}

	private void trackPackSelected( long packId ) {
		// Tracking
		Cursor cursor = getContext().getContentResolver().query( PackageManagerUtils.getCDSProviderContentUri( getContext(), "pack/id/" + packId + "/content" ),
				new String[] { PacksColumns.IDENTIFIER, PacksContentColumns.IS_FREE_PURCHASE }, null, null, null );

		String identifier = null;
		int free = 0;

		if ( null != cursor ) {
			try {
				if ( cursor.moveToFirst() ) {
					identifier = cursor.getString( 0 );
					free = cursor.getInt( 1 );
				}
			} finally {
				IOUtils.closeSilently( cursor );
			}
		}

		if ( null != identifier ) {
			HashMap<String, String> packSelectedAttributes = new HashMap<String, String>();
			packSelectedAttributes.put( "Pack", identifier );
			packSelectedAttributes.put( "isFree", free == 0 ? "False" : "True" );
			Tracker.recordTag( "(" + identifier + ")" + "ListView: Selected", packSelectedAttributes );
		}
	}
	
	public static PackOptionWithPrice getFromInventory( IAPWrapper instance, String identifier ) {
		logger.info( "getFromInventory: %s - %s", instance, identifier );
		Inventory inventory = null;
		ArrayList<String> skus = new ArrayList<String>();
		skus.add( identifier );
		try {
			if ( instance.isAvailable() ) {
				inventory = instance.queryInventory( true, skus );
			} else {
				return new PackOptionWithPrice( PackOption.ERROR, null );
			}
		} catch ( IabException e ) {
			e.printStackTrace();
			return new PackOptionWithPrice( PackOption.ERROR, null );
		}

		if ( null != inventory ) {
			Purchase itemPurchase = inventory.getPurchase( identifier );
			if ( itemPurchase != null && IAPWrapper.verifyDeveloperPayload( itemPurchase ) ) {
				return new PackOptionWithPrice( PackOption.RESTORE, null );
			} else {
				SkuDetails itemDetails = inventory.getSkuDetails( identifier );
				if ( null != itemDetails ) {
					return new PackOptionWithPrice( PackOption.PURCHASE, itemDetails.getPrice() );
				} else {
					return new PackOptionWithPrice( PackOption.ERROR, null );
				}
			}
		}
		return new PackOptionWithPrice( PackOption.ERROR, null );
	}	
	
	
	/**
	 * Container for {@link PackOption} and price
	 * @author alessandro
	 *
	 */
	public static class PackOptionWithPrice {
		public String price;
		public PackOption option;

		public PackOptionWithPrice ( PackOption option ) {
			this( option, null );
		}

		public PackOptionWithPrice ( PackOption option, String price ) {
			this.option = option;
			this.price = price;
		}

		@Override
		public String toString() {
			return "PackOptionWithPrice{ option: " + option.name() + ", price: " + price + "}";
		}
	}	
}
