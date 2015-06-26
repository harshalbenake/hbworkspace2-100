package com.example.rotatingwheel;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;

public class MainActivity extends Activity {

		
		private static Bitmap imageOriginal, imageScaled;
		private static Matrix matrix;
		 
		private ImageView dialer;
		
		  private int dialerHeight, dialerWidth;
		     
		    private GestureDetector detector;
		    
		     
		    // needed for detecting the inversed rotations
		    private boolean[] quadrantTouched;
		     
		    private boolean allowRotating;
		 
		@Override
		public void onCreate(Bundle savedInstanceState) {
		    super.onCreate(savedInstanceState);
		    setContentView(R.layout.activity_main);
		     
		    // load the image only once
		    if (imageOriginal == null) {
		        imageOriginal = BitmapFactory.decodeResource(getResources(), R.drawable.hbwheel);
		    }
		     
		    // initialize the matrix only once
		    if (matrix == null) {
		        matrix = new Matrix();
		    } else {
		        // not needed, you can also post the matrix immediately to restore the old state
		        matrix.reset();
		    }
		 
		     
		    detector = new GestureDetector(this, new MyGestureDetector());
	         
		    // there is no 0th quadrant, to keep it simple the first value gets ignored
	        quadrantTouched = new boolean[] { false, false, false, false, false };
	         
	        allowRotating = true;
	         
		    dialer = (ImageView) findViewById(R.id.imageView_ring);
		    dialer.setOnTouchListener(new MyOnTouchListener());
		    dialer.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
		 
		        @Override
		        public void onGlobalLayout() {                // method called more than once, but the values only need to be initialized one time
	                if (dialerHeight == 0 || dialerWidth == 0) {
	                    dialerHeight = dialer.getHeight();
	                    dialerWidth = dialer.getWidth();
	                     
	                    // resize
	                    Matrix resize = new Matrix();
	                    resize.postScale((float)Math.min(dialerWidth, dialerHeight) / (float)imageOriginal.getWidth(), (float)Math.min(dialerWidth, dialerHeight) / (float)imageOriginal.getHeight());
	                    imageScaled = Bitmap.createBitmap(imageOriginal, 0, 0, imageOriginal.getWidth(), imageOriginal.getHeight(), resize, false);
	                     
	                    // translate to the image view's center
	                    float translateX = dialerWidth / 2 - imageScaled.getWidth() / 2;
	                    float translateY = dialerHeight / 2 - imageScaled.getHeight() / 2;
	                    matrix.postTranslate(translateX, translateY);
	                     
	                    dialer.setImageBitmap(imageScaled);
	                    dialer.setImageMatrix(matrix);
	                }}
		    });
		     
		    
		    
		}
		 /**
	     * Simple implementation of an {@link OnTouchListener} for registering the dialer's touch events.
	     */
	    private class MyOnTouchListener implements OnTouchListener {
	         
	        private double startAngle;
	 
	        @Override
	        public boolean onTouch(View v, MotionEvent event) {
	 
	        	 switch (event.getAction()) {
	                 
	                 case MotionEvent.ACTION_DOWN:
	                      
	                     // reset the touched quadrants
	                     for (int i = 0; i < quadrantTouched.length; i++) {
	                         quadrantTouched[i] = false;
	                     }
	                      
	                     allowRotating = false;
	                      
	                     startAngle = getAngle(event.getX(), event.getY());
	                     break;
	                      
	                 case MotionEvent.ACTION_MOVE:
	                     double currentAngle = getAngle(event.getX(), event.getY());
	                     rotateDialer((float) (startAngle - currentAngle));
	                     startAngle = currentAngle;
	                     break;
	                      
	                 case MotionEvent.ACTION_UP:
	                     allowRotating = true;
	                     break;
	             
	            }
	             
	            // set the touched quadrant to true
	            quadrantTouched[getQuadrant(event.getX() - (dialerWidth / 2), dialerHeight - event.getY() - (dialerHeight / 2))] = true;
	             
	            detector.onTouchEvent(event);
	             
	            
	           
	            return true;
	        }
	    }
	     
	    
	    /**
	     * Simple implementation of a {@link SimpleOnGestureListener} for detecting a fling event.
	     */
	    private class MyGestureDetector extends SimpleOnGestureListener {
	        @Override
	        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
	             
	            // get the quadrant of the start and the end of the fling
	            int q1 = getQuadrant(e1.getX() - (dialerWidth / 2), dialerHeight - e1.getY() - (dialerHeight / 2));
	            int q2 = getQuadrant(e2.getX() - (dialerWidth / 2), dialerHeight - e2.getY() - (dialerHeight / 2));
	 
	            // the inversed rotations
	            if ((q1 == 2 && q2 == 2 && Math.abs(velocityX) < Math.abs(velocityY))
	                    || (q1 == 3 && q2 == 3)
	                    || (q1 == 1 && q2 == 3)
	                    || (q1 == 4 && q2 == 4 && Math.abs(velocityX) > Math.abs(velocityY))
	                    || ((q1 == 2 && q2 == 3) || (q1 == 3 && q2 == 2))
	                    || ((q1 == 3 && q2 == 4) || (q1 == 4 && q2 == 3))
	                    || (q1 == 2 && q2 == 4 && quadrantTouched[3])
	                    || (q1 == 4 && q2 == 2 && quadrantTouched[3])) {
	             
	                dialer.post(new FlingRunnable(-1 * (velocityX + velocityY)));
	            } else {
	                // the normal rotation
	                dialer.post(new FlingRunnable(velocityX + velocityY));
	            }
	 
	            return true;
	        }
	    }

	    /**
	     * A {@link Runnable} for animating the the dialer's fling.
	     */
	    private class FlingRunnable implements Runnable {
	 
	        private float velocity;
	 
	        public FlingRunnable(float velocity) {
	            this.velocity = velocity;
	        }
	 
	        @Override
	        public void run() {
	            if (Math.abs(velocity) > 5 && allowRotating) {
	                rotateDialer(velocity / 75);
	                velocity /= 1.0666F;
	 
	                // post this instance again
	                dialer.post(this);
	            }
	        }
	    }
		
		
		/**
		 * @return The angle of the unit circle with the image view's center
		 */
		private double getAngle(double xTouch, double yTouch) {
		    double x = xTouch - (dialerWidth / 2d);
		    double y = dialerHeight - yTouch - (dialerHeight / 2d);
		 
		    switch (getQuadrant(x, y)) {
		        case 1:
		            return Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
		        case 2:
		            return 180 - Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
		        case 3:
		            return 180 + (-1 * Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI);
		        case 4:
		            return 360 + Math.asin(y / Math.hypot(x, y)) * 180 / Math.PI;
		        default:
		            return 0;
		    }
		}
		 
		/**
		 * @return The selected quadrant.
		 */
		private static int getQuadrant(double x, double y) {
		    if (x >= 0) {
		        return y >= 0 ? 1 : 4;
		    } else {
		        return y >= 0 ? 2 : 3;
		    }
		}
		
		
		
		/**
	     * Rotate the dialer.
	     *
	     * @param degrees The degrees, the dialer should get rotated.
	     */
	    private void rotateDialer(float degrees) {
	        matrix.postRotate(degrees, dialerWidth / 2, dialerHeight / 2);
	         
	        dialer.setImageMatrix(matrix);
	    }
	    
	    
	    
	   
	}



	

