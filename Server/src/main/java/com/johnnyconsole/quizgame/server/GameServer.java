package com.johnnyconsole.quizgame.server;

import com.johnnyconsole.quizgame.server.enums.Role;
import com.johnnyconsole.quizgame.server.screens.StatusPanel;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class GameServer extends WebSocketServer {

    private final StatusPanel statusPanel;

    public GameServer(int port, StatusPanel statusPanel) {
        super(new InetSocketAddress(port));
        this.statusPanel = statusPanel;
        // Prevents "Address already in use" errors when restarting the app quickly
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection from: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received: " + message);

        // Expected handshake format from apps: "REGISTER:HOST", "REGISTER:PLAYER", or "REGISTER:OPPONENT"
        if (message.startsWith("REGISTER:")) {
            String roleStr = message.substring("REGISTER:".length()).trim().toUpperCase();
            try {
                Role role = Role.valueOf(roleStr);
                boolean accepted = statusPanel.register(role, conn);
                if (accepted) {
                    conn.send("REGISTERED:" + role.name());
                } else {
                    conn.send("ERROR:ROLE_TAKEN");
                }
            } catch (IllegalArgumentException e) {
                conn.send("ERROR:INVALID_ROLE");
            }
        } else {
            // Pass gameplay messages (like answers or host button clicks) to the main Server class
            statusPanel.handleGameMessage(conn, message);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress());
        statusPanel.deregister(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        if (conn != null) {
            statusPanel.deregister(conn);
        }
    }

    @Override
    public void onStart() {
        System.out.println("Quiz WebSocket Server started successfully on port " + getPort());
    }
}
