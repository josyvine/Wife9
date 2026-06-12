package com.wife.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.util.Log;

import java.io.OutputStream;

public class AudioCaptureManager {
    private static final String TAG = "AudioCapture";

    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Context context;
    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private Thread recordThread;

    private AcousticEchoCanceler echoCanceler;
    private NoiseSuppressor noiseSuppressor;

    public AudioCaptureManager(Context context) {
        this.context = context;
    }

    @SuppressLint("MissingPermission")
    public synchronized void startCapture(final OutputStream outputStream) {
        if (isRecording) return;

        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord state is not initialized.");
            return;
        }

        // Enable hardware Echo Cancellation and Noise Suppression if available
        int audioSessionId = audioRecord.getAudioSessionId();
        if (AcousticEchoCanceler.isAvailable()) {
            echoCanceler = AcousticEchoCanceler.create(audioSessionId);
            if (echoCanceler != null) echoCanceler.setEnabled(true);
        }
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(audioSessionId);
            if (noiseSuppressor != null) noiseSuppressor.setEnabled(true);
        }

        audioRecord.startRecording();
        isRecording = true;

        recordThread = new Thread(() -> {
            byte[] buffer = new byte[minBufferSize];
            try {
                while (isRecording) {
                    int readBytes = audioRecord.read(buffer, 0, buffer.length);
                    if (readBytes > 0) {
                        outputStream.write(buffer, 0, readBytes);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Audio capture streaming error: " + e.getMessage());
            }
        });
        recordThread.start();
        Log.d(TAG, "Audio capture started at session " + audioSessionId);
    }

    public synchronized void stopCapture() {
        isRecording = false;
        if (recordThread != null) {
            recordThread.interrupt();
            recordThread = null;
        }
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) { e.printStackTrace(); }
            audioRecord = null;
        }
        if (echoCanceler != null) {
            echoCanceler.release();
            echoCanceler = null;
        }
        if (noiseSuppressor != null) {
            noiseSuppressor.release();
            noiseSuppressor = null;
        }
        Log.d(TAG, "Audio capture stopped.");
    }
}
