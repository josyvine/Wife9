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
        WifeLogger.log(TAG, "startCall() invoked. Target Peer IP: " + peerIp + " | Checking active status...");
        if (isCallActive) {
            WifeLogger.log(TAG, "startCall() aborted: A voice call session is already active.");
            return;
        }
        isCallActive = true;

        executorService.execute(() -> {
            WifeLogger.log(TAG, "Outbound voice call thread launched. Attempting socket connection to " + peerIp + " on Port: " + Constants.OFF_PORT_VOICE);
            try {
                Log.d(TAG, "Starting VoiceCall connection. Connecting to: " + peerIp);
                // Connect outbound to caller's receiver port
                activeSocket = new Socket(peerIp, Constants.OFF_PORT_VOICE);
                WifeLogger.log(TAG, "Outbound Voice Socket connected successfully with " + peerIp + " on port " + Constants.OFF_PORT_VOICE);
                
                // Initialize hardware feedback
                WifeLogger.log(TAG, "Enabling system speakerphone routing via AudioManager.");
                playbackManager.setSpeakerphoneOn(true);
                
                // Begin bidirectional PCM streaming
                WifeLogger.log(TAG, "Initializing AudioCaptureManager on socket OutputStream.");
                captureManager.startCapture(activeSocket.getOutputStream());

                WifeLogger.log(TAG, "Initializing AudioPlaybackManager on socket InputStream.");
                playbackManager.startPlayback(activeSocket.getInputStream());
                
                Log.d(TAG, "Outgoing VoiceCall pipeline established.");
                WifeLogger.log(TAG, "Outgoing VoiceCall pipeline established and active.");
            } catch (Exception e) {
                Log.e(TAG, "Failed establishing VoiceCall: " + e.getMessage());
                WifeLogger.log(TAG, "Failed establishing outbound Voice Socket connection to " + peerIp + " | Exception: " + e.getMessage(), e);
                teardown();
            }
        });
    }

    public synchronized void listenForIncomingCall() {
        WifeLogger.log(TAG, "listenForIncomingCall() invoked. Checking active status...");
        if (isCallActive) {
            WifeLogger.log(TAG, "listenForIncomingCall() aborted: A voice call session is already active.");
            return;
        }
        isCallActive = true;

        executorService.execute(() -> {
            WifeLogger.log(TAG, "Inbound voice call thread launched. Initializing ServerSocket on Port: " + Constants.OFF_PORT_VOICE);
            try {
                serverSocket = new ServerSocket(Constants.OFF_PORT_VOICE);
                Log.d(TAG, "Listening for incoming VoiceCall on port " + Constants.OFF_PORT_VOICE);
                WifeLogger.log(TAG, "Inbound voice ServerSocket bound successfully. Awaiting incoming connections...");
                
                activeSocket = serverSocket.accept();
                String remoteIp = activeSocket.getInetAddress() != null ? activeSocket.getInetAddress().getHostAddress() : "Unknown IP";
                Log.d(TAG, "VoiceCall connection accepted from " + remoteIp);
                WifeLogger.log(TAG, "Inbound Voice socket connection accepted from client: " + remoteIp);

                WifeLogger.log(TAG, "Enabling system speakerphone routing via AudioManager.");
                playbackManager.setSpeakerphoneOn(true);
                
                WifeLogger.log(TAG, "Initializing AudioCaptureManager on socket OutputStream.");
                captureManager.startCapture(activeSocket.getOutputStream());

                WifeLogger.log(TAG, "Initializing AudioPlaybackManager on socket InputStream.");
                playbackManager.startPlayback(activeSocket.getInputStream());
                
                Log.d(TAG, "Inbound VoiceCall pipelines loaded.");
                WifeLogger.log(TAG, "Inbound VoiceCall pipelines loaded and active.");
            } catch (Exception e) {
                Log.e(TAG, "Inbound VoiceCall server failed: " + e.getMessage());
                WifeLogger.log(TAG, "Inbound voice server or capture initialization failed: " + e.getMessage(), e);
                teardown();
            }
        });
    }

    public synchronized void muteMicrophone(boolean mute) {
        WifeLogger.log(TAG, "muteMicrophone() invoked. Mute: " + mute);
        if (mute) {
            WifeLogger.log(TAG, "Muting microphone. Stopping AudioCaptureManager.");
            captureManager.stopCapture();
        } else if (activeSocket != null && activeSocket.isConnected()) {
            try {
                WifeLogger.log(TAG, "Unmuting microphone. Restarting AudioCaptureManager on socket OutputStream.");
                captureManager.startCapture(activeSocket.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
                WifeLogger.log(TAG, "Error unmuting and restarting capture stream: " + e.getMessage(), e);
            }
        } else {
            WifeLogger.log(TAG, "muteMicrophone(false) ignored: Socket is null or disconnected.");
        }
    }

    public void setSpeakerphoneEnabled(boolean enabled) {
        WifeLogger.log(TAG, "setSpeakerphoneEnabled() invoked. Enabled: " + enabled);
        playbackManager.setSpeakerphoneOn(enabled);
    }

    public synchronized void endCall() {
        WifeLogger.log(TAG, "endCall() invoked. Terminating active session.");
        teardown();
    }

    private synchronized void teardown() {
        WifeLogger.log(TAG, "teardown() invoked. Terminating voice socket connections and stopping audio pipelines.");
        isCallActive = false;
        
        WifeLogger.log(TAG, "Stopping AudioCaptureManager.");
        captureManager.stopCapture();
        
        WifeLogger.log(TAG, "Stopping AudioPlaybackManager.");
        playbackManager.stopPlayback();

        if (activeSocket != null) {
            try {
                WifeLogger.log(TAG, "Closing active Voice Socket client connection.");
                activeSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                WifeLogger.log(TAG, "Error closing active Voice Socket: " + e.getMessage(), e);
            }
            activeSocket = null;
        }

        if (serverSocket != null) {
            try {
                WifeLogger.log(TAG, "Closing inbound voice ServerSocket.");
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                WifeLogger.log(TAG, "Error closing voice ServerSocket: " + e.getMessage(), e);
            }
            serverSocket = null;
        }
        Log.d(TAG, "VoiceCall pipelines terminated.");
        WifeLogger.log(TAG, "Voice call pipeline teardown completed cleanly.");
    }
}