package com.example.flownix.screens

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.WindowInsets
import android.view.WindowManager
import com.example.flownix.R
import com.example.flownix.databinding.ActivitySplashScreenBinding
import com.example.flownix.firebase.FirestoreClass


class SplashScreen : AppCompatActivity() {

    private var binding : ActivitySplashScreenBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
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

        val typeFace : Typeface = Typeface.createFromAsset(assets,"ArsenalSC_Bold.ttf")
        binding?.tvAppName?.typeface = typeFace

        Handler().postDelayed({
            var currentUserID = FirestoreClass().getCurrentUserId()

            if(currentUserID != null){
                startActivity(Intent(this,MainActivity::class.java))
            }
            else{
                startActivity(Intent(this,GetStarted::class.java))
            }

            finish()
        } , 2000)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}