package com.example.flownix.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.flownix.adapters.BoardItemsAdapter
import com.example.flownix.R
import com.example.flownix.databinding.ActivityMainBinding
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.Board
import com.example.flownix.models.User
import com.example.flownix.utils.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity() , NavigationView.OnNavigationItemSelectedListener {


    companion object{
        const val MY_PROFILE_REQUEST_CODE : Int = 11
        const val CREATE_BOARD_REQUEST_CODE : Int = 12
    }

    private lateinit var mUserName : String
    private lateinit var mSharedPreferences : SharedPreferences

    private var binding : ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setupActionBar()
        binding?.navView?.setNavigationItemSelectedListener(this)

        mSharedPreferences =
            this.getSharedPreferences(Constants.FLOWNIX_PREFERENCES, Context.MODE_PRIVATE)

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED,false)

        if(tokenUpdated){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this,true)

        } else {
//            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(this@MainActivity,){ instanceIdResult ->
//                updateFCMToken(instanceIdResult.token)
//            }
            // Use FirebaseMessaging to get the new FCM token
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                updateFCMToken(token)
            }.addOnFailureListener { e ->
                Log.e("FCM Token", "Failed to retrieve token", e)
            }
        }

        FirestoreClass().loadUserData(this,true)

        val fabButton : FloatingActionButton = findViewById(R.id.fab_create_board)
        fabButton.setOnClickListener {
            val intent = Intent(this,CreateBoardActivity::class.java)

            intent.putExtra(Constants.NAME,mUserName)
            startActivityForResult(intent,CREATE_BOARD_REQUEST_CODE)
        }

        // Disable StrictMode for network operations on the main thread (only for testing)
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)


    }


    fun populateBoardsListToUI (boardsList : ArrayList<Board>){
        hideProgressDialog()
        val rvBoardsList = findViewById<RecyclerView>(R.id.rvBoardsList)
        val tvNoBoardsAvailability = findViewById<TextView>(R.id.tvNoBoardsAvailable)

        hideProgressDialog()
        if(boardsList.size > 0){

            rvBoardsList.visibility = View.VISIBLE
            tvNoBoardsAvailability.visibility = View.GONE

            // Step 1 : set the layoutManager to Recycler View
            rvBoardsList.layoutManager = LinearLayoutManager(this)

            // Step 2 : Set that size of Recycler view will change or not on changing the adapter (optional)
            rvBoardsList.setHasFixedSize(true)

            // Step 3: Set the adapter to the Recycler View
            val adapter = BoardItemsAdapter(this,boardsList)
            rvBoardsList.adapter = adapter

            // Step 4: Set the onClick event
            adapter.setOnClickListener(object : BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity,TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID,model.documentId)
                    Log.d("board",model.documentId)
                    startActivity(intent)
                }
            })

        } else {
            rvBoardsList.visibility = View.GONE
            tvNoBoardsAvailability.visibility = View.VISIBLE
        }
    }

    private fun setupActionBar(){
        val actionBar  : Toolbar = findViewById(R.id.toolbar_main_activity)
        setSupportActionBar(actionBar)

        actionBar.setNavigationIcon(R.drawable.ic_action_navigation_menu)
        actionBar.setNavigationOnClickListener{
            toggleDrawer()
        }
    }

    private fun toggleDrawer(){
        if(binding?.drawerLayout!!.isDrawerOpen(GravityCompat.START)){
            binding?.drawerLayout!!.closeDrawer(GravityCompat.START)
        } else {
            binding?.drawerLayout!!.openDrawer(GravityCompat.START)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()

        if(binding?.drawerLayout!!.isDrawerOpen(GravityCompat.START)){
            binding?.drawerLayout!!.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }



    fun updateNavigationUserDetails(user : User , readBoardsList : Boolean){
        hideProgressDialog()
        mUserName = user.name
        Log.d("muser",mUserName)

        val navUserImage : ImageView = findViewById(R.id.nav_user_image)
        val userName : TextView = findViewById(R.id.tv_username)
        Glide
            .with(this)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navUserImage);

        userName.text = user.name
        if(readBoardsList){
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE){
            FirestoreClass().loadUserData(this@MainActivity)
        }
        else if(resultCode == Activity.RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE){
            FirestoreClass().getBoardsList(this)
        }

        else {
            Log.e("Cancelled","activity Cancelled")
        }
    }



    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.nav_my_profile -> {
                val intent = Intent(this,MyProfileActivity::class.java)
                startActivityForResult(intent, MY_PROFILE_REQUEST_CODE)
            }

            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()
                mSharedPreferences.edit().clear().apply()

                val intent = Intent(this,GetStarted::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding?.drawerLayout!!.closeDrawer(GravityCompat.START)
        return true
    }

    // Token update Success
    fun tokenUpdateSuccess(){
        hideProgressDialog()
        val editor : SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED,true)

        editor.apply()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this,true)
    }

    private fun updateFCMToken(token : String){
        val userHashMap = HashMap<String,Any>()
        userHashMap[Constants.FCM_TOKEN] = token

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this,userHashMap)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}


