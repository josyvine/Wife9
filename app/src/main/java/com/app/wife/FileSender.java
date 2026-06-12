package com.wife.app;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileSender {
    private static final String TAG = "FileSender";
    private static volatile FileSender instance;

    private final Context context;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public interface FileTransferListener {
        void onProgress(int percent);
        void onComplete(String path);
        void onError(String error);
    }

    public static FileSender getInstance(Context context) {
        if (instance == null) {
            synchronized (FileSender.class) {
                if (instance == null) {
                    instance = new FileSender(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private FileSender(Context context) {
        this.context = context;
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void sendFile(final Uri fileUri, final String originalFileName, final long fileSize, final FileTransferListener listener) {
        final String peerIp = ConnectionManager.getInstance(context).getPeerIpAddress();
        if (peerIp == null || peerIp.isEmpty()) {
            if (listener != null) listener.onError("No connected peer available.");
            return;
        }

        executorService.execute(() -> {
            try (Socket socket = new Socket(peerIp, Constants.OFF_PORT_FILE);
                 OutputStream os = socket.getOutputStream();
                 PrintWriter pw = new PrintWriter(os, true);
                 InputStream is = context.getContentResolver().openInputStream(fileUri)) {

                if (is == null) {
                    throw new Exception("Unable to open Uri input stream.");
                }

                // 1. Send JSON descriptor block
                JsonObject fileMeta = new JsonObject();
                fileMeta.addProperty("type", "file");
                fileMeta.addProperty("name", originalFileName);
                fileMeta.addProperty("size", fileSize);
                
                pw.println(fileMeta.toString());
                
                // 2. Stream raw binary data
                byte[] buffer = new byte[8192];
                int readBytes;
                long totalBytesSent = 0;

                while ((readBytes = is.read(buffer)) != -1) {
                    os.write(buffer, 0, readBytes);
                    totalBytesSent += readBytes;

                    if (listener != null && fileSize > 0) {
                        final int progress = (int) ((totalBytesSent * 100) / fileSize);
                        mainHandler.post(() -> listener.onProgress(progress));
                    }
                }
                os.flush();
                Log.d(TAG, "File sent successfully: " + originalFileName);

                // Save record to DB
                FileEntity entity = new FileEntity(originalFileName, fileSize, fileUri.toString(), System.currentTimeMillis());
                RoomDatabaseManager.getInstance(context).fileDao().insert(entity);

                if (listener != null) {
                    mainHandler.post(() -> listener.onComplete(fileUri.toString()));
                }

            } catch (Exception e) {
                Log.e(TAG, "Failed sending file to " + peerIp + ": " + e.getMessage());
                if (listener != null) {
                    mainHandler.post(() -> listener.onError(e.getMessage()));
                }
            }
        });
    }
}
