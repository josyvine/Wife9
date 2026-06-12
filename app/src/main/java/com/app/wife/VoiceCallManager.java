package com.wife.app;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceCallManager {
    private static final String TAG = "VoiceCallManager";
    private static volatile VoiceCallManager instance;

    private final Context context;
    private final AudioCaptureManager captureManager;
    private final AudioPlaybackManager playbackManager;
    private final ExecutorService executorService;

    private ServerSocket serverSocket;
    private Socket activeSocket;
    private boolean isCallActive = false;

    public static VoiceCallManager getInstance(Context context) {
        if (instance == null) {
            synchronized (VoiceCallManager.class) {
                if (instance == null) {
                    instance = new VoiceCallManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private VoiceCallManager(Context context) {
        this.context = context;
        this.captureManager = new AudioCaptureManager(context);
        this.playbackManager = new AudioPlaybackManager(context);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public synchronized void startCall(final String peerIp) {
        if (isCallActive) return;
        isCallActive = true;

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Starting VoiceCall connection. Connecting to: " + peerIp);
                // Connect outbound to caller's receiver port
                activeSocket = new Socket(peerIp, Constants.OFF_PORT_VOICE);
                
                // Initialize hardware feedback
                playbackManager.setSpeakerphoneOn(true);
                
                // Begin bidirectional PCM streaming
                captureManager.startCapture(activeSocket.getOutputStream());
                playbackManager.startPlayback(activeSocket.getInputStream());
                
                Log.d(TAG, "Outgoing VoiceCall pipeline established.");
            } catch (Exception e) {
                Log.e(TAG, "Failed establishing VoiceCall: " + e.getMessage());
                teardown();
            }
        });
    }

    public synchronized void listenForIncomingCall() {
        if (isCallActive) return;
        isCallActive = true;

        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(Constants.OFF_PORT_VOICE);
                Log.d(TAG, "Listening for incoming VoiceCall on port " + Constants.OFF_PORT_VOICE);
                
                activeSocket = serverSocket.accept();
                Log.d(TAG, "VoiceCall connection accepted from " + activeSocket.getInetAddress().getHostAddress());

                playbackManager.setSpeakerphoneOn(true);
                
                captureManager.startCapture(activeSocket.getOutputStream());
                playbackManager.startPlayback(activeSocket.getInputStream());
                
                Log.d(TAG, "Inbound VoiceCall pipelines loaded.");
            } catch (Exception e) {
                Log.e(TAG, "Inbound VoiceCall server failed: " + e.getMessage());
                teardown();
            }
        });
    }

    public synchronized void muteMicrophone(boolean mute) {
        if (mute) {
            captureManager.stopCapture();
        } else if (activeSocket != null && activeSocket.isConnected()) {
            try {
                captureManager.startCapture(activeSocket.getOutputStream());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public void setSpeakerphoneEnabled(boolean enabled) {
        playbackManager.setSpeakerphoneOn(enabled);
    }

    public synchronized void endCall() {
        teardown();
    }

    private synchronized void teardown() {
        isCallActive = false;
        captureManager.stopCapture();
        playbackManager.stopPlayback();

        if (activeSocket != null) {
            try { activeSocket.close(); } catch (IOException e) { e.printStackTrace(); }
            activeSocket = null;
        }

        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException e) { e.printStackTrace(); }
            serverSocket = null;
        }
        Log.d(TAG, "VoiceCall pipelines terminated.");
    }
}
