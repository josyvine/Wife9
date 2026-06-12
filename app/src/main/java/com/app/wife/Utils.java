package com.wife.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Utils {
    private Utils() {}

    @SuppressLint("HardwareIds")
    public static String getDeviceId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getDeviceModel() {
        return android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    public static String formatDuration(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    public static String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public static boolean copyStream(InputStream is, OutputStream os) {
        byte[] buffer = new byte[8192];
        int numRead;
        try {
            while ((numRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, numRead);
            }
            os.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
