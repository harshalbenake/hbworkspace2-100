package recievers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


/**
 * This class is used to receive device boot event.
 * Created by <b>Harshal Benake</b> on 28/2/15.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        Toast.makeText(context, "BootReceiver", Toast.LENGTH_SHORT).show();
        System.out.println("BootReceiver");
    }

}
