package com.example.hbdemo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

/**
 * This class is used to unzip zip file from sdcard.
 * @author <b>Harshal Benake</b>
 *
 */
public class Async_unzipping extends AsyncTask<String, String, String> {
	Activity mActivity;
	private ProgressDialog mProgressDialog;

	public Async_unzipping(Activity activity) {
		this.mActivity=activity;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		mProgressDialog = new ProgressDialog(mActivity);
		mProgressDialog.setMessage("Extracting file..");
		mProgressDialog.setCancelable(false);
		mProgressDialog.show();
	}
	  
	@Override
	protected String doInBackground(String... params) {
		 
	    try  { 
	      FileInputStream fin = new FileInputStream(Constant.SDCARD+Constant.FILENAME); 
	      ZipInputStream zin = new ZipInputStream(fin); 
	      ZipEntry ze = null; 
	      while ((ze = zin.getNextEntry()) != null) { 		 
	       
	          FileOutputStream fout = new FileOutputStream(Constant.SDCARD + ze.getName()); 
	          for (int c = zin.read(); c != -1; c = zin.read()) { 
	            fout.write(c); 
	          } 
	 
	          zin.closeEntry(); 
	          fout.close(); 
	        } 
	         
	      zin.close(); 
	    } catch(Exception e) { 
	    } 
	 
	  
		return null;
	} 
	
	@Override
	protected void onPostExecute(String unused) {
		mProgressDialog.dismiss();
	}
		 
}
