package com.example.demo;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.provider.Settings.Secure;

/**
 * This is a manager class used to manage account related information.
 * 
 */
public class AccountManager
{
	Context mContext;
	Resources mResources;
	String URL;
	String key;
	String userId = null;

	public AccountManager(Context mContext)
	{
		this.mContext = mContext;
		mResources = mContext.getResources();
		URL = mContext.getResources().getString(R.string.url);
		key = mContext.getResources().getString(R.string.key);
	}

	/**
	 * This method is used to send device information for gcm notification.
	 * 
	 * @param context
	 * @param user_id
	 * @return
	 */
	public String requestDeviceRegistration(Context context, String user_id)
	{
		String responseStr = "";
		String methodName = mResources.getString(R.string.method_device_register);
		try
		{
			// Create a new HttpClient and Post Header
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(URL);

			try
			{
				Utility utility = new Utility();

				String unique_device_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

				if(unique_device_id == null || unique_device_id.equalsIgnoreCase("null") || unique_device_id.equalsIgnoreCase(""))
				{
					unique_device_id = utility.getDeviceSerialNumber();
				}

				String device_name = utility.getDeviceName();
				String model_no = utility.getDeviceModelName();
				String device_os_version = utility.getDeviceSDKVersion();
				String app_id = context.getResources().getString(R.string.version);
				String notification_id = utility.getNotificationID(context);
				System.out.println("notification_id" + notification_id);
				PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
				String app_version = pInfo.versionName;

				// Add your data
				List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
				nameValuePairs.add(new BasicNameValuePair("key", key));
				nameValuePairs.add(new BasicNameValuePair("method", methodName));
				nameValuePairs.add(new BasicNameValuePair("user_id", user_id));
				nameValuePairs.add(new BasicNameValuePair("app_id", app_id));
				nameValuePairs.add(new BasicNameValuePair("unique_device_id", unique_device_id));
				nameValuePairs.add(new BasicNameValuePair("notification_id", notification_id));
				nameValuePairs.add(new BasicNameValuePair("device_name", device_name));
				nameValuePairs.add(new BasicNameValuePair("model_no", model_no));
				nameValuePairs.add(new BasicNameValuePair("device_os_version", device_os_version));
				nameValuePairs.add(new BasicNameValuePair("app_version", app_version));
				nameValuePairs.add(new BasicNameValuePair("device_icon", ""));

				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

				// Execute HTTP Post Request
				HttpResponse httpResponse = httpclient.execute(httppost);

				HttpEntity responseEntity = httpResponse.getEntity();
				if(responseEntity != null)
				{
					responseStr = EntityUtils.toString(responseEntity);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return responseStr;
	}

}
