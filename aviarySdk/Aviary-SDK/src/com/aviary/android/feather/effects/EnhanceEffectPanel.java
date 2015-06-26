package com.aviary.android.feather.effects;

import org.json.JSONException;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.aviary.android.feather.R;
import com.aviary.android.feather.common.utils.SystemUtils;
import com.aviary.android.feather.common.utils.os.AviaryAsyncTask;
import com.aviary.android.feather.headless.moa.Moa;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.EnhanceFilter;
import com.aviary.android.feather.library.filters.EnhanceFilter.Types;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.services.LocalDataService;
import com.aviary.android.feather.library.utils.BitmapUtils;
import com.aviary.android.feather.widget.AviaryHighlightImageButton;
import com.aviary.android.feather.widget.AviaryHighlightImageButton.OnCheckedChangeListener;

public class EnhanceEffectPanel extends AbstractOptionPanel implements OnCheckedChangeListener {

	// current rendering task
	private RenderTask mCurrentTask;
	// panel is renderding
	volatile boolean mIsRendering = false;

	private Filters mFilterType;
	boolean enableFastPreview = false;
	MoaActionList mActions = null;
	// ui buttons
	AviaryHighlightImageButton mButton1, mButton2, mButton3;
	// current selected button
	private AviaryHighlightImageButton mCurrent;

	public EnhanceEffectPanel ( IAviaryController context, ToolEntry entry, Filters type ) {
		super( context, entry );
		mFilterType = type;
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		ViewGroup panel = getOptionView();

		mButton1 = (AviaryHighlightImageButton) panel.findViewById( R.id.button1 );
		mButton1.setOnCheckedChangeListener( this );
		if ( mButton1.isChecked() ) mCurrent = mButton1;

		mButton2 = (AviaryHighlightImageButton) panel.findViewById( R.id.button2 );
		mButton2.setOnCheckedChangeListener( this );
		if ( mButton2.isChecked() ) mCurrent = mButton2;

		mButton3 = (AviaryHighlightImageButton) panel.findViewById( R.id.button3 );
		mButton3.setOnCheckedChangeListener( this );
		if ( mButton3.isChecked() ) mCurrent = mButton3;
	}

	@Override
	public void onActivate() {
		super.onActivate();
		mPreview = BitmapUtils.copy( mBitmap, Config.ARGB_8888 );

		LocalDataService dataService = getContext().getService( LocalDataService.class );
		enableFastPreview = dataService.getFastPreviewEnabled();
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();

		mButton1.setOnCheckedChangeListener( null );
		mButton2.setOnCheckedChangeListener( null );
		mButton3.setOnCheckedChangeListener( null );
	}

	@Override
	public boolean isRendering() {
		return mIsRendering;
	}

	@Override
	public void onBitmapReplaced( Bitmap bitmap ) {
		super.onBitmapReplaced( bitmap );

		if ( isActive() ) {
			mButton1.setChecked( false );
			mButton2.setChecked( false );
			mButton3.setChecked( false );
		}
	}

	@Override
	public void onCheckedChanged( AviaryHighlightImageButton buttonView, boolean isChecked, boolean fromUser ) {

		if ( mCurrent != null && !buttonView.equals( mCurrent ) ) {
			mCurrent.setChecked( false );
		}

		final int id = buttonView.getId();
		mCurrent = buttonView;

		if ( !isActive() || !isEnabled() || !fromUser ) return;

		Types type = Types.HiDef;

		killCurrentTask();

		if ( id == R.id.button1 ) {
			type = Types.HiDef;
		} else if ( id == R.id.button2 ) {
			type = Types.Illuminate;
		} else if ( id == R.id.button3 ) {
			type = Types.ColorFix;
		}

		if ( !isChecked ) {
			// restore the original image
			BitmapUtils.copy( mBitmap, mPreview );
			onPreviewChanged( mPreview, false, true );
			setIsChanged( false );
			mActions = null;

			mTrackingAttributes.clear();

		} else {
			if ( type != null ) {
				mCurrentTask = new RenderTask();
				mCurrentTask.execute( type );

				mTrackingAttributes.put( "Effects", type.name() );
			}
		}
	}

	@Override
	protected void onProgressStart() {
		if ( !enableFastPreview ) {
			onProgressModalStart();
			return;
		}
		super.onProgressStart();
	}

	@Override
	protected void onProgressEnd() {
		if ( !enableFastPreview ) {
			onProgressModalEnd();
			return;
		}
		super.onProgressEnd();
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_enhance, parent, false );
	}

	@Override
	public boolean onBackPressed() {
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
	public boolean onCancel() {
		killCurrentTask();
		return super.onCancel();
	}

	private void killCurrentTask() {
		if ( mCurrentTask != null ) {
			synchronized ( mCurrentTask ) {
				mCurrentTask.cancel( true );
				mCurrentTask.renderFilter.stop();
				onProgressEnd();
			}
			mIsRendering = false;
			mCurrentTask = null;
		}
	}

	@Override
	public boolean getIsChanged() {
		return super.getIsChanged() || mIsRendering == true;
	}

	class RenderTask extends AviaryAsyncTask<Types, Void, Bitmap> {

		String mError;

		volatile EnhanceFilter renderFilter;

		public RenderTask () {
			renderFilter = (EnhanceFilter) FilterLoaderFactory.get( mFilterType );
			mError = null;
		}

		@Override
		protected void PreExecute() {
			onProgressStart();
		}

		@Override
		protected Bitmap doInBackground( Types... params ) {
			if ( isCancelled() ) return null;

			Bitmap result = null;
			mIsRendering = true;
			Types type = params[0];
			renderFilter.setType( type );

			try {
				result = renderFilter.execute( mBitmap, mPreview, 1, 1 );
				mActions = renderFilter.getActions();
			} catch ( JSONException e ) {
				e.printStackTrace();
				mError = e.getMessage();
				return null;
			}

			if ( isCancelled() ) return null;
			return result;
		}

		@Override
		protected void PostExecute( Bitmap result ) {

			if ( !isActive() ) return;

			onProgressEnd();

			if ( isCancelled() ) return;

			if ( result != null ) {

				if ( SystemUtils.isHoneyComb() ) {
					Moa.notifyPixelsChanged( mPreview );
				}

				onPreviewChanged( mPreview, false, true );
			} else {
				if ( mError != null ) {
					onGenericError( mError, android.R.string.ok, null );
				}
			}

			mIsRendering = false;
			mCurrentTask = null;
		}

		@Override
		protected void onCancelled() {
			renderFilter.stop();
			super.onCancelled();
		}
	}

	@Override
	protected void onGenerateResult() {

		if ( mIsRendering ) {
			GenerateResultTask task = new GenerateResultTask();
			task.execute();
		} else {
			onComplete( mPreview, mActions );
		}
	}

	class GenerateResultTask extends AviaryAsyncTask<Void, Void, Void> {

		/** The m progress. */
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
