package com.wife.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {
    
    public static final int PERMISSION_REQUEST_CODE = 420;

    public static String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        
        // Location is mandatory for WiFi Direct discovery on all versions (and background scanning)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        
        // Camera & Audio for Voice/Video calling
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        
        // Storage Perms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        return permissions.toArray(new String[0]);
    }

    public static boolean hasAllPermissions(Context context) {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                // Ignore WRITE_EXTERNAL_STORAGE on Android 11+ if not strict, but let's check properly
                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    public static void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, getRequiredPermissions(), PERMISSION_REQUEST_CODE);
    }
}
