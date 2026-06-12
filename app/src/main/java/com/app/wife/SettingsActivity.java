package com.wife.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wife.app.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefs = getSharedPreferences("WifeSettings", MODE_PRIVATE);

        setupToolbar();
        displayCurrentSettings();

        binding.btnClearChatHistory.setOnClickListener(v -> {
            RoomDatabaseManager.getInstance(SettingsActivity.this).clearAllTables();
            Toast.makeText(SettingsActivity.this, "Local database wiped successfully.", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbarSettings);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarSettings.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void displayCurrentSettings() {
        String savedAlias = prefs.getString("custom_alias", Utils.getDeviceModel());
        binding.etDeviceName.setText(savedAlias);

        String secureId = Utils.getDeviceId(this);
        binding.tvSettingDeviceId.setText("Hardware Signature ID: " + secureId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save current alias to settings sharePreferences
        String aliasEntry = binding.etDeviceName.getText().toString().trim();
        if (!aliasEntry.isEmpty()) {
            prefs.edit().putString("custom_alias", aliasEntry).apply();
        }
    }
}
