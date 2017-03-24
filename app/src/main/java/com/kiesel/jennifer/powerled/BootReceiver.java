package com.kiesel.jennifer.powerled;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Jenny on 31.12.2014.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, powerLEDService.class);
        context.startService(serviceIntent);
//        Intent activity = new Intent(context, MainActivity.class);
//        activity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(activity);
        context.unregisterReceiver(this);
    }
}
