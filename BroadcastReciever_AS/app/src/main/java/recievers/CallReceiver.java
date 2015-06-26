package recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

/**
 * Broadcast Receiver For New Incomming Phone Call
 * Created by <b>Harshal Benake</b>
 */
public class CallReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Toast.makeText(context, "RecieveCall", Toast.LENGTH_SHORT).show();

            // TELEPHONY MANAGER class object to register one listner
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            //Create Listner
            MyPhoneStateListener PhoneListener = new MyPhoneStateListener();
            // Register listener for LISTEN_CALL_STATE
            telephonyManager.listen(PhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        } catch (Exception e) {
            Log.e("Phone Receive Error", " " + e);
        }
    }

    /**
     * MyPhoneListener for New Phone Call Event. Incomming Number
     */
    private class MyPhoneStateListener extends PhoneStateListener {

        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == 1) {
                String msg = "New Phone Call Event. Incomming Number : "+incomingNumber;
                System.out.println("msg: "+msg);
            }
        }
    }
}
