package com.myappscoauthorization;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.text.Html;
import android.text.util.Linkify;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * DRM:<br>
 * 
 * Step1:Need to add following code into launcher activity.
 * <br>
 * <b>DRM.start(MainActivity.this);</b>
 * <br>
 * <br>
 * Step2: Need Internet permissions & Network state permission for application
 * <br>
 * <b>android.permission.INTERNET</b>
 * <br>
 * <b>android.permission.ACCESS_NETWORK_STATE</b>
 * <br>
 * 
 * @author Amol Wadekar
 */
public class DRM {

	public static void start(Activity activity){
		if(Utility.isConnected(activity)){
			(new DRM()).new Async_validateUser(activity).execute("");
			
		}else{
			popForNoInternetConnection(activity);
		}
	}
	
	/**
	 * This method is used to show message to user that the application key is not valid.
	 */
	/*private static void popForNoInternetConnection(Activity activity){
		AlertDialog.Builder alBuilder=new AlertDialog.Builder(activity);
		alBuilder.setTitle("Internet Connection.");
		alBuilder.setMessage(Html.fromHtml("Please check your internet connection."));
		alBuilder.setPositiveButton("OK", new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				android.os.Process.killProcess(android.os.Process.myPid());

			}
		});
		alBuilder.setCancelable(false);
		alBuilder.show();
	}*/
	
	private static void popForNoInternetConnection(Activity activity){
		LayoutInflater inflater = LayoutInflater.from(activity);
		View rowView =inflater.inflate(R.layout.custom_dialog, null);
		final Dialog dialog=new Dialog(activity);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(rowView);
		dialog.show();
		
		TextView mTitleTextView=(TextView)rowView.findViewById(R.id.dailog_title);
		TextView dailog_description=(TextView)rowView.findViewById(R.id.dailog_description);
		Button mFirstButton=(Button)rowView.findViewById(R.id.first_btn);

		//set values...
		mTitleTextView.setText("Internet Connection.");
		dailog_description.setText("Please check your internet connection.");
		/********************setFirstButton*************************/
			mFirstButton.setVisibility(View.VISIBLE);
			mFirstButton.setText(Html.fromHtml("<b>OK</b>"));

			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.MATCH_PARENT,
					LinearLayout.LayoutParams.MATCH_PARENT);

			params.weight=100;

			mFirstButton.setLayoutParams(params);
			
			mFirstButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					dialog.dismiss();
					android.os.Process.killProcess(android.os.Process.myPid());
				}
				
				
				
			});

	}
	
	/**
	 * This class is used to validate user from server.
	 */
	class Async_validateUser extends AsyncTask<String, String, String>{
		Activity activity;
		ProgressDialog progressDialog;
		String isValidUser;
		PreferenceManagerClass preferenceManagerClass;
		Async_validateUser(Activity activity){
			this.activity=activity;
		}

		@Override
		protected void onPreExecute() {
			progressDialog=new ProgressDialog(activity);
			progressDialog.setTitle("Validating application key.");
			progressDialog.setMessage("Please wait...");
			progressDialog.setCancelable(false);

			preferenceManagerClass=new PreferenceManagerClass(activity);
			isValidUser=preferenceManagerClass.getUserValidatePreference();
			if(isValidUser.equalsIgnoreCase("-1")){
				progressDialog.show();
			}else if(isValidUser.equalsIgnoreCase("0")){
				customPopUp(activity);
				cancel(true);
			}else{
				cancel(true);
			}
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {

			if(isValidUser.equalsIgnoreCase("-1")){
				Utility utility=new Utility();
				String deviceID=utility.getDeviceId(activity);
				String packageName=activity.getPackageName();
				checkUserValidation(packageName,deviceID);
				isValidUser=preferenceManagerClass.getUserValidatePreference();
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			if(progressDialog.isShowing())
				progressDialog.dismiss();

			if(isValidUser.equalsIgnoreCase("0")){
				customPopUp(activity);
				cancel(true);
			}else{
				cancel(true);
			}
			super.onPostExecute(result);
		}

		/**
		 * This method is used to check user validity from server.
		 * @param String packageName
		 * @param String deviceID
		 */
		private void checkUserValidation(String packageName, String deviceID){
			try {
				System.out.println("checkUserValidation packageName "+packageName+" deviceID "+deviceID);
				HttpClient httpClient = new DefaultHttpClient();
				String key = deviceID+"#"+packageName;
				key=new String(Base64.encode(key.getBytes(), Base64.DEFAULT));
				System.out.println("Base64 key "+key);
				key = URLEncoder.encode(key, "utf-8");
				System.out.println("URLEncoder key "+key);
				String url = activity.getResources().getString(R.string.URL);
				HttpGet httpGet = new HttpGet(url);
				try {
					HttpResponse response = httpClient.execute(httpGet);
					StatusLine statusLine = response.getStatusLine();
					if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
						HttpEntity entity = response.getEntity();
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						entity.writeTo(out);
						out.close();
						String responseStr = out.toString();
						System.out.println("Response "+responseStr);
						// do something with response 
						validateUserResponse(responseStr);
					} else {
						// handle bad response
					}
				} catch (ClientProtocolException e) {
					// handle exception
				} catch (IOException e) {
					// handle exception
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * This method is used to validate user response & add it on preferences.
		 */
		private void validateUserResponse(String response){
			try{
				
				if(response==null || response.equalsIgnoreCase("")){
					preferenceManagerClass.addUserValidatePreference("0");
				}else{
					JSONObject jsonObject=new JSONObject(response);
					String licenceKey=jsonObject.getString("licenceKey");
					if(licenceKey!=null && !licenceKey.equalsIgnoreCase("") && !licenceKey.equalsIgnoreCase("0"))
						preferenceManagerClass.addUserValidatePreference("1");
					else
						preferenceManagerClass.addUserValidatePreference("0");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}

		/**
		 * This method is used to show message to user that the application key is not valid.
		 *//*
		private void popForInvalidUser(){
			AlertDialog.Builder alBuilder=new AlertDialog.Builder(activity);
			alBuilder.setTitle("Invalid App");
			alBuilder.setMessage(Html.fromHtml("Please download valid app from MyAppsCo App Store. <br><br><b><a href=\"https://play.google.com/store/apps/details?id=com.myappscobeta\">Link</a></b>"));
			alBuilder.setPositiveButton("OK", new OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					android.os.Process.killProcess(android.os.Process.myPid());

				}
			});
			alBuilder.setCancelable(false);
			alBuilder.show();
			
			
		}*/
		
		private void customPopUp(Activity activity){
			LayoutInflater inflater = LayoutInflater.from(activity);
			View rowView =inflater.inflate(R.layout.custom_dialog, null);
			final Dialog dialog=new Dialog(activity);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(rowView);
			dialog.show();
			
			TextView mTitleTextView=(TextView)rowView.findViewById(R.id.dailog_title);
			TextView dailog_description=(TextView)rowView.findViewById(R.id.dailog_description);
			Button mFirstButton=(Button)rowView.findViewById(R.id.first_btn);

			//set values...
			mTitleTextView.setText("Invalid App");
			dailog_description.setText(Html.fromHtml("Please download valid app from MyAppsCo App Store. <br><br><b>https://play.google.com/store/apps/details?id=com.myappscobeta</b>"));
			Linkify.addLinks(dailog_description, Linkify.ALL);
			/********************setFirstButton*************************/
				mFirstButton.setVisibility(View.VISIBLE);
				mFirstButton.setText(Html.fromHtml("<b>OK</b>"));

				LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.MATCH_PARENT,
						LinearLayout.LayoutParams.MATCH_PARENT);

				params.weight=100;

				mFirstButton.setLayoutParams(params);
				
				mFirstButton.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						dialog.dismiss();
						android.os.Process.killProcess(android.os.Process.myPid());
					}
					
					
					
				});

		}
		
	}// Asyn

}
