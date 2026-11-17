package com.johnnyconsole.quizgame.clients.android

import android.content.Intent
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
import com.johnnyconsole.quizgame.clients.android.socket.SocketManager.client
import com.johnnyconsole.quizgame.clients.android.socket.SocketManager.isConnected

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var connecting = false

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
                    R.drawable.dr_border_rounded_blue
                )
                rgRoleSelect.background = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.dr_border_rounded_blue
                )

                if (!isConnected && !connecting) {
                    connecting = true
                    setInputsEnabled(false)
                    pbProgressIndicator.visibility = VISIBLE

                    val role = when(rgRoleSelect.checkedRadioButtonId) {
                        rbPlayerStation.id -> "PLAYER"
                        rbChaserStation.id -> "CHASER"
                        else -> "HOST"
                    }

                    client.apply {
                        onConnected = {
                            // Send the registration command immediately upon handshake
                            registerRole(Role.valueOf(role))

                            runOnUiThread {
                                connecting = false
                                isConnected = true
                                btConnect.text = getString(R.string.lb_disconnect)
                                pbProgressIndicator.visibility = GONE
                                etEndpoint.background = AppCompatResources.getDrawable(
                                    this@MainActivity,
                                    R.drawable.dr_border_rounded_green
                                )
                                rgRoleSelect.background = AppCompatResources.getDrawable(
                                    this@MainActivity,
                                    R.drawable.dr_border_rounded_green
                                )
                            }
                        }

                        onMessageReceived = { message ->
                            runOnUiThread {
                                Log.d("QuizGame", "Received Message: $message")

                                when {
                                    message == "REGISTERED:$role" -> {
                                        // Role accepted by server! Launch the appropriate Activity
                                        val targetActivity = when (Role.valueOf(role)) {
                                            Role.HOST -> HostActivity::class.java
                                            else -> PlayerActivity::class.java
                                        }
                                        startActivity(Intent(this@MainActivity, targetActivity))
                                    }

                                    message == "ERROR:ROLE_TAKEN" -> {
                                        // Server rejected the role; disconnect so the user can pick another
                                        client.disconnect()
                                        rgRoleSelect.background = AppCompatResources.getDrawable(
                                            this@MainActivity,
                                            R.drawable.dr_border_rounded_red
                                        )
                                        Snackbar.make(
                                            this@MainActivity, main,
                                            "The $role station is already taken!",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }

                        onDisconnected = { error ->
                            runOnUiThread {
                                btConnect.text = getString(R.string.lb_connect)
                                Log.e("QuizGame", "Error: $error")
                                connecting = false
                                isConnected = false
                                setInputsEnabled(true)
                                Snackbar.make(
                                    this@MainActivity, main,
                                    "Disconnected from game endpoint",
                                    Snackbar.LENGTH_LONG
                                ).show()
                                if (error != "Client closed connection") {
                                    etEndpoint.background = AppCompatResources.getDrawable(
                                        this@MainActivity,
                                        R.drawable.dr_border_rounded_red
                                    )
                                    rgRoleSelect.background = AppCompatResources.getDrawable(
                                        this@MainActivity,
                                        R.drawable.dr_border_rounded_red
                                    )
                                }
                                pbProgressIndicator.visibility = GONE
                            }
                        }
                        try {
                            connect("${etEndpoint.text}")
                        } catch (ex: Exception) {
                            runOnUiThread {
                                Snackbar.make(main, "Error connecting to game endpoint${ex.message?.let { ": $it" } ?: ""}",
                                    Snackbar.LENGTH_LONG).show()
                                Log.e("ConnectionError", ex.stackTrace.contentToString())
                                connecting = false
                                setInputsEnabled(true)
                                etEndpoint.background = AppCompatResources.getDrawable(this@MainActivity, R.drawable.dr_border_rounded_red)
                                pbProgressIndicator.visibility = GONE
                            }
                        }

                    }
                }
                else if(!isConnected){
                    Snackbar.make(main, "Connection in Progress, Please wait...",
                        Snackbar.LENGTH_LONG).show()
                }
                else {
                    client.disconnect()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        with(binding) {
            setInputsEnabled(!isConnected)
            if (isConnected) {
                btConnect.text = getString(R.string.lb_disconnect)
            } else {
                btConnect.text = getString(R.string.lb_connect)
                etEndpoint.background = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.dr_border_rounded_blue
                )
                rgRoleSelect.background = AppCompatResources.getDrawable(
                    this@MainActivity,
                    R.drawable.dr_border_rounded_blue
                )
            }
        }
    }

    private fun setInputsEnabled(enabled: Boolean) {
        with(binding) {
            etEndpoint.isEnabled = enabled
            rgRoleSelect.isEnabled = enabled
            rbHostStation.isEnabled = enabled
            rbPlayerStation.isEnabled = enabled
            rbChaserStation.isEnabled = enabled
        }
    }
}