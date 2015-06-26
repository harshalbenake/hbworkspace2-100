/*
 * Copyright (C) 2009  Michael Novak <mike@androidnerds.org>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.michaelrnovak.util.logger.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

public class LogProcessor extends Service {
	
	private static Handler mHandler;
	private String mFile;
	private String mBuffer = "main";
	private Vector<String> mScrollback;
	private int mLines;
	private int mType;
	private String mFilterTag;
	private volatile boolean threadKill = false;
	private volatile boolean mStatus = false;
	public int MAX_LINES = 250;
	public static final int MSG_READ_FAIL = 1;
	public static final int MSG_LOG_FAIL = 2;
	public static final int MSG_NEW_LINE = 3;
	public static final int MSG_RESET_LOG = 4;
	public static final int MSG_LOG_SAVE = 5;
	
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		Log.i("Logger", "Logger Service has hit the onStart method.");
	}

	Runnable worker = new Runnable() {
		public void run() {
			runLog();
			mStatus = true;
			Log.d("Logger", "status... " + mStatus);
			return;
		}
	};
	
	private void runLog() {
		Process process = null;
		
		try {
			
			if (mType == 0) {
				process = Runtime.getRuntime().exec("/system/bin/logcat -b " + mBuffer);
			} else if (mType == 1) {
				process = Runtime.getRuntime().exec("dmesg -s 1000000");
			}
			
		} catch (IOException e) {
			communicate(MSG_LOG_FAIL);
		}
		
		BufferedReader reader = null;
		
		try {
			reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			String line;
			
			while (!killRequested()) {
				line = reader.readLine();
				
				logLine(line);
				
				if (mLines == MAX_LINES) {
					mScrollback.removeElementAt(0);
				}
				
				mScrollback.add(line);
				mLines++;
			}
			
			Log.i("Logger", "Prepping thread for termination");
			reader.close();
			process.destroy();
			process = null;
			reader = null;
			mScrollback.removeAllElements();
			mScrollback = null;
			mLines = 0;
		} catch (IOException e) {
			communicate(MSG_READ_FAIL);
		}
		
		Log.d("Logger", "Exiting thread...");
		return;
	}
	
	private synchronized void requestKill() {
		threadKill = true;
	}
	
	private synchronized boolean killRequested() {
		return threadKill;
	}
	
	private void communicate(int msg) {
		Message.obtain(mHandler, msg, "error").sendToTarget();
	}
	
	private void logLine(String line) {
		Message.obtain(mHandler, MSG_NEW_LINE, line).sendToTarget();
	}
	
	public static void setHandler(Handler handler) {
		mHandler = handler;
	}
	
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		requestKill();
		stopSelf();
		
		return false;
	}
	
	private final ILogProcessor.Stub mBinder = new ILogProcessor.Stub() {
		public void reset(String buffer) {
			requestKill();

			while (!mStatus) {
				try {
					Log.d("Logger", "waiting...");
				} catch (Exception e) {
					Log.d("Logger", "Woot! obj has been interrupted!");
				}
			}
			
			threadKill = false;
			mBuffer = buffer.toLowerCase();
			mLines = 0;
			mScrollback = new Vector<String>();
			Thread thr = new Thread(worker);
			thr.start();
		}
		
		public void run(int type) {
			mType = type;
			mLines = 0;
			mScrollback = new Vector<String>();
			Thread thr = new Thread(worker);
			thr.start();
		}
		
		public void restart(int type) {
			requestKill();
			
			while(!mStatus) {
				try {
					Log.d("Logger", "waiting...");
				} catch (Exception e) {
					Log.d("Logger", "Woot! we have an exception");
				}
			}
			
			threadKill = false;
			run(type);
		}
		
		public void stop() {
			Log.i("Logger", "stop() method called in service.");
			requestKill();
			stopSelf();
		}
		
		public void write(String file, String tag) {
			mFilterTag = tag;
			mFile = file;
			Thread thr = new Thread(writer);
			thr.start();
		}
	};
	
	Runnable writer = new Runnable() {
		public void run() {
			writeLog();
			return;
		}
	};
	
	private void writeLog() {
		
		try {			
			File f = new File("/sdcard/" + mFile);
			FileWriter w = new FileWriter(f);
			
			for (int i = 0; i < mScrollback.size(); i++) {
				String line = mScrollback.elementAt(i);
				
				if (!mFilterTag.equals("")) {
		    		String tag = line.substring(2, line.indexOf("("));
		    		
		    		if (mFilterTag.toLowerCase().equals(tag.toLowerCase().trim())) {
		    			w.write(line + "\n");
		    		}
		    	} else {
		    		w.write(mScrollback.elementAt(i) + "\n");
		    	}

				i++;
			}
			
			if (!mFile.equals("tmp.log")) {
				Message.obtain(mHandler, MSG_LOG_SAVE, "saved").sendToTarget();
			} else {
				Message.obtain(mHandler, MSG_LOG_SAVE, "attachment").sendToTarget();
			}
			
			w.close();
			f = null;
		} catch (Exception e) {
			Log.e("Logger", "Error writing the log to a file. Exception: " + e.toString());
			Message.obtain(mHandler, MSG_LOG_SAVE, "error").sendToTarget();
		}
		
		return;
	}

}
