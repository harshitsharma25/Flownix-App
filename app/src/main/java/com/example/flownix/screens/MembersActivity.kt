package com.example.flownix.screens

import android.Manifest
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flownix.R
import com.example.flownix.adapters.MembersListItemAdapter
import com.example.flownix.databinding.ActivityMembersBinding
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.Board
import com.example.flownix.models.User
import com.example.flownix.screens.AccessToken.getAccessToken
import com.example.flownix.utils.Constants
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MembersActivity : BaseActivity() {

    private var binding : ActivityMembersBinding? = null
    private lateinit var mBoardDetails : Board
    private lateinit var mAssignedMembersList : ArrayList<User>
    private var anyChangesMade : Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails = intent.getParcelableExtra<Board>(Constants.BOARD_DETAIL)!!
        }
        setActionBar()
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this,mBoardDetails.assignedTo)


    }

    private fun setActionBar(){
        setSupportActionBar(binding?.toolbarMembersActivity)

        if(supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = resources.getString(R.string.members)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_icon)
        }

        binding?.toolbarMembersActivity?.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun setupMembersList(list : ArrayList<User>){
        mAssignedMembersList = list

        hideProgressDialog()
        val membersRecyclerView = findViewById<RecyclerView>(R.id.rvMembersList)
        membersRecyclerView.layoutManager = LinearLayoutManager(this)
        membersRecyclerView.setHasFixedSize(true)

        val adapter = MembersListItemAdapter(this,list)
        membersRecyclerView.adapter = adapter
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_add_member,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_add_member -> {
                dialogSearchMember()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun dialogSearchMember(){
        val dialog = Dialog(this)

        dialog.setContentView(R.layout.dialog_search_member)
        dialog.findViewById<TextView>(R.id.tv_add).setOnClickListener {
            val email = dialog.findViewById<EditText>(R.id.et_email_search_member).text.toString()
            if(email.isNotEmpty()){
                dialog.dismiss()
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getMemberDetails(this,email)
            }
            else{
                Toast.makeText(this,"Please Enter the Email",Toast.LENGTH_SHORT).show()
            }
        }
        dialog.findViewById<TextView>(R.id.tv_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

    fun memberDetails(user: User) {
        mBoardDetails.assignedTo.add(user.id) // user which is assigned a new task
        FirestoreClass().assignMemberToBoard(this,mBoardDetails,user)// it means user passed will be assigned to particular board
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(anyChangesMade){
            setResult(Activity.RESULT_OK)
        }
        super.onBackPressed()
    }

    // New member assigned to current board successfully after entering the entry in firebase firestore.
    fun memberAssignSuccess(user : User){
        hideProgressDialog()
        anyChangesMade = true
        mAssignedMembersList.add(user)
        Log.d("New Member added", user.name)
        Log.d("New member id", user.id)           // this use id will pass to notification function
        Log.d("fcm token",user.fcmToken)

        setupMembersList(mAssignedMembersList)

        notificationPermissions(user)

        //sendNotification(user.fcmToken, "Assigned to the Board ${mBoardDetails.name}", "You have been assigned to a new board.")
    }

    fun sendNotification(token: String, title: String, message: String) {
        val accessToken = getAccessToken(applicationContext)

        val client = OkHttpClient()

        // Create the notification payload
        val jsonBody = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", token)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", message)
                })
            })
        }

        // Build the request
        val request = Request.Builder()
            .url(Constants.FCM_SEND_URL)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create("application/json".toMediaType(), jsonBody.toString()))
            .build()


        // Execute the request
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FCM", "Notification sending failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("FCM", "Notification sent successfully!")
                } else {
                    Log.e("FCM", "Notification sending failed: ${response.message}")
                }
            }
        })
    }

    private fun notificationPermissions(user: User) {
        Dexter.withContext(this)
            .withPermissions(
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // For API 28 and below, no need to request notification permission explicitly
                    listOf()
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    // For API 29 to 32, no explicit request for notification permission, it is granted by default
                    listOf()
                } else {
                    // For API 33 and above, explicitly request notification permission
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                }
            )

            .withListener(object : MultiplePermissionsListener {

                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    Log.d("toastMessage", "Permission Check started")

                    if (report!!.areAllPermissionsGranted()) {
                        Log.d("toastMessage", "All permissions granted!")

                        sendNotification(user.fcmToken, "Assigned to the Board ${mBoardDetails.name}", "You have been assigned to a new board.")

                        Toast.makeText(
                            this@MembersActivity,
                            "Notification permissions are granted!",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Proceed to pick a photo from the gallery
                    } else {
                        Toast.makeText(
                            this@MembersActivity,
                            "Notification permissions not granted!",
                            Toast.LENGTH_SHORT
                        ).show()
                        //Log.d("toastMessage", "Not all permissions granted!")
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    // Show rationale dialog for permissions if needed
                    showRationalDialogForPermissions()
                }
            }).onSameThread().check()
    }


    private fun showRationalDialogForPermissions() {

        AlertDialog.Builder(this@MembersActivity)
            .setMessage(
                "It looks like you've turned off permission required. Now you can re-enable it in Application Settings."
            )
            .setPositiveButton("GO TO SETTINGS") { dialog, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    dialog.dismiss()
                    Log.d("Permission", "Navigated to settings")
                } catch (e: ActivityNotFoundException) {
                    Log.e("Permission", "Error navigating to settings", e)
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                Log.d("Permission", "User canceled rationale dialog")
                dialog.dismiss()
            }
            .show()
    }

}