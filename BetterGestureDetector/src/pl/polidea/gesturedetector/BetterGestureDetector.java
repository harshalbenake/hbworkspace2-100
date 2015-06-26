package pl.polidea.gesturedetector;

import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;

// TODO: Auto-generated Javadoc
/**
 * The Class BetterGestureDetector.
 */
public class BetterGestureDetector {

    /**
     * The listener interface for receiving betterGesture events. The class that
     * is interested in processing a betterGesture event implements this
     * interface, and the object created with that class is registered with a
     * component using the component's
     * <code>addBetterGestureListener<code> method. When
     * the betterGesture event occurs, that object's appropriate
     * method is invoked.
     * 
     * @see BetterGestureEvent
     */
    public interface BetterGestureListener {

        /**
         * On show press.
         * 
         * @param motionEvent
         *            the motion event
         */
        void onPress(MotionEvent motionEvent);

        /**
         * On single tap up.
         * 
         * @param motionEvent
         *            the motion event
         */
        void onTap(MotionEvent motionEvent);

        /**
         * On drag.
         * 
         * @param motionEvent
         *            the motion event
         */
        void onDrag(MotionEvent motionEvent);

        /**
         * On move.
         * 
         * @param motionEvent
         *            the motion event
         */
        void onMove(MotionEvent motionEvent);

        /**
         * On release.
         * 
         * @param motionEvent
         *            the motion event
         */
        void onRelease(MotionEvent motionEvent);

        /**
         * On long press.
         * 
         * @param motionEvent
         *            the motion event
         */
        void onLongPress(MotionEvent motionEvent);

        /**
         * On double tap.
         * 
         * @param motionEvent
         *            the motion event
         * @param clicks
         */
        void onMultiTap(MotionEvent motionEvent, int clicks);
    }

    /**
     * The Class BetterGestureAdapter.
     */
    class BetterGestureAdapter implements BetterGestureListener {

        /*
         * (non-Javadoc)
         * 
         * @see
         * pl.polidea.test.BetterGestureDetector.BetterGestureListener#onShowPress
         * ()
         */
        @Override
        public void onPress(MotionEvent motionEvent) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * pl.polidea.test.BetterGestureDetector.BetterGestureListener#onSingleTapUp
         * ()
         */
        @Override
        public void onTap(MotionEvent motionEvent) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * pl.polidea.test.BetterGestureDetector.BetterGestureListener#onDrag()
         */
        @Override
        public void onDrag(MotionEvent motionEvent) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * pl.polidea.test.BetterGestureDetector.BetterGestureListener#onMove()
         */
        @Override
        public void onMove(MotionEvent motionEvent) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * pl.polidea.test.BetterGestureDetector.BetterGestureListener#onRelease
         * ()
         */
        @Override
        public void onRelease(MotionEvent motionEvent) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * pl.polidea.gesturedetector.BetterGestureDetector.BetterGestureListener
         * #onLongPress(android.view.MotionEvent)
         */
        @Override
        public void onLongPress(MotionEvent motionEvent) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onMultiTap(MotionEvent motionEvent, int clicks) {
            // TODO Auto-generated method stub

        }

    }

    /** The Constant DEFAULT_PRESS_TIMEOUT. */
    private static final int DEFAULT_PRESS_TIMEOUT = 100;

    /** The Constant DEFAULT_TAP_TIMEOUT. */
    private static final int DEFAULT_TAP_TIMEOUT = 300;

    /** The Constant DEFAULT_MOVE_EPSILON. */
    private static final int DEFAULT_MOVE_EPSILON = 4;

    /** The press timeout. */
    private int pressTimeout = DEFAULT_PRESS_TIMEOUT;

    /** The tap timeout. */
    private int tapTimeout = DEFAULT_TAP_TIMEOUT;

    /** The move epsilon. */
    private int moveEpsilon = DEFAULT_MOVE_EPSILON;

    /** The handler. */
    private Handler handler;

    /** The tap handler. */
    Runnable tapHandler;

    /** The press handler. */
    Runnable pressHandler;

    /** The listener. */
    private final BetterGestureListener listener;

    /** The prev touch time. */
    private long prevTouchTime;

    /** The prev touch y. */
    private float prevTouchY;

    /** The prev touch x. */
    private float prevTouchX;

    /** The vertical move cancel. */
    boolean verticalMoveCancel = true;

    /** The horizontal move cancel. */
    boolean horizontalMoveCancel = true;
    /** The dragging. */
    private boolean dragging;

    /** The moving. */
    private boolean moving;

    /** The long press handler. */
    private Runnable longPressHandler;

    /** The long press timeout. */
    private long longPressTimeout = 500;

    private int clicks = 0;

    /**
     * Instantiates a new better gesture detector.
     * 
     * @param listener
     *            the listener
     */
    public BetterGestureDetector(BetterGestureListener listener) {
        if (listener == null) {
            throw new NullPointerException("Listener cannot be null");
        }
        this.listener = listener;
        handler = new Handler();
    }

    /**
     * On touch event.
     * 
     * @param motionEvent
     *            the motion event
     */
    public void onTouchEvent(final MotionEvent motionEvent) {
        float dx = motionEvent.getX() - prevTouchX;
        float dy = motionEvent.getY() - prevTouchY;
        long dt = System.currentTimeMillis() - prevTouchTime;
        handler.removeCallbacks(longPressHandler);
        longPressHandler = null;

        switch (motionEvent.getAction()) {
        case MotionEvent.ACTION_DOWN:
            dragging = false;
            moving = false;
            if (tapHandler != null) {
                clicks++;
                Log.e("clicks", "" + clicks);
                handler.removeCallbacks(tapHandler);
                tapHandler = null;
            }

            handler.removeCallbacks(pressHandler);
            pressHandler = new Runnable() {

                @Override
                public void run() {
                    // handler.removeCallbacks(tapHandler);
                    // tapHandler = null;
                    onPress(motionEvent);
                }

            };
            handler.postDelayed(pressHandler, pressTimeout);

            longPressHandler = new Runnable() {

                @Override
                public void run() {
                    onLongPress(motionEvent);
                }
            };
            handler.postDelayed(longPressHandler, getLongPressTimeout());
            prevTouchTime += dt;
            break;
        case MotionEvent.ACTION_UP:
            if (dt > tapTimeout || moving || dragging) {
                onRelease(motionEvent);
                break;
            }

            tapHandler = new Runnable() {

                @Override
                public void run() {
                    handler.removeCallbacks(longPressHandler);
                    longPressHandler = null;
                    onTap(motionEvent, clicks);
                }
            };
            handler.postDelayed(tapHandler, tapTimeout - dt);

            prevTouchTime += dt;
            break;

        case MotionEvent.ACTION_MOVE:
            if (Math.abs(dx) > moveEpsilon || Math.abs(dy) > moveEpsilon || dragging || moving) {
                if (pressHandler == null && !moving) {
                    dragging = true;
                }
                handler.removeCallbacks(tapHandler);
                tapHandler = null;
                handler.removeCallbacks(pressHandler);
                pressHandler = null;
                handler.removeCallbacks(longPressHandler);
                longPressHandler = null;
                moving = true;
                if (dragging) {
                    onDrag(motionEvent);
                } else {
                    onMove(motionEvent);
                }
            }
            break;
        case MotionEvent.ACTION_CANCEL:
            handler.removeCallbacks(pressHandler);
            pressHandler = null;
            handler.removeCallbacks(tapHandler);
            tapHandler = null;
            handler.removeCallbacks(longPressHandler);
            longPressHandler = null;
            onRelease(motionEvent);
        }
        prevTouchX = motionEvent.getX();
        prevTouchY = motionEvent.getY();
    }

    /**
     * On release.
     * 
     * @param motionEvent
     *            the motion event
     */
    private void onRelease(MotionEvent motionEvent) {
        Log.e("gesture", "release");
        clicks = 0;
        listener.onRelease(motionEvent);
    }

    /**
     * On drag.
     * 
     * @param motionEvent
     *            the motion event
     */
    private void onDrag(MotionEvent motionEvent) {
        Log.e("gesture", "drag");
        clicks = 0;
        listener.onDrag(motionEvent);
    }

    /**
     * On move.
     * 
     * @param motionEvent
     *            the motion event
     */
    private void onMove(MotionEvent motionEvent) {
        Log.e("gesture", "move");
        clicks = 0;
        listener.onMove(motionEvent);
    }

    /**
     * On single tap up.
     * 
     * @param motionEvent
     *            the motion event
     */
    protected void onTap(MotionEvent motionEvent, int clicks) {
        tapHandler = null;
        if (clicks == 0) {
            Log.e("gesture", "tap");
            listener.onTap(motionEvent);
        } else {
            Log.e("gesture", "multitap");
            listener.onMultiTap(motionEvent, clicks + 1);
        }
        this.clicks = 0;
    }

    /**
     * On long press.
     * 
     * @param motionEvent
     *            the motion event
     */
    protected void onLongPress(MotionEvent motionEvent) {
        Log.e("gesture", "longpress");
        clicks = 0;
        longPressHandler = null;
        listener.onLongPress(motionEvent);
    }

    /**
     * On show press.
     * 
     * @param motionEvent
     *            the motion event
     */
    protected void onPress(MotionEvent motionEvent) {
        Log.e("gesture", "press");
        pressHandler = null;
        listener.onPress(motionEvent);
    }

    /**
     * Gets the press timeout.
     * 
     * @return the press timeout
     */
    public int getPressTimeout() {
        return pressTimeout;
    }

    /**
     * Sets the press timeout.
     * 
     * @param pressTimeout
     *            the new press timeout
     */
    public void setPressTimeout(int pressTimeout) {
        this.pressTimeout = pressTimeout;
    }

    /**
     * Gets the tap timeout.
     * 
     * @return the tap timeout
     */
    public int getTapTimeout() {
        return tapTimeout;
    }

    /**
     * Sets the tap timeout.
     * 
     * @param tapTimeout
     *            the new tap timeout
     */
    public void setTapTimeout(int tapTimeout) {
        this.tapTimeout = tapTimeout;
    }

    /**
     * Gets the move epsilon.
     * 
     * @return the move epsilon
     */
    public int getMoveEpsilon() {
        return moveEpsilon;
    }

    /**
     * Sets the move epsilon.
     * 
     * @param moveEpsilon
     *            the new move epsilon
     */
    public void setMoveEpsilon(int moveEpsilon) {
        this.moveEpsilon = moveEpsilon;
    }

    /**
     * Gets the long press timeout.
     * 
     * @return the long press timeout
     */
    public long getLongPressTimeout() {
        return longPressTimeout;
    }

    /**
     * Sets the long press timeout.
     * 
     * @param longPressTimeout
     *            the new long press timeout
     */
    public void setLongPressTimeout(long longPressTimeout) {
        this.longPressTimeout = longPressTimeout;
    }
}
