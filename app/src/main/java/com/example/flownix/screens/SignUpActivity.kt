package com.example.flownix.screens

import android.graphics.Color
import android.health.connect.datatypes.units.Length
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.StringRes
import com.example.flownix.R
import com.example.flownix.databinding.ActivitySignUpBinding
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SignUpActivity : BaseActivity() {

    private var binding: ActivitySignUpBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding?.root)

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

        // setting the toolBar for signUp
        setupActionBar()

        binding?.btnSignUp?.setOnClickListener{
            registerUser()
        }

    }

    private fun setupActionBar(){
        setSupportActionBar(binding?.toolbarFlownixSignUp)

        if(supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = "Sign Up"
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_icon)
        }

        binding?.toolbarFlownixSignUp?.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }


    // used for register the user in Firebase firestore
    fun userRegisteredSuccess(){
        Toast.makeText(this@SignUpActivity,"User Registered Successfully",Toast.LENGTH_SHORT).show()
        hideProgressDialog()
        FirebaseAuth.getInstance().signOut()
        finish()
    }


    // used for register the user in Authentication
    private fun registerUser(){
        val name : String = binding?.etName?.text.toString().trim{ it <= ' '}
        val email : String = binding?.etEmail?.text.toString().trim{ it <= ' '}
        val password : String = binding?.etPassword?.text.toString().trim{ it <= ' '}
        // Trims all whitespace characters and any other control characters
       // (e.g., newlines, tabs) with ASCII values less than or equal to a space (' ').

        if(validateForm(name,email,password)){
            //Toast.makeText(this@SignUpActivity,"Registered Successfully",Toast.LENGTH_SHORT).show()

            showProgressDialog(resources.getString(R.string.please_wait))

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email,password).addOnCompleteListener { task ->
                    hideProgressDialog()

                    if (task.isSuccessful) {
                        val firebaseUser: FirebaseUser = task.result!!.user!!
                        val registeredEmail = firebaseUser.email!!
                        val user = User(firebaseUser.uid,name,email)

                        FirestoreClass().registerUser(this,user)

                    } else {
                        Toast.makeText(
                            this,
                            "Registration Failed", Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }

        }
    }

    private fun validateForm(name:String, email: String, password: String) : Boolean {
        return when{
            name.isEmpty() -> {
                showErrorSnackBar("Please Enter Name")
                false
            }
            email.isEmpty() -> {
                showErrorSnackBar("Please Enter Email")
                false
            }
            password.isEmpty() -> {
                showErrorSnackBar("Please Enter Password")
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