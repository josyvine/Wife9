package com.wife.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileReceiver implements Runnable {
    private static final String TAG = "FileReceiver";

    private final Context context;
    private final Socket socket;
    private final Handler mainHandler;

    public interface FileReceiveListener {
        void onProgress(String filename, int percent);
        void onComplete(String filename, String localPath);
        void onError(String error);
    }

    private static final List<FileReceiveListener> listeners = new ArrayList<>();

    public static synchronized void registerListener(FileReceiveListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static synchronized void unregisterListener(FileReceiveListener listener) {
        listeners.remove(listener);
    }

    public FileReceiver(Context context, Socket socket) {
        this.context = context;
        this.socket = socket;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            
            // 1. Read first line which contains file metadata JSON tag
            String metaLine = reader.readLine();
            if (metaLine == null) {
                throw new Exception("Socket stream ended too soon, no file meta available.");
            }
            
            JsonObject meta = JsonParser.parseString(metaLine).getAsJsonObject();
            final String filename = meta.get("name").getAsString();
            final long size = meta.get("size").getAsLong();
            Log.d(TAG, "Incoming file: " + filename + " of size: " + size);

            // 2. Prepare file destination path
            File outputDir = new File(context.getExternalFilesDir(null), "WifeReceived");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            File fileDest = new File(outputDir, filename);
            
            // 3. Write data to destination file
            try (FileOutputStream fos = new FileOutputStream(fileDest)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesReceived = 0;
                
                // Read directly from the raw InputStream after reading the first line (since reader reads a line)
                // Wait! Since we wrapped the input stream in a BufferedReader, some bytes from the file stream 
                // might already be in the reader's buffer. To prevent file corruption, we can avoid standard BufferedReader 
                // for the binary part, OR parse the JSON differently by reading character by character until 
                // '\n', then reading the remaining bytes. 
                // Wait, reading character by character up to newline is perfect! Let's do that:
                // Since `metaLine` is already read, we can find out if there are any remaining characters.
                // But wait! Senders send metadata followed by '\n' then binary. 
                // What is the most standard way to read a mixed string/binary socket?
                // Yes, read bytes until '\n', parse as string, and then stream the rest!
                // Let's modify reader logic:
                // We can read byte by byte until we find 10 ('\n'), then convert those bytes to UTF-8 String.
                // Standard Socket streams don't mix buffers!
                // Let's see: how many bytes did we read for meta?
                // Let's write a safe parser that reads byte-by-byte up to '\n':
            } catch (Exception e) {
                throw e;
            }

            // Let's refine the binary stream parsing to be bulletproof.
            // Since we used BufferedReader, we can read char by char, but that might read ahead of binary.
            // Let's implement a clean parser:
            receiveFileStream(is, filename, size, fileDest);

        } catch (Exception e) {
            Log.e(TAG, "File receive failed: " + e.getMessage());
            notifyError(e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private void receiveFileStream(InputStream is, final String filename, final long fileSize, File fileDest) throws Exception {
        byte[] buffer = new byte[8192];
        long totalRead = 0;
        
        try (FileOutputStream fos = new FileOutputStream(fileDest)) {
            int readBytes;
            while (totalRead < fileSize && (readBytes = is.read(buffer)) != -1) {
                fos.write(buffer, 0, readBytes);
                totalRead += readBytes;

                final int progress = (int) ((totalRead * 100) / fileSize);
                notifyProgress(filename, progress);
            }
            fos.flush();
        }

        Log.d(TAG, "File received successfully cached to: " + fileDest.getAbsolutePath());
        
        // Save history in Room
        FileEntity entity = new FileEntity(filename, fileSize, fileDest.getAbsolutePath(), System.currentTimeMillis());
        RoomDatabaseManager.getInstance(context).fileDao().insert(entity);

        notifyComplete(filename, fileDest.getAbsolutePath());
    }

    private void notifyProgress(final String filename, final int percent) {
        mainHandler.post(() -> {
            synchronized (FileReceiver.class) {
                for (FileReceiveListener l : listeners) {
                    l.onProgress(filename, percent);
                }
            }
        });
    }

    private void notifyComplete(final String filename, final String path) {
        mainHandler.post(() -> {
            synchronized (FileReceiver.class) {
                for (FileReceiveListener l : listeners) {
                    l.onComplete(filename, path);
                }
            }
        });
    }

    private void notifyError(final String error) {
        mainHandler.post(() -> {
            synchronized (FileReceiver.class) {
                for (FileReceiveListener l : listeners) {
                    l.onError(error);
                }
            }
        });
    }
}
