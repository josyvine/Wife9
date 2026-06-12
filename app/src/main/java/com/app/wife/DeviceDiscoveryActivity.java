package com.wife.app;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.wife.app.databinding.ActivityDeviceDiscoveryBinding;

import java.util.ArrayList;
import java.util.List;

public class DeviceDiscoveryActivity extends AppCompatActivity implements 
        WiFiDirectManager.PeerChangeListener, 
        WiFiDirectManager.ConnectionChangeListener {

    private ActivityDeviceDiscoveryBinding binding;
    private WiFiDirectManager wifiDirectManager;
    private DeviceAdapter adapter;
    private final List<WifiP2pDevice> peerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceDiscoveryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        wifiDirectManager = WiFiDirectManager.getInstance(this);

        setupToolbar();
        setupRecyclerView();

        binding.btnStartDiscovery.setOnClickListener(v -> {
            binding.pbDiscoveryProgress.setVisibility(View.VISIBLE);
            wifiDirectManager.discoverPeers();
        });

        // Trigger an initial discovery sweep
        wifiDirectManager.discoverPeers();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarDiscovery);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarDiscovery.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new DeviceAdapter(peerList, device -> {
            Toast.makeText(this, "Connecting to " + device.deviceName + "...", Toast.LENGTH_SHORT).show();
            wifiDirectManager.connect(device, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(DeviceDiscoveryActivity.this, "Connection request posted successfully.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(DeviceDiscoveryActivity.this, "Failed connecting. Reason: " + reason, Toast.LENGTH_SHORT).show();
                }
            });
        });
        binding.rvDiscoveredDevices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvDiscoveredDevices.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        wifiDirectManager.registerPeerChangeListener(this);
        wifiDirectManager.registerConnectionChangeListener(this);
        
        // Populate current peer list immediately if available
        onPeersChanged(wifiDirectManager.getPeersList());
    }

    @Override
    protected void onPause() {
        super.onPause();
        wifiDirectManager.unregisterPeerChangeListener(this);
        wifiDirectManager.unregisterConnectionChangeListener(this);
    }

    @Override
    public void onPeersChanged(List<WifiP2pDevice> peers) {
        binding.pbDiscoveryProgress.setVisibility(View.GONE);
        peerList.clear();
        peerList.addAll(peers);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onConnectionChanged(android.net.wifi.p2p.WifiP2pInfo info) {
        if (info != null && info.groupFormed) {
            Toast.makeText(this, "P2P Network Group Formed successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Go back to Home Dashboard once connected, where detailed status will show
        }
    }
}
