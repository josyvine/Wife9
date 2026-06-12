package com.wife.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class ReconnectManager {
    private static final String TAG = "ReconnectManager";
    private static volatile ReconnectManager instance;

    private final Context context;
    private final Handler handler;
    private boolean isReconnecting = false;

    public static ReconnectManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ReconnectManager.class) {
                if (instance == null) {
                    instance = new ReconnectManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ReconnectManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public synchronized void triggerReconnect() {
        if (isReconnecting) return;
        isReconnecting = true;
        Log.d(TAG, "Starting auto-reconnection sequence...");

        // Try peer discovery every 10 seconds until connected
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (ConnectionManager.getInstance(context).isConnected()) {
                    isReconnecting = false;
                    Log.d(TAG, "Device re-connected. Halting auto-reconnect sweeps.");
                    return;
                }

                Log.d(TAG, "Initiating WifiDirect discovery sweep for reconnection...");
                WiFiDirectManager.getInstance(context).discoverPeers();

                // Repeat in 10 seconds
                handler.postDelayed(this, 10000);
            }
        });
    }
}
