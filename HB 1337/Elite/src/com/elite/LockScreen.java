package com.elite;

import android.app.Activity;
import android.os.Bundle;

/**
 * This class is used for lock screen.
 * @author <b>Elite</b>
 *
 */
public class LockScreen extends Activity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		overridePendingTransition(R.anim.fadein, R.anim.fadeout);  
		setContentView(R.layout.lock_screen);
	}
}
