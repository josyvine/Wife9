package com.wife.app;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "WiFiDirectReceiver";

    private final WifiP2pManager manager;
    private final WifiP2pManager.Channel channel;
    private final WiFiDirectManager wifiDirectManager;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, WiFiDirectManager wifiDirectManager) {
        this.manager = manager;
        this.channel = channel;
        this.wifiDirectManager = wifiDirectManager;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive ACTION: " + action);

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.d(TAG, "WiFi P2P state is enabled");
            } else {
                Log.d(TAG, "WiFi P2P state is disabled");
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            if (manager != null) {
                manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        Log.d(TAG, "Peers discovered: " + peers.getDeviceList().size());
                        wifiDirectManager.updatePeers(peers);
                    }
                });
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            if (manager == null) return;

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.isConnected()) {
                manager.requestConnectionInfo(channel, new WifiP2pManager.ConnectionInfoListener() {
                    @Override
                    public void onConnectionInfoAvailable(WifiP2pInfo info) {
                        Log.d(TAG, "Connection info available: Group formed=" + info.groupFormed + ", Is owner=" + info.isGroupOwner);
                        wifiDirectManager.updateConnectionInfo(info);
                        
                        // Notify standard local listeners about connection change
                        Intent connectionIntent = new Intent(Constants.ACTION_CONNECTION_CHANGED);
                        context.sendBroadcast(connectionIntent);
                    }
                });
            } else {
                Log.d(TAG, "Disconnected or connection lost.");
                wifiDirectManager.updateConnectionInfo(null);
                Intent connectionIntent = new Intent(Constants.ACTION_CONNECTION_CHANGED);
                context.sendBroadcast(connectionIntent);
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            Log.d(TAG, "This device configuration changed.");
        }
    }
}
