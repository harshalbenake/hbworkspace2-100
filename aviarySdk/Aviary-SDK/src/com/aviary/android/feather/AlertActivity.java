package com.aviary.android.feather;

import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.os.Bundle;

import com.aviary.android.feather.common.AviaryIntent;
import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.common.utils.ThrowableUtils;

public class AlertActivity extends Activity implements OnDismissListener, OnCancelListener {

	Logger logger = LoggerFactory.getLogger( "AlertActivity", LoggerType.ConsoleLoggerType );

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );

		Intent intent = getIntent();

		logger.info( "onCreate: %s", intent );

		if ( !handleIntent( intent ) ) {
			finish();
		}
	}

	private boolean handleIntent( final Intent intent ) {
		if ( null != intent ) {
			final String action = intent.getAction();
			final Bundle extras = intent.getExtras();

			if ( null != extras ) {
				if ( AviaryIntent.ACTION_ALERT.equals( action ) ) {
					AlertDialog dialog = handleAlertMessage( extras );
					if( null != dialog ) {
						dialog.show();
						dialog.setOnDismissListener( this );
						dialog.setOnCancelListener( this );
						return true;
					}
				}
			}
		}
		return false;
	}

	@SuppressWarnings ( "unchecked" )
	private AlertDialog handleAlertMessage( Bundle extras ) {
		logger.info( "handleAlertMessage: %s", extras );
		
		final String title = extras.getString( Intent.EXTRA_TITLE );
		final Object exceptions = extras.getSerializable( AviaryIntent.EXTRA_EXCEPTIONS );
		final String message = extras.getString( Intent.EXTRA_TEXT );
		
		logger.log( "title: %s", title );
		logger.log( "exceptions: %s", exceptions );
		logger.log( "text: %s", message );

		if ( null != title && ( extras.containsKey( AviaryIntent.EXTRA_EXCEPTIONS ) || extras.containsKey( Intent.EXTRA_TEXT )) ) {
			
			String text = null;
			
			if( extras.containsKey( AviaryIntent.EXTRA_EXCEPTIONS )) {
				if( null != exceptions && ( exceptions instanceof ArrayList<?> ) ) {
					Collection<Throwable> throwables = (Collection<Throwable>) exceptions;
					text = ThrowableUtils.getLocalizedMessage( throwables, "\n\n" );
				}
			} else if( extras.containsKey( Intent.EXTRA_TEXT )) {
				text = extras.getString( Intent.EXTRA_TEXT );
			}
			
			AlertDialog.Builder alert = new AlertDialog.Builder( this );
			alert.setTitle( title );
			alert.setIcon( android.R.drawable.ic_dialog_alert );
			
			if( null != text ) {
				alert.setMessage( text );
			}
			
			alert.setPositiveButton( android.R.string.cancel, new OnClickListener() {
				
				@Override
				public void onClick( DialogInterface dialog, int which ) {
					dialog.dismiss();
				}
			} );
			return alert.create();
		}
		return null;
	}

	@Override
	public void onCancel( DialogInterface dialog ) {
		logger.info( "onCancel" );
		finish();
	}

	@Override
	public void onDismiss( DialogInterface dialog ) {
		logger.info( "onDismiss" );
		finish();
	}

}
