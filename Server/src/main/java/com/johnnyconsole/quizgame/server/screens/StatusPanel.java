package com.johnnyconsole.quizgame.server.screens;

import com.johnnyconsole.quizgame.server.GameServer;
import com.johnnyconsole.quizgame.server.enums.Role;

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

public class StatusPanel extends Application {

    private final Label hostStatus = new Label("Disconnected"),
            playerStatus = new Label("Disconnected"),
            opponentStatus = new Label("Disconnected");
    private final Button start = new Button("Begin Game");

    @Override
    public void start(Stage ps) {
        GridPane root = new GridPane();
        root.setPadding(new Insets(20));
        root.setHgap(10);
        root.setVgap(root.getHgap());

        Label endpoint = new Label("ws://" + getEndpoint() + ":" + PORT + "/");
        endpoint.setStyle("-fx-text-fill: #162DA3FF; -fx-font-weight: bold;");

        start.setMaxWidth(Double.MAX_VALUE);
        start.setDisable(true);
        start.setOnAction(e -> beginGame());
        updateUI();

        root.addRow(0, new Label("Endpoint:"), endpoint);
        root.addRow(1, new Label("Host Station:"), hostStatus);
        root.addRow(2, new Label("Player Station:"), playerStatus);
        root.addRow(3, new Label("Opponent Station:"), opponentStatus);
        root.add(start, 0, 4, 2, 1);

        // Start the WebSocket server in the background
        wsServer = new GameServer(PORT, this);
        wsServer.start();

        // Ensure the background server thread stops when you close the window
        ps.setOnCloseRequest(e -> {
            try {
                wsServer.stop(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        });

        Scene scene = new Scene(root);
        ps.setScene(scene);
        ps.setTitle("eQuiz Server App");
        ps.show();
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
            // Example: "ANSWER:A" from PLAYER/OPPONENT, or "COMMAND:REVEAL" / "COMMAND:NEXT" from HOST
            System.out.println(senderRole + " sent: " + message);
        });
    }

    private void beginGame() {
        start.setDisable(true);
        // Broadcast to all connected stations that the game is starting
        stations.values().forEach(socket -> socket.send("GAME_START"));
        // TODO: Fetch first question and send to stations
    }

    private void updateUI() {
        updateLabel(hostStatus, stations.containsKey(Role.HOST));
        updateLabel(playerStatus, stations.containsKey(Role.PLAYER));
        updateLabel(opponentStatus, stations.containsKey(Role.OPPONENT));
        start.setDisable(stations.size() < 3);
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
}