package com.wife.app;

import android.content.Context;
import java.util.ArrayList;
import java.util.List;

public class ChatManager {
    private static volatile ChatManager instance;
    private final Context context;
    private final List<MessageListener> listeners = new ArrayList<>();

    public interface MessageListener {
        void onMessageReceived(MessageEntity message);
    }

    public static ChatManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ChatManager.class) {
                if (instance == null) {
                    instance = new ChatManager(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private ChatManager(Context context) {
        this.context = context;
    }

    public synchronized void registerMessageListener(MessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public synchronized void unregisterMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }

    public synchronized void notifyMessageReceived(MessageEntity message) {
        for (MessageListener listener : listeners) {
            listener.onMessageReceived(message);
        }
    }

    // Fixed: Added check to see if any observers are active in the foreground
    public synchronized boolean hasListeners() {
        return !listeners.isEmpty();
    }
}