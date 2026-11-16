package com.johnnyconsole.quizgame.clients.android

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.johnnyconsole.quizgame.clients.android.databinding.ActivityMainBinding
import android.view.View.VISIBLE
import android.view.View.GONE
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.snackbar.Snackbar
import com.johnnyconsole.quizgame.clients.android.enums.Role
import com.johnnyconsole.quizgame.clients.android.socket.GameSocketClient
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var socket: GameSocketClient
    private var connecting = false
    private var connected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        with(binding) {
            setContentView(root)
            ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            btConnect.setOnClickListener { _ ->
                etEndpoint.background = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.drawable_box_border
                )
                rgRoleSelect.background = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.drawable_box_border
                )

                if (!connected && !connecting) {
                    connecting = true
                    pbProgressIndicator.visibility = VISIBLE

                    val role = when(rgRoleSelect.checkedRadioButtonId) {
                        rbPlayerStation.id -> "PLAYER"
                        rbChaserStation.id -> "CHASER"
                        else -> "HOST"
                    }

                    socket = GameSocketClient(
                        onConnected = {
                            runOnUiThread {
                                connecting = false
                                connected = true
                                btConnect.text = getString(R.string.lb_disconnect)
                                pbProgressIndicator.visibility = GONE
                                etEndpoint.background = AppCompatResources.getDrawable(
                                    this@MainActivity,
                                    R.drawable.drawable_connected_box_border
                                )
                                rgRoleSelect.background = AppCompatResources.getDrawable(
                                    this@MainActivity,
                                    R.drawable.drawable_connected_box_border
                                )
                                Snackbar.make(this@MainActivity, main,
                                    "Connected to game endpoint as: $role",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        },
                        onMessageReceived = { jsonMessage ->
                            runOnUiThread {
                                Log.d("QuizGame", "Received Message: $jsonMessage")
                                // Parse incoming quiz question or game state and update UI

                            }
                        },
                        onDisconnected = { error ->
                            runOnUiThread {
                                btConnect.text = getString(R.string.lb_connect)
                                Log.e("QuizGame", "Error: $error")
                                connected = false
                                Snackbar.make(this@MainActivity, main,
                                    "Disconnected from game endpoint",
                                    Snackbar.LENGTH_LONG).show()
                                if(error != "Client closed connection") {
                                    etEndpoint.background = AppCompatResources.getDrawable(
                                        this@MainActivity,
                                        R.drawable.drawable_error_box_border
                                    )
                                    rgRoleSelect.background = AppCompatResources.getDrawable(
                                        this@MainActivity,
                                        R.drawable.drawable_error_box_border
                                    )
                                }

                            }
                        }
                    )

                    socket.connect("${etEndpoint.text}")
                    socket.registerRole(Role.valueOf(role))

                }
                else if(!connected){
                    Snackbar.make(main, "Connection in Progress, Please wait...",
                        Snackbar.LENGTH_LONG).show()
                }

                else {
                    socket.disconnect()

                }

            }
        }
    }
}