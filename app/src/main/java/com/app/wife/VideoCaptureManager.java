package com.wife.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VideoCaptureManager {
    private static final String TAG = "VideoCaptureManager";

    private final Context context;
    private final ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;
    private ImageAnalysis imageAnalysis;
    private boolean isCapturing = false;
    private int lensFacing = CameraSelector.LENS_FACING_FRONT;

    public VideoCaptureManager(Context context) {
        this.context = context;
        this.cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void startCapture(final LifecycleOwner lifecycleOwner, final OutputStream outputStream) {
        if (isCapturing) return;
        isCapturing = true;

        ContextCompat.getMainExecutor(context).execute(() -> {
            try {
                cameraProvider = ProcessCameraProvider.getInstance(context).get();
                
                CameraSelector selector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(320, 240))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> {
                    try {
                        if (!isCapturing) {
                            imageProxy.close();
                            return;
                        }

                        Image img = imageProxy.getImage();
                        if (img != null) {
                            byte[] jpegData = convertYuvToJpeg(imageProxy);
                            if (jpegData != null) {
                                // Write JPEG size as integer (4 bytes), then the actual byte payload
                                ByteBuffer sizeBuf = ByteBuffer.allocate(4);
                                sizeBuf.putInt(jpegData.length);
                                
                                synchronized (outputStream) {
                                    outputStream.write(sizeBuf.array());
                                    outputStream.write(jpegData);
                                    outputStream.flush();
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Frame analysis stream failed: " + e.getMessage());
                    } finally {
                        imageProxy.close();
                    }
                });

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, imageAnalysis);
                Log.d(TAG, "CameraX analyzer configured and bound.");

            } catch (Exception e) {
                Log.e(TAG, "CameraX initialization failed: " + e.getMessage());
            }
        });
    }

    public synchronized void switchCamera(final LifecycleOwner lifecycleOwner, final OutputStream outputStream) {
        lensFacing = (lensFacing == CameraSelector.LENS_FACING_FRONT) ? 
                CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT;
        if (isCapturing) {
            stopCapture();
            startCapture(lifecycleOwner, outputStream);
        }
    }

    public synchronized void stopCapture() {
        isCapturing = false;
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private byte[] convertYuvToJpeg(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        int width = image.getWidth();
        int height = image.getHeight();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 70, out);
            return out.toByteArray();
        } catch (Exception e) {
            Log.e(TAG, "Nv21 compression to JPEG failed: " + e.getMessage());
            return null;
        }
    }
}
