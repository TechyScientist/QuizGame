package com.johnnyconsole.quizgame.clients.android

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.johnnyconsole.quizgame.clients.android.databinding.ActivityResponseBinding
import com.johnnyconsole.quizgame.clients.android.enums.Role
import com.johnnyconsole.quizgame.clients.android.socket.SocketManager
import com.johnnyconsole.quizgame.clients.android.socket.SocketManager.client

class ResponseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResponseBinding
    private lateinit var role: String
    private lateinit var defaultBackgroundA: Drawable
    private lateinit var defaultBackgroundB: Drawable
    private lateinit var defaultBackgroundC: Drawable

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityResponseBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        with(binding) {
            setContentView(root)
            ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            val buttons = mapOf("A" to btAnswerA, "B" to btAnswerB, "C" to btAnswerC)

            defaultBackgroundA = btAnswerA.background
            defaultBackgroundB = btAnswerB.background
            defaultBackgroundC = btAnswerC.background
            role = intent.getStringExtra("role")!!
            tvRole.text = resources.getString(R.string.lb_role_text, role)
            tvRole.setTextColor(resources.getColor(
                    if(role == "Challenger") R.color.color_blue else R.color.color_red, theme))

            client.onMessageReceived = { message ->
                runOnUiThread {
                    Log.d("QuizGame", "Message Received: $message")
                    if (message == "GAME_START") {
                        pbProgressIndicator.visibility = INVISIBLE
                    }
                    else if (message == "GAME_END") {
                        client.disconnect()
                        finish()
                    }
                    else if (message.startsWith("QUESTION:")) {
                        val parts = message.substring(message.indexOf(':') + 1)
                            .split("\\")
                        tvQuestionText.text = parts[0]
                        tvAnswerAText.text = parts[1]
                        tvAnswerBText.text = parts[2]
                        tvAnswerCText.text = parts[3]
                        btAnswerA.background = defaultBackgroundA
                        btAnswerB.background = defaultBackgroundB
                        btAnswerC.background = defaultBackgroundC
                        btAnswerA.backgroundTintList = AppCompatResources.getColorStateList(
                            this@ResponseActivity, R.color.color_purple)
                        btAnswerB.backgroundTintList = AppCompatResources.getColorStateList(
                            this@ResponseActivity, R.color.color_purple)
                        btAnswerC.backgroundTintList = AppCompatResources.getColorStateList(
                            this@ResponseActivity, R.color.color_purple)
                        tvChallengerLocked.visibility = INVISIBLE
                        tvChaserLocked.visibility = INVISIBLE
                        tvTimeUp.visibility = INVISIBLE
                        btAnswerA.isEnabled = true
                        btAnswerB.isEnabled = true
                        btAnswerC.isEnabled = true

                    }
                    else if (message.startsWith("REVEAL:")) {
                        val parts = message.substring(message.indexOf(":") + 1)
                            .split("\\")
                        when (parts[0]) {
                            "CHALLENGER" ->  buttons[parts[1]]!!.backgroundTintList =
                                AppCompatResources.getColorStateList(this@ResponseActivity,
                                    R.color.color_blue)

                            "CHASER" -> buttons[parts[1]]!!.background =
                                AppCompatResources.getDrawable(this@ResponseActivity,
                                    R.drawable.dr_border_rounded_red)

                            else -> buttons[parts[1]]!!.backgroundTintList =
                                AppCompatResources.getColorStateList(this@ResponseActivity,
                                    R.color.color_green)
                        }
                    }
                    else if(message.startsWith("LOCKED:")) {
                        val locked = Role.valueOf(message.substring(message.indexOf(":") + 1))
                        if(locked == Role.CHALLENGER) {
                            tvChallengerLocked.visibility = VISIBLE
                        }
                        else {
                            tvChaserLocked.visibility = VISIBLE
                        }
                    }
                    else if(message == "TIMER_START") {

                    }
                    else if(message == "TIMER_UP") {
                        tvTimeUp.visibility = VISIBLE
                        buttons.values.forEach { it.isEnabled = false }
                    }
                }
            }

            client.onDisconnected = {
                runOnUiThread {
                    SocketManager.isConnected = false
                    finish()
                }
            }

            buttons.forEach { (letter, button) ->
                button.setOnClickListener { _ ->
                    client.sendMessage("ANSWER:${role.uppercase()}\\$letter")
                    buttons.forEach {(_, bt) -> bt.isEnabled = false }
                }
            }
        }
    }
}