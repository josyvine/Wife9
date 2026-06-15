package com.wife.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wife.app.databinding.ActivityVideoCallBinding;
import com.wife.app.VideoDecoderManager.VideoFrameListener;
import com.google.gson.JsonObject;

public class VideoCallActivity extends AppCompatActivity implements 
        CallSignalingManager.SignalingEventListener, 
        VideoFrameListener,
        VideoCaptureManager.LocalFrameListener {

    private static final String TAG = "VideoCallActivity";

    private ActivityVideoCallBinding binding;
    private VideoCallManager videoCallManager;
    private String peerIp;
    private String peerName;
    private boolean isInbound;
    private long callStartTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        WifeLogger.log(TAG, "onCreate() invoked. Initializing VideoCallActivity components.");

        videoCallManager = VideoCallManager.getInstance(this);

        peerIp = getIntent().getStringExtra(Constants.EXTRA_PEER_IP);
        peerName = getIntent().getStringExtra(Constants.EXTRA_PEER_NAME);
        isInbound = getIntent().getBooleanExtra("IS_INBOUND", false);

        if (peerIp == null || peerIp.isEmpty()) {
            peerIp = ConnectionManager.getInstance(this).getPeerIpAddress();
        }
        if (peerName == null || peerName.isEmpty()) {
            peerName = "Video Peer";
        }

        WifeLogger.log(TAG, "Target Peer IP: " + peerIp + " | Peer Name: " + peerName + " | Inbound Call Direction: " + isInbound);

        binding.tvVideoPeerName.setText(peerName);

        // Bind our local camera listener to stream real-time local previews into our PIP view
        VideoCaptureManager.setLocalFrameListener(this);

        setupClickListeners();
        configureCallingState();
    }

    private void setupClickListeners() {
        binding.fabSwitchCamera.setOnClickListener(v -> {
            WifeLogger.log(TAG, "User triggered Switch Camera button. Rotating active lens.");
            videoCallManager.switchCamera(this);
            Toast.makeText(this, "Flipped focus", Toast.LENGTH_SHORT).show();
        });

        binding.fabAcceptVideo.setOnClickListener(v -> {
            WifeLogger.log(TAG, "User accepted inbound video invitation.");
            acceptCall();
        });

        binding.fabDeclineVideo.setOnClickListener(v -> {
            WifeLogger.log(TAG, "User declined/ended active video call session.");
            declineOrEndCall();
        });
    }

    private void configureCallingState() {
        WifeLogger.log(TAG, "Configuring Call Signaling Manager. Registering Activity listener.");
        CallSignalingManager.getInstance(this).registerListener(this);

        if (isInbound) {
            WifeLogger.log(TAG, "Call State configured as: INBOUND_REQUEST.");
            binding.tvVideoCallState.setText("Inbound Video Request...");
            binding.fabAcceptVideo.setVisibility(View.VISIBLE);
        } else {
            WifeLogger.log(TAG, "Call State configured as: OUTBOUND_CALL. Dispatching signaling request to: " + peerIp);
            binding.tvVideoCallState.setText("Negotiating Link...");
            binding.fabAcceptVideo.setVisibility(View.GONE);
            // Send request over control signal
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_VIDEO_REQUEST);
        }
    }

    private void acceptCall() {
        binding.fabAcceptVideo.setVisibility(View.GONE);
        binding.tvVideoCallState.setText("Opening Video channel...");

        // Alert initiator
        WifeLogger.log(TAG, "Accepting Call. Relaying accept signaling packet to: " + peerIp);
        CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_VIDEO_ACCEPT);

        // Parallel audio: Listen for incoming audio stream
        WifeLogger.log(TAG, "Concurrently starting incoming audio listener on Port: " + Constants.OFF_PORT_VOICE);
        VoiceCallManager.getInstance(this).listenForIncomingCall();

        // Bind incoming stream
        WifeLogger.log(TAG, "Concurrently starting incoming video listener on Port: " + Constants.OFF_PORT_VIDEO);
        videoCallManager.listenForIncomingCall(this, this);
        startCallService();
    }

    private void declineOrEndCall() {
        WifeLogger.log(TAG, "declineOrEndCall() invoked. Processing signal termination...");
        if (isInbound && binding.fabAcceptVideo.getVisibility() == View.VISIBLE) {
            WifeLogger.log(TAG, "Call declined before connection. Dispatching REJECT signal to: " + peerIp);
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_VIDEO_REJECT);
        } else {
            WifeLogger.log(TAG, "Call ended during active stream. Dispatching END signal to: " + peerIp);
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_VIDEO_END);
        }
        hangUp();
    }

    private void startCallService() {
        WifeLogger.log(TAG, "Starting VideoCallForegroundService foreground worker.");
        Intent serviceIntent = new Intent(this, VideoCallForegroundService.class);
        startService(serviceIntent);

        binding.tvVideoCallState.setText("Mesh Streaming Live");
        callStartTime = System.currentTimeMillis();
    }

    private void hangUp() {
        WifeLogger.log(TAG, "hangUp() invoked. Stopping services and releasing active calling managers.");
        stopCallService();

        // Parallel audio: Terminate the parallel voice pipeline
        WifeLogger.log(TAG, "Concurrently halting concurrent voice call pipelines.");
        VoiceCallManager.getInstance(this).endCall();

        WifeLogger.log(TAG, "Halting video call pipelines.");
        videoCallManager.endCall();

        // Calculate call duration
        long duration = 0;
        if (callStartTime > 0) {
            duration = (System.currentTimeMillis() - callStartTime) / 1000;
        }
        WifeLogger.log(TAG, "Calculated call duration: " + duration + " seconds. Inserting log record to Call Dao.");

        // Save Call Record in DB
        CallEntity entity = new CallEntity(peerName, "Video", duration, System.currentTimeMillis());
        try {
            RoomDatabaseManager.getInstance(this).callDao().insert(entity);
            WifeLogger.log(TAG, "Call duration log record written successfully to SQLite DB.");
        } catch (Exception e) {
            WifeLogger.log(TAG, "Failed writing call log record to database: " + e.getMessage(), e);
        }

        WifeLogger.log(TAG, "Finalizing VideoCallActivity.");
        finish();
    }

    private void stopCallService() {
        WifeLogger.log(TAG, "stopCallService() invoked. Unregistering SignalingEventListener and terminating foreground service.");
        CallSignalingManager.getInstance(this).unregisterListener(this);
        stopService(new Intent(this, VideoCallForegroundService.class));
    }

    @Override
    public void onSignalReceived(String action, String senderIp, JsonObject payload) {
        WifeLogger.log(TAG, "onSignalReceived callback triggered. Action: " + action + " | Sender IP: " + senderIp);
        runOnUiThread(() -> {
            switch (action) {
                case Constants.SIGNAL_VIDEO_ACCEPT:
                    WifeLogger.log(TAG, "Signal matched: ACCEPT. Starting outbound audio/video stream parameters.");
                    binding.tvVideoCallState.setText("Streaming Active");
                    
                    // Parallel audio: Start outbound audio stream
                    WifeLogger.log(TAG, "Concurrently starting outbound audio client connection to: " + peerIp);
                    VoiceCallManager.getInstance(VideoCallActivity.this).startCall(peerIp);

                    videoCallManager.startCall(this, peerIp, this);
                    startCallService();
                    break;
                case Constants.SIGNAL_VIDEO_REJECT:
                    WifeLogger.log(TAG, "Signal matched: REJECT. Teardown active activity context.");
                    Toast.makeText(this, "Video invitation rejected.", Toast.LENGTH_SHORT).show();
                    hangUp();
                    break;
                case Constants.SIGNAL_VIDEO_END:
                    WifeLogger.log(TAG, "Signal matched: END. Teardown active activity context.");
                    Toast.makeText(this, "Video channel terminated.", Toast.LENGTH_SHORT).show();
                    hangUp();
                    break;
            }
        });
    }

    @Override
    public void onFrameReceived(Bitmap bitmap) {
        runOnUiThread(() -> {
            if (bitmap != null) {
                binding.ivPeerVideo.setImageBitmap(bitmap);
                // REMOVED: binding.ivLocalVideo.setImageBitmap(bitmap);
                // Cloned remote-bitmap assignment is cleared. The PIP view ivLocalVideo is now 
                // fed dynamically by the real-time local camera preview loop.
            }
        });
    }

    @Override
    public void onLocalFrameCaptured(Bitmap bitmap) {
        runOnUiThread(() -> {
            if (bitmap != null) {
                // Render your own local camera feed inside your PIP window
                binding.ivLocalVideo.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    public void onDecoderError(String error) {
        WifeLogger.log(TAG, "Decoder error received from VideoDecoderManager: " + error);
        runOnUiThread(() -> {
            Toast.makeText(this, "Decorder synchronization error: " + error, Toast.LENGTH_SHORT).show();
            hangUp();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifeLogger.log(TAG, "onDestroy() invoked. Cleaning up static LocalFrameListener handles.");
        VideoCaptureManager.setLocalFrameListener(null);
    }

    @Override
    public void onBackPressed() {
        WifeLogger.log(TAG, "User triggered native system back button press.");
        declineOrEndCall();
        super.onBackPressed();
    }
}