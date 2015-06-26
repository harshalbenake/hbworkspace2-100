package com.aviary.android.feather.async_tasks;

import it.sephiroth.android.library.media.ExifInterfaceExtended;
import android.os.Bundle;

import com.aviary.android.feather.common.threading.ThreadPool.Job;
import com.aviary.android.feather.common.threading.ThreadPool.Worker;

public class ExifTask implements Job<String, Bundle> {

	@Override
	public Bundle run( Worker<String, Bundle> context, String... params ) throws Exception {
		if ( params == null ) {
			return null;
		}

		Bundle result = new Bundle();

		try {
			ExifInterfaceExtended exif = new ExifInterfaceExtended( params[0] );
			exif.copyTo( result );

		} catch ( Throwable t ) {
			t.printStackTrace();
		}
		return result;
	}

}
