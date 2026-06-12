package com.wife.app;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.InputStream;

public class AudioPlaybackManager {
    private static final String TAG = "AudioPlayback";

    private static final int SAMPLE_RATE = 8000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final Context context;
    private AudioTrack audioTrack;
    private boolean isPlaying = false;
    private Thread playThread;

    public AudioPlaybackManager(Context context) {
        this.context = context;
    }

    public synchronized void startPlayback(final InputStream inputStream) {
        if (isPlaying) return;

        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build())
                .setBufferSizeInBytes(minBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack initialization failed.");
            return;
        }

        audioTrack.play();
        isPlaying = true;

        playThread = new Thread(() -> {
            byte[] buffer = new byte[minBufferSize];
            try {
                while (isPlaying) {
                    int numRead = inputStream.read(buffer, 0, buffer.length);
                    if (numRead > 0) {
                        audioTrack.write(buffer, 0, numRead);
                    } else if (numRead == -1) {
                        break; // End of stream
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Audio playback streaming error: " + e.getMessage());
            }
        });
        playThread.start();
        Log.d(TAG, "Audio playback thread started.");
    }

    public synchronized void stopPlayback() {
        isPlaying = false;
        if (playThread != null) {
            playThread.interrupt();
            playThread = null;
        }
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception e) { e.printStackTrace(); }
            audioTrack = null;
        }
        Log.d(TAG, "Audio playback stopped.");
    }

    public void setSpeakerphoneOn(boolean on) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            audioManager.setSpeakerphoneOn(on);
            Log.d(TAG, "Speakerphone set to: " + on);
        }
    }
}
