package com.johnnyconsole.quizgame.clients.android

import android.animation.ObjectAnimator
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.johnnyconsole.quizgame.clients.android.databinding.ActivityHostBinding
import com.johnnyconsole.quizgame.clients.android.enums.Role
import com.johnnyconsole.quizgame.clients.android.socket.SocketManager
import com.johnnyconsole.quizgame.clients.android.socket.SocketManager.client

class HostActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHostBinding

    private var challengerLocked = false
    private var chaserLocked = false
    private var timerExpired = false

    private var timerAnimator: ObjectAnimator? = null
    private var mediaPlayer: MediaPlayer? = null

    // Track per-option state so CHALLENGER (fill) and CHASER (border) never overwrite each other
    private val optionFillColors = mutableMapOf(
        "A" to R.color.color_purple,
        "B" to R.color.color_purple,
        "C" to R.color.color_purple
    )
    private val optionHasRedBorder = mutableMapOf(
        "A" to false,
        "B" to false,
        "C" to false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityHostBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        with(binding) {
            setContentView(root)
            ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            val optionViews: Map<String, TextView> = mapOf(
                "A" to tvOptionA,
                "B" to tvOptionB,
                "C" to tvOptionC
            )

            // Optional: drop timer_tick.mp3 into res/raw/ to enable countdown audio
            val soundResId = R.raw.timer
            if (soundResId != 0) {
                mediaPlayer = MediaPlayer.create(this@HostActivity, soundResId)
            }

            client.onMessageReceived = { message ->
                runOnUiThread {
                    Log.d("QuizGame", "Host Received: $message")

                    if (message == "GAME_END") {
                        stopCountdown()
                        client.disconnect()
                        finish()
                    }
                    else if (message.startsWith("QUESTION:")) {
                        stopCountdown()
                        challengerLocked = false
                        chaserLocked = false
                        timerExpired = false

                        val parts = message.substring(message.indexOf(':') + 1).split("\\")
                        tvQuestionText.text = parts[0]
                        tvOptionA.text = "A: ${parts[1]}"
                        tvOptionB.text = "B: ${parts[2]}"
                        tvOptionC.text = "C: ${parts[3]}"

                        // Reset player lock-in pills
                        tvChallengerStatus.text = resources.getString(R.string.lb_player_status,
                            "Challenger", "Waiting")
                        tvChallengerStatus.backgroundTintList =
                            AppCompatResources.getColorStateList(this@HostActivity, android.R.color.darker_gray)

                        tvChaserStatus.text = resources.getString(R.string.lb_player_status,
                            "Chaser", "Waiting")
                        tvChaserStatus.backgroundTintList =
                            AppCompatResources.getColorStateList(this@HostActivity, android.R.color.darker_gray)

                        // Lock all flow buttons until players answer
                        btRevealChallenger.isEnabled = false
                        btRevealChaser.isEnabled = false
                        btRevealCorrect.isEnabled = false
                        btNextQuestion.isEnabled = false
                        // Inside QUESTION: reset
                        listOf("A", "B", "C").forEach { letter ->
                            optionFillColors[letter] = R.color.color_purple
                            optionHasRedBorder[letter] = false
                            renderOptionCard(letter, optionViews[letter]!!)
                        }
                    }
                    else if (message.startsWith("LOCKED:")) {
                        val lockedRole = Role.valueOf(message.substring(message.indexOf(':') + 1))
                        if (lockedRole == Role.CHALLENGER) {
                            challengerLocked = true
                            tvChallengerStatus.text = resources.getString(R.string.lb_player_status,
                                "Challenger", "Locked")
                            tvChallengerStatus.backgroundTintList =
                                AppCompatResources.getColorStateList(this@HostActivity, R.color.color_blue)
                            btRevealChallenger.isEnabled = true
                        } else if (lockedRole == Role.CHASER) {
                            chaserLocked = true
                            tvChaserStatus.text = resources.getString(R.string.lb_player_status,
                                "Chaser", "Locked")
                            tvChaserStatus.backgroundTintList =
                                AppCompatResources.getColorStateList(this@HostActivity, R.color.color_red)
                            btRevealChaser.isEnabled = true
                        }
                        updateCorrectButtonState()
                    }
                    else if (message == "TIMER_START") {
                        startCountdown()
                    }
                    else if (message == "TIMER_STOP") {
                        stopCountdown()
                    }
                    else if (message == "TIMER_UP") {
                        stopCountdown()
                        timerExpired = true
                        if (!challengerLocked) {
                            tvChallengerStatus.text = resources.getString(R.string.lb_player_status,
                                "Challenger", "Time Up")
                        }
                        if (!chaserLocked) {
                            tvChaserStatus.text = resources.getString(R.string.lb_player_status,
                                "Chaser", "Time Up")
                        }
                        updateCorrectButtonState()
                    }
                    else if (message.startsWith("REVEAL:")) {
                        val parts = message.substring(message.indexOf(':') + 1).split("\\")
                        val roleType = parts[0]
                        val letter = parts[1].trim()
                        val targetView = optionViews[letter] ?: return@runOnUiThread

                        when (roleType) {
                            "CHALLENGER" -> optionFillColors[letter] = R.color.color_blue
                            "CHASER"     -> optionHasRedBorder[letter] = true
                            "CORRECT"    -> optionFillColors[letter] = R.color.color_green
                        }
                        renderOptionCard(letter, targetView)
                    }
                }
            }

            client.onDisconnected = {
                runOnUiThread {
                    stopCountdown()
                    SocketManager.isConnected = false
                    finish()
                }
            }

            btRevealChallenger.setOnClickListener {
                client.sendMessage("REVEAL:CHALLENGER")
                it.isEnabled = false
            }

            btRevealChaser.setOnClickListener {
                client.sendMessage("REVEAL:CHASER")
                it.isEnabled = false
            }

            btRevealCorrect.setOnClickListener {
                client.sendMessage("REVEAL:CORRECT")
                it.isEnabled = false
                btNextQuestion.isEnabled = true
            }

            btNextQuestion.setOnClickListener {
                client.sendMessage("NEXT")
                it.isEnabled = false
            }

            btEndGame.setOnClickListener {
                AlertDialog.Builder(this@HostActivity)
                    .setTitle("End Game?")
                    .setMessage("This will disconnect all stations and end the current session.")
                    .setPositiveButton("End Game") { _, _ ->
                        client.sendMessage("END")
                        client.disconnect()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun updateCorrectButtonState() {
        val roundFinished = (challengerLocked && chaserLocked) || timerExpired
        if (roundFinished) {
            binding.btRevealCorrect.isEnabled = true
        }
    }

    private fun startCountdown() {
        stopCountdown()
        binding.pbLockoutTimer.progress = 500
        binding.pbLockoutTimer.visibility = VISIBLE

        timerAnimator = ObjectAnimator.ofInt(binding.pbLockoutTimer, "progress", 500, 0).apply {
            duration = 5000L
            interpolator = LinearInterpolator()
            start()
        }

        mediaPlayer?.let {
            it.seekTo(0)
            it.start()
        }
    }

    private fun stopCountdown() {
        timerAnimator?.cancel()
        timerAnimator = null
        binding.pbLockoutTimer.visibility = INVISIBLE

        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                it.seekTo(0)
            }
        }
    }

    override fun onDestroy() {
        stopCountdown()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    private fun renderOptionCard(letter: String, view: TextView) {
        val fillRes = optionFillColors[letter] ?: R.color.color_purple
        val drawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f * resources.displayMetrics.density
            setColor(ContextCompat.getColor(this@HostActivity, fillRes))
            if (optionHasRedBorder[letter] == true) {
                val strokePx = (4 * resources.displayMetrics.density).toInt()
                setStroke(strokePx, ContextCompat.getColor(this@HostActivity, R.color.color_red))
            }
        }
        view.background = drawable
    }
}