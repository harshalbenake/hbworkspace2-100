package com.aviary.android.feather.utils;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import android.graphics.Bitmap;
import android.os.Handler;

public class SimpleBitmapCache {

	private static final int DELAY_BEFORE_PURGE = 30 * 1000;
	private static final int HARD_CACHE_CAPACITY = 4;
	private final Handler purgeHandler = new Handler();

	private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>( HARD_CACHE_CAPACITY / 2, 0.75f, true ) {

		private static final long serialVersionUID = 7320831300767054723L;

		@Override
		protected boolean removeEldestEntry( LinkedHashMap.Entry<String, Bitmap> eldest ) {
			if ( size() > HARD_CACHE_CAPACITY ) {
				sSoftBitmapCache.put( eldest.getKey(), new SoftReference<Bitmap>( eldest.getValue() ) );
				return true;
			} else return false;
		}
	};

	private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
			HARD_CACHE_CAPACITY / 2 );

	public SimpleBitmapCache () {}

	public Bitmap getBitmapFromCache( String url ) {
		synchronized ( sHardBitmapCache ) {
			final Bitmap bitmap = sHardBitmapCache.get( url );
			if ( bitmap != null ) {
				sHardBitmapCache.remove( url );
				sHardBitmapCache.put( url, bitmap );
				return bitmap;
			}
		}

		SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get( url );
		if ( bitmapReference != null ) {
			final Bitmap bitmap = bitmapReference.get();
			if ( bitmap != null ) {
				return bitmap;
			} else {
				sSoftBitmapCache.remove( url );
			}
		}

		return null;
	}

	public void addBitmapToCache( String url, Bitmap bitmap ) {
		if ( bitmap != null ) {
			synchronized ( sHardBitmapCache ) {
				sHardBitmapCache.put( url, bitmap );
			}
		}
	}

	public void clearCache() {
		synchronized ( sHardBitmapCache ) {
			sHardBitmapCache.clear();
		}
		sSoftBitmapCache.clear();
	}

	public void resetPurgeTimer() {
		purgeHandler.removeCallbacks( mPurger );
		purgeHandler.postDelayed( mPurger, DELAY_BEFORE_PURGE );
	}

	private final Runnable mPurger = new Runnable() {

		@Override
		public void run() {
			clearCache();
		}
	};
}
