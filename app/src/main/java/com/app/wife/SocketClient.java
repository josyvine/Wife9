package com.wife.app;

import android.content.Context;
import android.util.Log;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketClient {
    private static final String TAG = "SocketClient";

    private final Context context;
    private final InetAddress hostAddress;
    private final ConnectionManager connectionManager;
    private final ExecutorService executorService;

    public SocketClient(Context context, InetAddress hostAddress, ConnectionManager connectionManager) {
        this.context = context;
        this.hostAddress = hostAddress;
        this.connectionManager = connectionManager;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void start() {
        Log.d(TAG, "SocketClient initialized with Host: " + hostAddress.getHostAddress());
        // Probe connection with a dummy heartbeat/handshake to let the Host learn our local IP
        sendControlMessage("{\"type\":\"handshake\",\"sender\":\"" + Utils.getDeviceId(context) + "\"}");
    }

    public void close() {
        executorService.shutdownNow();
        Log.d(TAG, "SocketClient shut down.");
    }

    public void sendControlMessage(final String jsonPayload) {
        executorService.execute(() -> {
            try (Socket socket = new Socket(hostAddress, Constants.OFF_PORT_CONTROL);
                 OutputStream os = socket.getOutputStream();
                 PrintWriter pw = new PrintWriter(os, true)) {
                
                pw.println(jsonPayload);
                Log.d(TAG, "Successfully sent control packet: " + jsonPayload);
            } catch (Exception e) {
                Log.e(TAG, "Error sending control packet: " + e.getMessage());
            }
        });
    }

    public void sendTextMessage(final String jsonPayload) {
        executorService.execute(() -> {
            try (Socket socket = new Socket(hostAddress, Constants.OFF_PORT_TEXT);
                 OutputStream os = socket.getOutputStream();
                 PrintWriter pw = new PrintWriter(os, true)) {
                
                pw.println(jsonPayload);
                Log.d(TAG, "Successfully sent message packet: " + jsonPayload);
            } catch (Exception e) {
                Log.e(TAG, "Error sending message packet: " + e.getMessage());
            }
        });
    }
}
