package pl.polidea.gesturedetector;

import pl.polidea.gesturedetector.BetterGestureDetector.BetterGestureListener;
import pl.polidea.test.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.TextView;

public class BetterGestureDetectorActivity extends Activity implements BetterGestureListener {
    private BetterGestureDetector gestureDetector;
    private TextView v;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        v = (TextView) findViewById(R.id.textView1);
        gestureDetector = new BetterGestureDetector(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onPress(MotionEvent motionEvent) {
        v.setText("press");
        v.setBackgroundColor(0xffff0000);
        v.invalidate();
    }

    @Override
    public void onTap(MotionEvent motionEvent) {
        v.setText("tap");
        v.setBackgroundColor(0xff00ff00);
        v.invalidate();
    }

    @Override
    public void onDrag(MotionEvent motionEvent) {
        v.setText("drag");
        v.setBackgroundColor(0xff0000ff);
        v.invalidate();
    }

    @Override
    public void onMove(MotionEvent motionEvent) {
        v.setText("move");
        v.setBackgroundColor(0xffff00ff);
        v.invalidate();
    }

    @Override
    public void onRelease(MotionEvent motionEvent) {
        v.setText("release");
        v.setBackgroundColor(0xffffff00);
        v.invalidate();
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {
        v.setText("longpress");
        v.setBackgroundColor(0xff00ffff);
        v.invalidate();
    }

    @Override
    public void onMultiTap(MotionEvent motionEvent, int clicks) {
        v.setText("multitap [" + clicks + "]");
        v.setBackgroundColor(0xff7f7f7f);
        v.invalidate();
    }
}