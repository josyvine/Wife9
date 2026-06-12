package com.wife.app;

import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wife.app.databinding.ActivityConnectionStatusBinding;

public class ConnectionStatusActivity extends AppCompatActivity {

    private ActivityConnectionStatusBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityConnectionStatusBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        displayLinkMetrics();

        binding.btnForceDisconnect.setOnClickListener(v -> {
            WiFiDirectManager.getInstance(this).disconnect(new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ConnectionStatusActivity.this, "Active group terminated.", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(ConnectionStatusActivity.this, "Disconnect command refused. Reason: " + reason, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarConnectionStatus);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarConnectionStatus.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void displayLinkMetrics() {
        ConnectionManager conn = ConnectionManager.getInstance(this);
        if (conn.isConnected()) {
            binding.tvRole.setText("Link Mode: Mesh Connected");
            binding.tvRemoteIP.setText("Linked IP Address: " + conn.getPeerIpAddress());
            binding.tvGroupActive.setText("Group State: Fully Formed");
        } else {
            binding.tvRole.setText("Role: Idle");
            binding.tvRemoteIP.setText("Peer IP: Disconnected");
            binding.tvGroupActive.setText("Group Formed: No");
        }
    }
}
