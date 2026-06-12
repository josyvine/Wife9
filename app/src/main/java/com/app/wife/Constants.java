package com.wife.app;

public final class Constants {
    private Constants() {}

    // Socket ports on the devices
    public static final int OFF_PORT_CONTROL = 8888;
    public static final int OFF_PORT_TEXT = 8889;
    public static final int OFF_PORT_FILE = 8900;
    public static final int OFF_PORT_VOICE = 8901;
    public static final int OFF_PORT_VIDEO = 8902;

    // Intents and extras keys
    public static final String EXTRA_PEER_IP = "com.wife.app.EXTRA_PEER_IP";
    public static final String EXTRA_PEER_NAME = "com.wife.app.EXTRA_PEER_NAME";
    public static final String EXTRA_PEER_MAC = "com.wife.app.EXTRA_PEER_MAC";
    public static final String EXTRA_IS_HOST = "com.wife.app.EXTRA_IS_HOST";

    // Call Signaling events
    public static final String SIGNAL_CALL_REQUEST = "CALL_REQUEST";
    public static final String SIGNAL_CALL_ACCEPT = "CALL_ACCEPT";
    public static final String SIGNAL_CALL_REJECT = "CALL_REJECT";
    public static final String SIGNAL_CALL_END = "CALL_END";

    public static final String SIGNAL_VIDEO_REQUEST = "VIDEO_CALL_REQUEST";
    public static final String SIGNAL_VIDEO_ACCEPT = "VIDEO_CALL_ACCEPT";
    public static final String SIGNAL_VIDEO_REJECT = "VIDEO_CALL_REJECT";
    public static final String SIGNAL_VIDEO_END = "VIDEO_CALL_END";

    // Broadcast actions
    public static final String ACTION_CONNECTION_CHANGED = "com.wife.app.ACTION_CONNECTION_CHANGED";
    public static final String ACTION_PEER_DISCOVERED = "com.wife.app.ACTION_PEER_DISCOVERED";
    public static final String ACTION_CALL_RECEIVED = "com.wife.app.ACTION_CALL_RECEIVED";
    public static final String ACTION_VIDEO_CALL_RECEIVED = "com.wife.app.ACTION_VIDEO_CALL_RECEIVED";
    
    public static final String EXTRA_CALL_SENDER = "EXTRA_CALL_SENDER";
    public static final String EXTRA_CALL_IP = "EXTRA_CALL_IP";
}
