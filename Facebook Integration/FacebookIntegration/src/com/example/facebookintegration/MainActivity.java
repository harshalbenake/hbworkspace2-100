package com.example.facebookintegration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.AsyncFacebookRunner.RequestListener;
import com.facebook.android.DialogError;
import com.facebook.android.Facebook;
import com.facebook.android.Facebook.DialogListener;
import com.facebook.android.FacebookError;

public class MainActivity extends Activity {
    // Your Facebook APP ID
    private static String APP_ID = "1501984976701694"; // Replace your App ID here
 
    // Instance of Facebook Class
    private Facebook facebook;
	private AsyncFacebookRunner mAsyncRunner;
    String FILENAME = "AndroidSSO_data";
    private SharedPreferences mPrefs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        facebook = new Facebook(APP_ID);
        mAsyncRunner = new AsyncFacebookRunner(facebook);
        
        Button button1=(Button)findViewById(R.id.button1);
        Button button2=(Button)findViewById(R.id.button2);
        Button button3=(Button)findViewById(R.id.button3);
        Button button4=(Button)findViewById(R.id.button4);
        generateHashKey();


		button1.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
                loginToFacebook();

				
			}
		});

		button2.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 postToWall();
			}
		});
		
		button3.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				getProfileInformation();	
			}
		});
		button4.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				logoutFromFacebook();
			}
		});
	}

	@SuppressWarnings("deprecation")
	public void loginToFacebook() {
	    mPrefs = getPreferences(MODE_PRIVATE);
	    String access_token = mPrefs.getString("access_token", null);
	    long expires = mPrefs.getLong("access_expires", 0);
	 
	    if (access_token != null) {
	        facebook.setAccessToken(access_token);
	    }
	 
	    if (expires != 0) {
	        facebook.setAccessExpires(expires);
	    }
	 
	    if (!facebook.isSessionValid()) {
	        facebook.authorize(MainActivity.this,
	                new String[] { "publish_stream" },
	                new DialogListener() {
	 
	                    @Override
	                    public void onCancel() {
	                        // Function to handle cancel event
	                    }
	 
	                    @Override
	                    public void onComplete(Bundle values) {
	                        // Function to handle complete event
	                        // Edit Preferences and update facebook acess_token
	                        SharedPreferences.Editor editor = mPrefs.edit();
	                        editor.putString("access_token",
	                                facebook.getAccessToken());
	                        editor.putLong("access_expires",
	                                facebook.getAccessExpires());
	                        editor.commit();
	                    }
	 
	                   
	                   
						@Override
						public void onFacebookError(FacebookError e) {
							// TODO Auto-generated method stub
							
						}

						@Override
						public void onError(DialogError e) {
							// TODO Auto-generated method stub
							
						}
	 
	                });
	    }
	}
	
	public void postToWall() {
	    // post on user's wall.
	    facebook.dialog(MainActivity.this, "feed", new DialogListener() {
	 
	        @Override
	        public void onFacebookError(FacebookError e) {
	        }
	 
	        @Override
	        public void onError(DialogError e) {
	        }
	 
	        @Override
	        public void onComplete(Bundle values) {
	        }
	 
	        @Override
	        public void onCancel() {
	        }
	    });
	 
	}
	
	public void getProfileInformation() {
	    mAsyncRunner.request("me", new RequestListener() {
	        @Override
	        public void onComplete(String response, Object state) {
	            Log.d("Profile", response);
	            String json = response;
	            try {
	                JSONObject profile = new JSONObject(json);
	                // getting name of the user
	                final String name = profile.getString("name");
	                // getting email of the user
	                final String email = profile.getString("email");
	 

	                runOnUiThread(new Runnable() {
	 
	                    @Override
	                    public void run() {
	                        Toast.makeText(getApplicationContext(), "Name: " + name + "\nEmail: " + email, Toast.LENGTH_LONG).show();
	                    }
	 
	                });
	 
	            } catch (JSONException e) {
	                e.printStackTrace();
	            }
	        }
	 
	        @Override
	        public void onIOException(IOException e, Object state) {
	        }
	 
	        @Override
	        public void onFileNotFoundException(FileNotFoundException e,
	                Object state) {
	        }
	 
	        @Override
	        public void onMalformedURLException(MalformedURLException e,
	                Object state) {
	        }
	 
	        @Override
	        public void onFacebookError(FacebookError e, Object state) {
	        }
	    });
	}
	
	
	public void logoutFromFacebook() {
	    mAsyncRunner.logout(this, new RequestListener() {
	        @Override
	        public void onComplete(String response, Object state) {
	            Log.d("Logout from Facebook", response);
	            if (Boolean.parseBoolean(response) == true) {
	                // User successfully Logged out
	            }
	        }
	 
	        @Override
	        public void onIOException(IOException e, Object state) {
	        }
	 
	        @Override
	        public void onFileNotFoundException(FileNotFoundException e,
	                Object state) {
	        }
	 
	        @Override
	        public void onMalformedURLException(MalformedURLException e,
	                Object state) {
	        }
	 
	        @Override
	        public void onFacebookError(FacebookError e, Object state) {
	        }
	    });
	}
	
	private void generateHashKey() {
		try {
	        PackageInfo info = getPackageManager().getPackageInfo(
	                getPackageName(), 
	                PackageManager.GET_SIGNATURES);
	        for (Signature signature : info.signatures) {
	            MessageDigest md = MessageDigest.getInstance("SHA");
	            md.update(signature.toByteArray());
	            Log.d("FBKEY", Base64.encodeToString(md.digest(), Base64.DEFAULT));
	            }
	    } catch (NameNotFoundException e) {

	    } catch (NoSuchAlgorithmException e) {

	    }
	}
}
