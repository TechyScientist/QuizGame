package com.johnnyconsole.quizgame.server;

import com.johnnyconsole.quizgame.server.enums.Role;
import com.johnnyconsole.quizgame.server.model.Question;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class GameState {

    public static final Map<Role, WebSocket> stations = new ConcurrentHashMap<>();
    public static final int PORT = 50505;
    public static GameServer wsServer;
    public static Queue<Question> questionQueue = null;
    public static Question currentQuestion = null;

}
