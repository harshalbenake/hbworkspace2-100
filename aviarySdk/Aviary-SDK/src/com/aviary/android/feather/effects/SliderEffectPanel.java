package com.aviary.android.feather.effects;

import java.util.Locale;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.aviary.android.feather.R;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.filters.FilterLoaderFactory;
import com.aviary.android.feather.library.filters.FilterLoaderFactory.Filters;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.tracking.Tracker;
import com.aviary.android.feather.widget.AviarySeekBar;
import com.aviary.android.feather.widget.AviaryWheel;
import com.aviary.android.feather.widget.AviaryWheel.OnWheelChangeListener;

public abstract class SliderEffectPanel extends AbstractOptionPanel implements OnSeekBarChangeListener, OnClickListener, OnWheelChangeListener {

	enum SliderStyle {
		SeekBarStyle, WheelStyle
	};

	private SliderStyle mStyle;
	AviaryWheel mWheel;
	AviarySeekBar mSeekBar;
	String mResourceName;
	View mButtonMinus, mButtonPlus;

	public SliderEffectPanel ( IAviaryController context, ToolEntry entry, Filters type, String resourcesBaseName ) {
		super( context, entry );
		mStyle = SliderStyle.WheelStyle;

		mFilter = FilterLoaderFactory.get( type );
		mResourceName = resourcesBaseName;
	}

	@Override
	public void onCreate( Bitmap bitmap, Bundle options ) {
		super.onCreate( bitmap, options );

		mButtonMinus = getOptionView().findViewById( R.id.aviary_button_minus );
		mButtonPlus = getOptionView().findViewById( R.id.aviary_button_plus );

		if ( mStyle == SliderStyle.SeekBarStyle ) {
			mSeekBar = (AviarySeekBar) getOptionView().findViewById( R.id.aviary_seekbar );
			mSeekBar.setProgress( 50 );
		} else {
			mWheel = (AviaryWheel) getOptionView().findViewById( R.id.aviary_wheel );
			mWheel.setValue( 50 );
		}
	}

	@Override
	public void onActivate() {
		super.onActivate();
		mButtonMinus.setOnClickListener( this );
		mButtonPlus.setOnClickListener( this );

		if ( mStyle == SliderStyle.SeekBarStyle ) {
			mSeekBar.setOnSeekBarChangeListener( this );
		} else {
			mWheel.setOnWheelChangeListener( this );
			disableHapticIsNecessary( mWheel );
		}
	}

	@Override
	public void onDeactivate() {
		super.onDeactivate();
		mButtonMinus.setOnClickListener( null );
		mButtonPlus.setOnClickListener( null );

		if ( mStyle == SliderStyle.SeekBarStyle ) {
			mSeekBar.setOnSeekBarChangeListener( null );
		} else {
			mWheel.setOnWheelChangeListener( this );
		}
	}

	protected void setValue( int value ) {
		if ( mStyle == SliderStyle.SeekBarStyle ) {
			mSeekBar.setProgress( value );
		} else {
			mWheel.setValue( value );
		}
	}

	@Override
	public void onClick( View v ) {
		final int id = v.getId();
		if ( id == mButtonMinus.getId() ) {
			decreaseValue();
		} else if ( id == mButtonPlus.getId() ) {
			increaseValue();
		}
	}

	protected void decreaseValue() {
		if ( mStyle == SliderStyle.SeekBarStyle ) {
			mSeekBar.setProgress( mSeekBar.getProgress() - 1 );
		} else {
			mWheel.setValue( mWheel.getValue() - 1 );
		}
	}

	protected void increaseValue() {
		if ( mStyle == SliderStyle.SeekBarStyle ) {
			mSeekBar.setProgress( mSeekBar.getProgress() + 1 );
		} else {
			mWheel.setValue( mWheel.getValue() + 1 );
		}
	}

	@Override
	protected ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent ) {
		if ( mStyle == SliderStyle.SeekBarStyle ) {
			return (ViewGroup) inflater.inflate( R.layout.aviary_panel_seekbar, parent, false );
		} else {
			return (ViewGroup) inflater.inflate( R.layout.aviary_panel_wheel, parent, false );
		}
	}

	@Override
	public final void onProgressChanged( SeekBar seekBar, int progress, boolean fromUser ) {
		onSliderChanged( progress, fromUser );
	}

	@Override
	public final void onStartTrackingTouch( SeekBar seekBar ) {
		Tracker.recordTag( getName().name().toLowerCase( Locale.US ) + ": SliderMoved" );
		onSliderStart( seekBar.getProgress() );
	}

	@Override
	public final void onStopTrackingTouch( SeekBar seekBar ) {
		onSliderEnd( seekBar.getProgress() );
	}

	@Override
	public final void onStartTrackingTouch( AviaryWheel view ) {
		Tracker.recordTag( getName().name().toLowerCase( Locale.US ) + ": WheelMoved" );
		onSliderStart( view.getValue() );
	}

	@Override
	public final void OnValueChanged( AviaryWheel view, int value ) {
		onSliderChanged( value, true );
	}

	@Override
	public void onStopTrackingTouch( AviaryWheel view ) {
		onSliderEnd( view.getValue() );
	}

	protected abstract void onSliderStart( int value );

	protected abstract void onSliderChanged( int value, boolean fromUser );

	protected abstract void onSliderEnd( int value );
}
