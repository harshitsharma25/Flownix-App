package com.example.flownix.screens

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.flownix.R
import com.example.flownix.databinding.ActivityCreateBoardBinding
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.Board
import com.example.flownix.utils.Constants
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.IOException

class CreateBoardActivity : BaseActivity() {

    private var binding : ActivityCreateBoardBinding? = null
    private var mSelectedImageFileUri : Uri? = null
    private lateinit var mUserName : String

    private var mBoardImageURL : String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBoardBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setActionBar()

        if(intent.hasExtra(Constants.NAME)){
            mUserName = intent.getStringExtra(Constants.NAME).toString()
        }

        binding?.ivBoardImage?.setOnClickListener{

            if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
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


        binding?.btnCreate?.setOnClickListener{
            if(mSelectedImageFileUri != null){
                uploadBoardImage()
            } else {
                showProgressDialog(resources.getString(R.string.please_wait))
                createBoard()
            }
        }
    }

    private fun setActionBar(){
        setSupportActionBar(binding?.toolbarCreateBoardActivity)

        if(supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = resources.getString(R.string.create_board_title)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_icon)
        }

        binding?.toolbarCreateBoardActivity?.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }

    }


    private fun createBoard(){
        val assignedUsersArrayList : ArrayList<String> = ArrayList()
        getCurrentUserID()?.let { assignedUsersArrayList.add(it) }

        var board = Board(
            binding?.etBoardName?.text.toString(),
            mBoardImageURL,
            mUserName,
            assignedUsersArrayList
        )

        FirestoreClass().createBoard(this,board)
    }

    private fun uploadBoardImage(){
        showProgressDialog(resources.getString(R.string.please_wait))

        if(mSelectedImageFileUri != null){
            val sRef : StorageReference = FirebaseStorage.getInstance().reference.child(
                "BOARD_IMAGE" + System.currentTimeMillis() + "." +
                        Constants.getFileExtension(this,mSelectedImageFileUri)
            )

            sRef.putFile(mSelectedImageFileUri!!).addOnSuccessListener {

                    taskSnapshot ->
                Log.i("Firebase Image URl",
                    taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                )

                taskSnapshot.metadata!!.reference!!.downloadUrl.addOnSuccessListener {
                        uri ->
                    Log.i("Board Image URL",uri.toString())

                    mBoardImageURL = uri.toString()

                    Toast.makeText(this@CreateBoardActivity,
                        "Image Uploaded",
                        Toast.LENGTH_SHORT).show()

                    hideProgressDialog()
                    createBoard()
                }
            }.addOnFailureListener{
                    exception ->
                Toast.makeText(this@CreateBoardActivity,
                    exception.message,
                    Toast.LENGTH_SHORT).show()

                hideProgressDialog()
            }
        }
    }


    fun boardCreatedSuccessfully(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
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

        AlertDialog.Builder(this)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK && requestCode == Constants.PICK_IMAGE_REQUEST_CODE && data!!.data != null){
            mSelectedImageFileUri = data.data

            try {
                val navUserImage : ImageView = findViewById(R.id.ivBoardImage)

                Glide.with(this)
                    .load(mSelectedImageFileUri)
                    .centerCrop()
                    .placeholder(R.drawable.ic_board_place_holder)
                    .into(navUserImage)
            } catch (e : IOException){
                e.printStackTrace()
            }

        }
    }



    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}