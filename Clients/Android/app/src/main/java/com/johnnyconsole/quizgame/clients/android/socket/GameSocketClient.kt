package com.johnnyconsole.quizgame.clients.android.socket

import com.johnnyconsole.quizgame.clients.android.enums.Role
import okhttp3.*

class GameSocketClient(
    private val onConnected: () -> Unit,
    private val onMessageReceived: (String) -> Unit,
    private val onDisconnected: (String) -> Unit
) {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null

    fun connect(url: String) {
        val request = Request.Builder()
            .url(url)
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onConnected()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                onMessageReceived(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                onDisconnected(reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onDisconnected(t.message ?: "Connection failed")
            }
        }

        webSocket = client.newWebSocket(request, listener)
    }

    fun sendMessage(message: String) {
        webSocket?.send(message)
    }

    fun disconnect() {
        webSocket?.close(1000, "Client closed connection")
    }

    fun registerRole(role: Role) {
        sendMessage("REGISTER:${role.name}")
    }
}
