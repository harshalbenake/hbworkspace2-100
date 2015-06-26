package com.aviary.android.feather.effects;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.aviary.android.feather.common.utils.PackageManagerUtils;
import com.aviary.android.feather.effects.AbstractPanel.OptionPanel;
import com.aviary.android.feather.library.Constants;
import com.aviary.android.feather.library.content.ToolEntry;
import com.aviary.android.feather.library.services.IAviaryController;
import com.aviary.android.feather.library.services.LocalDataService;
import com.aviary.android.feather.library.services.PreferenceService;
import com.aviary.android.feather.widget.VibrationWidget;

abstract class AbstractOptionPanel extends AbstractPanel implements OptionPanel {

	protected ViewGroup mOptionView;

	/**
	 * Instantiates a new abstract option panel.
	 * 
	 * @param context
	 *            the context
	 */
	public AbstractOptionPanel ( IAviaryController context, ToolEntry entry ) {
		super( context, entry );
	}

	@Override
	public final ViewGroup getOptionView( LayoutInflater inflater, ViewGroup parent ) {
		mOptionView = generateOptionView( inflater, parent );
		return mOptionView;
	}

	/**
	 * Gets the panel option view.
	 * 
	 * @return the option view
	 */
	public final ViewGroup getOptionView() {
		return mOptionView;
	}

	@Override
	protected void onDispose() {
		mOptionView = null;
		super.onDispose();
	}

	@Override
	public void setEnabled( boolean value ) {
		getOptionView().setEnabled( value );
		super.setEnabled( value );
	}

	/**
	 * Generate option view.
	 * 
	 * @param inflater
	 *            the inflater
	 * @param parent
	 *            the parent
	 * @return the view group
	 */
	protected abstract ViewGroup generateOptionView( LayoutInflater inflater, ViewGroup parent );

	/**
	 * Disable vibration feedback for each view in the passed array if necessary
	 * 
	 * @param views
	 */
	protected void disableHapticIsNecessary( VibrationWidget... views ) {
		boolean vibration = true;

		LocalDataService dataService = getContext().getService( LocalDataService.class );

		if ( dataService.getIntentContainsKey( Constants.EXTRA_TOOLS_DISABLE_VIBRATION ) ) {
			vibration = false;
		} else {

			if ( null != getContext() && null != getContext().getBaseContext() ) {
				PreferenceService pref_service = getContext().getService( PreferenceService.class );
				if ( null != pref_service ) {
					if ( PackageManagerUtils.isStandalone( getContext().getBaseContext() ) ) {
						vibration = pref_service.getStandaloneBoolean( "feather_app_vibration", true );
					}
				}
			}
		}

		for ( VibrationWidget view : views ) {
			view.setVibrationEnabled( vibration );
		}
	}

}
