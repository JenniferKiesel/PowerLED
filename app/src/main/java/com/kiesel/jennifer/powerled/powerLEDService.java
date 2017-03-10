package com.kiesel.jennifer.powerled;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.BatteryManager;
import android.os.IBinder;
import android.util.Log;
import android.view.Display;

public class powerLEDService extends Service {

    private final String TAG = powerLEDService.class.getSimpleName();
    private final int POWERLED_NOTIFICATION = 1;
    private final int COLOR_ORANGE = 0xFFFF5500;

    private NotificationManager notificationManager;
    private BroadcastReceiver powerConnectedReceiver;
    private BroadcastReceiver powerDisconnectedReceiver;
    private BroadcastReceiver powerChangedReceiver;
    private BroadcastReceiver screenOffReceiver;
    private boolean ledIsOrange;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        // TODO: ggf. gleich zu anfang prüfen ob connected?

        // register connect receiver
        powerConnectedReceiver = new PowerConnectedReceiver();
        registerReceiver(powerConnectedReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // unregister receivers
        try {
            unregisterReceiver(powerConnectedReceiver);
            unregisterReceiver(powerDisconnectedReceiver);
            unregisterReceiver(powerChangedReceiver);
            unregisterReceiver(screenOffReceiver);
        } catch (Exception e) {}

        super.onDestroy();
    }

    /* POWER ON */
    private class PowerConnectedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Connected", "onReceive\n");

            // register screen off receiver (led blinks only when screen is off)
            screenOffReceiver = new ScreenOffReceiver();
            registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));

            // register power changed receiver
            powerChangedReceiver = new PowerChangedReceiver();
            registerReceiver(powerChangedReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

            // register disconnect receiver
            powerDisconnectedReceiver = new PowerDisconnectedReceiver();
            registerReceiver(powerDisconnectedReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));

            // unregister myself
            unregisterReceiver(this);
        }
    }

    /* POWER OFF */
    private class PowerDisconnectedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("Disconnected", "onReceive");

            // unregister power changed receiver
            unregisterReceiver(powerChangedReceiver);

            // unregister screen off receiver
            unregisterReceiver(screenOffReceiver);

            notificationManager.cancel(POWERLED_NOTIFICATION);

            // register connect receiver
            powerConnectedReceiver = new PowerConnectedReceiver();
            registerReceiver(powerConnectedReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));

            // unregister myself
            unregisterReceiver(this);
        }
    }

    /* LOADING */
    private class PowerChangedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            boolean full = status == BatteryManager.BATTERY_STATUS_FULL;

            // wenn voll (wird nur einmal geschickt)
            if (full) {
                makeNotification(false);
            }
            // wenn leerer geworden (nur bei Umstieg von grün auf orange notwendig)
            else if (!full && !ledIsOrange) {
                makeNotification(true);
            }
        }
    }

    /* SCREEN OFF */
    private class ScreenOffReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // make notification
            if (batteryFull()) {
                makeNotification(false);
            } else {
                makeNotification(true);
            }
        }
    }

    private boolean batteryFull() {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent intent = registerReceiver(null, iFilter);
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        return status == BatteryManager.BATTERY_STATUS_FULL;
    }

    private void makeNotification(boolean orange) {
        // build notification
        Notification.Builder nBuilder = new Notification.Builder(getApplicationContext());
        nBuilder.setContentTitle("powerLED");
        nBuilder.setContentText("powerLED service running");
        nBuilder.setSmallIcon(R.drawable.test_icon);
        nBuilder.setOngoing(true);

        // color choose
        if (orange) {
            nBuilder.setLights(COLOR_ORANGE, 7000, 500);
            ledIsOrange = true;
        } else {
            nBuilder.setLights(Color.GREEN, 60000, 500);
            ledIsOrange = false;
        }

        // fire notification
        notificationManager.notify(POWERLED_NOTIFICATION, nBuilder.build());
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
