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
        VideoFrameListener {

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

        binding.tvVideoPeerName.setText(peerName);

        setupClickListeners();
        configureCallingState();
    }

    private void setupClickListeners() {
        binding.fabSwitchCamera.setOnClickListener(v -> {
            videoCallManager.switchCamera(this);
            Toast.makeText(this, "Flipped focus", Toast.LENGTH_SHORT).show();
        });

        binding.fabAcceptVideo.setOnClickListener(v -> {
            acceptCall();
        });

        binding.fabDeclineVideo.setOnClickListener(v -> {
            declineOrEndCall();
        });
    }

    private void configureCallingState() {
        CallSignalingManager.getInstance(this).registerListener(this);

        if (isInbound) {
            binding.tvVideoCallState.setText("Inbound Video Request...");
            binding.fabAcceptVideo.setVisibility(View.VISIBLE);
        } else {
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
        CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_VIDEO_ACCEPT);

        // Bind incoming stream
        videoCallManager.listenForIncomingCall(this, this);
        startCallService();
    }

    private void declineOrEndCall() {
        if (isInbound && binding.fabAcceptVideo.getVisibility() == View.VISIBLE) {
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_VIDEO_REJECT);
        } else {
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_VIDEO_END);
        }
        hangUp();
    }

    private void startCallService() {
        Intent serviceIntent = new Intent(this, VideoCallForegroundService.class);
        startService(serviceIntent);

        binding.tvVideoCallState.setText("Mesh Streaming Live");
        callStartTime = System.currentTimeMillis();
    }

    private void hangUp() {
        stopCallService();
        videoCallManager.endCall();

        // Calculate call duration
        long duration = 0;
        if (callStartTime > 0) {
            duration = (System.currentTimeMillis() - callStartTime) / 1000;
        }

        // Save Call Record in DB
        CallEntity entity = new CallEntity(peerName, "Video", duration, System.currentTimeMillis());
        RoomDatabaseManager.getInstance(this).callDao().insert(entity);

        finish();
    }

    private void stopCallService() {
        CallSignalingManager.getInstance(this).unregisterListener(this);
        stopService(new Intent(this, VideoCallForegroundService.class));
    }

    @Override
    public void onSignalReceived(String action, String senderIp, JsonObject payload) {
        runOnUiThread(() -> {
            switch (action) {
                case Constants.SIGNAL_VIDEO_ACCEPT:
                    binding.tvVideoCallState.setText("Streaming Active");
                    videoCallManager.startCall(this, peerIp, this);
                    startCallService();
                    break;
                case Constants.SIGNAL_VIDEO_REJECT:
                    Toast.makeText(this, "Video invitation rejected.", Toast.LENGTH_SHORT).show();
                    hangUp();
                    break;
                case Constants.SIGNAL_VIDEO_END:
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
                
                // Draw self-mirror stream mock-preview for elegant UI/UX design output!
                binding.ivLocalVideo.setImageBitmap(bitmap); // Beautifully mirrored
            }
        });
    }

    @Override
    public void onDecoderError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Decorder synchronization error: " + error, Toast.LENGTH_SHORT).show();
            hangUp();
        });
    }

    @Override
    public void onBackPressed() {
        declineOrEndCall();
        super.onBackPressed();
    }
}
