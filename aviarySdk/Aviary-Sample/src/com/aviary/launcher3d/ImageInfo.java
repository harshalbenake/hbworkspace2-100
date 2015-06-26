package com.aviary.launcher3d;

import it.sephiroth.android.library.media.ExifInterfaceExtended;

import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;

import com.aviary.android.feather.common.utils.IOUtils;
import com.aviary.android.feather.common.utils.StringUtils;
import com.aviary.android.feather.headless.utils.CameraUtils;

public class ImageInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private static final int INDEX_CAPTION = 1;
	private static final int INDEX_LATITUDE = 3;
	private static final int INDEX_LONGITUDE = 4;
	private static final int INDEX_DATA = 8;
	private static final int INDEX_ORIENTATION = 9;
	private static final int INDEX_SIZE_ID = 11;

	public static final String[] PROJECTION = { ImageColumns._ID, // 0
			ImageColumns.TITLE, // 1
			ImageColumns.MIME_TYPE, // 2
			ImageColumns.LATITUDE, // 3
			ImageColumns.LONGITUDE, // 4
			ImageColumns.DATE_TAKEN, // 5
			ImageColumns.DATE_ADDED, // 6
			ImageColumns.DATE_MODIFIED, // 7
			ImageColumns.DATA, // 8
			ImageColumns.ORIENTATION, // 9
			ImageColumns.BUCKET_ID, // 10
			ImageColumns.SIZE, // 11
			ImageColumns.BUCKET_DISPLAY_NAME, // 12
	};

	public long id;
	public String caption;
	public String mimeType;
	public long fileSize = 0L;
	public float latitude = INVALID_LATLNG;
	public float longitude = INVALID_LATLNG;
	public long dateTakenInMs;
	public long dateAddedInSec;
	public long dateModifiedInSec;
	public String filePath;
	public int bucketId;
	public int rotation = 0;

	private String dateTime;
	private int width, height;
	private int orientation;
	private Address address;

	private String dateTimeFile;

	private String process;

	private int quality;

	private String exifVersion;

	private String camera;

	private String software;

	private String artist;

	private String copyright;

	private String dateTimeDigitized;

	private String dateTimeOriginal;

	private String flash;

	private String focalLength;

	private String digitalZoom;

	private String ccdWidth;

	private String exposureTime;

	private String apertureSize;

	private String brightness;

	private String colorSpace;

	private String subjectDistance;

	private String subjectDistanceRange;

	private String exposureBias;

	private String whiteBalance;

	private String lightSource;

	private String meteringMode;

	private String exposureProgram;

	private String exposureMode;

	private String shutterSpeed;

	private String sensingMethod;

	private String sceneCaptureType;

	private String altitude;

	private String latitudeString;

	private String longitudeString;

	public static final float INVALID_LATLNG = 0f;

	public ImageInfo () {}

	public ImageInfo ( Context context, Uri uri ) throws IOException {

		String path;
		path = IOUtils.getRealFilePath( context, uri );

		onLoadFromUri( context, uri );

		if ( null != path ) {
			onLoadExifData( path );
			onLoadImageSize( path, rotation );
		}
	}

	private void onLoadFromUri( Context context, Uri imageUri ) {
		Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
		Cursor cursor;

		if ( ContentResolver.SCHEME_CONTENT.equals( imageUri.getScheme() ) ) {
			cursor = context.getContentResolver().query( imageUri, PROJECTION, null, null, null );
		} else {
			cursor = context.getContentResolver().query( uri, PROJECTION,
					ImageColumns.DATA + " LIKE '%" + imageUri.toString() + "%'", null, null );
		}

		if ( null != cursor ) {
			if ( cursor.moveToFirst() ) {
				onLoadFromCursor( cursor );
			}
			cursor.close();
		}
	}

	protected void onLoadFromCursor( Cursor cursor ) {
		caption = cursor.getString( INDEX_CAPTION );
		latitude = cursor.getFloat( INDEX_LATITUDE );
		longitude = cursor.getFloat( INDEX_LONGITUDE );
		filePath = cursor.getString( INDEX_DATA );
		rotation = cursor.getInt( INDEX_ORIENTATION );
		fileSize = cursor.getLong( INDEX_SIZE_ID );
	}

	private void onLoadExifData( String path ) {
		if ( null != path ) {
			try {
				ExifInterfaceExtended mExif = new ExifInterfaceExtended( path );

				if ( null != mExif ) {
					NumberFormat decimalFormatter = DecimalFormat.getNumberInstance();

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_JPEG_FILESIZE ) ) {
						fileSize = mExif.getAttributeInt( ExifInterfaceExtended.TAG_JPEG_FILESIZE, 0 );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_JPEG_FILE_DATETIME ) ) {

						Date datetimeFile = new Date( mExif.getDateTime( mExif
								.getAttribute( ExifInterfaceExtended.TAG_JPEG_FILE_DATETIME ) ) );
						dateTimeFile = datetimeFile.toString();
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_ORIENTATION ) ) {
						int value = mExif.getOrientation();
						if ( value != 0 ) {
							orientation = value;
						}
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_JPEG_IMAGE_WIDTH )
							&& mExif.hasAttribute( ExifInterfaceExtended.TAG_JPEG_IMAGE_HEIGHT ) ) {
						int exif_width = mExif.getAttributeInt( ExifInterfaceExtended.TAG_JPEG_IMAGE_WIDTH, 0 );
						int exif_height = mExif.getAttributeInt( ExifInterfaceExtended.TAG_JPEG_IMAGE_HEIGHT, 0 );
						if ( exif_width > 0 && exif_height > 0 ) {
							width = exif_width;
							height = exif_height;
							if ( orientation == 90 || orientation == 270 ) {
								int w = width;
								width = height;
								height = w;
							}
						}
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_JPEG_PROCESS ) ) {
						int value = mExif.getAttributeInt( ExifInterfaceExtended.TAG_JPEG_PROCESS, 0 );
						process = parseProcess( value );
					}

					quality = mExif.getJpegQuality();

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_VERSION ) ) {
						exifVersion = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_VERSION );
					}

					StringBuilder sb = new StringBuilder();

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_MAKE ) ) {
						sb.append( mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_MAKE ) );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_MODEL ) ) {
						if ( sb.length() > 0 ) {
							sb.append( "/" );
						}
						sb.append( mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_MODEL ) );
					}

					if ( sb.length() > 0 ) {
						camera = sb.toString();
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_SOFTWARE ) ) {
						software = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_SOFTWARE );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_ARTIST ) ) {
						artist = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_ARTIST );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_COPYRIGHT ) ) {
						String value = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_COPYRIGHT );
						if ( null != value ) {
							value = value.trim();
							if ( value.length() > 0 ) {
								copyright = value;
							}
						}
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_DATETIME ) ) {
						Date date = new Date( mExif.getDateTime( mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_DATETIME ) ) );
						dateTime = date.toString();
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_DATETIME_DIGITIZED ) ) {
						Date date = new Date( mExif.getDateTime( mExif
								.getAttribute( ExifInterfaceExtended.TAG_EXIF_DATETIME_DIGITIZED ) ) );
						dateTimeDigitized = date.toString();
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_DATETIME_ORIGINAL ) ) {
						Date date = new Date( mExif.getDateTime( mExif
								.getAttribute( ExifInterfaceExtended.TAG_EXIF_DATETIME_ORIGINAL ) ) );
						dateTimeOriginal = date.toString();
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_FLASH ) ) {
						int value = mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_FLASH, 0 );
						flash = processFlash( value );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_FOCAL_LENGHT ) ) {
						String value = mExif.getAttributeDouble( ExifInterfaceExtended.TAG_EXIF_FOCAL_LENGHT, 0 ) + "mm";

						if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_FOCAL_LENGTH_35_MM ) ) {
							value += " (35mm equivalent: "
									+ mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_FOCAL_LENGTH_35_MM, 0 ) + "mm)";
						}

						focalLength = value;
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_DIGITAL_ZOOM_RATIO ) ) {
						digitalZoom = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_DIGITAL_ZOOM_RATIO );
					}

					double ccd_width = mExif.getCCDWidth();
					if ( ccd_width > 0 ) {
						decimalFormatter.setMaximumFractionDigits( 1 );
						ccdWidth = decimalFormatter.format( ccd_width ) + "mm";
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_EXPOSURE_TIME ) ) {
						exposureTime = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_EXPOSURE_TIME ) + "s";
					}

					sb = new StringBuilder();
					double fNumber = mExif.getApertureSize();

					if ( fNumber > 0 ) {
						sb.append( "f/" + fNumber + " " );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_ISO_SPEED_RATINGS ) ) {
						sb.append( "ISO-" + mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_ISO_SPEED_RATINGS ) );
					}

					if ( sb.length() > 0 ) {
						apertureSize = sb.toString();
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_BRIGHTNESS ) ) {
						brightness = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_BRIGHTNESS );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_COLOR_SPACE ) ) {
						colorSpace = processColorSpace( mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_COLOR_SPACE, 0 ) );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_SUBJECT_DISTANCE ) ) {
						double distance = mExif.getAttributeDouble( ExifInterfaceExtended.TAG_EXIF_SUBJECT_DISTANCE, 0 );
						if ( distance > 0 ) {
							subjectDistance = distance + "m";
						} else {
							subjectDistance = "Infinite";
						}
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_SUBJECT_DISTANCE_RANGE ) ) {
						int value = mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_SUBJECT_DISTANCE_RANGE, 0 );
						subjectDistanceRange = processSubjectDistanceRange( value );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_EXPOSURE_BIAS ) ) {
						exposureBias = mExif.getAttribute( ExifInterfaceExtended.TAG_EXIF_EXPOSURE_BIAS );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_WHITE_BALANCE ) ) {
						whiteBalance = processWhiteBalance( mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_WHITE_BALANCE, 0 ) );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_LIGHT_SOURCE ) ) {
						lightSource = processLightSource( mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_LIGHT_SOURCE, 0 ) );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_METERING_MODE ) ) {
						meteringMode = processMeteringMode( mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_METERING_MODE, 0 ) );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_EXPOSURE_PROGRAM ) ) {
						exposureProgram = processExposureProgram( mExif.getAttributeInt(
								ExifInterfaceExtended.TAG_EXIF_EXPOSURE_PROGRAM, 0 ) );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_EXPOSURE_MODE ) ) {
						exposureMode = processExposureMode( mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_EXPOSURE_MODE, 0 ) );
					}

					if ( mExif.getAttributeDouble( ExifInterfaceExtended.TAG_EXIF_SHUTTER_SPEED_VALUE, 0 ) > 0 ) {
						double value = mExif.getAttributeDouble( ExifInterfaceExtended.TAG_EXIF_SHUTTER_SPEED_VALUE, 0 );

						decimalFormatter.setMaximumFractionDigits( 1 );
						String string = "1/" + decimalFormatter.format( Math.pow( 2, value ) ) + "s";
						shutterSpeed = string;
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_SENSING_METHOD ) ) {
						sensingMethod = processSensingMethod( mExif.getAttributeInt( ExifInterfaceExtended.TAG_EXIF_SENSING_METHOD,
								0 ) );
					}

					if ( mExif.hasAttribute( ExifInterfaceExtended.TAG_EXIF_SCENE_CAPTURE_TYPE ) ) {
						sceneCaptureType = processSceneCaptureType( mExif.getAttributeInt(
								ExifInterfaceExtended.TAG_EXIF_SCENE_CAPTURE_TYPE, 0 ) );
					}

					double alt = mExif.getAltitude( 0 );
					if ( alt != 0 ) {
						decimalFormatter.setMaximumFractionDigits( 1 );
						altitude = decimalFormatter.format( alt ) + "m";
					}

					float[] lat = new float[] { INVALID_LATLNG, INVALID_LATLNG };
					mExif.getLatLong( lat );

					if ( lat[0] != INVALID_LATLNG ) {
						latitude = lat[0];
						longitude = lat[1];

						latitudeString = mExif.getLatitude();
						longitudeString = mExif.getLongitude();
					}
				}

			} catch ( IOException e ) {
				e.printStackTrace();
				return;
			}
		}
	}

	void onLoadImageSize( String path, int orientation ) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		try {
			BitmapFactory.decodeFile( path, options );
		} catch ( Throwable t ) {
			return;
		}

		width = options.outWidth;
		height = options.outHeight;

		if ( this.orientation == 0 ) {
			this.orientation = orientation;
		}

		if ( orientation == 90 || orientation == 270 ) {
			width = options.outHeight;
			height = options.outWidth;
		}

	}

	public void setAddress( Address value ) {
		address = value;
	}

	public String getAddressRepr() {
		if ( null != address ) {
			List<String> lines = new ArrayList<String>();
			if ( null != address.getThoroughfare() ) lines.add( address.getThoroughfare() );
			if ( null != address.getPostalCode() ) lines.add( address.getPostalCode() );
			if ( null != address.getLocality() ) lines.add( address.getLocality() );
			if ( null != address.getAdminArea() ) lines.add( address.getAdminArea() );
			if ( null != address.getCountryCode() ) lines.add( address.getCountryCode() );

			return StringUtils.join( lines, ", " );
		}
		return null;
	}

	public List<Info> getInfo() {
		List<Info> result = new ArrayList<Info>();

		if ( null != caption ) {
			result.add( new Info( "Title", caption ) );
		}

		if ( fileSize > 0 ) {
			String value = humanReadableByteCount( fileSize, true );
			result.add( new Info( "File size", value ) );
		}

		if ( null != dateTimeFile ) {
			result.add( new Info( "File Date", dateTimeFile ) );
		}

		if ( width > 0 && height > 0 ) {
			result.add( new Info( "Dimension", width + "x" + height + " (" + CameraUtils.getMegaPixels( width, height ) + "MP)" ) );
		}

		StringBuilder sb = new StringBuilder();

		if ( null != process ) {
			sb.append( "Process: " + process );
		}

		if ( quality > 0 ) {
			if ( sb.length() > 0 ) {
				sb.append( ", " );
			}
			sb.append( "quality: " + quality );
		}

		if ( sb.length() > 0 ) {
			result.add( new Info( "JPEG Info", sb.toString() ) );
		}

		if ( null != exifVersion ) {
			result.add( new Info( "Exif Version", exifVersion ) );
		}

		if ( null != camera ) {
			result.add( new Info( "Camera", camera ) );
		}

		if ( null != software ) {
			result.add( new Info( "Software", software ) );
		}

		if ( null != artist ) {
			result.add( new Info( "Artist", artist ) );
		}

		if ( null != copyright ) {
			result.add( new Info( "Copyright", copyright ) );
		}

		if ( orientation != 0 ) {
			result.add( new Info( "Orientation", orientation + "¡" ) );
		}

		if ( null != dateTime ) {
			result.add( new Info( "Date", dateTime ) );
		}

		if ( null != dateTimeDigitized ) {
			result.add( new Info( "Date Digitized", dateTimeDigitized ) );
		}

		if ( null != dateTimeOriginal ) {
			result.add( new Info( "Date Original", dateTimeOriginal ) );
		}

		if ( null != flash ) {
			result.add( new Info( "Flash", flash ) );
		}

		if ( null != focalLength ) {
			result.add( new Info( "Focal Length", focalLength ) );
		}

		if ( null != digitalZoom ) {
			result.add( new Info( "Digital Zoom", digitalZoom ) );
		}

		if ( null != ccdWidth ) {
			result.add( new Info( "CCD Width", ccdWidth ) );
		}

		if ( null != exposureTime ) {
			result.add( new Info( "Exposure Time", exposureTime ) );
		}

		if ( null != apertureSize ) {
			result.add( new Info( "Aperture Size", apertureSize ) );
		}

		if ( null != brightness ) {
			result.add( new Info( "Brightness", brightness ) );
		}

		if ( null != colorSpace ) {
			result.add( new Info( "Color Space", colorSpace ) );
		}

		if ( null != subjectDistance ) {
			result.add( new Info( "Subject Distance", subjectDistance ) );
		}

		if ( null != subjectDistanceRange ) {
			result.add( new Info( "Subject Distance Range", subjectDistanceRange ) );
		}

		if ( null != exposureBias ) {
			result.add( new Info( "Exposure Bias", exposureBias ) );
		}

		if ( null != whiteBalance ) {
			result.add( new Info( "White Balance", whiteBalance ) );
		}

		if ( null != lightSource ) {
			result.add( new Info( "Light Source", lightSource ) );
		}

		if ( null != meteringMode ) {
			result.add( new Info( "Metering Mode", meteringMode ) );
		}

		if ( null != exposureProgram ) {
			result.add( new Info( "Exposure Program", exposureProgram ) );
		}

		if ( null != exposureMode ) {
			result.add( new Info( "Exposure Mode", exposureMode ) );
		}

		if ( null != shutterSpeed ) {
			result.add( new Info( "Shutter Speed", shutterSpeed ) );
		}

		if ( null != sensingMethod ) {
			result.add( new Info( "Sensing Method", sensingMethod ) );
		}

		if ( null != sceneCaptureType ) {
			result.add( new Info( "Scene Capture Type", sceneCaptureType ) );
		}

		Info addressInfo = new Info( "Address", "" );
		boolean shouldAdd = false;

		float[] latlong = new float[] { INVALID_LATLNG, INVALID_LATLNG };
		getLatLong( latlong );

		if ( latlong[0] != INVALID_LATLNG ) {
			shouldAdd = true;
			addressInfo.rawData = latlong;

			if ( null != latitudeString && null != longitudeString ) {
				addressInfo.value = latitudeString + "/" + longitudeString;
			} else {
				addressInfo.value = "";
			}

			if ( null != address ) {
				String value = getAddressRepr();
				if ( null != value ) {
					shouldAdd = true;
					addressInfo.value = value;
				}
			}
		}

		if ( shouldAdd ) {
			result.add( addressInfo );
			if ( null != altitude ) {
				result.add( new Info( "Altitude", altitude ) );
			}
		}

		if ( null != filePath ) result.add( new Info( "Path", filePath ) );
		return result;
	}
	
	public void getLatLong( float[] latlong ) {
		latlong[0] = latitude;
		latlong[1] = longitude;
	}	

	private String processSceneCaptureType( int value ) {
		switch ( value ) {
			case 0:
				return "Standard";
			case 1:
				return "Landscape";
			case 2:
				return "Portrait";
			case 3:
				return "Night scene";
			default:
				return "Unknown";
		}
	}

	private String processSensingMethod( int value ) {
		switch ( value ) {
			case 1:
				return "Not defined";
			case 2:
				return "One-chip color area sensor";
			case 3:
				return "Two-chip color area sensor JEITA CP-3451 - 41";
			case 4:
				return "Three-chip color area sensor";
			case 5:
				return "Color sequential area sensor";
			case 7:
				return "Trilinear sensor";
			case 8:
				return "Color sequential linear sensor";
			default:
				return "Unknown";
		}
	}

	private String processColorSpace( int value ) {
		switch ( value ) {
			case 1:
				return "sRGB";
			case 0xFFFF:
				return "Uncalibrated";
			default:
				return "Unknown";
		}
	}

	private String processExposureMode( int mode ) {
		switch ( mode ) {
			case 0:
				return "Auto exposure";
			case 1:
				return "Manual exposure";
			case 2:
				return "Auto bracket";
			default:
				return "Unknown";
		}
	}

	private String processExposureProgram( int program ) {
		switch ( program ) {
			case 1:
				return "Manual control";
			case 2:
				return "Program normal";
			case 3:
				return "Aperture priority";
			case 4:
				return "Shutter priority";
			case 5:
				return "Program creative (slow program)";
			case 6:
				return "Program action(high-speed program)";
			case 7:
				return "Portrait mode";
			case 8:
				return "Landscape mode";
			default:
				return "Unknown";
		}
	}

	private String processMeteringMode( int mode ) {
		switch ( mode ) {
			case 1:
				return "Average";
			case 2:
				return "CenterWeightedAverage";
			case 3:
				return "Spot";
			case 4:
				return "MultiSpot";
			case 5:
				return "Pattern";
			case 6:
				return "Partial";
			case 255:
				return "Other";
			default:
				return "Unknown";
		}
	}

	private String processLightSource( int value ) {
		switch ( value ) {
			case 0:
				return "Auto";
			case 1:
				return "Daylight";
			case 2:
				return "Fluorescent";
			case 3:
				return "Tungsten (incandescent light)";
			case 4:
				return "Flash";
			case 9:
				return "Fine weather";
			case 10:
				return "Cloudy weather";
			case 11:
				return "Shade";
			case 12:
				return "Daylight fluorescent (D 5700 Ð 7100K)";
			case 13:
				return "Day white fluorescent (N 4600 Ð 5400K)";
			case 14:
				return "Cool white fluorescent (W 3900 Ð 4500K)";
			case 15:
				return "White fluorescent (WW 3200 Ð 3700K)";
			case 17:
				return "Standard light A";
			case 18:
				return "Standard light B";
			case 19:
				return "Standard light C";
			case 20:
				return "D55";
			case 21:
				return "D65";
			case 22:
				return "D75";
			case 23:
				return "D50";
			case 24:
				return "ISO studio tungsten";
			case 255:
				return "Other light source";
			default:
				return "Unknown";
		}
	}

	private String processWhiteBalance( int value ) {
		switch ( value ) {
			case 0:
				return "Auto";
			case 1:
				return "Manual";
			default:
				return "Unknown";
		}
	}

	private String processSubjectDistanceRange( int value ) {
		switch ( value ) {
			case 1:
				return "Macro";
			case 2:
				return "Close View";
			case 3:
				return "Distant View";
			default:
				return "Unknown";
		}
	}

	private String processFlash( int flash ) {
		switch ( flash ) {
			case 0x0000:
				return "Flash did not fire";
			case 0x0001:
				return "Flash fired";
			case 0x0005:
				return "Strobe return light not detected";
			case 0x0007:
				return "Strobe return light detected";
			case 0x0009:
				return "Flash fired, compulsory flash mode";
			case 0x000D:
				return "Flash fired, compulsory flash mode, return light not detected";
			case 0x000F:
				return "Flash fired, compulsory flash mode, return light detected";
			case 0x0010:
				return "Flash did not fire, compulsory flash mode";
			case 0x0018:
				return "Flash did not fire, auto mode";
			case 0x0019:
				return "Flash fired, auto mode";
			case 0x001D:
				return "Flash fired, auto mode, return light not detected";
			case 0x001F:
				return "Flash fired, auto mode, return light detected";
			case 0x0020:
				return "No flash function";
			case 0x0041:
				return "Flash fired, red-eye reduction mode";
			case 0x0045:
				return "Flash fired, red-eye reduction mode, return light not detected";
			case 0x0047:
				return "Flash fired, red-eye reduction mode, return light detected";
			case 0x0049:
				return "Flash fired, compulsory flash mode, red-eye reduction mode";
			case 0x004D:
				return "Flash fired, compulsory flash mode, red-eye reduction mode, return light not detected";
			case 0x004F:
				return "Flash fired, compulsory flash mode, red-eye reduction mode, return light detected";
			case 0x0059:
				return "Flash fired, auto mode, red-eye reduction mode";
			case 0x005D:
				return "Flash fired, auto mode, return light not detected, red-eye reduction mode";
			case 0x005F:
				return "Flash fired, auto mode, return light detected, red-eye reduction mode";
			default:
				return "Reserved";
		}
	}

	private String parseProcess( int process ) {
		switch ( process ) {
			case 192:
				return "Baseline";
			case 193:
				return "Extended sequential";
			case 194:
				return "Progressive";
			case 195:
				return "Lossless";
			case 197:
				return "Differential sequential";
			case 198:
				return "Differential progressive";
			case 199:
				return "Differential lossless";
			case 201:
				return "Extended sequential, arithmetic coding";
			case 202:
				return "Progressive, arithmetic coding";
			case 203:
				return "Lossless, arithmetic coding";
			case 205:
				return "Differential sequential, arithmetic coding";
			case 206:
				return "Differential progressive, arithmetic codng";
			case 207:
				return "Differential lossless, arithmetic coding";
		}
		return "Unknown";
	}

	public String humanReadableByteCount( long bytes, boolean si ) {
		int unit = si ? 1000 : 1024;
		if ( bytes < unit ) return bytes + " B";
		int exp = (int) ( Math.log( bytes ) / Math.log( unit ) );
		String pre = ( si ? "kMGTPE" : "KMGTPE" ).charAt( exp - 1 ) + ( si ? "" : "i" );
		return String.format( Locale.US, "%.1f %sB", bytes / Math.pow( unit, exp ), pre );
	}

	public static final class Info {

		private final String tag;
		private String value;
		private Object rawData;

		public Info ( String t, String v ) {
			tag = t;
			value = v;
		}

		public String getValue() {
			return value;
		}

		public String getTag() {
			return tag;
		}

		public Object getRawData() {
			return rawData;
		}
	}
}
