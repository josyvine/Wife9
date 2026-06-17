package com.wife.app;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class EmojiLoader {
    private static final String TAG = "EmojiLoader";
    private static final String ASSET_FILE_NAME = "data-by-group.json";

    public static class EmojiDTO {
        private String emoji;
        private String name;
        private String slug;

        public String getEmoji() {
            return emoji;
        }

        public String getName() {
            return name;
        }

        public String getSlug() {
            return slug;
        }
    }

    private EmojiLoader() {}

    public static Map<String, List<EmojiDTO>> loadEmojisFromAssets(Context context) throws Exception {
        WifeLogger.log(TAG, "loadEmojisFromAssets() invoked. Opening stream for assets asset: " + ASSET_FILE_NAME);
        long startTime = System.currentTimeMillis();

        InputStream is = null;
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();

        try {
            is = context.getAssets().open(ASSET_FILE_NAME);
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }

            String jsonString = sb.toString();
            long readTime = System.currentTimeMillis() - startTime;
            WifeLogger.log(TAG, "Successfully read raw asset bytes into memory string. Size: " + jsonString.length() + " chars | Time elapsed: " + readTime + "ms");

            Type mapType = new TypeToken<Map<String, List<EmojiDTO>>>() {}.getType();
            Gson gson = new Gson();
            
            Map<String, List<EmojiDTO>> emojiMap = gson.fromJson(jsonString, mapType);
            long totalTime = System.currentTimeMillis() - startTime;
            
            if (emojiMap != null) {
                WifeLogger.log(TAG, "Successfully deserialized emoji map. Categories detected: " + emojiMap.size() + " | Total parsing time: " + totalTime + "ms");
            } else {
                WifeLogger.log(TAG, "Gson deserialization returned a null mapping object.");
            }

            return emojiMap;
        } catch (Exception e) {
            WifeLogger.log(TAG, "Error reading or deserializing offline emoji JSON database: " + e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (reader != null) reader.close();
                if (is != null) is.close();
            } catch (Exception ignored) {}
        }
    }
}