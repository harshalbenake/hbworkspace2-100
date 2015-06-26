package com.aviary.android.feather.utils;

import java.lang.ref.SoftReference;
import java.util.HashMap;

import android.content.res.AssetManager;
import android.graphics.Typeface;

public class TypefaceUtils {

	private static final HashMap<String, SoftReference<Typeface>> sTypeCache = new HashMap<String, SoftReference<Typeface>>();

	public static Typeface createFromAsset( final AssetManager assets, final String fontname ) {
		Typeface result = null;
		SoftReference<Typeface> cachedFont = getFromCache( fontname );

		if ( null != cachedFont && cachedFont.get() != null ) {
			result = cachedFont.get();
		} else {
			result = Typeface.createFromAsset( assets, fontname );
			putIntoCache( fontname, result );
		}

		return result;
	}

	private static SoftReference<Typeface> getFromCache( final String fontname ) {
		synchronized ( sTypeCache ) {
			return sTypeCache.get( fontname );
		}
	}

	private static void putIntoCache( final String fontname, final Typeface font ) {
		synchronized ( sTypeCache ) {
			sTypeCache.put( fontname, new SoftReference<Typeface>( font ) );
		}
	}
}
