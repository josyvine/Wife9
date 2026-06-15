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
        WifeLogger.log(TAG, "startPlayback() invoked. Checking active playback status...");
        if (isPlaying) {
            WifeLogger.log(TAG, "startPlayback() aborted: Playback is already active.");
            return;
        }

        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        WifeLogger.log(TAG, "Resolved min buffer size for AudioTrack: " + minBufferSize + " bytes. Initializing AudioTrack builder...");

        try {
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
        } catch (Exception builderEx) {
            WifeLogger.log(TAG, "Exception thrown inside AudioTrack builder: " + builderEx.getMessage(), builderEx);
            return;
        }

        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack initialization failed.");
            WifeLogger.log(TAG, "AudioTrack state check failed: STATE_UNINITIALIZED. Playback cannot start.");
            return;
        }

        WifeLogger.log(TAG, "AudioTrack initialized successfully. Initiating hardware output play stream.");
        try {
            audioTrack.play();
        } catch (Exception playEx) {
            WifeLogger.log(TAG, "Failed to start AudioTrack play stream: " + playEx.getMessage(), playEx);
            return;
        }
        
        isPlaying = true;

        playThread = new Thread(() -> {
            WifeLogger.log(TAG, "Audio playback streaming thread spawned. Starting read-loop on input stream.");
            byte[] buffer = new byte[minBufferSize];
            long totalBytesPlayed = 0;
            try {
                while (isPlaying) {
                    int numRead = inputStream.read(buffer, 0, buffer.length);
                    if (numRead > 0) {
                        audioTrack.write(buffer, 0, numRead);
                        totalBytesPlayed += numRead;
                    } else if (numRead == -1) {
                        WifeLogger.log(TAG, "Audio playback stream read returned -1. Input socket stream terminated by sender.");
                        break; // End of stream
                    }
                }
                WifeLogger.log(TAG, "Exited audio playback streaming read-loop. Total PCM bytes written: " + totalBytesPlayed);
            } catch (Exception e) {
                Log.e(TAG, "Audio playback streaming error: " + e.getMessage());
                WifeLogger.log(TAG, "Audio playback streaming loop encountered an exception: " + e.getMessage(), e);
            }
        });
        playThread.start();
        Log.d(TAG, "Audio playback thread started.");
        WifeLogger.log(TAG, "Audio playback thread started successfully.");
    }

    public synchronized void stopPlayback() {
        WifeLogger.log(TAG, "stopPlayback() invoked. Stopping playback variables and releasing track...");
        isPlaying = false;
        if (playThread != null) {
            WifeLogger.log(TAG, "Interrupting active audio playback thread.");
            playThread.interrupt();
            playThread = null;
        }
        if (audioTrack != null) {
            try {
                if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    WifeLogger.log(TAG, "Stopping AudioTrack output stream.");
                    audioTrack.stop();
                }
                WifeLogger.log(TAG, "Releasing native AudioTrack resource buffers.");
                audioTrack.release();
            } catch (Exception e) {
                e.printStackTrace();
                WifeLogger.log(TAG, "Error stopping or releasing AudioTrack resource: " + e.getMessage(), e);
            }
            audioTrack = null;
        }
        Log.d(TAG, "Audio playback stopped.");
        WifeLogger.log(TAG, "Audio playback halted cleanly.");
    }

    public void setSpeakerphoneOn(boolean on) {
        WifeLogger.log(TAG, "setSpeakerphoneOn() invoked. Value: " + on);
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            try {
                WifeLogger.log(TAG, "Setting AudioManager mode to MODE_IN_COMMUNICATION.");
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                
                WifeLogger.log(TAG, "Executing setSpeakerphoneOn(" + on + ") call.");
                audioManager.setSpeakerphoneOn(on);
                Log.d(TAG, "Speakerphone set to: " + on);
                WifeLogger.log(TAG, "AudioManager speakerphone routed successfully.");
            } catch (Exception e) {
                WifeLogger.log(TAG, "Failed modifying system audio parameters: " + e.getMessage(), e);
            }
        } else {
            WifeLogger.log(TAG, "AudioManager was unresolved. Cannot route speaker output.");
        }
    }
}