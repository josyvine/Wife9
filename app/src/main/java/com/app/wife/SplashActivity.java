package com.wife.app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Pre-warm local singleton processes
        RoomDatabaseManager.getInstance(this);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (PermissionManager.hasAllPermissions(this)) {
                navigateToMain();
            } else {
                PermissionManager.requestPermissions(this);
            }
        }, 2000);
    }

    private void navigateToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionManager.PERMISSION_REQUEST_CODE) {
            // Settle inside MainActivity regardless to keep UX fluid, or ask permissions inside main
            navigateToMain();
        }
    }
}
