package com.example.harshalb.myapplication;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomCameraActivity extends Activity implements SurfaceHolder.Callback {

	Camera mCamera;
	SurfaceView mSurfaceView;
	SurfaceHolder mSurfaceHolder;
	boolean previewing = false;
	LayoutInflater controlInflater = null;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private boolean safeToTakePicture = false;

    private boolean isRecording = false;
    private MediaRecorder mMediaRecorder;
    private CountDownTimer mCountDownTimer;
    private boolean isStarted=false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        getWindow().setFormat(PixelFormat.UNKNOWN);
        mSurfaceView = (SurfaceView)findViewById(R.id.camerapreview);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        LayoutInflater inflater = LayoutInflater.from(getBaseContext());
        View view = inflater.inflate(R.layout.custom, null);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        final TextView txtCameraTime=(TextView)view.findViewById(R.id.txtCameraTime);
        this.addContentView(view, layoutParams);

        mCountDownTimer = new CountDownTimer(10000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                startAudioRecording();
                txtCameraTime.setText("seconds remaining: " + millisUntilFinished / 1000);
                isStarted=true;
            }

            @Override
            public void onFinish() {
                stopAudioRecording();
                Toast.makeText(getBaseContext(), "stopAudioRecording", Toast.LENGTH_SHORT).show();
               /* Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + "7588871488"));
                startActivity(intent);*/
                finish();
            }
        };


        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                /*if (safeToTakePicture) {
                    mCamera.takePicture(null, null, mPicture);
                    safeToTakePicture = false;
                }*/


                if(isStarted==false) {
                    mCountDownTimer.start();
                }
                else {
                    mCountDownTimer.cancel();
                }


            }
        });

    }


    public void startAudioRecording(){
        if (isRecording==false && isStarted==false) {

            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                isRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                // inform user
            }
        }
    }

    public void stopAudioRecording(){
        if (isRecording) {
            // stop recording and release camera
            mMediaRecorder.stop();
             // stop the recording
            releaseMediaRecorder();
            if (mCamera != null) {
                // take camera access back from MediaRecorder
                mCamera.lock();
            }
            isRecording = false;
        }
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    @Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if(previewing){
			mCamera.stopPreview();
			previewing = false;
		}
		
		if (mCamera != null){
			try {
				mCamera.setPreviewDisplay(mSurfaceHolder);
				mCamera.startPreview();
                mCamera.startPreview();
                safeToTakePicture = true;
				previewing = true;
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.stopPreview();
		mCamera.release();
		mCamera = null;
		previewing = false;
	}


    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            camera.startPreview();
            if (pictureFile == null){
                //no path to picture
                safeToTakePicture = true;
                return;
            }

            try {
                if(!pictureFile.exists()){
                    pictureFile.createNewFile();
                }
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
            //finished saving picture
            safeToTakePicture = true;
        }
    };


    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = Environment.getExternalStorageDirectory();

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +"IMG_"+timeStamp+".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +"VID_" +timeStamp+".3gp");
        } else {
            return null;
        }
        return mediaFile;
    }

    private boolean prepareVideoRecorder(){

        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        // Step 4: Set output file
        mMediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mMediaRecorder.setPreviewDisplay(mSurfaceView.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            releaseMediaRecorder();
            return false;
        }
        return true;
    }
}