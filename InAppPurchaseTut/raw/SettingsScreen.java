package com.fastre;

import com.fastre.asynctask.Async_LoadSettingsData;
import com.fastre.asynctask.Async_Sync_Contacts;
import com.fastre.inapp.InAppManager;
import com.fastre.user.AccountStatus;
import com.fastre.user.UserManager;
import com.fastre.utils.JSONResponse;
import com.fastre.utils.PreferencesManagerClass;
import com.fastre.utils.Utility;
import com.fastre.widgets.ComingSoonDialog;
import com.fastre.widgets.CustomActionBar;
import com.fastre.widgets.CustomToast;
import com.fastre.widgets.LogoutDialog;
import com.fastre.widgets.SyncContactsGuideDialog;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.gson.Gson;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

@SuppressLint("InlinedApi")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SettingsScreen extends Activity {
	private static final String SYNC_CONTACTS_GUIDE = "sync_contacts_guide";
	private static final String GUIDE_POPUP = "sync_guide_popup";

	public static final int LOGOUT = 1;
	private TextView mFlashRe, mHelp, mAbout, mBlockUser, mUnBlockUser, mDevice, mSync, mSyncText, mConnectFrd, mConnectFrdText, mPushNotification, mPushNotificationText, mDeciceText;
	private Button mUpdatePremium;
	private Typeface mTypeface;
	private String mIsPushNotification = null;
	private RelativeLayout mSettings_push_notification_layout, mSetting_sync_contact_layout;
	private CheckBox settings_push_notification_checkbox, settings_sync_contact_checkbox;
	public LinearLayout mSettingDateCointainer;
	public TextView mSyncDateAndTime;
	private SharedPreferences pref;
	private InAppManager mInAppManager;
	private TextView mSettingsUserAccountStatus;
	private TextView mRateUs;
	private TextView mLikeUs;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			// Remove title bar
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			this.requestWindowFeature(Window.FEATURE_ACTION_BAR);
		}
		setContentView(R.layout.settings_activity);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			android.app.ActionBar actionBar = getActionBar();

			Drawable actionBarBg = getResources().getDrawable(R.drawable.red_strip);

			actionBar.setBackgroundDrawable(actionBarBg);
			actionBar.setTitle(Html.fromHtml("<font color=\"#ffffff\"><b>" + getString(R.string.action_settings) + "</b></font>"));
			actionBar.setDisplayHomeAsUpEnabled(true);
			new Utility().setActionBarTitleTypeface(SettingsScreen.this);
		} else {

			LinearLayout mainActionBar_Layout = (LinearLayout) findViewById(R.id.setting_actionbarcontanier);
			CustomActionBar actionBar = new CustomActionBar(SettingsScreen.this);

			ImageView actionbar_home_logo = (ImageView) actionBar.findViewById(R.id.actionbar_home_logo);
			ImageView actionbar_back_btn = (ImageView) actionBar.findViewById(R.id.actionbar_back_btn);
			actionbar_back_btn.setVisibility(View.VISIBLE);
			// hide search
			actionBar.hideSearchIcon();

			actionBar.setTitle(getString(R.string.action_settings));

			ActionBarClickListener aBarClickListener = new ActionBarClickListener(actionBar);
			actionbar_home_logo.setOnClickListener(aBarClickListener);
			actionbar_back_btn.setOnClickListener(aBarClickListener);

			// actionbar add settings icon
			ImageView logoutIcon = new ImageView(SettingsScreen.this);
			logoutIcon.setImageResource(R.drawable.logout_ico);
			logoutIcon.setPadding(0, 0, 10, 0);
			logoutIcon.setId(LOGOUT);
			logoutIcon.setOnClickListener(aBarClickListener);
			actionBar.addExtraIcon(logoutIcon);
			mainActionBar_Layout.addView(actionBar);
		}
		initializeAndSetTypeface();
		// Sending request to server to get all settings
		new Async_LoadSettingsData(getBaseContext()).execute("");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			new Utility().menuTypeface(SettingsScreen.this);

			menu.add(Menu.NONE, LOGOUT, 0, "Logout").setIcon(R.drawable.logout_ico).setTitle(R.string.logout_title).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home :
				handleHomeButtonClick();
				break;

			case LOGOUT :
				new LogoutDialog(SettingsScreen.this).show();
				break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void initializeAndSetTypeface() {

		SharedPreferences preferences = getSharedPreferences(SYNC_CONTACTS_GUIDE, MODE_PRIVATE);
		boolean flag = preferences.getBoolean(GUIDE_POPUP, false);
		if (flag == false)
			showSyncContactsGuideDialog();

		mFlashRe = (TextView) findViewById(R.id.setting_flashre_text);
		mHelp = (TextView) findViewById(R.id.setting_help_text);
		mAbout = (TextView) findViewById(R.id.setting_about_text);
		mRateUs = (TextView) findViewById(R.id.setting_rateus_text);
		mLikeUs = (TextView) findViewById(R.id.setting_likeus_text);

		mBlockUser = (TextView) findViewById(R.id.setting_block_text);
		mUnBlockUser = (TextView) findViewById(R.id.setting_unblock_text);
		mDevice = (TextView) findViewById(R.id.settings_device);
		mSync = (TextView) findViewById(R.id.settings_sinc);
		mSyncText = (TextView) findViewById(R.id.setting_sync_contact_text);
		mConnectFrd = (TextView) findViewById(R.id.setting_connect_frd);
		mConnectFrdText = (TextView) findViewById(R.id.setting_connectfriend_text);
		mPushNotification = (TextView) findViewById(R.id.settings_push_notification);
		mPushNotificationText = (TextView) findViewById(R.id.setting_pushnotification_text);
		mUpdatePremium = (Button) findViewById(R.id.setting_btn_upgrade);
		mSettings_push_notification_layout = (RelativeLayout) findViewById(R.id.settings_push_notification_layout);
		mSetting_sync_contact_layout = (RelativeLayout) findViewById(R.id.setting_sync_contact_layout);
		mDeciceText = (TextView) findViewById(R.id.setting_device_text);
		settings_push_notification_checkbox = (CheckBox) findViewById(R.id.settings_push_notification_checkbox);
		settings_sync_contact_checkbox = (CheckBox) findViewById(R.id.settings_sync_contact_checkbox);
		mSettingDateCointainer = (LinearLayout) findViewById(R.id.setting_date_cointiner);
		mSyncDateAndTime = (TextView) findViewById(R.id.setting_sync_date);
		mSettingsUserAccountStatus = (TextView) findViewById(R.id.settings_user_account_status);

		// DO NOT DELETE
		mInAppManager = new InAppManager(SettingsScreen.this);
		mInAppManager.setUpgradeToPremimumClickListener(mUpdatePremium);

		// mUpdatePremium.setOnClickListener(textonclicklistener);

		mTypeface = new Utility().getNormalFontTypeface(getApplicationContext());

		PreferencesManagerClass preferencesManagerClass = new PreferencesManagerClass(getApplicationContext());
		mIsPushNotification = preferencesManagerClass.getSettingsNotificationFlagPreference();
		if (mIsPushNotification.equalsIgnoreCase("0"))
			settings_push_notification_checkbox.setChecked(false);
		else
			settings_push_notification_checkbox.setChecked(true);

		settings_push_notification_checkbox.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new Asyn_IsPushNotification().execute("");
			}
		});

		// do not delete this line.
		setAccountStatus();

		settings_sync_contact_checkbox.setEnabled(false);
		// initialize shared preference and display current date on sync
		// contact.
		showSyncDetails();

		mFlashRe.setTypeface(mTypeface);
		mHelp.setTypeface(mTypeface);
		mAbout.setTypeface(mTypeface);
		mRateUs.setTypeface(mTypeface);
		mLikeUs.setTypeface(mTypeface);
		mBlockUser.setTypeface(mTypeface);
		mUnBlockUser.setTypeface(mTypeface);
		mDevice.setTypeface(mTypeface);
		mSync.setTypeface(mTypeface);
		mSyncText.setTypeface(mTypeface);
		mConnectFrd.setTypeface(mTypeface);
		mConnectFrdText.setTypeface(mTypeface);
		mPushNotification.setTypeface(mTypeface);
		mPushNotificationText.setTypeface(mTypeface);
		mUpdatePremium.setTypeface(mTypeface);
		mDeciceText.setTypeface(mTypeface);
		mSettingsUserAccountStatus.setTypeface(mTypeface);
		mSyncDateAndTime.setTypeface(mTypeface);

		mHelp.setOnClickListener(textonclicklistener);
		mAbout.setOnClickListener(textonclicklistener);
		mRateUs.setOnClickListener(textonclicklistener);
		mLikeUs.setOnClickListener(textonclicklistener);
		mBlockUser.setOnClickListener(textonclicklistener);
		mUnBlockUser.setOnClickListener(textonclicklistener);

		mSettings_push_notification_layout.setOnClickListener(textonclicklistener);
		mSetting_sync_contact_layout.setOnClickListener(textonclicklistener);
		mDeciceText.setOnClickListener(textonclicklistener);

		updateAccountStatus();
	}

	public void setAccountStatus() {
		PreferencesManagerClass preferencesManagerClass = new PreferencesManagerClass(SettingsScreen.this);
		AccountStatus accountStatus = preferencesManagerClass.getUserAccountStatus();
		boolean isPremiumUser = new UserManager().isUserPremiumUser(SettingsScreen.this);
		if (isPremiumUser == true && accountStatus != null && !accountStatus.day_remaining.equalsIgnoreCase("") && !accountStatus.day_remaining.equalsIgnoreCase("0")) {
			String message = String.format(getString(R.string.settings_account_expire_message), accountStatus.day_remaining);
			mSettingsUserAccountStatus.setVisibility(View.VISIBLE);
			mSettingsUserAccountStatus.setText(message);
		} else {
			mSettingsUserAccountStatus.setVisibility(View.INVISIBLE);
		}

	}

	void setPushnotificationUpdate() {
		PreferencesManagerClass preferencesManagerClass = new PreferencesManagerClass(getApplicationContext());
		mIsPushNotification = preferencesManagerClass.getSettingsNotificationFlagPreference();
		if (mIsPushNotification.equalsIgnoreCase("0"))
			settings_push_notification_checkbox.setChecked(false);
		else
			settings_push_notification_checkbox.setChecked(true);

	}

	class ActionBarClickListener implements OnClickListener {
		CustomActionBar cActionBar;

		ActionBarClickListener(CustomActionBar cActionBar) {
			this.cActionBar = cActionBar;
		}

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.actionbar_home_logo :
					handleHomeButtonClick();
					break;

				case R.id.actionbar_back_btn :
					handleHomeButtonClick();
					break;
				case LOGOUT :
					// showLogoutDialog();
					new LogoutDialog(SettingsScreen.this).show();
					break;
			}
		}
	}

	@Override
	public void onBackPressed() {
		handleHomeButtonClick();
		super.onBackPressed();
	}

	private void handleHomeButtonClick() {
		finish();
		overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
	}

	OnClickListener textonclicklistener = new OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.setting_help_text :
					Intent helpintent = new Intent();
					helpintent.setClass(getApplicationContext(), HelpActivity.class);
					startActivity(helpintent);
					overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
					break;

				case R.id.setting_about_text :
					Intent aboutintent = new Intent();
					aboutintent.setClass(getApplicationContext(), AboutActivity.class);
					startActivity(aboutintent);
					overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
					break;

				case R.id.setting_rateus_text : {
					showRateUsDialog();
				}
					break;
				case R.id.setting_likeus_text : {
					Intent likeUsIntent = new Intent(SettingsScreen.this, LikeUs.class);
					startActivity(likeUsIntent);
					overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
				}
					break;
				case R.id.setting_block_text :
					Intent blockintent = new Intent();
					blockintent.setClass(getApplicationContext(), BlockUnBlockUsers.class);
					blockintent.putExtra("ReqdScreen", "BlockScreen");
					// used Screen Name to minimize confusion
					startActivity(blockintent);
					overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
					break;

				case R.id.setting_unblock_text :
					Intent unblockintent = new Intent();
					unblockintent.setClass(getApplicationContext(), BlockUnBlockUsers.class);
					unblockintent.putExtra("ReqdScreen", "UnBlockScreen");
					// used Screen Name to minimize confusion
					startActivity(unblockintent);
					overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
					break;

				case R.id.setting_sync_contact_layout :
					// new CustomSyncContactsDialog(SettingsScreen.this,
					// FLAG).show();
					SharedPreferences pref = getSharedPreferences("syncdetails", Context.MODE_PRIVATE);
					String syncDetails = pref.getString("syncdetails", "");
					if (syncDetails != null && !syncDetails.equalsIgnoreCase("")) {
						new SyncContactsGuideDialog(SettingsScreen.this).show();
					} else {
						new Async_Sync_Contacts(SettingsScreen.this).execute("");
					}

					break;

				case R.id.setting_btn_upgrade :
					UserManager userManager = new UserManager();
					if (userManager.isUserPremiumUser(getBaseContext()) == false) {
						showComingSoonDialog();
					} else {
						CustomToast.makeText(getBaseContext(), R.string.setting_already_premium_user_text, CustomToast.LENGTH_SHORT).show();
					}
					break;

				case R.id.settings_push_notification_layout :
					new Asyn_IsPushNotification().execute("");
					break;
				case R.id.setting_device_text :
					// by amol DeviceListActivity not get committed on SVN.
					Intent devicelistintent = new Intent();
					devicelistintent.setClass(getApplicationContext(), DeviceListActivity.class);
					startActivity(devicelistintent);
					overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
					break;
			}

		}
	};

	public void showSyncDetails() {

		pref = getApplicationContext().getSharedPreferences("syncdetails", Context.MODE_PRIVATE);

		String syncDetails = pref.getString("syncdetails", "");

		if (syncDetails != null && !syncDetails.equalsIgnoreCase("")) {
			settings_sync_contact_checkbox.setChecked(true);
			mSettingDateCointainer.setVisibility(View.VISIBLE);
			mSyncDateAndTime.setText(syncDetails);
		} else {
			mSettingDateCointainer.setVisibility(View.GONE);
			settings_sync_contact_checkbox.setChecked(false);
		}
	}

	protected void showRateUsDialog() {

		ContextThemeWrapper themedContext;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			themedContext = new ContextThemeWrapper(SettingsScreen.this, android.R.style.Theme_Holo_Light_Dialog);
		} else {
			themedContext = new ContextThemeWrapper(SettingsScreen.this, android.R.style.Theme_Light);
		}

		// Put up the Yes/No message box
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);

		String message = getResources().getString(R.string.setting_rate_us_message);
		String title = getResources().getString(R.string.setting_rate_fastre_title);
		String rate_now = getResources().getString(R.string.setting_rate_now_text);
		String rate_later = getResources().getString(R.string.setting_rate_later_text);

		builder.setTitle(title);
		builder.setMessage(Html.fromHtml(message));

		builder.setPositiveButton(Html.fromHtml(rate_now), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Uri uri = Uri.parse("market://details?id=" + getPackageName());
				Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
				try {
					startActivity(myAppLinkToMarket);
					overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
				} catch (ActivityNotFoundException e) {
					CustomToast.makeText(SettingsScreen.this, "unable to find market app", CustomToast.LENGTH_SHORT).show();
				}
			}
		});

		builder.setNegativeButton(rate_later, null); // Do nothing on no
		AlertDialog dialog = builder.show();

		TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
		TextView button1 = (TextView) dialog.findViewById(android.R.id.button1);
		TextView button2 = (TextView) dialog.findViewById(android.R.id.button2);
		TextView button3 = (TextView) dialog.findViewById(android.R.id.button3);

		Typeface typeface = new Utility().getNormalFontTypeface(getApplicationContext());
		messageText.setTypeface(typeface);
		button1.setTypeface(typeface);
		button2.setTypeface(typeface);
		button3.setTypeface(typeface);
	}

	/**
	 * This class is used to handle push notifications requests
	 * 
	 * @author Shailendra Patil.
	 */
	class Asyn_IsPushNotification extends AsyncTask<String, String, String> {
		ProgressDialog progressDialog;
		// public ProgressDialog progressDialog;
		public String jsonResponse = "";
		String prevFlag = "1";

		@Override
		protected void onPreExecute() {
			if (Utility.isConnected(getBaseContext())) {
				progressDialog = new ProgressDialog(SettingsScreen.this);
				String message = getApplicationContext().getResources().getString(R.string.loading_text);
				progressDialog.setCancelable(true);
				progressDialog.setMessage(message);
				progressDialog.show();
			} else {
				cancel(true);
				CustomToast.makeText(getBaseContext(), R.string.check_internet_connection, CustomToast.LENGTH_SHORT).show();
			}
			super.onPreExecute();
		}

		@Override
		protected String doInBackground(String... params) {
			if (!isCancelled()) {
				if (mIsPushNotification.equalsIgnoreCase("1"))
					prevFlag = "0";
				else
					prevFlag = "1";

				UserManager userManager = new UserManager();
				String user_id = userManager.getUserId(getApplicationContext());

				UserManager mUserAccountManager = new UserManager();
				jsonResponse = mUserAccountManager.isPushNotification(SettingsScreen.this, user_id, prevFlag);
			}
			return jsonResponse;
		}

		@Override
		protected void onPostExecute(String result) {
			if (progressDialog != null && progressDialog.isShowing())
				progressDialog.dismiss();

			if (jsonResponse != null && !jsonResponse.equalsIgnoreCase("")) {
				try {

					Gson gson = new Gson();
					JSONResponse response = gson.fromJson(jsonResponse, JSONResponse.class);
					if (response != null && response.status != null && response.status.equalsIgnoreCase("success")) {
						CustomToast.makeText(getApplicationContext(), getString(R.string.setting_push_notification_done), CustomToast.LENGTH_LONG).show();

						PreferencesManagerClass manager = new PreferencesManagerClass(getApplicationContext());
						manager.addSettingsNotificationFlagPreference(prevFlag);

						setPushnotificationUpdate();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			super.onPostExecute(result);
		}
	}

	/**
	 * This class is used to handle push notifications requests
	 * 
	 * @author Shailendra Patil.
	 */
	public class Asyn_GetUserStatus extends AsyncTask<String, String, Boolean> {
		public String jsonResponse = "";

		@Override
		protected void onPreExecute() {
			if (!Utility.isConnected(getBaseContext())) {
				cancel(true);
			}
			super.onPreExecute();
		}

		@Override
		protected Boolean doInBackground(String... params) {

			UserManager userManager = new UserManager();
			String user_id = userManager.getUserId(getApplicationContext());
			jsonResponse = userManager.getUserAccountStatusFromServer(SettingsScreen.this, user_id);

			if (jsonResponse != null && !jsonResponse.equalsIgnoreCase("")) {
				try {
					AccountStatus accountStatus = new Gson().fromJson(jsonResponse, AccountStatus.class);
					if (accountStatus.status != null && accountStatus.status.equalsIgnoreCase("success")) {
						PreferencesManagerClass prefManagerClass = new PreferencesManagerClass(SettingsScreen.this);
						prefManagerClass.storeUserAccountStatus(accountStatus);
						return true;
					} else {
						return false;
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				return false;
			} else {
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean result) {
			if (result)
				setAccountStatus();

			super.onPostExecute(result);
		}
	}

	public void showSyncContactsGuideDialog() {
		SharedPreferences preferences = getSharedPreferences(SYNC_CONTACTS_GUIDE, MODE_PRIVATE);
		Editor editor = preferences.edit();
		editor.putBoolean(GUIDE_POPUP, true);
		editor.commit();
		SyncContactsGuideDialog syncContactsGuideDialog = new SyncContactsGuideDialog(SettingsScreen.this);
		syncContactsGuideDialog.setCancelable(false);
		syncContactsGuideDialog.show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Pass on the activity result to the helper for handling
		if (mInAppManager != null && !mInAppManager.getHelper().handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...

			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	public void showComingSoonDialog() {
		new ComingSoonDialog(SettingsScreen.this).show();
	}

	/**
	 * This will get new account status from server and will update the UI.
	 */
	public void updateAccountStatus() {
		new Asyn_GetUserStatus().execute("");
	}

	@Override
	protected void onStart() {
		super.onStart();
		EasyTracker.getInstance(SettingsScreen.this).activityStart(SettingsScreen.this);
	}

	@Override
	protected void onStop() {
		super.onStop();
		EasyTracker.getInstance(SettingsScreen.this).activityStop(SettingsScreen.this);
	}

}