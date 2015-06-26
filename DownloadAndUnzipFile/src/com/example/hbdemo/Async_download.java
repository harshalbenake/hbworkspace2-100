package com.example.hbdemo;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

	/**
	 * This class is used to download zip file.
	 * @author <b>Harshal Benake</b>
	 *
	 */
	public class Async_download extends AsyncTask<String, String, String> {
		Activity mActivity;
		private ProgressDialog mProgressDialog;
		public Async_download(Activity activity) {
			this.mActivity=activity;
		}
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mActivity);
			mProgressDialog.setMessage("Downloading file..");
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setCancelable(false);
			mProgressDialog.show();
		}

		@Override
		protected String doInBackground(String... aurl) {
			int count;
		try {
		URL url = new URL(aurl[0]);
		URLConnection connection = url.openConnection();
		connection.connect();

		int lenghtOfFile = connection.getContentLength();
		
		InputStream input = new BufferedInputStream(url.openStream());
		OutputStream output = new FileOutputStream(Constant.SDCARD+Constant.FILENAME);

		byte data[] = new byte[1024];
		long total = 0;
		
			while ((count = input.read(data)) != -1) {
				total += count;
				publishProgress(""+(int)((total*100)/lenghtOfFile));
				output.write(data, 0, count);
			}

			output.flush();
			output.close();
			input.close();
		} catch (Exception e) {}
		return null;

		}
		protected void onProgressUpdate(String... progress) {
			 mProgressDialog.setProgress(Integer.parseInt(progress[0]));
		}

		@Override
		protected void onPostExecute(String unused) {
			mProgressDialog.dismiss();
		}

}
