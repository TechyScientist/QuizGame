package com.johnnyconsole.quizgame.clients.android

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.johnnyconsole.quizgame.clients.android.databinding.ActivityMainBinding
import android.view.View.VISIBLE
import android.view.View.GONE
import com.google.android.material.snackbar.Snackbar
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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
                if (!connecting) {
                    connecting = true
                    pbProgressIndicator.visibility = VISIBLE

                    val role = when(rgRoleSelect.checkedRadioButtonId) {
                        rbPlayerStation.id -> "PLAYER"
                        rbChaserStation.id -> "CHASER"
                        else -> "HOST"
                    }

                    var conn = URL("${etEndpoint.text}/register/$role").openConnection() as HttpURLConnection

                }
                else {
                    Snackbar.make(main, "Connection in Progress, Please wait...",
                        Snackbar.LENGTH_LONG).show()
                }


            }
        }
    }
}