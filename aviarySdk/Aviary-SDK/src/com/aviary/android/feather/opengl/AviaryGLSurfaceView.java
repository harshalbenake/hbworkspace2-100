package com.aviary.android.feather.opengl;

import java.lang.ref.WeakReference;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;

import com.aviary.android.feather.common.log.LoggerFactory;
import com.aviary.android.feather.common.log.LoggerFactory.Logger;
import com.aviary.android.feather.common.log.LoggerFactory.LoggerType;
import com.aviary.android.feather.common.threading.Future;
import com.aviary.android.feather.common.threading.FutureListener;
import com.aviary.android.feather.common.threading.ThreadPool.Job;
import com.aviary.android.feather.common.threading.ThreadPool.Worker;

public class AviaryGLSurfaceView extends GLSurfaceView {

	private static final Logger logger = LoggerFactory.getLogger( "gl-surface", LoggerType.ConsoleLoggerType );

	private static final Handler mUIHandler = new Handler();

	public AviaryGLSurfaceView ( Context context ) {
		super( context );
		mPtr = init( context, null );
	}

	public AviaryGLSurfaceView ( Context context, AttributeSet attrs ) {
		super( context, attrs );
		mPtr = init( context, attrs );
	}

	private long init( Context context, AttributeSet attrs ) {
		this.setEGLContextClientVersion( 2 );
		this.setEGLConfigChooser( 8, 8, 8, 8, 0, 0 );
		this.setRenderer( new AviaryGLRenderer() );
		this.setRenderMode( RENDERMODE_WHEN_DIRTY );
		return nativeCreate();
	}

	@Override
	protected void onDetachedFromWindow() {
		logger.info( "onDetachedfromWindow" );
		nativeDispose();
		super.onDetachedFromWindow();
	}

	private void initializeOpenGL() {
		InitializeOpenGLJob job = new InitializeOpenGLJob();
		FutureListener<Void> listener = new FutureListener<Void>() {

			@Override
			public void onFutureDone( Future<Void> arg0 ) {
				fireOnSurfaceCreated();
			}
		};
		submit( job, listener );
	}

	private void setRenderbufferSize( final boolean changed, final int width, final int height ) {
		SetRenderbufferSizeJob job = new SetRenderbufferSizeJob();
		FutureListener<Void> listener = new FutureListener<Void>() {

			@Override
			public void onFutureDone( Future<Void> arg0 ) {
				fireOnSurfaceChanged( changed, width, height );
			}
		};
		submit( job, listener, width, height );
	}

	public Future<Boolean> executeEffect( String effectName, Bitmap input, boolean use_gpu, FutureListener<Boolean> listener ) {
		RenderJob job = new RenderJob( input );
		return submit( job, listener, effectName );
	}

	public Future<Boolean> writeBitmap( Bitmap output, FutureListener<Boolean> listener ) {
		WriteBitmapJob job = new WriteBitmapJob( output );
		return submit( job, listener );
	}

	/**
	 * Returns the native pointer to the MoaGLSurfaceView
	 * 
	 * @return
	 */
	private long nativeCreate() {
		logger.log( "nativeCreate" );
		synchronized ( mNativeLock ) {
			return n_create();
		}
	}

	/**
	 * Free the native instance
	 */
	private void nativeDispose() {
		logger.log( "nativeDispose" );
		synchronized ( mNativeLock ) {
			n_dispose( mPtr );
		}
	}

	/**
	 * Initialize the GL data in the native instance
	 * 
	 * @return
	 */
	private boolean nativeInitialize() {
		logger.log( "nativeInitialize" );
		synchronized ( mNativeLock ) {
			return n_initialize( mPtr );
		}
	}

	/**
	 * Change the render buffer size in the native instance
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	private boolean nativeSetRenderbufferSize( int width, int height ) {
		logger.log( "nativeSetRenderBufferSize" );
		synchronized ( mNativeLock ) {
			return n_setRenderbufferSize( mPtr, width, height );
		}
	}

	private boolean nativeRender( Bitmap input, String effect ) {
		logger.log( "nativeRender" );
		synchronized ( mNativeLock ) {
			return n_render( mPtr, input, effect );
		}
	}

	private boolean nativeWriteBitmap( Bitmap output ) {
		logger.log( "nativeWriteBitmap" );
		synchronized ( mNativeLock ) {
			return n_writeCurrentBitmap( mPtr, output );
		}
	}

	public <I, O> Future<O> submit( final Job<I, O> job, FutureListener<O> listener, final I... params ) {
		throw new RuntimeException( "Not Implemented" );
		
//		Worker<I, O> w = new Worker<I, O>( job, listener, params ) {
//
//			@Override
//			public void run() {
//				O result = null;
//
//				if ( setMode( ThreadMediaPool.MODE_CPU ) ) {
//					try {
//						result = job.run( this, params );
//					} catch ( Throwable ex ) {
//						Log.e( Worker.TAG, "Exception in running a job", ex );
//					}
//				}
//
//				synchronized ( this ) {
//					setMode( ThreadMediaPool.MODE_NONE );
//					setResult( result );
//					setIsDone();
//					notifyAll();
//				}
//
//				fireOnDoneEvent();
//			}
//		};
//		queueEvent( w );
//		return w;
	}

	private final Object mNativeLock = new Object();
	private final long mPtr;

	private static native long n_create();

	private static native boolean n_dispose( long ptr );

	private static native boolean n_initialize( long ptr );

	private static native boolean n_setRenderbufferSize( long ptr, int width, int heights );

	private static native boolean n_render( long ptr, Bitmap input, String effect );

	private static native boolean n_writeCurrentBitmap( long ptr, Bitmap output );

	class RenderJob implements Job<String, Boolean> {

		WeakReference<Bitmap> mBitmap;

		public RenderJob ( Bitmap bitmap ) {
			mBitmap = new WeakReference<Bitmap>( bitmap );
		}

		@Override
		public Boolean run( Worker<String, Boolean> worker, String... params ) throws Exception {
			logger.log( "RenderJob::run" );
			if ( null != mBitmap && null != mBitmap.get() ) {
				return nativeRender( mBitmap.get(), params[0] );
			}
			return false;
		}
	}

	class WriteBitmapJob implements Job<Void, Boolean> {

		WeakReference<Bitmap> mBitmap;

		public WriteBitmapJob ( Bitmap bitmap ) {
			mBitmap = new WeakReference<Bitmap>( bitmap );
		}

		@Override
		public Boolean run( Worker<Void, Boolean> worker, Void... params ) throws Exception {
			if ( null != mBitmap && null != mBitmap.get() ) {
				return nativeWriteBitmap( mBitmap.get() );
			}
			return false;
		}

	}

	class InitializeOpenGLJob implements Job<Void, Void> {
		
		@Override
		public Void run( Worker<Void, Void> worker, Void... params ) throws Exception {
			logger.log( "InitializeOpenGlJob::run" );
			nativeInitialize();
			logger.log( "end::nativeInitialize" );
			return null;
		}
	}

	class SetRenderbufferSizeJob implements Job<Integer, Void> {

		public SetRenderbufferSizeJob () {}

		@Override
		public Void run( Worker<Integer, Void> worker, Integer... params ) throws Exception {
			logger.log( "SetRenderbufferSizeJob::run" );
			nativeSetRenderbufferSize( params[0], params[1] );
			return null;
		}
	}

	private class AviaryGLRenderer implements GLSurfaceView.Renderer {

		private int mWidth, mHeight;

		@Override
		public void onDrawFrame( GL10 gl ) {
			logger.log( "onDrawFrame" );
		}

		@Override
		public void onSurfaceChanged( GL10 gl, int width, int height ) {
			logger.log( "onSurfaceChanged. " + width + "x" + height );

			final boolean changed = mWidth != width || mHeight != height;
			mWidth = width;
			mHeight = height;

			setRenderbufferSize( changed, width, height );
		}

		@Override
		public void onSurfaceCreated( GL10 gl, EGLConfig config ) {

			Log.d( "GL", "GL_RENDERER = " + gl.glGetString( GL10.GL_RENDERER ) );
			Log.d( "GL", "GL_VENDOR = " + gl.glGetString( GL10.GL_VENDOR ) );
			Log.d( "GL", "GL_VERSION = " + gl.glGetString( GL10.GL_VERSION ) );
			Log.i( "GL", "GL_EXTENSIONS = " + gl.glGetString( GL10.GL_EXTENSIONS ) );

			logger.log( "onSurfaceCreated" );
			initializeOpenGL();
		}
	}

	private GLRendererListener mGlRendererListener;

	public void setOnGlRendererListener( GLRendererListener listener ) {
		mGlRendererListener = listener;
	}

	private void fireOnSurfaceCreated() {
		if ( mGlRendererListener != null ) {
			mGlRendererListener.OnSurfaceCreated();
		}
	}

	private void fireOnSurfaceChanged( final boolean changed, final int width, final int height ) {
		if ( mGlRendererListener != null ) {
			getHandler().post( new Runnable() {

				@Override
				public void run() {
					mGlRendererListener.OnSurfaceChanged( changed, width, height );
				}
			} );
		}
	}

	public static interface GLRendererListener {

		public void OnSurfaceCreated();

		public void OnSurfaceChanged( boolean changed, int width, int height );
	}
}
