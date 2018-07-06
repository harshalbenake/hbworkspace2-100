package com.example.azure;

import java.io.FileOutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.CloudBlob;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.ListBlobItem;

public class MainActivity extends Activity {
	public static final String ACCOUNT_NAME = "xxxACCOUNT_NAMExxx";
	public static final String ACCOUNT_KEY = "xxxACCOUNT_KEYxxx";
	public static final String CONTAINER_REF = "xxxCONTAINER_REFxxx";
	public static final String FILENAME = "xxxFILENAMExxx";
	public static final String STORAGE_CONNECTION_STRING = "DefaultEndpointsProtocol=http;"+ "AccountName=" + ACCOUNT_NAME + ";" + "AccountKey=" + ACCOUNT_KEY;
	public static final String STORED_ZIP_FILE_LOCATION = Environment.getExternalStorageDirectory().getPath();
	private CloudBlobContainer mContainer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Button button = (Button) findViewById(R.id.button1);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {
					public void run() {
						downloadViaAzure();
					}
				}).start();
				
			}
		});
	}

	public void downloadViaAzure() {
		try {
			// Retrieve storage account from connection-string.
			CloudStorageAccount storageAccount = CloudStorageAccount
					.parse(STORAGE_CONNECTION_STRING);
			// Create the blob client.
			CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
			// Get a reference to a container.
			// The container name must be lower case
			mContainer = blobClient.getContainerReference(CONTAINER_REF);
			// Create the container if it does not exist.
			mContainer.createIfNotExists();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Create a permissions object.
		BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
		// Include public access in the permissions object.
		containerPermissions
				.setPublicAccess(BlobContainerPublicAccessType.CONTAINER);
		// Set the permissions on the container.
		try {
			mContainer.uploadPermissions(containerPermissions);
		} catch (StorageException e1) {
			e1.printStackTrace();
		}

		try {
			// Retrieve storage account from connection-string.
			CloudStorageAccount storageAccount = CloudStorageAccount
					.parse(STORAGE_CONNECTION_STRING);
			// Create the blob client.
			CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
			// Retrieve reference to a previously created container.
			CloudBlobContainer container = blobClient
					.getContainerReference(CONTAINER_REF);
			// Loop over blobs within the container and output the URI to each
			// of them.
			for (ListBlobItem blobItem : container.listBlobs()) {
				 System.out.println("uri " +blobItem.getUri());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			// Retrieve storage account from connection-string.
			CloudStorageAccount storageAccount = CloudStorageAccount
					.parse(STORAGE_CONNECTION_STRING);
			// Create the blob client.
			CloudBlobClient blobClient = storageAccount.createCloudBlobClient();
			// Retrieve reference to a previously created container.
			CloudBlobContainer container = blobClient
					.getContainerReference(CONTAINER_REF);
			// Loop through each blob item in the container.
			for (ListBlobItem blobItem : container.listBlobs()) {
				// If the item is a blob, not a virtual directory.
				if (blobItem instanceof CloudBlob) {
					// Download the item and save it to a file with the same
					// name.
					CloudBlob blob = (CloudBlob) blobItem;
					blob.download(new FileOutputStream(STORED_ZIP_FILE_LOCATION
							+ FILENAME));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
    public class AsyncDownloadViaAzure extends AsyncTask<String, Void, Boolean> {
    	private ProgressDialog mProgressDialog;

		@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		mProgressDialog = new ProgressDialog(MainActivity.this);
    		mProgressDialog.setMessage("Downloading...");
    		mProgressDialog.setCancelable(false);
    		mProgressDialog.show();
    	}
    	
		@Override
		protected Boolean doInBackground(String... params) {
			try {
				downloadViaAzure();
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		
		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			mProgressDialog.dismiss();
		}
    	
    }
}
