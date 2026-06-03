package com.tradeanalyst.app;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import java.util.Locale;

public class TextToSpeechManager {

    private static final String TAG = "TextToSpeech";
    private TextToSpeech mTTS;
    private boolean mIsInitialized = false;

    public TextToSpeechManager(Context context) {
        try {
            mTTS = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (status == TextToSpeech.SUCCESS) {
                        int result = mTTS.setLanguage(Locale.US);
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e(TAG, "English Locale is not supported.");
                        } else {
                            mIsInitialized = true;
                        }
                    } else {
                        Log.e(TAG, "TextToSpeech Initialization failed.");
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "TTS creation exception", e);
        }
    }

    public void speak(String text) {
        if (mTTS != null && mIsInitialized) {
            mTTS.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LiveTradeAssistantTTS");
        }
    }

    public void stop() {
        if (mTTS != null) {
            mTTS.stop();
        }
    }

    public void destroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }
    }
}
