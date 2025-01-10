package com.example.flownix.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.flownix.R
import com.example.flownix.adapters.TaskListItemsAdapter
import com.example.flownix.databinding.ActivityTaskListBinding
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.Board
import com.example.flownix.models.Card
import com.example.flownix.models.Task
import com.example.flownix.models.User
import com.example.flownix.utils.Constants

class TaskListActivity : BaseActivity() {

    private var binding : ActivityTaskListBinding? = null
    private lateinit var mBoardDetails :Board
    private lateinit var mBoardDocumentId : String
    lateinit var mAssignedMemberDetailList : ArrayList<User>  // Members Assigned to the particular Board

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTaskListBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if(intent.hasExtra(Constants.DOCUMENT_ID)){
            mBoardDocumentId = intent.getStringExtra(Constants.DOCUMENT_ID).toString()
            Log.d("boardId",mBoardDocumentId)
        }

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this,mBoardDocumentId)
    }


    private fun setActionBar(){
        setSupportActionBar(binding?.toolbarTaskListActivity)

        if(supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = mBoardDetails.name
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_icon)
        }

        binding?.toolbarTaskListActivity?.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun boardMembersDetailsList(list : ArrayList<User>){
        mAssignedMemberDetailList = list
        hideProgressDialog()

        if(mBoardDetails.taskList.isEmpty()){
            val addTaskList = Task(resources.getString(R.string.add_list))
            mBoardDetails.taskList.add(addTaskList)
        }

        binding?.rvTaskList?.layoutManager = LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false)
        binding?.rvTaskList?.setHasFixedSize(true)

        val adapter = TaskListItemsAdapter(this,mBoardDetails.taskList)
        binding?.rvTaskList?.adapter = adapter
    }


    fun boardDetails(board: Board){

        mBoardDetails = board

        hideProgressDialog()
        setActionBar()

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(this,
            mBoardDetails.assignedTo)
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getBoardDetails(this,mBoardDetails.documentId)
    }

    fun createTaskList(taskListName : String){
        val task = Task(taskListName,FirestoreClass().getCurrentUserId())
        mBoardDetails.taskList.add(0,task)


        showProgressDialog(resources.getString(R.string.please_wait))

        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun updateTaskList(position : Int, listName : String, model : Task){
        val cards = mBoardDetails.taskList[position].cards
        val task = Task(listName,model.createdBy,cards)
        mBoardDetails.taskList[position] = task

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun deleteTaskList(position: Int){
        mBoardDetails.taskList.removeAt(position)

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }

    fun addCardToTaskList(position: Int, cardName: String) {

        val cardAssignedUsersList : ArrayList<String> = ArrayList()

        // todo : update this
        val card = Card(cardName,FirestoreClass().getCurrentUserId(),cardAssignedUsersList)
        //adding card to cardList
        val cardsList = mBoardDetails.taskList[position].cards
        cardsList.add(card)

        val task = Task(
            mBoardDetails.taskList[position].title,
            mBoardDetails.taskList[position].createdBy,
            cardsList)

        mBoardDetails.taskList[position] = task
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)

    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_members, menu)

        // Iterate through menu items and change title color
        menu?.let {
            for (i in 0 until it.size()) {
                val menuItem = it.getItem(i)
                val spannableTitle = SpannableString(menuItem.title)
                spannableTitle.setSpan(ForegroundColorSpan(Color.BLACK), 0, spannableTitle.length, 0)
                menuItem.title = spannableTitle
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    // This is an alternative of onActivityResult()
    private val membersActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardDetails(this, mBoardDocumentId)
        } else {
            Log.e("cancelled", "Cancelled")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_members -> {
                val intent = Intent(this, MembersActivity::class.java)
                intent.putExtra(Constants.BOARD_DETAIL, mBoardDetails)
                membersActivityLauncher.launch(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun cardDetails(taskListPosition : Int, cardPosition : Int){
        val intent = Intent(this,CardDetailsActivity::class.java)
        intent.putExtra(Constants.BOARD_DETAIL,mBoardDetails)
        intent.putExtra(Constants.TASK_LIST_ITEM_POSITION,taskListPosition)
        intent.putExtra(Constants.CARD_LIST_ITEM_POSITION,cardPosition)
        intent.putExtra(Constants.BOARD_MEMBERS_LIST,mAssignedMemberDetailList)
        membersActivityLauncher.launch(intent)
    }

    fun updateCardsInTaskList(taskListPosition : Int, cards : ArrayList<Card>){
        mBoardDetails.taskList[taskListPosition].cards = cards

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this,mBoardDetails)
    }


//    @Deprecated("Deprecated in Java")
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if(resultCode == RESULT_OK && requestCode == MEMBERS_REQUEST_CODE){
//            showProgressDialog(resources.getString(R.string.please_wait))
//            FirestoreClass().getBoardDetails(this,mBoardDocumentId)
//        }else{
//            Log.e("cancelled","Cancelled")
//        }
//        super.onActivityResult(requestCode, resultCode, data)
//    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }

}