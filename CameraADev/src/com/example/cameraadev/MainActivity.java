package com.example.cameraadev;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.cameraadev.R.id;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends Activity {
	private Uri fileUri;
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
	public static final String TAG = "Camera";

	  private Camera mCamera;
	    private boolean isRecording = false;
	    private SurfaceView mPreview;
	    private MediaRecorder mMediaRecorder;
	    public static final int MEDIA_TYPE_IMAGE = 1;
	    public static final int MEDIA_TYPE_VIDEO = 2;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
//
//		// create Intent to take a picture and return control to the calling application
//	    Intent intent_image = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//
//	    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE); // create a file to save the image
//	    intent_image.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name
//
//	    // start the image capture Intent
//	    startActivityForResult(intent_image, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);


	    //create new Intent
	    Intent intent_video = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

	    fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);  // create a file to save the video
	    intent_video.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name

	    intent_video.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high

	    // start the Video Capture Intent
	    startActivityForResult(intent_video, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);

	  
        
     // Create an instance of Camera
        mCamera = getCameraInstance();

        // get Camera parameters
        Camera.Parameters params = mCamera.getParameters();
        // set the focus mode
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        // set Camera parameters
        mCamera.setParameters(params);
        
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
          // Autofocus mode is supported
        }

        if (params.getMaxNumMeteringAreas() > 0){ // check that metering areas are supported
            List<Camera.Area> meteringAreas = new ArrayList<Camera.Area>();

            Rect areaRect1 = new Rect(-100, -100, 100, 100);    // specify an area in center of image
            meteringAreas.add(new Camera.Area(areaRect1, 600)); // set weight to 60%
            Rect areaRect2 = new Rect(800, -1000, 1000, -800);  // specify an area in upper right of image
            meteringAreas.add(new Camera.Area(areaRect2, 400)); // set weight to 40%
            params.setMeteringAreas(meteringAreas);
        }

        mCamera.setParameters(params);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        
     // Add a listener to the Capture button
        final Button captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isRecording) {
                        // stop recording and release camera
                        mMediaRecorder.stop();  // stop the recording
                        releaseMediaRecorder(); // release the MediaRecorder object
                        mCamera.lock();         // take camera access back from MediaRecorder

                        // inform the user that recording has stopped
                        setCaptureButtonText("Capture");
                        isRecording = false;
                    } else {
                        // initialize video camera
                        if (prepareVideoRecorder()) {
                            // Camera is available and unlocked, MediaRecorder is prepared,
                            // now you can start recording
                            mMediaRecorder.start();

                            // inform the user that recording has started
                            setCaptureButtonText("Stop");
                            isRecording = true;
                        } else {
                            // prepare didn't work, release the camera
                            releaseMediaRecorder();
                            // inform user
                        }
                    }
                }

				private void setCaptureButtonText(String buttonText) {
					captureButton.setText(buttonText);
					
				}
            }
        );
        
       
        mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
    
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	            // Image captured and saved to fileUri specified in the Intent
	            Toast.makeText(this, "Image saved to:\n" +
	                     data.getData(), Toast.LENGTH_LONG).show();
	        } else if (resultCode == RESULT_CANCELED) {
	            // User cancelled the image capture
	        } else {
	            // Image capture failed, advise user
	        }
	    }

	    if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
	        if (resultCode == RESULT_OK) {
	            // Video captured and saved to fileUri specified in the Intent
	            Toast.makeText(this, "Video saved to:\n" +
	                     data.getData(), Toast.LENGTH_LONG).show();
	        } else if (resultCode == RESULT_CANCELED) {
	            // User cancelled the video capture
	        } else {
	            // Video capture failed, advise user
	        }
	    }
	}
	    /** Check if this device has a camera */
	    private boolean checkCameraHardware(Context context) {
	        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	            // this device has a camera
	            return true;
	        } else {
	            // no camera on this device
	            return false;
	        }
	    }
	    
	    /** A safe way to get an instance of the Camera object. */
	    public static Camera getCameraInstance(){
	        Camera c = null;
	        try {
	            c = Camera.open(); // attempt to get a Camera instance
	        }
	        catch (Exception e){
	            // Camera is not available (in use or does not exist)
	        }
	        return c; // returns null if camera is unavailable
	    }
	    
	    
	    /** A basic Camera preview class */
	    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	        private SurfaceHolder mHolder;
	        private Camera mCamera;

	        public CameraPreview(Context context, Camera camera) {
	            super(context);
	            mCamera = camera;

	            // Install a SurfaceHolder.Callback so we get notified when the
	            // underlying surface is created and destroyed.
	            mHolder = getHolder();
	            mHolder.addCallback(this);
	            // deprecated setting, but required on Android versions prior to 3.0
	            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	        }

	      

	        public void surfaceDestroyed(SurfaceHolder holder) {
	            // empty. Take care of releasing the Camera preview in your activity.
	        }

	        public void surfaceCreated(SurfaceHolder holder) {
	            try {
	                mCamera.setPreviewDisplay(holder);
	                mCamera.startPreview();

	                startFaceDetection(); // start face detection feature

	            } catch (IOException e) {
	                Log.d(TAG, "Error setting camera preview: " + e.getMessage());
	            }
	        }

	        public void startFaceDetection(){
	    	    // Try starting Face Detection
	    	    Camera.Parameters params = mCamera.getParameters();

	    	    // start face detection only *after* preview has started
	    	    if (params.getMaxNumDetectedFaces() > 0){
	    	        // camera supports face detection, so can start it:
	    	        mCamera.startFaceDetection();
	    	    }
	    	}



			public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

	            if (mHolder.getSurface() == null){
	                // preview surface does not exist
	                Log.d(TAG, "mHolder.getSurface() == null");
	                return;
	            }

	            try {
	                mCamera.stopPreview();

	            } catch (Exception e){
	                // ignore: tried to stop a non-existent preview
	                Log.d(TAG, "Error stopping camera preview: " + e.getMessage());
	            }

	            try {
	                mCamera.setPreviewDisplay(mHolder);
	                mCamera.startPreview();

	                startFaceDetection(); // re-start face detection feature

	            } catch (Exception e){
	                // ignore: tried to stop a non-existent preview
	                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
	            }
	        }
	    }
	    
	    private PictureCallback mPicture = new PictureCallback() {

	        @Override
	        public void onPictureTaken(byte[] data, Camera camera) {

	            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
	            if (pictureFile == null){
	             //   Log.d(TAG, "Error creating media file, check storage permissions: " + e.getMessage());
	                return;
	            }

	            try {
	                FileOutputStream fos = new FileOutputStream(pictureFile);
	                fos.write(data);
	                fos.close();
	            } catch (FileNotFoundException e) {
	                Log.d(TAG, "File not found: " + e.getMessage());
	            } catch (IOException e) {
	                Log.d(TAG, "Error accessing file: " + e.getMessage());
	            }
	        }
	    };
	    
	    private boolean prepareVideoRecorder(){

	        mCamera = getCameraInstance();
	        mMediaRecorder = new MediaRecorder();

	        // Step 1: Unlock and set camera to MediaRecorder
	        mCamera.unlock();
	        mMediaRecorder.setCamera(mCamera);

	        // Step 2: Set sources
	        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

	        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
	        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_HIGH));
	        // Step 3: Set output format and encoding (for versions prior to API Level 8)
	        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
	        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

	        // Step 4: Set output file
	        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

	        // Step 5: Set the preview output
	        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
	        mMediaRecorder.setCaptureRate(0.1); // capture a frame every 10 seconds

	        // Step 6: Prepare configured MediaRecorder
	        try {
	            mMediaRecorder.prepare();
	        } catch (IllegalStateException e) {
	            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
	            releaseMediaRecorder();
	            return false;
	        } catch (IOException e) {
	            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
	            releaseMediaRecorder();
	            return false;
	        }
	        return true;
	    }
	    
	    

	    @Override
	    protected void onPause() {
	        super.onPause();
	        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
	        releaseCamera();              // release the camera immediately on pause event
	    }

	    private void releaseMediaRecorder(){
	        if (mMediaRecorder != null) {
	            mMediaRecorder.reset();   // clear recorder configuration
	            mMediaRecorder.release(); // release the recorder object
	            mMediaRecorder = null;
	            mCamera.lock();           // lock camera for later use
	        }
	    }

	    private void releaseCamera(){
	        if (mCamera != null){
	            mCamera.release();        // release the camera for other applications
	            mCamera = null;
	        }
	    }


/** Create a file Uri for saving an image or video */
private static Uri getOutputMediaFileUri(int type){
      return Uri.fromFile(getOutputMediaFile(type));
}

/** Create a File for saving an image or video */
private static File getOutputMediaFile(int type){
    // To be safe, you should check that the SDCard is mounted
    // using Environment.getExternalStorageState() before doing this.

    File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
              Environment.DIRECTORY_PICTURES), "MyCameraApp");
    // This location works best if you want the created images to be shared
    // between applications and persist after your app has been uninstalled.

    // Create the storage directory if it does not exist
    if (! mediaStorageDir.exists()){
        if (! mediaStorageDir.mkdirs()){
            Log.d("MyCameraApp", "failed to create directory");
            return null;
        }
    }

    // Create a media file name
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    File mediaFile;
    if (type == MEDIA_TYPE_IMAGE){
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
        "IMG_"+ timeStamp + ".jpg");
    } else if(type == MEDIA_TYPE_VIDEO) {
        mediaFile = new File(mediaStorageDir.getPath() + File.separator +
        "VID_"+ timeStamp + ".mp4");
    } else {
        return null;
    }

    return mediaFile;
}

class MyFaceDetectionListener implements Camera.FaceDetectionListener {

   
	@Override
	public void onFaceDetection(Face[] faces, Camera camera) {
		// TODO Auto-generated method stub
		if (faces.length > 0){
			Log.d("FaceDetection", "face detected: "+ faces.length +
					" Face 1 Location X: " + faces[0].rect.centerX() +
					"Y: " + faces[0].rect.centerY() );
		}
		
	}
	
	
}
}
