package com.wife.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class VideoDecoderManager {
    private static final String TAG = "VideoDecoderManager";

    private final Handler mainHandler;
    private boolean isDecoding = false;
    private Thread decodeThread;

    public interface VideoFrameListener {
        void onFrameReceived(Bitmap bitmap);
        void onDecoderError(String error);
    }

    public VideoDecoderManager() {
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public synchronized void startDecoding(final InputStream inputStream, final VideoFrameListener listener) {
        if (isDecoding) return;
        isDecoding = true;

        decodeThread = new Thread(() -> {
            try {
                // Buffer to read the 4-byte size header
                byte[] sizeHeader = new byte[4];

                while (isDecoding) {
                    // Read 4 bytes for the size
                    int bytesRead = 0;
                    while (bytesRead < 4 && isDecoding) {
                        int r = inputStream.read(sizeHeader, bytesRead, 4 - bytesRead);
                        if (r == -1) {
                            throw new Exception("Video input stream ended abruptly.");
                        }
                        bytesRead += r;
                    }

                    int frameSize = ByteBuffer.wrap(sizeHeader).getInt();
                    if (frameSize <= 0 || frameSize > 2 * 1024 * 1024) {
                        Log.e(TAG, "Corrupted video frame length read: " + frameSize);
                        continue;
                    }

                    // Read 'frameSize' bytes
                    byte[] jpegBuffer = new byte[frameSize];
                    int totalRead = 0;
                    while (totalRead < frameSize && isDecoding) {
                        int r = inputStream.read(jpegBuffer, totalRead, frameSize - totalRead);
                        if (r == -1) {
                            throw new Exception("Abrupt termination reading JPEG payload.");
                        }
                        totalRead += r;
                    }

                    if (!isDecoding) break;

                    // Decode to Bitmap
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(jpegBuffer, 0, jpegBuffer.length);
                    if (bitmap != null && listener != null) {
                        mainHandler.post(() -> listener.onFrameReceived(bitmap));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Video decode failure: " + e.getMessage());
                if (listener != null) {
                    mainHandler.post(() -> listener.onDecoderError(e.getMessage()));
                }
            }
        });
        decodeThread.start();
        Log.d(TAG, "Video decoding thread spawned.");
    }

    public synchronized void stopDecoding() {
        isDecoding = false;
        if (decodeThread != null) {
            decodeThread.interrupt();
            decodeThread = null;
        }
        Log.d(TAG, "Video decoding thread stopped.");
    }
}
