# QuizGame
A client-server application to simulate a quiz show, similar to ITV's The Chase.

### Installation
To install the quiz game server, download the Server directory and open the maven project in any IDE. The server requires Java 26 to run. The server can then be started using the command ```mvn javafx:run.

The client apps are available for Android (Requires Android Studio Rabbit 2 and Android 11 or later) and iOS (requires Xcode and iOS 27 or later).

### Usage
The server application will provide the endpoint for the client apps to connect to. This is usually the server device's local IP Address. The address is prefixed with _ws://_ to specify connection via the WebSocket protocol. The game server uses port 50505 by default - this can be changed by modifying the **PORT** property of the GameState class before running the build. As stations connect, the labels for each station will change from red "Disconnected" state to green and bold "Connected" state. Once all three stations are connected, the "Begin Game" button will be visible. Once clicked, the server will query the Open Trivia API to retrieve 100 random questions before starting the game and broadcasting the first question to all stations.


