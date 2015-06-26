package selvin.ListSyncSample.authenticator;

import selvin.ListSyncSample.Constants;
import selvin.ListSyncSample.Database;
import selvin.ListSyncSample.R;
import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.example.android.samplesync domain.
 */
class Authenticator extends AbstractAccountAuthenticator {
	// Authentication Service context
	private final Context mContext;

	public Authenticator(Context context) {
		super(context);
		mContext = context;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response,
			Account account) throws NetworkErrorException {
		new Database.OpenHelper(mContext).Recreate().close();
		return super.getAccountRemovalAllowed(response, account);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
			String authTokenType, String[] requiredFeatures, Bundle options) {
		final AccountManager am = AccountManager.get(mContext);
		final Intent intent = new Intent(mContext, AuthenticatorActivity.class);

		if (am.getAccountsByType(accountType).length == 0) {

			intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			final Bundle bundle = new Bundle();
			bundle.putParcelable(AccountManager.KEY_INTENT, intent);
			return bundle;
		}
		final Bundle bundle = new Bundle();
		
		bundle.putInt(AccountManager.KEY_ERROR_CODE,
				AccountManager.ERROR_CODE_BAD_REQUEST);
		bundle.putString(AccountManager.KEY_ERROR_MESSAGE, smsg);
		handler.sendEmptyMessage(0);
		return bundle;
	}
	static final String smsg = "ListSyncSample account already exists.\nOnly one account is supported.";
	final Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 0)
				Toast.makeText(mContext, smsg, Toast.LENGTH_LONG).show();
		};
	};

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle confirmCredentials(AccountAuthenticatorResponse response,
			Account account, Bundle options) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
		throw new UnsupportedOperationException();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
			String authTokenType, Bundle loginOptions) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAuthTokenLabel(String authTokenType) {
		if (authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
			return mContext.getString(R.string.label);
		}
		return null;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
			String[] features) {
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Bundle updateCredentials(AccountAuthenticatorResponse response,
			Account account, String authTokenType, Bundle loginOptions) {
		return null;
	}

}
