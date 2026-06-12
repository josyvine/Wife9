package com.wife.app;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CallSignalingManager {
    private static final String TAG = "CallSignalingManager";
    private static volatile CallSignalingManager instance;

    private final Context context;
    private final ExecutorService executorService;
    private final List<SignalingEventListener> listeners = new ArrayList<>();

    public interface SignalingEventListener {
        void onSignalReceived(String action, String peerIp, JsonObject payload);
    }

    public static CallSignalingManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CallSignalingManager.class) {
                if (instance == null) {
                    instance = new CallSignalingManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private CallSignalingManager(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public synchronized void registerListener(SignalingEventListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void unregisterListener(SignalingEventListener listener) {
        listeners.remove(listener);
    }

    public void sendSignal(final String peerIp, final String type) {
        executorService.execute(() -> {
            try (Socket socket = new Socket(peerIp, Constants.OFF_PORT_CONTROL);
                 OutputStream os = socket.getOutputStream();
                 PrintWriter pw = new PrintWriter(os, true)) {

                JsonObject json = new JsonObject();
                json.addProperty("type", type);
                json.addProperty("sender", Utils.getDeviceId(context));
                json.addProperty("senderName", Utils.getDeviceModel());

                pw.println(json.toString());
                Log.d(TAG, "Sent signal: " + type + " to IP: " + peerIp);

            } catch (Exception e) {
                Log.e(TAG, "Failed sending signaling packet: " + e.getMessage());
            }
        });
    }

    public void handleReceivedSignal(String action, JsonObject payload, String peerIp) {
        Log.d(TAG, "Handling received signal: " + action + " from peer " + peerIp);
        
        // Notify any active call screen UI
        synchronized (this) {
            if (!listeners.isEmpty()) {
                for (SignalingEventListener listener : listeners) {
                    listener.onSignalReceived(action, peerIp, payload);
                }
                return;
            }
        }

        // Standard actions if no listener is currently registered (e.g., launching call UI when background/idle)
        if (Constants.SIGNAL_CALL_REQUEST.equals(action)) {
            Intent callIntent = new Intent(context, VoiceCallActivity.class);
            callIntent.putExtra(Constants.EXTRA_PEER_IP, peerIp);
            callIntent.putExtra(Constants.EXTRA_PEER_NAME, payload.get("senderName").getAsString());
            callIntent.putExtra("IS_INBOUND", true);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(callIntent);
            
        } else if (Constants.SIGNAL_VIDEO_REQUEST.equals(action)) {
            Intent videoIntent = new Intent(context, VideoCallActivity.class);
            videoIntent.putExtra(Constants.EXTRA_PEER_IP, peerIp);
            videoIntent.putExtra(Constants.EXTRA_PEER_NAME, payload.get("senderName").getAsString());
            videoIntent.putExtra("IS_INBOUND", true);
            videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(videoIntent);
        }
    }
}
