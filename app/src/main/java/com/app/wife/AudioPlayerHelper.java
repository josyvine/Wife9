package com.wife.app;

import android.media.MediaPlayer;
import android.util.Log;
import java.io.File;

public class AudioPlayerHelper {
    private static final String TAG = "AudioPlayerHelper";
    private static volatile AudioPlayerHelper instance;
    private MediaPlayer mediaPlayer;
    private String currentPlayingPath;
    private AudioPlayerListener currentListener;

    public interface AudioPlayerListener {
        void onStart();
        void onStop();
        void onError(String error);
    }

    public static AudioPlayerHelper getInstance() {
        if (instance == null) {
            synchronized (AudioPlayerHelper.class) {
                if (instance == null) {
                    instance = new AudioPlayerHelper();
                }
            }
        }
        return instance;
    }

    private AudioPlayerHelper() {}

    public synchronized void play(String filePath, AudioPlayerListener listener) {
        if (mediaPlayer != null) {
            stop();
        }

        currentPlayingPath = filePath;
        currentListener = listener;

        try {
            File file = new File(filePath);
            if (!file.exists()) {
                WifeLogger.log(TAG, "Audio file play aborted. File not found at path: " + filePath);
                if (listener != null) {
                    listener.onError("Audio file not found.");
                }
                return;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> {
                WifeLogger.log(TAG, "Audio playback completed naturally.");
                stop();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                WifeLogger.log(TAG, "MediaPlayer error. What Code: " + what + " Extra Code: " + extra);
                if (listener != null) {
                    listener.onError("Playback engine error code: " + what);
                }
                stop();
                return true;
            });

            mediaPlayer.start();
            if (listener != null) {
                listener.onStart();
            }
            WifeLogger.log(TAG, "Audio playback started successfully for: " + filePath);
        } catch (Exception e) {
            WifeLogger.log(TAG, "Failed playing voice note: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError(e.getMessage());
            }
            stop();
        }
    }

    public synchronized void stop() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                WifeLogger.log(TAG, "Error releasing MediaPlayer resource: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        if (currentListener != null) {
            currentListener.onStop();
            currentListener = null;
        }
        currentPlayingPath = null;
        WifeLogger.log(TAG, "Audio playback stopped.");
    }

    public synchronized boolean isPlaying(String filePath) {
        return mediaPlayer != null && mediaPlayer.isPlaying() && filePath.equals(currentPlayingPath);
    }
}