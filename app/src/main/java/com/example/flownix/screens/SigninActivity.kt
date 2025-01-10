package com.example.flownix.screens

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import com.example.flownix.R
import com.example.flownix.databinding.ActivitySigninBinding
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.User
import com.google.firebase.auth.FirebaseAuth

class SigninActivity : BaseActivity() {

    private var binding : ActivitySigninBinding? = null
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySigninBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // setting the Auth
        auth = FirebaseAuth.getInstance()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insetsController = window.insetsController
            insetsController?.hide(WindowInsets.Type.statusBars())
            window.statusBarColor = Color.TRANSPARENT
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            window.statusBarColor = Color.TRANSPARENT
        }

        setActionBar()
        binding?.btnSignIn?.setOnClickListener{
            signInUser()
        }
    }


    private fun signInUser(){
        val email : String = binding?.etEmailSignin?.text.toString().trim{ it <= ' '}
        val password : String = binding?.etPasswordSignin?.text.toString().trim{ it <= ' '}

        if(validForm(email,password)){
            showProgressDialog(resources.getString(R.string.please_wait))

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email,password).addOnCompleteListener{ task ->
                hideProgressDialog()
                if(task.isSuccessful){
                    FirestoreClass().loadUserData(this)
                }
                else{
                    Toast.makeText(
                        baseContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    fun signInSuccess(loggedInUser: User) {
        hideProgressDialog()
        startActivity(Intent(this,MainActivity::class.java))
        finish()
    }

    private fun setActionBar(){
        setSupportActionBar(binding?.toolbarFlownixSignIn)

        if(supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = "Sign In"
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_icon)
        }

        binding?.toolbarFlownixSignIn?.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun validForm(email : String, password : String): Boolean {
        return when{
            email.isEmpty() -> {
                showErrorSnackBar("Enter Email")
                false
            }

            password.isEmpty() -> {
                showErrorSnackBar("Enter Password")
                false
            }

            else -> {
                true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}