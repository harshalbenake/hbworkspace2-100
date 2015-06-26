package com.aviary.android.feather.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aviary.android.feather.R;
import com.aviary.android.feather.cds.CdsUtils;
import com.aviary.android.feather.cds.CdsUtils.PackOption;
import com.aviary.android.feather.widget.IAPDialogMain.PackOptionWithPrice;

public class IAPBuyButton extends RelativeLayout {

	TextView mTextView;
	View mProgress;
	PackOptionWithPrice mOption;
	long mPackId;

	public IAPBuyButton ( Context context, AttributeSet attrs ) {
		super( context, attrs );
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mTextView = (TextView) findViewById( R.id.aviary_buy_button_text );
		mProgress = findViewById( R.id.aviary_buy_button_loader );
	}

	public PackOptionWithPrice getPackOption() {
		return mOption;
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		getHandler().removeCallbacks( checkDownloadStatus );
	};
	
	Runnable checkDownloadStatus = new Runnable() {
		
		@Override
		public void run() {
			Log.d( VIEW_LOG_TAG, "checkDownloadStatus for " + mPackId );
			if( mPackId > -1 && null != getContext() && null != mOption ) {
				Pair<PackOption, String> result = CdsUtils.getPackOptionDownloadStatus( getContext(), mPackId );
				if( null != result ) {
					if( null != getContext() ) {
						setPackOption( new PackOptionWithPrice( result.first ), mPackId );
					}
				}
			}
		}
	};
	
	public void setPackOption( PackOptionWithPrice option, long packId ) {
		if ( null != option && option.equals( mOption ) ) {
			// no need to update
			return;
		}

		mOption = option;
		mPackId = packId;
		
//		if( LoggerFactory.LOG_ENABLED ) {
//			Log.i( VIEW_LOG_TAG, "setPackOption(" + packId + "): " + option );
//		}

		if( null != getHandler() )
			getHandler().removeCallbacks( checkDownloadStatus );
		
		if ( null == option ) {
			return;
		}
		
		setEnabled( true );
		
		switch ( option.option ) {
			case RESTORE:
				mTextView.setText( R.string.feather_iap_restore );
				mProgress.setVisibility( View.INVISIBLE );
				mTextView.setVisibility( View.VISIBLE );
				break;

			case PURCHASE:
				mProgress.setVisibility( View.INVISIBLE );
				mTextView.setVisibility( View.VISIBLE );
				if ( null != option.price ) {
					mTextView.setText( option.price );
				} else {
					mTextView.setText( R.string.feather_iap_unavailable );
				}
				break;

			case OWNED:
				mProgress.setVisibility( View.INVISIBLE );
				mTextView.setVisibility( View.VISIBLE );
				mTextView.setText( R.string.feather_iap_owned );
				setEnabled( false );
				break;

			case ERROR:
				mProgress.setVisibility( View.INVISIBLE );
				mTextView.setVisibility( View.VISIBLE );
				mTextView.setText( R.string.feather_iap_retry );
				break;

			case FREE:
				mProgress.setVisibility( View.INVISIBLE );
				mTextView.setVisibility( View.VISIBLE );
				mTextView.setText( R.string.feather_iap_download );
				break;
				
			case DOWNLOAD_COMPLETE:
				mProgress.setVisibility( View.INVISIBLE );
				mTextView.setVisibility( View.VISIBLE );
				mTextView.setText( R.string.feather_iap_installing );			
				setEnabled( false );
				break;

			case DOWNLOADING:
				mProgress.setVisibility( View.VISIBLE );
				mTextView.setVisibility( View.INVISIBLE );
				setEnabled( false );
				
				if( null != getHandler() )
					getHandler().postDelayed( checkDownloadStatus, (long) ( ( Math.random() * 100 ) + 900 ) );
				break;

			case PACK_OPTION_BEING_DETERMINED:
				mProgress.setVisibility( View.VISIBLE );
				mTextView.setVisibility( View.INVISIBLE );
				setEnabled( false );
				break;

			case DOWNLOAD_ERROR:
				mProgress.setVisibility( View.INVISIBLE );
				mTextView.setVisibility( View.VISIBLE );
				mTextView.setText( R.string.feather_iap_retry );
				break;
		}
	}

}
