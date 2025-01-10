package com.example.flownix.screens

import android.app.Activity
import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
import com.example.flownix.utils.Constants

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
        setupMembersList(mAssignedMembersList)
    }
}