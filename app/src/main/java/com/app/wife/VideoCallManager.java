package com.wife.app;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoCallManager {
    private static final String TAG = "VideoCallManager";
    private static volatile VideoCallManager instance;

    private final Context context;
    private final VideoCaptureManager captureManager;
    private final VideoDecoderManager decoderManager;
    private final ExecutorService executorService;

    private ServerSocket serverSocket;
    private Socket activeSocket;
    private boolean isCallActive = false;

    public static VideoCallManager getInstance(Context context) {
        if (instance == null) {
            synchronized (VideoCallManager.class) {
                if (instance == null) {
                    instance = new VideoCallManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private VideoCallManager(Context context) {
        this.context = context;
        this.captureManager = new VideoCaptureManager(context);
        this.decoderManager = new VideoDecoderManager();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public synchronized void startCall(final LifecycleOwner lifecycleOwner, final String peerIp, final VideoDecoderManager.VideoFrameListener frameListener) {
        if (isCallActive) return;
        isCallActive = true;

        executorService.execute(() -> {
            try {
                Log.d(TAG, "Connecting outbound Video Socket to: " + peerIp);
                activeSocket = new Socket(peerIp, Constants.OFF_PORT_VIDEO);
                
                // Start capture and decoding
                captureManager.startCapture(lifecycleOwner, activeSocket.getOutputStream());
                decoderManager.startDecoding(activeSocket.getInputStream(), frameListener);
                
                Log.d(TAG, "Outbound video calling pipeline active.");
            } catch (Exception e) {
                Log.e(TAG, "Outbound video calling socket failed: " + e.getMessage());
                teardown();
            }
        });
    }

    public synchronized void listenForIncomingCall(final LifecycleOwner lifecycleOwner, final VideoDecoderManager.VideoFrameListener frameListener) {
        if (isCallActive) return;
        isCallActive = true;

        executorService.execute(() -> {
            try {
                serverSocket = new ServerSocket(Constants.OFF_PORT_VIDEO);
                Log.d(TAG, "Listening for incoming Video Call on port " + Constants.OFF_PORT_VIDEO);
                
                activeSocket = serverSocket.accept();
                Log.d(TAG, "Inbound video call socket accepted.");

                captureManager.startCapture(lifecycleOwner, activeSocket.getOutputStream());
                decoderManager.startDecoding(activeSocket.getInputStream(), frameListener);

                Log.d(TAG, "Inbound video calling pipeline active.");
            } catch (Exception e) {
                Log.e(TAG, "Inbound video calling server failed: " + e.getMessage());
                teardown();
            }
        });
    }

    public synchronized void switchCamera(LifecycleOwner lifecycleOwner) {
        if (isCallActive && activeSocket != null) {
            try {
                captureManager.switchCamera(lifecycleOwner, activeSocket.getOutputStream());
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    public synchronized void muteCamera(boolean mute) {
        if (mute) {
            captureManager.stopCapture();
        } else if (activeSocket != null && activeSocket.isConnected()) {
            // Restart capture (this would typically need a LifecycleOwner. 
            // We can delegate this camera on/off to our UI activity easily.)
        }
    }

    public synchronized void endCall() {
        teardown();
    }

    private synchronized void teardown() {
        isCallActive = false;
        captureManager.stopCapture();
        decoderManager.stopDecoding();

        if (activeSocket != null) {
            try { activeSocket.close(); } catch (IOException e) { e.printStackTrace(); }
            activeSocket = null;
        }

        if (serverSocket != null) {
            try { serverSocket.close(); } catch (IOException e) { e.printStackTrace(); }
            serverSocket = null;
        }
        Log.d(TAG, "VideoCall pipelines terminated.");
    }
}
