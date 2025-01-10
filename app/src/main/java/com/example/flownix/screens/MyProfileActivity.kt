package com.example.flownix.screens

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.flownix.R
import com.example.flownix.utils.Constants
import com.example.flownix.databinding.ActivityMyProfileBinding
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class MyProfileActivity : BaseActivity() {


    private var mSelectedImageFileUri : Uri? = null
    private var mProfileImageURL : String = ""
    private lateinit var mUserDetails : User

    private var binding : ActivityMyProfileBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setActionBar()
        FirestoreClass().loadUserData(this)


        binding?.btnUpdate?.setOnClickListener{
            if(mSelectedImageFileUri != null){
                uploadUserImage()
            } else {
                updateUserProfileData()
            }
        }


        binding?.ivProfileUserImage?.setOnClickListener{

            if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
                // todo: show image chooser
                Constants.showImageChooser(this)
            } else {
                val permissionList: Array<String> = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    // For API 28 and below, request READ and WRITE permissions
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    // For API 29 to 32, request only READ_EXTERNAL_STORAGE
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                } else {
                    // For API 33 and above, request granular permissions
                    arrayOf(
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VIDEO
                    )
                }
                // Request permissions
                ActivityCompat.requestPermissions(
                    this,
                    permissionList, // Pass the Array<String> directly
                    Constants.READ_STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Constants.READ_STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Constants.showImageChooser(this)
            } else {
                // Check if the rationale dialog should be shown
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        permissions[0]
                    )
                ) {
                    // Show rationale dialog
                    showRationalDialogForPermissions()
                } else {
                    // Permission denied and "Don't ask again" is selected
                    Toast.makeText(
                        this,
                        "Permission denied. You can enable it in Settings.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun showRationalDialogForPermissions() {

        AlertDialog.Builder(this@MyProfileActivity)
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

    // To adding onActivityResult here is important, because if we taking image from the gallery so we should pass
    // onActivityResult() to tell that to insert desired selected image into our desired View.
    // Otherwise(if not added onActivityResult()) selected image from gallery will not show in view.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data!!.data != null){
            mSelectedImageFileUri = data.data

            try {
                val navUserImage : ImageView = findViewById(R.id.ivProfileUserImage)

                Glide.with(this@MyProfileActivity)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(navUserImage)
            } catch (e : IOException){
                e.printStackTrace()
            }

        }
    }

    private fun setActionBar(){
        setSupportActionBar(binding?.toolbarMyProfileActivity)

        if(supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = resources.getString(R.string.myProfile)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_icon)
        }

        binding?.toolbarMyProfileActivity?.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }




    // This data is dynamically loading from the firebase automatically.
    fun setUserDataInUI(user: User){
        val navUserImage : ImageView = findViewById(R.id.ivProfileUserImage)

        mUserDetails = user

        Glide.with(this@MyProfileActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navUserImage)

        binding?.etName?.setText(user.name)
        binding?.etEmail?.setText(user.email)

        if(user.mobile != 0L){
            binding?.etMobile?.setText(user.mobile.toString())
        }
    }


    private fun updateUserProfileData(){
        val userHashMap = HashMap<String,Any>()
        val name: String = binding?.etName?.text.toString().trim()
        val mobile: Long = binding?.etMobile?.text?.toString()?.toLongOrNull() ?: 0L
        var anyChangesMade = false

        if(mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image){
            userHashMap[Constants.IMAGE] = mProfileImageURL
            anyChangesMade = true
        }

        if(name != mUserDetails.name){
            userHashMap[Constants.NAME] = name
            anyChangesMade = true
        }

        if(mobile != mUserDetails.mobile){
            userHashMap[Constants.MOBILE] = mobile
            anyChangesMade = true
        }

        if(anyChangesMade)
         FirestoreClass().updateUserProfileData(this,userHashMap)
    }

    private fun uploadUserImage(){
        showProgressDialog(resources.getString(R.string.please_wait))

        if(mSelectedImageFileUri != null){
            val sRef : StorageReference = FirebaseStorage.getInstance().reference.child(
                "USER_IMAGE" + System.currentTimeMillis() + "." +
                Constants.getFileExtension(this,mSelectedImageFileUri)
            )

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {

                taskSnapshot ->
                Log.i("Firebase Image URl",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                    uri ->
                    Log.i("Downloadable Image URL",uri.toString())

                    mProfileImageURL = uri.toString()

                    Toast.makeText(this@MyProfileActivity,
                        "Image Uploaded",
                        Toast.LENGTH_SHORT).show()

                    hideProgressDialog()

                    // now updating the whole data
                    updateUserProfileData()
                }
            }.addOnFailureListener{
                exception ->
                Toast.makeText(this@MyProfileActivity,
                    exception.message,
                    Toast.LENGTH_SHORT).show()

                hideProgressDialog()
            }
        }
    }


    fun profileUpdateSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}


// Below approach will not work becuase image is storing in firebase Storage and we need to pass the
// image Url of stored image to the firebase Database.
// Means - Store the data like image in "Storage" of firebase and than create it's url and store that
// url in the Firebase firestore database.

//    private fun updateUserInfo() {
//        val name: String = binding?.etName?.text.toString().trim()
//        val email: String = binding?.etEmail?.text.toString().trim()
//        val mobile: Long = binding?.etMobile?.text?.toString()?.toLongOrNull() ?: 0L
//        val image: String = "" // Replace with actual image URL or path if applicable
//
//        val currentUserId = FirestoreClass().getCurrentUserId()
//        val mFirestore = FirebaseFirestore.getInstance()
//
//        // Validate inputs
//        when {
//            name.isEmpty() -> {
//                Toast.makeText(this, "Please Enter the Name", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            email.isEmpty() -> {
//                Toast.makeText(this, "Please Enter the Email", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            mobile <= 0 -> {
//                Toast.makeText(this, "Please Enter a Valid Mobile Number", Toast.LENGTH_SHORT).show()
//                return
//            }
//
//            else -> {
//                val userObject = User(
//                    id = currentUserId,
//                    name = name,
//                    email = email,
//                    image = image,
//                    mobile = mobile,
//                    fcmToken = "" // Set if available
//                )
//
//                try {
//                    mFirestore.collection(Constants.USERS)
//                        .document(currentUserId)
//                        .set(userObject, SetOptions.merge())
//                        .addOnSuccessListener {
//                            Toast.makeText(this, "User Info Updated Successfully", Toast.LENGTH_SHORT).show()
//                        }
//                        .addOnFailureListener { e ->
//                            Log.e("Firestore Error", "Error updating user info", e)
//                        }
//
//                    finish()
//                } catch (e: Exception) {
//                    Log.e("Exception", "Error: ${e.message}", e)
//                }
//            }
//        }
//    }

