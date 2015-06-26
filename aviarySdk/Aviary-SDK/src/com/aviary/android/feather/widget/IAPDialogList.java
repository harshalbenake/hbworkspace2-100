package com.aviary.android.feather.widget;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.CursorAdapter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.util.Linkify;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.aviary.android.feather.FeatherActivity;
import com.aviary.android.feather.R;
import com.aviary.android.feather.cds.AviaryCds;
import com.aviary.android.feather.cds.AviaryCds.PackType;
import com.aviary.android.feather.cds.CdsUtils;
import com.aviary.android.feather.cds.CdsUtils.PackOption;
import com.aviary.android.feather.cds.IAPWrapper;
import com.aviary.android.feather.cds.PacksColumns;
import com.aviary.android.feather.cds.PacksContentColumns;
import com.aviary.android.feather.cds.PacksItemsColumns;
import com.aviary.android.feather.cds.RestoreAllHelper;
import com.aviary.android.feather.cds.billing.util.IabException;
import com.aviary.android.feather.cds.billing.util.IabHelper;
import com.aviary.android.feather.cds.billing.util.IabResult;
import com.aviary.android.feather.cds.billing.util.Inventory;
import com.aviary.android.feather.cds.billing.util.Purchase;
import com.aviary.android.feather.cds.billing.util.SkuDetails;
import com.aviary.android.feather.common.AviaryIntent;
import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.common.threading.Future;
import com.aviary.android.feather.common.threading.FutureListener;
import com.aviary.android.feather.common.utils.PackageManagerUtils;
import com.aviary.android.feather.common.utils.SystemUtils;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.library.Constants;
import com.aviary.android.feather.library.content.FeatherIntent;
import com.aviary.android.feather.library.services.LocalDataService;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.utils.PackIconCallable;
import com.aviary.android.feather.widget.IAPDialogList.ListAdapter.ViewHolder;
import com.aviary.android.feather.widget.IAPDialogMain.IAPUpdater;
import com.aviary.android.feather.widget.IAPDialogMain.PackOptionWithPrice;
import com.haarman.listviewanimations.swinginadapters.AnimationAdapter;
import com.haarman.listviewanimations.swinginadapters.prepared.SwingBottomInAnimationAdapter;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;


public class IAPDialogList extends LinearLayout implements OnClickListener, OnItemClickListener, OnScrollListener {

	static Logger logger = LoggerFactory.getLogger( "IAPDialogList", LoggerType.ConsoleLoggerType );

	static interface onPackSelectedListener {
		void onPackSelected( long packid, PackType packType );
	}

	/** returns if dialog is currently attached to parent */
	private boolean mAttached;

	/** current dataprovider */
	private IAPUpdater mData;
	private IAPWrapper mWrapper;

	private ListAdapter mAdapter;
	private AnimationAdapter mAnimationAdapter;
	private HashMap<Long, PackOptionWithPrice> priceMap;

	private boolean mScrollStateChanged;

	private TextView mSummaryText;
	private IAPDialogMain mParent;
	private ListView mList;
	private Button mRestoreAllButton;
	private View mListProgress;
	private onPackSelectedListener mPackSelectedListener;
	private Picasso mPicassoLibrary;
	private LocalDataService mDataService;

	// listen to pack purchase status change
	ContentObserver mPackPurchasedContentObserver = new ContentObserver( new Handler() ) {
		@Override
		public void onChange( boolean selfChange ) {
			onChange( selfChange, null );
		}

		@Override
		public void onChange( boolean selfChange, Uri uri ) {
			logger.info( "** mPackPurchasedContentObserver::onChange: %s **", uri );
			if ( !isValidContext() || null == mData ) return;
			
			if( null != uri ) {
				int purchased = Integer.parseInt( uri.getLastPathSegment() );
				long packId = Integer.parseInt( uri.getPathSegments().get( uri.getPathSegments().size() - 2 ) );
				
				if( null != priceMap ) {
					priceMap.put( packId, new PackOptionWithPrice( purchased == 1 ? PackOption.OWNED : PackOption.ERROR ) );
				}
				logger.log( "purchased status changed(%d) for packId: %d", purchased, packId );
			}
			update( mData, mParent );
		};
	};

	// listen for service complete events
	ContentObserver mServiceFinishedContentObserver = new ContentObserver( new Handler() ) {

		@Override
		public void onChange( boolean selfChange ) {
			onChange( selfChange, null );
		};

		@Override
		public void onChange( boolean selfChange, Uri uri ) {
			logger.info( "** mServiceFinishedContentObserver::onChange **" );
			if ( !isValidContext() || null == mData ) return;

			IAPUpdater data = (IAPUpdater) mData.clone();
			mData = null;
			update( data, mParent );
		};

	};

	public IAPDialogList ( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	public void update( final IAPUpdater updater, final IAPDialogMain parent ) {
		final boolean dataChanged = !updater.equals( mData );
		
		logger.info( "mData: %s", mData );
		logger.info( "updated: %s", updater );
		logger.info( "update: %s, dataChanged: %b", updater.getPackType(), dataChanged );
		
		mParent = parent;
		priceMap = mParent.getPriceMap( updater.getPackType() );
		mData = (IAPUpdater) updater.clone();
		processList( mData.getPackType(), dataChanged );
	}

	void onDownloadStatusChanged( Uri uri ) {
		logger.info( "onDownloadStatusChanged: %s ", uri );
		mAdapter.notifyDataSetChanged();
	}

	void onPurchaseSuccess( long packId, String identifier, String packType ) {
		logger.info( "onPurchaseSuccess: %s - %s", identifier, packType );

		if ( !isValidContext() ) return;

		// priceMap.put( packId, new PackOptionWithPrice( PackOption.OWNED ) );
		mAdapter.notifyDataSetChanged();
	}

	private void initializeWrapper() {
		if ( isInEditMode() ) return;
		
		logger.info( "initializeWrapper" );

		if ( null == mWrapper ) {
			mWrapper = IAPWrapper.createNew( getContext(), mDataService.getIntentValue( Constants.EXTRA_IN_BILLING_PUBLIC_KEY, "" ) );
		}
	}

	public IAPUpdater getData() {
		return mData;
	}

	private void onRestoreAll() {
		logger.info( "onRestoreAll" );

		HashMap<String, String> restoreAllAttributes = new HashMap<String, String>();
		restoreAllAttributes.put( "ContentType", mData.getPackType().toCdsString() );
		Tracker.recordTag( "Content: Restored All", restoreAllAttributes );
		mScrollStateChanged = true;
		
		Toast.makeText( getContext(), R.string.feather_restore_all_request_sent, Toast.LENGTH_SHORT ).show();
		
		final String secret = mDataService.getIntentValue( Constants.EXTRA_IN_API_KEY_SECRET, "" );
		final String billingPublicKey = mDataService.getIntentValue( Constants.EXTRA_IN_BILLING_PUBLIC_KEY, "" );
		Intent intent = AviaryIntent.createCdsRestoreAllIntent( getContext(), mData.getPackType().toCdsString(), secret, billingPublicKey );
		
		if( mParent.getContext().startService( intent ) != null ) {
			displayProgressNotification();
		}
	}
	
	/**
	 * Restore all request has been sent, in the meantime let's send also
	 * a system notification to show that....
	 */
	private void displayProgressNotification() {
		final Context context = getContext();
		
		if( null != context ) {
			NotificationCompat.Builder notification = RestoreAllHelper.createNotification( context );
			notification.setProgress( 100, 0, true );
			NotificationManager notificationManager = (NotificationManager) context.getSystemService( Context.NOTIFICATION_SERVICE );
			notificationManager.notify( RestoreAllHelper.NOTIFICATION_ONGOING_ID, notification.build() );
		}
	}

	public void setOnPackSelectedListener( onPackSelectedListener listener ) {
		mPackSelectedListener = listener;
	}

	private String getPackTypeString( PackType packType ) {

		int res = -1;
		switch ( packType ) {
			case FRAME:
				res = R.string.feather_borders;
				break;

			case EFFECT:
				res = R.string.feather_effects;
				break;

			case STICKER:
				res = R.string.feather_stickers;
				break;
		}

		if ( res > 0 ) {
			return getContext().getString( res );
		}
		return "";
	}

	private void processList( final PackType packType, final boolean update ) {
		logger.info( "processList: %b", update );

		if ( null != packType ) {

			int delayTime = getResources().getInteger( android.R.integer.config_mediumAnimTime ) + 300;

			registerContentObservers( packType );
			
			if ( null == getHandler() ) return;
			
			getHandler().postDelayed( new Runnable() {

				@Override
				public void run() {
					if( update ) {
						new QueryInventoryAsyncTask().execute( packType );
					} else {
						if( android.os.Build.VERSION.SDK_INT < 16 ) {
							final Cursor cursor = createCursor( packType );
							mAdapter.changeCursor( cursor );							
						} else {
							mAdapter.notifyDataSetChanged();
						}
					}
				}
			}, delayTime );
			

		} else {
			mAdapter.changeCursor( null );
		}

	}

	private Cursor createCursor( PackType packType ) {
		String query = "pack/type/" + packType.toCdsString() + "/content/available/list";
		if( android.os.Build.VERSION.SDK_INT < 16 ) {
			query += "/not_purchased";
		}
		
		return getContext().getContentResolver().query(
				PackageManagerUtils.getCDSProviderContentUri( getContext(), query ),
				new String[] { PacksColumns._ID + " as _id", PacksColumns._ID, PacksColumns.PACK_TYPE, PacksColumns.IDENTIFIER, PacksContentColumns._ID,
						PacksContentColumns.CONTENT_PATH, PacksContentColumns.CONTENT_URL, PacksContentColumns.DISPLAY_NAME, PacksContentColumns.ICON_PATH,
						PacksContentColumns.ICON_URL, PacksContentColumns.IS_FREE_PURCHASE, PacksContentColumns.PURCHASED, PacksContentColumns.PACK_ID,
						PacksContentColumns.ITEMS_COUNT }, 
				null, 
				null, 
				PacksContentColumns.PURCHASED + " ASC, " + PacksColumns.DISPLAY_ORDER + " ASC" );
	}
	

	String getPackType( int type ) {
		switch ( type ) {
			case FeatherIntent.PluginType.TYPE_BORDER:
				return AviaryCds.PACKTYPE_FRAME;

			case FeatherIntent.PluginType.TYPE_FILTER:
				return AviaryCds.PACKTYPE_EFFECT;

			case FeatherIntent.PluginType.TYPE_STICKER:
				return AviaryCds.PACKTYPE_STICKER;
		}
		return null;
	}

	private void registerContentObservers( PackType packType ) {
		logger.info( "registerContentObserver" );
		unregisterContentObservers();
		getContext().getContentResolver().registerContentObserver( PackageManagerUtils.getCDSProviderContentUri( getContext(), "pack/purchased" ), true, mPackPurchasedContentObserver );
		getContext().getContentResolver().registerContentObserver( PackageManagerUtils.getCDSProviderContentUri( getContext(), "service/finished" ), false,
				mServiceFinishedContentObserver );
	}

	private void unregisterContentObservers() {
		logger.info( "unregisterContentObserver" );
		getContext().getContentResolver().unregisterContentObserver( mServiceFinishedContentObserver );
		getContext().getContentResolver().unregisterContentObserver( mPackPurchasedContentObserver );
	}

	@Override
	protected void onAttachedToWindow() {
		logger.info( "onAttachedToWindow" );
		super.onAttachedToWindow();

		mAttached = true;
		
		mPicassoLibrary = Picasso.with( getContext() );
		
		mDataService = ((FeatherActivity) getContext()).getMainController().getService( LocalDataService.class );
		
		mList = (ListView) findViewById( R.id.aviary_list );
		mRestoreAllButton = (Button) findViewById( R.id.aviary_button );
		mRestoreAllButton.setOnClickListener( this );
		mListProgress = findViewById( R.id.aviary_iap_list_progress );
		mSummaryText = (TextView) findViewById( R.id.aviary_summary );
		String summary = getContext().getString( R.string.feather_iap_restore_all_summary );
		
		if( PackageManagerUtils.isStandalone( getContext() ) ) {
			
			String link = getContext().getString( R.string.feather_details ) + ""; 
			
			SpannableString baseString = new SpannableString( summary + " " + link );
			
			ClickableSpan span = new ClickableSpan() {
				
				@Override
				public void onClick( View widget ) {
					Intent intent = new Intent( Intent.ACTION_VIEW );
					intent.setComponent( AviaryIntent.getTutorialComponent( getContext() ) );
					intent.setData( Uri.parse( "aviary://launch-activity/iap_tutorial" ) );
					try {
						getContext().startActivity( intent );
					} catch( ActivityNotFoundException e ) {
						e.printStackTrace();
					}				
				}
			};
			
			baseString.setSpan( span, summary.length() + 1, baseString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE );
			mSummaryText.setText( baseString );
			Linkify.addLinks( mSummaryText, Linkify.ALL );
			mSummaryText.setMovementMethod( LinkMovementMethod.getInstance() );
		} else {
			mSummaryText.setText( summary );
		}
		
		mAdapter = new ListAdapter( getContext(), null );
		
		if( android.os.Build.VERSION.SDK_INT >= 16 && SystemUtils.getCpuMhz() >= Constants.MHZ_CPU_FAST ) {
			mAnimationAdapter = new ListAnimator( mAdapter );
			mAnimationAdapter.setAbsListView( mList );
			mList.setAdapter( mAnimationAdapter );
		} else {
			mList.setAdapter( mAdapter );
		}
		
		mList.setOnItemClickListener( this );
		mList.setItemsCanFocus( true );
		mList.setOnScrollListener( this );

		initializeWrapper();
	}

	@Override
	protected void onDetachedFromWindow() {
		logger.info( "onDetachedFromWindow" );
		super.onDetachedFromWindow();
		
		unregisterContentObservers();

		mRestoreAllButton.setOnClickListener( null );
		mAdapter.changeCursor( null );
		mList.setOnItemClickListener( null );
		mList.setOnScrollListener( null );

		if ( null != mWrapper ) {
			mWrapper.dispose();
			mWrapper = null;
		}
		
		mList.setAdapter( null );
		
		if( null != mAnimationAdapter ) {
			mAnimationAdapter.setAbsListView( null );
		}

		mAttached = false;
	}

	@Override
	protected void onConfigurationChanged( Configuration newConfig ) {
		logger.info( "onConfigurationChanged" );
		super.onConfigurationChanged( newConfig );
	}

	@Override
	protected void onFinishInflate() {
		logger.info( "onFinishInflate" );
		super.onFinishInflate();
	}

	@Override
	protected void onVisibilityChanged( View changedView, int visibility ) {
		logger.info( "onVisibilityChanged: " + visibility );
		super.onVisibilityChanged( changedView, visibility );
	}

	@Override
	public void onClick( View v ) {
		final int id = v.getId();
		if ( id == mRestoreAllButton.getId() ) {
			onRestoreAll();
		}
	}

	class ListAdapter extends CursorAdapter {

		class ViewHolder {
			long packid;
			String identifier;
			TextView title;
			TextView text;
			ImageView icon;
			IAPBuyButton buttonContainer;

			FutureListener<Pair<Long, PackOptionWithPrice>> listener = new FutureListener<Pair<Long, PackOptionWithPrice>>() {

				@Override
				public void onFutureDone( Future<Pair<Long, PackOptionWithPrice>> future ) {
					Pair<Long, PackOptionWithPrice> result = null;
					if ( null != future ) {
						try {
							result = future.get();
						} catch ( Throwable t ) {
							return;
						}
					}

					if ( packid == result.first ) {
						buttonContainer.setPackOption( result.second, packid );
					}
				}
			};
		}

		BitmapDrawable mFolderIcon;
		String mPackTypeText;
		int idColumnIndex;
		int displayNameColumnIndex;
		int iconPackColumnIndex;
		int identifierColumnIndex;
		int itemsCountColumnIndex;
		int mMaxImageSize;

		String[] itemsCursorProjectionIn = new String[] { PacksItemsColumns._ID };

		public ListAdapter ( Context context, Cursor c ) {
			super( context, c, false );
			mFolderIcon = (BitmapDrawable) context.getResources().getDrawable( R.drawable.aviary_effects_pack_background );
			mMaxImageSize = context.getResources().getDimensionPixelSize( R.dimen.aviary_iap_list_item_image_size );
			initCursor( c );
		}

		@Override
		public Cursor swapCursor( Cursor newCursor ) {
			initCursor( newCursor );
			return super.swapCursor( newCursor );
		}

		private void initCursor( Cursor cursor ) {
			if ( null != cursor ) {
				idColumnIndex = cursor.getColumnIndex( PacksColumns._ID );
				displayNameColumnIndex = cursor.getColumnIndex( PacksContentColumns.DISPLAY_NAME );
				iconPackColumnIndex = cursor.getColumnIndex( PacksContentColumns.ICON_PATH );
				identifierColumnIndex = cursor.getColumnIndex( PacksColumns.IDENTIFIER );
				itemsCountColumnIndex = cursor.getColumnIndex( PacksContentColumns.ITEMS_COUNT );
			}
		}

		@Override
		public void bindView( View view, Context context, Cursor cursor ) {

			if ( !isValidContext() ) return;

			final ViewHolder holder = (ViewHolder) view.getTag();
			if ( null == holder ) return;

			long packid = cursor.getLong( idColumnIndex );
			String title = cursor.getString( displayNameColumnIndex );
			final String iconPath = cursor.getString( iconPackColumnIndex );
			String identifier = cursor.getString( identifierColumnIndex );
			int itemsCount = cursor.getInt( itemsCountColumnIndex );

			boolean process = true;
			
			if ( null != iconPath ) {

				Object tag = holder.icon.getTag();
				int hashCode = iconPath.hashCode();
				if ( tag instanceof Integer ) {
					if ( ( (Integer) tag ).intValue() == hashCode ) {
						logger.warn( "no need to download the icon again" );
						process = false;
					}
				}
				
				if ( process ) {
					
					// from here
					mPicassoLibrary
						.load( iconPath )
						.resize( mMaxImageSize, mMaxImageSize, true )
						.transform( new PackIconCallable( getResources(), mData.getPackType(), iconPath ) )
						.error( R.drawable.aviary_ic_na_gold )
						.noFade()
						.into( holder.icon, new Callback() {
							
							@Override
							public void onSuccess() {
								holder.icon.setTag( iconPath.hashCode() );
							}
							
							@Override
							public void onError() {
							}
						} );
				}
			} else {
				holder.icon.setImageBitmap( null );
				holder.icon.setTag( null );
			}

			holder.packid = packid;
			holder.identifier = identifier;
			
			if( process ) {
				holder.title.setText( title );

				if ( null == mPackTypeText ) {
					mPackTypeText = getPackTypeString( getData().getPackType() );
				}
	
				if ( itemsCount > 0 ) {
					holder.text.setText( "(" + itemsCount + " " + mPackTypeText + ")" );
				} else {
					holder.text.setText( "" );
				}
			}

			PackOptionWithPrice result;
			Pair<PackOption, String> pair = CdsUtils.getPackOptionDownloadStatus( getContext(), holder.packid );
			PackOptionWithPrice cacheValue = priceMap.get( holder.packid );
			
			if ( null != pair ) {
				// special case, pack is downloaded and is also already installed
				if( null != cacheValue && cacheValue.option == PackOption.OWNED && pair.first == PackOption.DOWNLOAD_COMPLETE ) {
					result = cacheValue;
				} else {
					result = new PackOptionWithPrice( pair.first );
				}
			} else {
				result = cacheValue;
				if ( null == result ) {
					result = new PackOptionWithPrice( PackOption.DOWNLOADING );
				}
			}

			holder.buttonContainer.setPackOption( result, holder.packid );
		}

		@Override
		public View newView( Context context, Cursor cursor, ViewGroup parent ) {
			final View view = LayoutInflater.from( context ).inflate( R.layout.aviary_iap_list_item, parent, false );
			
			@SuppressWarnings ( "unused" )
			final int position = cursor.getPosition();

			IAPBuyButton buttonContainer = (IAPBuyButton) view.findViewById( R.id.aviary_buy_button );
			TextView textView1 = (TextView) view.findViewById( R.id.aviary_title );
			TextView textView2 = (TextView) view.findViewById( R.id.aviary_text );
			ImageView imageView = (ImageView) view.findViewById( R.id.aviary_image );

			ViewHolder holder = new ViewHolder();
			holder.title = textView1;
			holder.text = textView2;
			holder.icon = imageView;
			holder.buttonContainer = buttonContainer;

			buttonContainer.setOnClickListener( new OnClickListener() {

				@Override
				public void onClick( View v ) {
					logger.info( "onClick: %s", v );

					mScrollStateChanged = true;

					PackOptionWithPrice packOption = ( (IAPBuyButton) v ).getPackOption();
					if ( null == packOption ) return;

					ViewGroup parent = (ViewGroup) v.getParent();
					if ( null == parent ) return;
					
					parent = (ViewGroup) parent.getParent();
					if ( null == parent ) return;

					ViewHolder holder = (ViewHolder) parent.getTag();
					logger.log( "holder: %s", holder );
					
					if ( null == holder || holder.packid < 0 || null == holder.identifier ) return;

					if ( null != packOption ) {
						
						logger.log( "option: " + packOption.option );
						
						switch ( packOption.option ) {
							case PURCHASE:
								mParent.launchPackPurchaseFlow( holder.packid, holder.identifier, mData.getPackType().toCdsString(), "ListView", packOption.price );
								break;

							case ERROR:
								new DeterminePackOptionAsyncTask( holder ).execute( holder.packid );
								break;

							case FREE:
							case RESTORE:
								IAPDialogMain.trackBeginPurchaseFlow( getContext(), holder.identifier, mData.getPackType().toCdsString(), "ListView",
										packOption.price, packOption.option == PackOption.RESTORE, packOption.option == PackOption.FREE );

							case DOWNLOAD_ERROR:
								PackOptionWithPrice newOption;
								logger.log( "requestPackDownload: " + holder.packid );

								if( packOption.option == PackOption.FREE ) {
									mParent.sendReceipt( holder.identifier, true, false );
								} else if( packOption.option == PackOption.RESTORE ){
									mParent.sendReceipt( holder.identifier, false, true );
								}
								
								try {
									mParent.requestPackDownload( holder.packid );
									Pair<PackOption, String> pair = CdsUtils.getPackOptionDownloadStatus( getContext(), holder.packid );
									if( null != pair ) {
										newOption = new PackOptionWithPrice( pair.first );
									} else {
										newOption = new PackOptionWithPrice( PackOption.DOWNLOADING );
									}
									
								} catch( Throwable t ) {
									newOption = new PackOptionWithPrice( PackOption.DOWNLOAD_ERROR );
									
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
								
								if( PackOption.isDetermined( newOption.option )) {
									priceMap.put( holder.packid, newOption );
								}
								
								holder.buttonContainer.setPackOption( newOption, holder.packid );
								break;

							default:
								break;
						}
					}
				}
			} );

			view.setTag( holder );

			if ( !mScrollStateChanged ) {
				/*
				view.setVisibility( View.INVISIBLE );
				Animation anim = AnimationUtils.loadAnimation( getContext(), android.R.anim.fade_in );
				anim.setStartOffset( position * 100 );
				anim.setAnimationListener( new AnimationListener() {

					@Override
					public void onAnimationStart( Animation animation ) {}

					@Override
					public void onAnimationRepeat( Animation animation ) {}

					@Override
					public void onAnimationEnd( Animation animation ) {
						view.setVisibility( View.VISIBLE );
					}
				} );
				view.startAnimation( anim );
				*/
			}

			return view;
		}

	}

	@Override
	public void onItemClick( AdapterView<?> parent, View view, int position, long id ) {
		// ListView item click
		if ( null != mPackSelectedListener ) {
			mPackSelectedListener.onPackSelected( id, mData.getPackType() );
			mScrollStateChanged = true;
		}
	}

	@Override
	public void onScrollStateChanged( AbsListView view, int scrollState ) {
		logger.info( "onScrollStateChanged: %d ", scrollState );

		if ( scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL ) {
			mScrollStateChanged = true;
			mList.setOnScrollListener( null );
		}
	}

	@Override
	public void onScroll( AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount ) {}

	class QueryInventoryAsyncTask extends AviaryAsyncTask<PackType, Void, HashMap<Long, PackOptionWithPrice>> {
		
		IabResult mIabResult;
		Cursor mCursor;

		@Override
		protected void PreExecute() {}

		@Override
		protected HashMap<Long, PackOptionWithPrice> doInBackground( PackType... params ) {
			HashMap<Long, PackOptionWithPrice> map = new HashMap<Long, PackOptionWithPrice>();

			long t0 = System.currentTimeMillis();

			if ( null != mWrapper ) {
				mIabResult = CdsUtils.waitForIAPSetupDone( mWrapper );
			}
			
			logger.log( "mIabresult: %s", mIabResult );

			long t1 = System.currentTimeMillis();
			logger.log( "wait setupdone time: %d", ( t1 - t0 ) );

			mCursor = createCursor( params[0] );
			if ( null == mCursor ) return map;

			HashMap<Long, String> checkList = new HashMap<Long, String>();
			long t2 = System.currentTimeMillis();

			try {
				while ( mCursor.moveToNext() ) {
					PacksColumns.PackCursorWrapper pack = PacksColumns.PackCursorWrapper.create( mCursor );
					PacksContentColumns.ContentCursorWrapper content = PacksContentColumns.ContentCursorWrapper.create( mCursor );
					pack.setContent( content );
					
					if( priceMap.containsKey( pack.getId() ) ) {
						PackOptionWithPrice cache = priceMap.get( pack.getId() );
						if( null != cache && ( cache.option == PackOption.OWNED || cache.option == PackOption.PURCHASE || cache.option == PackOption.RESTORE ) ) {
							// don't query the inventory if this pack has already been determined 
							continue;
						}
					}

					PackOptionWithPrice result = new PackOptionWithPrice( CdsUtils.getPackOption( getContext(), pack ), null );
					
					switch ( result.option ) {
						case ERROR:
						case PACK_OPTION_BEING_DETERMINED:
							if ( null != mWrapper && null != mIabResult && mIabResult.isSuccess() ) {
								checkList.put( pack.getId(), pack.getIdentifier() );
							} else {
								map.put( pack.getId(), new PackOptionWithPrice( PackOption.ERROR ) );
							}
							break;

						case RESTORE:
						case OWNED:
						case FREE:
						case PURCHASE:
							map.put( pack.getId(), result );
							break;

						default:
							break;
					}
				}
			} finally {
				// IOUtils.closeSilently( mCursor );
			}

			long t3 = System.currentTimeMillis();

			logger.log( "need to check %d items in the inventory", checkList.size() );
			
			if( checkList.size() > 0 ) {
				if ( null != mWrapper && null != mIabResult && mIabResult.isSuccess() ) {
					
					// try..catch
					HashMap<Long, PackOptionWithPrice> inventoryMap = null;
					
					try {
						inventoryMap = getPackOptionsFromInventory( mWrapper, checkList );
					} catch( NullPointerException e ) {
						e.printStackTrace();
					}
					
					if ( null != inventoryMap ) {
						map.putAll( inventoryMap );
					} else {
						logger.error( "must put errors!" );
						for( Long id : checkList.keySet() ) {
							map.put( id, new PackOptionWithPrice( PackOption.ERROR ) );
						}
					}
				}
			}

			long t4 = System.currentTimeMillis();

			logger.log( "checking packs time: %d", ( t3 - t2 ) );
			logger.log( "query inventory time: %d", ( t4 - t3 ) );
			logger.log( "total time: %d", ( t4 - t0 ) );

			return map;
		}

		@Override
		protected void PostExecute( HashMap<Long, PackOptionWithPrice> result ) {
			logger.info( "QueryInventoryAsyncTask::PostExecute" );
			logger.log( "result: %s", result );
			logger.log( "mIabResult: %s", mIabResult );
			
			if( null != mIabResult && mIabResult.isFailure() ) {
				logger.warn( mIabResult.getMessage() );
				if( mIabResult.getResponse() != IabHelper.IABHELPER_MISSING_SIGNATURE ) {
					Toast.makeText( getContext(), mIabResult.getMessage(), Toast.LENGTH_SHORT ).show();
				}
			}

			priceMap.putAll( result );
			
			mAdapter.changeCursor( mCursor );
			// mAdapter.notifyDataSetChanged();
			mListProgress.setVisibility( View.GONE );
		}
	}

	class DeterminePackOptionAsyncTask extends AviaryAsyncTask<Long, PackOptionWithPrice, PackOptionWithPrice> {

		IabResult mResult;
		PacksColumns.PackCursorWrapper mPack;
		WeakReference<ViewHolder> mTargetView;
		IabResult mIabResult;

		public DeterminePackOptionAsyncTask ( ViewHolder view ) {
			mTargetView = new WeakReference<ViewHolder>( view );
		}

		private void onPackOptionUpdated( final PackOptionWithPrice option, final PacksColumns.PackCursorWrapper pack ) {
			if ( !isValidContext() ) return;

			ViewHolder holder = mTargetView.get();
			if ( null == holder || holder.packid != pack.getId() ) return;

			holder.buttonContainer.setPackOption( option, pack.getId() );
		}

		@Override
		protected void PreExecute() {}

		@Override
		protected void ProgressUpdate( PackOptionWithPrice... values ) {
			if ( null != values ) {
				PackOptionWithPrice option = values[0];
				logger.info( "DeterminePackOptionAsyncTask::ProgressUpdate: %s - %s", mPack.getIdentifier(), option );
				onPackOptionUpdated( option, mPack );
			}
		}

		@Override
		protected void PostExecute( PackOptionWithPrice result ) {
			if ( !isValidContext() ) return;
			if ( null == mPack ) return;

			logger.info( "DeterminePackOptionAsyncTask::PostExecute: %s - %s", mPack.getIdentifier(), result );

			if ( null != result ) {
				onPackOptionUpdated( result, mPack );
			} else {
				onPackOptionUpdated( new PackOptionWithPrice( PackOption.ERROR, null ), mPack );
			}
			
			if( null != mIabResult && mIabResult.isFailure() ) {
				logger.warn( mIabResult.getMessage() );
				Toast.makeText( getContext(), mIabResult.getMessage(), Toast.LENGTH_SHORT ).show();
			}
		}

		@Override
		protected PackOptionWithPrice doInBackground( Long... params ) {

			if ( !isValidContext() ) return null;

			long packId = params[0];
			mPack = CdsUtils.getPackFullInfoById( getContext(), packId );

			publishProgress( new PackOptionWithPrice( PackOption.DOWNLOADING ) );

			Pair<PackOption, String> pair = CdsUtils.getPackOptionDownloadStatus( getContext(), mPack.getId() );
			if ( null != pair ) {
				return new PackOptionWithPrice( pair.first );
			}

			PackOptionWithPrice result = priceMap.get( mPack.getId() );
			if ( null != result && result.option != PackOption.ERROR ) {
				return result;
			}

			// 3. check pack option status ( free/restore/owned )
			result = new PackOptionWithPrice( CdsUtils.getPackOption( getContext(), mPack ), null );

			if ( result.option == PackOption.PACK_OPTION_BEING_DETERMINED ) {

				publishProgress( result );
				
				if( null != mWrapper && !mWrapper.isDisposed() ) {
					mIabResult = CdsUtils.waitForIAPSetupDone( mWrapper );
					if( null == mIabResult || mIabResult.isFailure() ) {
						result = new PackOptionWithPrice( PackOption.ERROR );
					} else {
						// need to check the google play inventory
						try {
							result = IAPDialogMain.getFromInventory( mWrapper, mPack.getIdentifier() );
						} catch( NullPointerException e ) {
							e.printStackTrace();
							result = new PackOptionWithPrice( PackOption.ERROR );
						}
					}
				} else {
					result = new PackOptionWithPrice( PackOption.ERROR );
				}
			}

			if ( PackOption.isDetermined( result.option ) ) {
				priceMap.put( mPack.getId(), result );
			}
			return result;
		}
	}

	private static HashMap<Long, PackOptionWithPrice> getPackOptionsFromInventory( IAPWrapper instance, HashMap<Long, String> skus ) {
		logger.info( "getPackOptionsFromInventory: %s", skus );
		
		HashMap<Long, PackOptionWithPrice> result = new HashMap<Long, PackOptionWithPrice>();
		Inventory inventory = null;
		try {
			if ( instance.isAvailable() ) {
				logger.log( "isAvailable" );
				ArrayList<String> array = new ArrayList<String>();
				array.addAll( skus.values() );
				logger.log( "checking skus: %s", array );
				inventory = instance.queryInventory( true, array );
			} else {
				logger.error( "isAvailable = false" );
				return null;
			}
		} catch ( IabException e ) {
			e.printStackTrace();
			return null;
		}

		if ( null != inventory ) {
			Iterator<Entry<Long, String>> iterator = skus.entrySet().iterator();
			while ( iterator.hasNext() ) {
				Entry<Long, String> entry = iterator.next();

				Purchase itemPurchase = inventory.getPurchase( entry.getValue() );
				if ( null != itemPurchase ) {
					result.put( entry.getKey(), new PackOptionWithPrice( PackOption.RESTORE ) );
				} else {
					SkuDetails itemDetails = inventory.getSkuDetails( entry.getValue() );
					if ( null != itemDetails ) {
						result.put( entry.getKey(), new PackOptionWithPrice( PackOption.PURCHASE, itemDetails.getPrice() ) );
					} else {
						result.put( entry.getKey(), new PackOptionWithPrice( PackOption.ERROR ) );
					}
				}
			}
			return result;
		}
		return null;
	}
	

	/**
	 * Returns true if the current context is valid and
	 * the view is currently attached
	 * 
	 * @return
	 */
	boolean isValidContext() {
		return null != getContext() && mAttached;
	}
	
	
	class ListAnimator extends SwingBottomInAnimationAdapter {

		public ListAnimator ( BaseAdapter baseAdapter ) {
			super( baseAdapter );
		}
		
		@Override
		protected Animator getAnimator( ViewGroup parent, View view ) {
			return ObjectAnimator.ofFloat(view, "translationY", view.getHeight(), 0);
		}
		
	}
}
