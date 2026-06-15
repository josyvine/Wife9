package com.wife.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wife.app.databinding.ActivityVoiceCallBinding;
import com.google.gson.JsonObject;

public class VoiceCallActivity extends AppCompatActivity implements CallSignalingManager.SignalingEventListener {

    private ActivityVoiceCallBinding binding;
    private VoiceCallManager voiceCallManager;
    private String peerIp;
    private String peerName;
    private boolean isInbound;
    private boolean isMuted = false;
    private boolean isSpeaker = true;

    private int callDurationSeconds = 0;
    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private Runnable timerRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVoiceCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        voiceCallManager = VoiceCallManager.getInstance(this);

        // Fetch intent boundaries
        peerIp = getIntent().getStringExtra(Constants.EXTRA_PEER_IP);
        peerName = getIntent().getStringExtra(Constants.EXTRA_PEER_NAME);
        isInbound = getIntent().getBooleanExtra("IS_INBOUND", false);

        if (peerIp == null || peerIp.isEmpty()) {
            peerIp = ConnectionManager.getInstance(this).getPeerIpAddress();
        }
        if (peerName == null || peerName.isEmpty()) {
            peerName = "Mesh Peer";
        }

        binding.tvVoicePeerName.setText(peerName);

        setupClickListeners();
        configureCallingState();
    }

    private void setupClickListeners() {
        binding.fabMute.setOnClickListener(v -> {
            isMuted = !isMuted;
            voiceCallManager.muteMicrophone(isMuted);
            binding.fabMute.setImageResource(isMuted ? 
                    android.R.drawable.ic_lock_silent_mode : android.R.drawable.ic_btn_speak_now);
            Toast.makeText(this, isMuted ? "Microphone muted" : "Microphone active", Toast.LENGTH_SHORT).show();
        });

        binding.fabSpeaker.setOnClickListener(v -> {
            isSpeaker = !isSpeaker;
            voiceCallManager.setSpeakerphoneEnabled(isSpeaker);
            binding.fabSpeaker.setImageResource(isSpeaker ? 
                    android.R.drawable.ic_lock_silent_mode_off : android.R.drawable.ic_lock_silent_mode);
            Toast.makeText(this, isSpeaker ? "Speaker active" : "Earpiece active", Toast.LENGTH_SHORT).show();
        });

        binding.fabAcceptVoice.setOnClickListener(v -> {
            acceptCall();
        });

        binding.fabDeclineVoice.setOnClickListener(v -> {
            declineOrEndCall();
        });
    }

    private void configureCallingState() {
        CallSignalingManager.getInstance(this).registerListener(this);

        if (isInbound) {
            binding.tvVoiceCallState.setText("Inbound Voice Call...");
            binding.fabAcceptVoice.setVisibility(View.VISIBLE);
        } else {
            binding.tvVoiceCallState.setText("Calling...");
            binding.fabAcceptVoice.setVisibility(View.GONE);
            // Send request over control signal
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_CALL_REQUEST);
        }
    }

    private void acceptCall() {
        binding.fabAcceptVoice.setVisibility(View.GONE);
        binding.tvVoiceCallState.setText("Connecting...");

        // Alert initiator
        CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_CALL_ACCEPT);

        // Launch server listener
        voiceCallManager.listenForIncomingCall();
        startCallServiceAndTimer();
    }

    private void declineOrEndCall() {
        if (isInbound && binding.fabAcceptVoice.getVisibility() == View.VISIBLE) {
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_CALL_REJECT);
        } else {
            CallSignalingManager.getInstance(this).sendSignal(peerIp, Constants.SIGNAL_CALL_END);
        }
        hangUp();
    }

    private void startCallServiceAndTimer() {
        // Start Foreground Service for continuous calling background state
        Intent serviceIntent = new Intent(this, VoiceCallForegroundService.class);
        startService(serviceIntent);

        binding.tvVoiceCallState.setText("Volume Active");
        binding.tvVoiceDuration.setVisibility(View.VISIBLE);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                callDurationSeconds++;
                binding.tvVoiceDuration.setText(Utils.formatDuration(callDurationSeconds));
                timerHandler.postDelayed(this, 1000);
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private void hangUp() {
        stopCallServiceAndTimer();
        voiceCallManager.endCall();

        // Save Call Record in Db
        CallEntity entity = new CallEntity(peerName, "Voice", callDurationSeconds, System.currentTimeMillis());
        RoomDatabaseManager.getInstance(this).callDao().insert(entity);

        finish();
    }

    private void stopCallServiceAndTimer() {
        CallSignalingManager.getInstance(this).unregisterListener(this);
        if (timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
        stopService(new Intent(this, VoiceCallForegroundService.class));
    }

    @Override
    public void onSignalReceived(String action, String senderIp, JsonObject payload) {
        runOnUiThread(() -> {
            switch (action) {
                case Constants.SIGNAL_CALL_ACCEPT:
                    binding.tvVoiceCallState.setText("Connected");
                    // Initiator launches active client stream to peer
                    voiceCallManager.startCall(peerIp);
                    startCallServiceAndTimer();
                    break;
                case Constants.SIGNAL_CALL_REJECT:
                    Toast.makeText(this, "Call rejected by peer.", Toast.LENGTH_SHORT).show();
                    hangUp();
                    break;
                case Constants.SIGNAL_CALL_END:
                    Toast.makeText(this, "Call terminated.", Toast.LENGTH_SHORT).show();
                    hangUp();
                    break;
            }
        });
    }

    @Override
    public void onBackPressed() {
        declineOrEndCall();
        super.onBackPressed();
    }
}