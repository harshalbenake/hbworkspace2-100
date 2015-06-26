package com.blundell.tut.cameraoverlay.util;

import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.os.Message;

public class Log {

	private static final String TAG = "CameraSpike";

	public static void d(String msg) {
		d(msg, null);
	}

	public static void d(String msg, Throwable e) {
		android.util.Log.d(TAG, Thread.currentThread().getName() + "| " + msg, e);
	}

	public static void i(String msg) {
		i(msg, null);
	}

	public static void i(String msg, Throwable e) {
		android.util.Log.i(TAG, Thread.currentThread().getName() + "| " + msg, e);
	}

	public static void e(String msg) {
		e(msg, null);
	}

	public static void e(String msg, Throwable e) {
		android.util.Log.e(TAG, Thread.currentThread().getName() + "| " + msg, e);
	}

	public static void v(String msg) {
		android.util.Log.v(TAG, Thread.currentThread().getName() + "| " + msg);
	}

	public static String identifyMessage(Resources res, Message msg) {
		try {
			return res.getResourceEntryName(msg.what);
		} catch (NotFoundException ignore) {
			return "not found";
		}
	}

	public static void w(String msg) {
		android.util.Log.w(TAG, Thread.currentThread().getName() + "| " + msg);
	}

	/**
	 * Use this when you want to debug a String that is too long to be printed in one Log line.
	 * This will print the string breaking it up into 500 character segments
	 *
	 * @param msg
	 */
	public static void debugLongString(String msg) {
		StringBuffer b = new StringBuffer();
		char[] c = msg.toCharArray();
		int x = 0;
		for (int i = 0; i < c.length; i++) {
			b.append(c[i]);
			if (x++ == 500) {
				d(b.toString());
				b = new StringBuffer();
				x = 0;
			}
		}
		d(b.toString());
	}
}