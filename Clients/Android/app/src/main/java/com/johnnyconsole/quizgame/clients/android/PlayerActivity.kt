package com.johnnyconsole.quizgame.clients.android

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.johnnyconsole.quizgame.clients.android.databinding.ActivityPlayerBinding
import com.johnnyconsole.quizgame.clients.android.enums.Role

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var role: String

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        with(binding) {
            setContentView(root)
            ViewCompat.setOnApplyWindowInsetsListener(main) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

            role = intent.getStringExtra("role")!!
            tvRole.text = resources.getString(R.string.lb_role_text, role)
            tvRole.setTextColor(resources.getColor(
                    if(role == "Challenger") R.color.primary else android.R.color.holo_red_dark,
                theme))

        }
    }
}