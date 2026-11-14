package com.johnnyconsole.quizgame.server;

import com.johnnyconsole.quizgame.server.enums.Role;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameState {

    public static final Map<Role, WebSocket> stations = new ConcurrentHashMap<>();
    public static final int PORT = 50505;
    public static GameServer wsServer;

}
