package com.aviary.android.feather.effects;

import org.json.JSONException;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.Bundle;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.SystemUtils;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.headless.filters.INativeRangeFilter;
import com.aviary.android.feather.headless.moa.Moa;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.headless.moa.MoaResult;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.utils.BitmapUtils;

public class NativeEffectRangePanel extends SliderEffectPanel {

	ApplyFilterTask mCurrentTask;
	volatile boolean mIsRendering = false;
	boolean enableFastPreview;
	MoaActionList mActions = null;

	public NativeEffectRangePanel ( IAviaryController context, ToolEntry entry, Filters type, String resourcesBaseName ) {
		super( context, entry, type, resourcesBaseName );
		mFilter = FilterLoaderFactory.get( type );
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );
	}

	@Override
	public void onBitmapReplaced( Bitmap bitmap ) {
		super.onBitmapReplaced( bitmap );

		if ( isActive() ) {
			applyFilter( 0, false );
			setValue( 50 );
		}
	}

	@Override
	public void onActivate() {
		super.onActivate();
		mPreview = BitmapUtils.copy( mBitmap, Bitmap.Config.ARGB_8888 );
		onPreviewChanged( mPreview, true, true );
	}

	@Override
	public boolean isRendering() {
		return mIsRendering;
	}

	@Override
	protected void onSliderStart( int value ) {
		if ( enableFastPreview ) {
			onProgressStart();
		}
	}

	@Override
	protected void onSliderEnd( int value ) {
		mLogger.info( "onProgressEnd: " + value );

		value = ( value - 50 ) * 2;
		applyFilter( value, !enableFastPreview );

		if ( enableFastPreview ) {
			onProgressEnd();
		}
	}

	@Override
	protected void onSliderChanged( int value, boolean fromUser ) {
		mLogger.info( "onProgressChanged: " + value + ", fromUser: " + fromUser );

		if ( enableFastPreview || !fromUser ) {
			value = ( value - 50 ) * 2;
			applyFilter( value, !fromUser );
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onDeactivate() {
		onProgressEnd();
		super.onDeactivate();
	}

	@Override
	protected void onGenerateResult() {
		mLogger.info( "onGenerateResult: " + mIsRendering );

		if ( mIsRendering ) {
			GenerateResultTask task = new GenerateResultTask();
			task.execute();
		} else {
			onComplete( mPreview, mActions );
		}
	}

	@Override
	public boolean onBackPressed() {
		mLogger.info( "onBackPressed" );
		killCurrentTask();
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
		return super.getIsChanged() && ( mIsRendering == true || mActions != null );
	}

	boolean killCurrentTask() {
		if ( mCurrentTask != null ) {
			return mCurrentTask.cancel( true );
		}
		return false;
	}

	protected void applyFilter( float value, boolean showProgress ) {
		mLogger.info( "applyFilter: " + value );
		killCurrentTask();

		if ( value == 0 ) {
			BitmapUtils.copy( mBitmap, mPreview );
			onPreviewChanged( mPreview, false, true );
			setIsChanged( false );
			mActions = null;
		} else {
			mCurrentTask = new ApplyFilterTask( value, showProgress );
			mCurrentTask.execute( mBitmap );
		}
	}

	class ApplyFilterTask extends AviaryAsyncTask<Bitmap, Void, Bitmap> {

		MoaResult mResult;
		boolean mShowProgress;

		public ApplyFilterTask ( float value, boolean showProgress ) {
			mShowProgress = showProgress;
			( (INativeRangeFilter) mFilter ).setValue( value );
		}

		@Override
		protected void PreExecute() {
			mLogger.info( "PreExecute" );

			try {
				mResult = ( (INativeRangeFilter) mFilter ).prepare( mBitmap, mPreview, 1, 1 );
			} catch ( JSONException e ) {
				e.printStackTrace();
			}

			if ( mShowProgress ) {
				onProgressStart();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mLogger.info( "onCancelled" );
			if ( mResult != null ) {
				mResult.cancel();
			}

			if ( mShowProgress ) {
				onProgressEnd();
			}
			mIsRendering = false;
		}

		@Override
		protected Bitmap doInBackground( Bitmap... arg0 ) {

			if ( isCancelled() ) return null;
			mIsRendering = true;

			long t1 = System.currentTimeMillis();
			try {
				mResult.execute();
				mActions = ( (INativeRangeFilter) mFilter ).getActions();
			} catch ( Exception exception ) {
				exception.printStackTrace();
				mLogger.error( exception.getMessage() );
				return null;
			}
			long t2 = System.currentTimeMillis();

			if ( null != mTrackingAttributes ) {
				mTrackingAttributes.put( "renderTime", Long.toString( t2 - t1 ) );
			}

			if ( isCancelled() ) return null;
			return mPreview;
		}

		@Override
		protected void PostExecute( Bitmap result ) {

			if ( !isActive() ) return;

			mLogger.info( "PostExecute" );

			if ( mShowProgress ) {
				onProgressEnd();
			}

			if ( result != null ) {
				if ( SystemUtils.isHoneyComb() ) {
					Moa.notifyPixelsChanged( mPreview );
				}
				onPreviewUpdated();
				// onPreviewChanged( mPreview, true );
			} else {
				BitmapUtils.copy( mBitmap, mPreview );
				onPreviewChanged( mPreview, false, true );
				setIsChanged( false );
			}
			mIsRendering = false;
			mCurrentTask = null;
		}
	}

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
				// mLogger.log( "waiting...." );
			}
			return null;
		}

		@Override
		protected void PostExecute( Void result ) {
			mLogger.info( "GenerateResultTask::PostExecute" );

			if ( getContext().getBaseActivity().isFinishing() ) return;
			if ( mProgress.isShowing() ) mProgress.dismiss();
			onComplete( mPreview, mActions );
		}
	}
}
