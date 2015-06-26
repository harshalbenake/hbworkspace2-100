package selvin.ListSyncSample.syncadapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import selvin.ListSyncSample.Constants;
import selvin.ListSyncSample.Database;
import selvin.ListSyncSample.Database.OpenHelper;
import selvin.ListSyncSample.Database.Tables;
import selvin.ListSyncSample.provider.ListProvider;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {

	static final String TAG = "ListSync";
	OpenHelper openHelper = null;
	private final AccountManager mAccountManager;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		openHelper = new Database.OpenHelper(context);
		mAccountManager = AccountManager.get(context);
	}

	static class GzipDecompressingEntity extends HttpEntityWrapper {

		public GzipDecompressingEntity(final HttpEntity entity) {
			super(entity);
		}

		@Override
		public InputStream getContent() throws IOException, IllegalStateException {
			InputStream wrappedin = wrappedEntity.getContent();
			return new GZIPInputStream(wrappedin);
		}

		@Override
		public long getContentLength() {
			return -1;
		}

	}

	static final String DOWNLOAD_SERVICE_URI = Constants.SERVICE_URI
			+ "DefaultScopeSyncService.svc/defaultscope/DownloadChanges?userid=";
	static final String UPLOAD_SERVICE_URI = Constants.SERVICE_URI
			+ "DefaultScopeSyncService.svc/defaultscope/UploadChanges?userid=";

	final static String send = "{\"d\":{\"__sync\":{\"moreChangesAvailable\":false,\"serverBlob\":\"%s\"},\"results\":[%s]}}";

	DefaultHttpClient GetClient() {
		HttpParams httpParameters = new BasicHttpParams();
		int timeout = 7000;
		HttpConnectionParams.setConnectionTimeout(httpParameters, timeout);
		HttpConnectionParams.setSoTimeout(httpParameters, timeout);
		DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
		httpClient.addRequestInterceptor(new HttpRequestInterceptor() {

			public void process(final HttpRequest request, final HttpContext context)
					throws HttpException, IOException {
				request.addHeader("Accept-Encoding", "gzip");
				request.setHeader("Accept", "application/json");
				request.setHeader("Content-type", "application/json; charset=utf-8");
			}

		});
		httpClient.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(final HttpResponse response, final HttpContext context)
					throws HttpException, IOException {
				HttpEntity entity = response.getEntity();
				Header ceheader = entity.getContentEncoding();
				if (ceheader != null) {
					HeaderElement[] codecs = ceheader.getElements();
					for (int i = 0; i < codecs.length; i++) {
						if (codecs[i].getName().equalsIgnoreCase("gzip")) {
							response.setEntity(new GzipDecompressingEntity(response
									.getEntity()));
							return;
						}
					}
				}
			}

		});
		return httpClient;
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {

		SQLiteDatabase db = openHelper.getWritableDatabase();
		ArrayList<String> notifyTables = new ArrayList<String>();
		db.beginTransaction();
		DefaultHttpClient httpClient = GetClient();
		getContext().sendOrderedBroadcast(new Intent(Constants.SYNCACTION_START), null);
		try {
			String authtoken = mAccountManager.blockingGetAuthToken(account,
					Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
			String serverBlob = null;
			Cursor c = db.query(Database.Settings.NAME,
					new String[] { Database.Settings.C_VALUE }, Database.Settings.C_NAME
							+ "=?", new String[] { "serverBlob" }, null, null, null);
			if (c.moveToFirst())
				serverBlob = c.getString(0);
			c.close();

			HttpRequestBase request = null;

			if (serverBlob != null) {
				HttpPost p = (HttpPost) (request = new HttpPost(UPLOAD_SERVICE_URI
						+ authtoken));
				boolean first = true;
				StringBuilder sb = new StringBuilder();
				sb
						.append("{\"d\":{\"__sync\":{\"moreChangesAvailable\":false,\"serverBlob\":\"");
				sb.append(serverBlob);
				sb.append("\"},\"results\":[");
				for (Tables tab : Tables.AllTables.values()) {
					first = tab.GetChanges(db, sb, first, notifyTables);
				}
				sb.append("]}}");
				if (!first) {
					String ent = sb.toString();
					Log.d("ListSync", "---" + ent);
					p.setEntity(new StringEntity(ent));
					HttpResponse response = httpClient.execute(request);
					if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
						InputStream instream = response.getEntity().getContent();
						BufferedReader r = new BufferedReader(new InputStreamReader(
								instream), 8000);
						StringBuilder total = new StringBuilder();
						String line;
						while ((line = r.readLine()) != null) {
							total.append(line);
						}
						instream.close();

						String bufstring = total.toString();
						JSONObject obj = new JSONObject(bufstring);
						JSONObject d = obj.getJSONObject("d");
						JSONObject sync = d.getJSONObject("__sync");
						serverBlob = sync.getString("serverBlob");
						Log.d("ListSync", bufstring);
						JSONArray results = d.getJSONArray("results");
						for (int i = 0; i < results.length(); i++) {
							JSONObject result = results.optJSONObject(i);
							JSONObject metadata = result.getJSONObject("__metadata");
							boolean isDeleted = metadata.optBoolean("isDeleted");
							String table = metadata.getString("type").replace(
									"DefaultScope.", "");
							Tables tab = Tables.AllTables.get(table);
							if (tab != null) {
								if (!isDeleted) {
									tab.UpdateResultJSON(result, metadata, db);
									syncResult.stats.numEntries++;
								} else {
									tab.DeleteWithUri(metadata.getString("uri"), db);
								}
							} else
								Log.d("ListSync", table);
						}

					} else {
						Log.e(TAG, "Server error in fetching remote contacts: "
								+ response.getStatusLine());
						throw new IOException();
					}

				}
			}

			if (serverBlob != null) {
				HttpPost p = (HttpPost) (request = new HttpPost(DOWNLOAD_SERVICE_URI
						+ authtoken));
				p.setEntity(new StringEntity(String.format(send, serverBlob, "")));
			} else {
				request = new HttpGet(DOWNLOAD_SERVICE_URI + authtoken);
			}
			boolean moreChanges = false;
			ContentValues vals = new ContentValues(1);
			do {
				HttpResponse response = httpClient.execute(request);
				if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
					InputStream instream = response.getEntity().getContent();
					BufferedReader r = new BufferedReader(
							new InputStreamReader(instream), 8000);
					StringBuilder total = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null) {
						total.append(line);
					}
					instream.close();

					String bufstring = total.toString();
					Log.d("ListSync", bufstring);
					JSONObject obj = new JSONObject(bufstring);
					JSONObject d = obj.getJSONObject("d");
					JSONObject sync = d.getJSONObject("__sync");
					serverBlob = sync.getString("serverBlob");
					moreChanges = sync.getBoolean("moreChangesAvailable");
					JSONArray results = d.getJSONArray("results");
					for (int i = 0; i < results.length(); i++) {
						JSONObject result = results.optJSONObject(i);
						JSONObject metadata = result.getJSONObject("__metadata");
						boolean isDeleted = metadata.optBoolean("isDeleted");
						String table = metadata.getString("type").replace(
								"DefaultScope.", "");
						if (!notifyTables.contains(table))
							notifyTables.add(table);
						Tables tab = Tables.AllTables.get(table);
						if (tab != null) {
							if (!isDeleted) {
								tab.SyncJSON(result, metadata, db, syncResult.stats);
								syncResult.stats.numEntries++;
							} else {
								tab.DeleteWithUri(metadata.getString("uri"), db);
								syncResult.stats.numDeletes++;
								syncResult.stats.numEntries++;
							}
						} else
							Log.d("ListSync", table);
					}

				} else {
					Log.e(TAG, "Server error in fetching remote contacts: "
							+ response.getStatusLine());
					throw new IOException();
				}
			} while (moreChanges);

			vals.put(Database.Settings.C_VALUE, serverBlob);
			if (db.update(Database.Settings.NAME, vals, "Name=?",
					new String[] { "serverBlob" }) == 0) {
				vals.put(Database.Settings.C_NAME, "serverBlob");
				db.insert(Database.Settings.NAME, null, vals);
			}
			for (String t : notifyTables) {
				getContext().getContentResolver().notifyChange(
						Uri.withAppendedPath(ListProvider.CONTENT_URI, t), null);
			}
			getContext().getContentResolver().notifyChange(
					Uri.withAppendedPath(ListProvider.CONTENT_URI, "Item/*"), null);
		} catch (final ConnectTimeoutException e) {
			Log.e(TAG, "ConnectTimeoutException", e);
			syncResult.stats.numIoExceptions++;
		} catch (final AuthenticatorException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "AuthenticatorException", e);
		} catch (final OperationCanceledException e) {
			Log.e(TAG, "OperationCanceledExcetpion", e);
		} catch (final IOException e) {
			Log.e(TAG, "IOException", e);
			syncResult.stats.numIoExceptions++;
			// } catch (final AuthenticationException e) {
			// mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE,
			// authtoken);
			// syncResult.stats.numAuthExceptions++;
			// Log.e(TAG, "AuthenticationException", e);
		} catch (final ParseException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "ParseException", e);
		} catch (final JSONException e) {
			syncResult.stats.numParseExceptions++;
			Log.e(TAG, "JSONException", e);
		}
		if (!syncResult.hasError()) {
			db.setTransactionSuccessful();
			getContext()
					.sendOrderedBroadcast(new Intent(Constants.SYNCACTION_STOP), null);
		}
		db.endTransaction();
	}
}