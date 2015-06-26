package com.aviary.android.feather.effects;

import org.json.JSONException;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.aviary.android.feather.R;
import com.aviary.android.feather.headless.moa.MoaActionList;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.AdjustFilter;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.widget.AdjustImageView;
import com.aviary.android.feather.widget.AdjustImageView.FlipType;
import com.aviary.android.feather.widget.AdjustImageView.OnResetListener;
import com.aviary.android.feather.widget.AviaryHighlightImageButton;

public class AdjustEffectPanel extends AbstractContentPanel implements OnClickListener, OnResetListener {

	boolean isClosing;

	// panel buttons
	private AviaryHighlightImageButton mButton1, mButton2, mButton3, mButton4;
	// image overlay
	private AdjustImageView mAdjustImageView;

	public AdjustEffectPanel ( IAviaryController context, ToolEntry entry, Filters adjust ) {
		super( context, entry );
		mFilter = FilterLoaderFactory.get( adjust );
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		mAdjustImageView = (AdjustImageView) getContentView().findViewById( R.id.aviary_overlay );

		View v = getOptionView();
		mButton1 = (AviaryHighlightImageButton) v.findViewById( R.id.aviary_button1 );
		mButton2 = (AviaryHighlightImageButton) v.findViewById( R.id.aviary_button2 );
		mButton3 = (AviaryHighlightImageButton) v.findViewById( R.id.aviary_button3 );
		mButton4 = (AviaryHighlightImageButton) v.findViewById( R.id.aviary_button4 );
	}

	@Override
	public void onActivate() {
		super.onActivate();

		mAdjustImageView.setImageBitmap( mBitmap );
		mAdjustImageView.setOnResetListener( this );

		mButton1.setOnClickListener( this );
		mButton2.setOnClickListener( this );
		mButton3.setOnClickListener( this );
		mButton4.setOnClickListener( this );

		// straighten stuff
		contentReady();
	}

	@Override
	public void onDeactivate() {
		mAdjustImageView.setOnResetListener( null );
		mButton1.setOnClickListener( null );
		mButton2.setOnClickListener( null );
		mButton3.setOnClickListener( null );
		mButton4.setOnClickListener( null );

		super.onDeactivate();
	}

	@Override
	public void onDestroy() {
		mAdjustImageView.setImageBitmap( null );
		super.onDestroy();
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		return (ViewGroup) inflater.inflate( R.layout.aviary_panel_adjust, parent, false );
	}

	@Override
	protected View generateContentView( LayoutInflater inflater ) {
		return inflater.inflate( R.layout.aviary_content_adjust, null );
	}

	@Override
	public void onClick( View v ) {

		if ( !isActive() || !isEnabled() ) return;

		final int id = v.getId();

		if ( id == R.id.aviary_button1 ) {
			mAdjustImageView.rotate90( false );
		} else if ( id == R.id.aviary_button2 ) {
			mAdjustImageView.rotate90( true );
		} else if ( id == R.id.aviary_button3 ) {
			mAdjustImageView.flip( true );
		} else if ( id == R.id.aviary_button4 ) {
			mAdjustImageView.flip( false );
		}
	}

	@Override
	public boolean getIsChanged() {
		mLogger.info( "getIsChanged" );

		boolean straightenStarted = mAdjustImageView.getStraightenStarted();
		final int rotation = (int) mAdjustImageView.getCurrentRotation();
		final int flip_type = mAdjustImageView.getFlipType();
		return rotation != 0 || ( flip_type != FlipType.FLIP_NONE.nativeInt ) || straightenStarted;
	}

	@Override
	protected void onGenerateResult() {
		final int rotation = (int) mAdjustImageView.getCurrentRotation();
		final double rotationFromStraighten = mAdjustImageView.getStraightenAngle();
		final boolean horizontal = mAdjustImageView.getHorizontalFlip();
		final boolean vertical = mAdjustImageView.getVerticalFlip();
		final double growthFactor = ( 1 / mAdjustImageView.getGrowthFactor() );

		mLogger.log( "rotation: " + rotation );
		mLogger.log( "rotationFromStraigthen: " + rotationFromStraighten );
		mLogger.log( "flip: " + horizontal + ", " + vertical );
		mLogger.log( "growFactor: " + growthFactor );

		AdjustFilter filter = (AdjustFilter) mFilter;
		filter.setFlip( horizontal, vertical );
		filter.setFixedRotation( rotation );
		filter.setStraighten( rotationFromStraighten, growthFactor, growthFactor );

		Bitmap output;

		try {
			output = filter.execute( mBitmap, null, 1, 1 );
		} catch ( JSONException e ) {
			e.printStackTrace();
			onGenericError( e );
			return;
		}

		mAdjustImageView.setImageBitmap( output );
		onComplete( output, (MoaActionList) filter.getActions().clone() );

	}

	@Override
	public boolean onCancel() {
		if ( isClosing ) return true;

		isClosing = true;
		setEnabled( false );

		final int rotation = (int) mAdjustImageView.getCurrentRotation();
		final boolean hflip = mAdjustImageView.getHorizontalFlip();
		final boolean vflip = mAdjustImageView.getVerticalFlip();
		boolean straightenStarted = mAdjustImageView.getStraightenStarted();
		final double rotationFromStraighten = mAdjustImageView.getStraightenAngle();

		if ( rotation != 0 || hflip || vflip || ( straightenStarted && rotationFromStraighten != 0 ) ) {
			mAdjustImageView.reset();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onResetComplete() {
		getContext().cancel();
	}
}
