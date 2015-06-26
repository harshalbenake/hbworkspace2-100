package selvin.ListSyncSample.authenticator;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;

import selvin.ListSyncSample.Constants;
import selvin.ListSyncSample.R;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

public class AuthenticatorActivity extends AccountAuthenticatorActivity {
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_AUTHTOKEN_TYPE = "authtokenType";

	private static final String TAG = "AuthenticatorActivity";

	private AccountManager mAccountManager;
	private Thread mAuthThread;
	private static String mAuthtoken;
	private String mAuthtokenType;

	private final Handler mHandler = new Handler();
	private TextView mMessage;

	private String mUsername;
	private EditText mUsernameEdit;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle icicle) {
		Log.i(TAG, "onCreate(" + icicle + ")");
		super.onCreate(icicle);
		mAccountManager = AccountManager.get(this);
		Log.i(TAG, "loading data from Intent");
		final Intent intent = getIntent();
		mUsername = intent.getStringExtra(PARAM_USERNAME);
		mAuthtokenType = intent.getStringExtra(PARAM_AUTHTOKEN_TYPE);
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.login_activity);
		getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
				android.R.drawable.ic_dialog_alert);

		mMessage = (TextView) findViewById(R.id.message);
		mUsernameEdit = (EditText) findViewById(R.id.username_edit);
		mUsernameEdit.setText(mUsername);
		mMessage.setText(getMessage());
	}

	/*
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setMessage(getText(R.string.ui_activity_authenticating));
		dialog.setIndeterminate(true);
		dialog.setCancelable(true);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				Log.i(TAG, "dialog cancel has been invoked");
				if (mAuthThread != null) {
					mAuthThread.interrupt();
					finish();
				}
			}
		});
		return dialog;
	}

	public void handleLogin(View view) {
		mUsername = mUsernameEdit.getText().toString();
		if (TextUtils.isEmpty(mUsername)) {
			mMessage.setText(getMessage());
		} else {
			showDialog(0);
			// Start authenticating...
			mAuthThread = attemptAuth(mUsername, mHandler, AuthenticatorActivity.this);
		}
	}

	private static void sendResult(final Boolean result, final Handler handler,
			final Context context) {
		if (handler == null || context == null) {
			return;
		}
		handler.post(new Runnable() {
			public void run() {
				((AuthenticatorActivity) context).onAuthenticationResult(result);
			}
		});
	}

	public static boolean authenticate(String username, Handler handler,
			final Context context) {
		HttpRequestBase request = new HttpGet(Constants.SERVICE_URI
				+ "/Login.ashx?username=" + username);
		request.setHeader("Accept", "application/json");
		request.setHeader("Content-type", "application/json; charset=utf-8");
		DefaultHttpClient httpClient = new DefaultHttpClient();
		try {
			HttpResponse response = httpClient.execute(request);
			InputStream instream = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(instream));
			StringBuilder total = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				total.append(line);
			}
			instream.close();
			String bufstring = total.toString();
			Log.d("ListSync", bufstring);
			mAuthtoken = bufstring;
			sendResult(true, handler, context);
			return true;

		} catch (Exception e) {
			Log.e("Auth", e.getLocalizedMessage());
		}
		sendResult(false, handler, context);
		return false;
	}

	public static Thread performOnBackgroundThread(final Runnable runnable) {
		final Thread t = new Thread() {
			@Override
			public void run() {
				try {
					runnable.run();
				} finally {

				}
			}
		};
		t.start();
		return t;
	}

	public static Thread attemptAuth(final String username, final Handler handler,
			final Context context) {
		final Runnable runnable = new Runnable() {
			public void run() {
				authenticate(username, handler, context);
			}
		};
		return performOnBackgroundThread(runnable);
	}

	public void onAuthenticationResult(boolean result) {
		dismissDialog(0);
		final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
		mAccountManager.addAccountExplicitly(account, "*", null);
		mAccountManager.setAuthToken(account, Constants.AUTHTOKEN_TYPE, mAuthtoken);
		ContentResolver.setSyncAutomatically(account, Constants.AUTHORITY, true);
		final Intent intent = new Intent();
		intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername);
		intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
		if (mAuthtokenType != null && mAuthtokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			intent.putExtra(AccountManager.KEY_AUTHTOKEN, mAuthtoken);
		}
		setAccountAuthenticatorResult(intent.getExtras());
		setResult(RESULT_OK, intent);
		finish();
	}

	private CharSequence getMessage() {
		getString(R.string.label);
		if (TextUtils.isEmpty(mUsername)) {
			final CharSequence msg = getText(R.string.login_activity_newaccount_text);
			return msg;
		}
		return null;
	}
}
