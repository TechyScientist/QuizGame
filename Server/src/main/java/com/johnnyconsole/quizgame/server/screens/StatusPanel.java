package com.johnnyconsole.quizgame.server.screens;

import com.johnnyconsole.quizgame.server.GameServer;
import com.johnnyconsole.quizgame.server.enums.Role;
import com.johnnyconsole.quizgame.server.model.Question;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import org.java_websocket.WebSocket;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;

import static com.johnnyconsole.quizgame.server.GameState.*;
import static com.johnnyconsole.quizgame.server.enums.Role.*;
import static com.johnnyconsole.quizgame.server.model.QuestionService.fetchQuestionsAsync;

public class StatusPanel extends Application {

    private final Label hostStatus = new Label("Disconnected"),
            challengerStatus = new Label("Disconnected"),
            chaserStatus = new Label("Disconnected");
    private final Button start = new Button("Begin Game"),
            exit = new Button("Exit");

    @Override
    public void start(Stage ps) {
        GridPane root = new GridPane();
        root.setPadding(new Insets(20));
        root.setHgap(10);
        root.setVgap(root.getHgap());

        Label endpoint = new Label("ws://" + getEndpoint() + ":" + PORT + "/");
        endpoint.setStyle("-fx-text-fill: #162da3; -fx-font-weight: bold;");

        start.setMaxWidth(Double.MAX_VALUE);
        exit.setMaxWidth(Double.MAX_VALUE);
        start.setDisable(true);
        start.setOnAction(_ -> beginGame());
        exit.setOnAction(_ -> Platform.exit());
        updateUI();

        root.addRow(0, new Label("Endpoint:"), endpoint);
        root.addRow(1, new Label("Host Station:"), hostStatus);
        root.addRow(2, new Label("Challenger Station:"), challengerStatus);
        root.addRow(3, new Label("Chaser Station:"), chaserStatus);
        root.add(start, 0, 4, 2, 1);
        root.add(exit, 0, 5, 2, 1);

        // Start the WebSocket server in the background
        wsServer = new GameServer(PORT, this);
        wsServer.start();

        // Ensure the background server thread stops when you close the window
        ps.setOnCloseRequest(_ -> Platform.exit());

        Scene scene = new Scene(root);
        ps.setScene(scene);
        ps.setTitle("Quiz Game: Status Panel");
        ps.show();
    }

    @Override
    public void stop() {
        try {
            if (wsServer != null) {
                wsServer.stop(1000);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        System.exit(0);
    }

    public boolean register(Role role, WebSocket socket) {
        // putIfAbsent atomically checks and inserts, returning null if the slot was empty
        if (stations.putIfAbsent(role, socket) == null) {
            Platform.runLater(this::updateUI);
            return true;
        }
        return false;
    }

    public void deregister(WebSocket socket) {
        boolean removed = stations.entrySet().removeIf(entry -> entry.getValue().equals(socket));
        if (removed) {
            Platform.runLater(this::updateUI);
        }
    }

    public void handleGameMessage(WebSocket sender, String message) {
        // Identify which role sent the message
        Role senderRole = stations.entrySet().stream()
                .filter(entry -> entry.getValue().equals(sender))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (senderRole == null) return; // Ignore unregistered sockets

        Platform.runLater(() -> {
            // Handle incoming commands from stations here!
            // Example: "ANSWER:A" from PLAYER/CHASER, or "COMMAND:REVEAL" / "COMMAND:NEXT" from HOST
            System.out.println(senderRole + " sent: " + message);
        });
    }

    private void beginGame() {
        start.setDisable(true);
        start.setText("Fetching Questions...");

        fetchQuestionsAsync(100).thenAccept(questions -> Platform.runLater(() -> {
            if (questions.isEmpty()) {
                start.setText("Fetch Failed - Retry");
                start.setDisable(stations.size() < 3);
                return;
            }

            questionQueue = questions;
            start.setText("Game In Progress...");
            stations.values().forEach(socket -> socket.send("GAME_START"));
            sendNextQuestion();
        }));
    }

    private void updateUI() {
        updateLabel(hostStatus, stations.containsKey(HOST));
        updateLabel(challengerStatus, stations.containsKey(CHALLENGER));
        updateLabel(chaserStatus, stations.containsKey(CHASER));
        start.setDisable(stations.size() < 3 || questionQueue != null);
    }

    private void updateLabel(Label label, boolean isConnected) {
        if (isConnected) {
            label.setText("Connected");
            label.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
        } else {
            label.setText("Disconnected");
            label.setStyle("-fx-text-fill: #dc2626;");
        }
    }

    public static String getEndpoint() {
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void sendNextQuestion() {
        if(questionQueue == null || questionQueue.isEmpty()) {
            stations.values().forEach(socket -> {socket.send("GAME_END"); });
            return;
        }
        currentQuestion =  questionQueue.remove();
        Map<String, String> answers = currentQuestion.getOptions();
        stations.values().forEach(socket -> socket.send(
                String.format("QUESTION:%s\\%s\\%s\\%s",
                        currentQuestion.getQuestionText(),
                        answers.get("A"), answers.get("B"), answers.get("C"))
        ));
    }
}