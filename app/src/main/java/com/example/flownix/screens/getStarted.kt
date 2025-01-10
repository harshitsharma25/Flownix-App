package com.example.flownix.screens

import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import com.example.flownix.R
import com.example.flownix.databinding.ActivityGetStartedBinding

class GetStarted : AppCompatActivity() {

    private var binding : ActivityGetStartedBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding?.root)


        // hide all screen decorations (such as the status bar) while this window is displayed.
        // This allows the window to use the entire display space for itself
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            window.statusBarColor = Color.TRANSPARENT
        }

        binding?.btnSignUp?.setOnClickListener{
            startActivity(Intent(this,SignUpActivity::class.java))
        }
        binding?.btnSignin?.setOnClickListener{
            startActivity(Intent(this,SigninActivity::class.java))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}