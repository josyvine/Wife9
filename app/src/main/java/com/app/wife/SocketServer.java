package com.wife.app;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer {
    private static final String TAG = "SocketServer";

    private final Context context;
    private final ConnectionManager connectionManager;
    private final ExecutorService executorService;

    private ServerSocket controlSocket;
    private ServerSocket textSocket;
    private ServerSocket fileSocket;

    private boolean isRunning = false;

    public SocketServer(Context context, ConnectionManager connectionManager) {
        this.context = context;
        this.connectionManager = connectionManager;
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public void start() {
        isRunning = true;
        
        executorService.execute(this::runControlServer);
        executorService.execute(this::runTextServer);
        executorService.execute(this::runFileServer);
        
        Log.d(TAG, "Socket servers started successfully.");
    }

    public void stop() {
        isRunning = false;
        try {
            if (controlSocket != null && !controlSocket.isClosed()) controlSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
        try {
            if (textSocket != null && !textSocket.isClosed()) textSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
        try {
            if (fileSocket != null && !fileSocket.isClosed()) fileSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
        
        executorService.shutdownNow();
        Log.d(TAG, "Socket servers stopped.");
    }

    private void runControlServer() {
        try {
            controlSocket = new ServerSocket(Constants.OFF_PORT_CONTROL);
            Log.d(TAG, "Control server running on port " + Constants.OFF_PORT_CONTROL);
            while (isRunning) {
                Socket client = controlSocket.accept();
                String remoteIp = client.getInetAddress().getHostAddress();
                connectionManager.updatePeerIpFromAccept(remoteIp);
                
                // Start control handler thread
                new Thread(new MessageReceiver(context, client, true)).start();
            }
        } catch (IOException e) {
            Log.d(TAG, "Control server closed or failed: " + e.getMessage());
        }
    }

    private void runTextServer() {
        try {
            textSocket = new ServerSocket(Constants.OFF_PORT_TEXT);
            Log.d(TAG, "Text communication server running on port " + Constants.OFF_PORT_TEXT);
            while (isRunning) {
                Socket client = textSocket.accept();
                String remoteIp = client.getInetAddress().getHostAddress();
                connectionManager.updatePeerIpFromAccept(remoteIp);

                // Start chat handler thread
                new Thread(new MessageReceiver(context, client, false)).start();
            }
        } catch (IOException e) {
            Log.d(TAG, "Text server closed or failed: " + e.getMessage());
        }
    }

    private void runFileServer() {
        try {
            fileSocket = new ServerSocket(Constants.OFF_PORT_FILE);
            Log.d(TAG, "File Transfer server running on port " + Constants.OFF_PORT_FILE);
            while (isRunning) {
                Socket client = fileSocket.accept();
                String remoteIp = client.getInetAddress().getHostAddress();
                connectionManager.updatePeerIpFromAccept(remoteIp);

                // Start file receiver thread
                new Thread(new FileReceiver(context, client)).start();
            }
        } catch (IOException e) {
            Log.d(TAG, "File server closed or failed: " + e.getMessage());
        }
    }
}
