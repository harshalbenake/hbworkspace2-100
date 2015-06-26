package com.aviary.launcher3d;

import java.util.List;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.aviary.launcher3d.ImageInfo.Info;

public class ImageInfoActivity extends ListActivity implements OnItemClickListener {

	private final String LOG_TAG = "ImageInfoActivity";
	private Button mCloseButton;
	private ImageInfo mImageInfo;
	private GetGeoLocationTask mLocationTask;

	@Override
	protected void onCreate( Bundle savedInstanceState ) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.image_info_layout );
		onHandleIntent( getIntent() );

		mCloseButton.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick( View v ) {
				finish();
			}
		} );
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if ( null != mLocationTask ) {
			mLocationTask.cancel( true );
		}
	}

	@Override
	public void onContentChanged() {
		Log.i( LOG_TAG, "onContentChanged" );
		mCloseButton = (Button) findViewById( R.id.button1 );
		super.onContentChanged();
	}

	private void onHandleIntent( Intent intent ) {
		if ( null != intent ) {
			Bundle extras = intent.getExtras();
			if ( null != extras ) {
				if ( extras.containsKey( "image-info" ) ) {
					try {
						mImageInfo = (ImageInfo) extras.getSerializable( "image-info" );
					} catch ( Throwable t ) {
						t.printStackTrace();
					}

					if ( null != mImageInfo ) {
						setListAdapter( new MyListAdapter( this, android.R.layout.simple_list_item_2, mImageInfo.getInfo() ) );
						getListView().setOnItemClickListener( this );
						onUpdateLocation();
					}
				}
			}
		}
	}

	private void onUpdateLocation() {
		if ( null != mImageInfo ) {
			float[] latlong = new float[] { ImageInfo.INVALID_LATLNG, ImageInfo.INVALID_LATLNG };
			mImageInfo.getLatLong( latlong );

			if ( latlong[0] != ImageInfo.INVALID_LATLNG ) {
				mLocationTask = new GetGeoLocationTask();
				mLocationTask.execute( latlong[0], latlong[1] );
			}
		}
	}

	private class MyListAdapter extends ArrayAdapter<ImageInfo.Info> {

		private LayoutInflater mInflater;
		private int mResourceId;

		public MyListAdapter( Context context, int resource, List<Info> objects ) {
			super( context, resource, objects );

			mInflater = (LayoutInflater) context.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			mResourceId = resource;
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent ) {

			View view;
			ImageInfo.Info info = getItem( position );

			if ( convertView == null ) {
				view = mInflater.inflate( mResourceId, parent, false );
			} else {
				view = convertView;
			}

			TextView t1 = (TextView) view.findViewById( android.R.id.text1 );
			TextView t2 = (TextView) view.findViewById( android.R.id.text2 );
			t1.setText( info.getTag() );
			t2.setText( info.getValue() );
			return view;
		}
	}

	private class GetGeoLocationTask extends AsyncTask<Float, Void, Address> {

		@Override
		protected Address doInBackground( Float... params ) {

			float lat = params[0];
			float lon = params[1];

			List<Address> result;

			try {
				Geocoder geo = new Geocoder( ImageInfoActivity.this );
				result = geo.getFromLocation( lat, lon, 1 );
			} catch ( Exception e ) {
				e.printStackTrace();
				return null;
			}

			if ( null != result && result.size() > 0 ) {
				return result.get( 0 );
			}

			return null;
		}

		@Override
		protected void onPostExecute( Address result ) {
			super.onPostExecute( result );

			if ( isCancelled() || isFinishing() ) return;

			if ( null != result ) {
				try {
					mImageInfo.setAddress( result );
					ImageInfoActivity.this.setListAdapter( new MyListAdapter( ImageInfoActivity.this,
							android.R.layout.simple_list_item_2, mImageInfo.getInfo() ) );
				} catch ( Throwable t ) {}
			}
		}
	}

	@Override
	public void onItemClick( AdapterView<?> listView, View item, int position, long id ) {
		if ( null != listView.getAdapter() ) {
			Object adapter = listView.getAdapter();
			if ( adapter instanceof MyListAdapter ) {
				Info entry = ( (MyListAdapter) adapter ).getItem( position );
				if ( null != entry ) {

					if ( "Address".equals( entry.getTag() ) ) {
						startMapView( entry );
					}
				}
			}
		}
	}

	private void startMapView( Info info ) {
		try {
			String geoAddress = "geo:";
			float[] latlong = (float[]) info.getRawData();
			geoAddress += latlong[0] + "," + latlong[1];
			geoAddress += "?z=14";
			geoAddress += "&q=" + latlong[0] + "," + latlong[1];
			Intent i = new Intent( android.content.Intent.ACTION_VIEW, Uri.parse( geoAddress ) );
			startActivity( i );
		} catch ( Exception e ) {
			e.printStackTrace();
		}
	}
}
