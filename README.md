# Quiz Game
A client-server application to simulate the table section of ITV's The Chase. This game does not implement Chaser offers and does not keep score.

### Installation
To install the quiz game server, download the Server directory and open the maven project in any IDE. The server requires Java 26 to run. The server can then be started using the terminal command `mvn javafx:run`.

The client apps are available for Android (Requires Android Studio Rabbit 2 and Android 11 or later) and iOS (requires Xcode and iOS 27 or later).

### Usage
The server application will provide the endpoint for the client apps to connect to. This is usually the server device's local IP Address. The address is prefixed with _ws://_ to specify connection via the WebSocket protocol. The game server uses port 50505 by default - this can be changed by modifying the **PORT** property of the GameState class before running the build. As stations connect, the labels for each station will change from red "Disconnected" state to green and bold "Connected" state. Once all three stations are connected, the "Begin Game" button will be visible. Once clicked, the server will query the Open Trivia API to retrieve 100 random questions before starting the game and broadcasting the first question to all stations.

The main screen for the client apps asks for the endpoint address and the role that the station will play in the game. The address must be prefixed with _ws://_ and suffixed with the port number (i.e., it should look like this: **ws://\<endpoint-address\>:\<port\>**). Stations can choose between one of three roles: **Challenger**, **Chaser**, and **Host**. The Challenger and Chaser roles are both players of the game (the role differences determine the order for revealing answers (_challenger -> correct -> chaser_). Other than this difference, both roles play the game in the same manner. The host station has controls to reveal the three answers (once both players have locked in) using the order above, to reveal the next question, and to end the game.

### End Conditions
The game ends when:
1. The question queue is empty because there are no more questions left, or
2. The host station manually ends the game, or
3. The station disconnects (on a client station), or
4. The server station is closed, severing all three connections at once.

