package com.example.flownix.screens

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flownix.R
import com.example.flownix.adapters.CardMemberListItemsAdapter
import com.example.flownix.databinding.ActivityCardDetailsBinding
import com.example.flownix.dialog.LabelColorListDialog
import com.example.flownix.dialog.MembersListDialog
import com.example.flownix.firebase.FirestoreClass
import com.example.flownix.models.Board
import com.example.flownix.models.Card
import com.example.flownix.models.SelectedMembers
import com.example.flownix.models.Task
import com.example.flownix.models.User
import com.example.flownix.utils.Constants
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CardDetailsActivity : BaseActivity() {

    private lateinit var mBoardDetails : Board
    private var mTaskListPosition = -1
    private var mCardPosition  = -1
    private var mSelectedColor = ""
    private lateinit var mMembersDetailList : ArrayList<User>
    private var mSelectedDueDateMilliSeconds : Long = 0

    private var binding : ActivityCardDetailsBinding? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCardDetailsBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        // Getting the intent data
        getIntentData()
        // Setting up the action bar
        setActionBar()

        val cardName = findViewById<EditText>(R.id.et_name_card_details)
        cardName.setText(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
        cardName.setSelection(cardName.text.toString().length)

        mSelectedColor = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].labelColor
        if(mSelectedColor.isNotEmpty()){
            setColor()
        }


        findViewById<Button>(R.id.btn_update_card_details).setOnClickListener {
            if(findViewById<EditText>(R.id.et_name_card_details).text.toString().isNotEmpty()){
                updateCardDetails()
            }
            else{
                Toast.makeText(this,"EnterCard Name",Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<TextView>(R.id.tv_select_label_color).setOnClickListener {
            labelColorsListDialog()
        }

        findViewById<TextView>(R.id.tv_select_members).setOnClickListener {
            membersListDialog()
        }

        mSelectedDueDateMilliSeconds = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].dueDate

        if(mSelectedDueDateMilliSeconds > 0){
            val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
            val selectedDate = simpleDateFormat.format(Date(mSelectedDueDateMilliSeconds))
            findViewById<TextView>(R.id.tv_select_due_date).text = selectedDate
        }

        findViewById<TextView>(R.id.tv_select_due_date).setOnClickListener {
            showDatePicker()
        }

        setupSelectedMembersList()

    }


    private fun setActionBar(){
        setSupportActionBar(binding?.toolbarCardDetailsActivity)

        if(supportActionBar != null){
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_icon)
            supportActionBar?.title = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name
        }

        binding?.toolbarCardDetailsActivity?.setNavigationOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun addUpdateTaskListSuccess(){
        hideProgressDialog()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun getIntentData(){
        if(intent.hasExtra(Constants.BOARD_DETAIL)){
            mBoardDetails = intent.getParcelableExtra(Constants.BOARD_DETAIL)!!
        }
        if(intent.hasExtra(Constants.TASK_LIST_ITEM_POSITION)){
            mTaskListPosition = intent.getIntExtra(Constants.TASK_LIST_ITEM_POSITION,-1)
        }
        if(intent.hasExtra(Constants.CARD_LIST_ITEM_POSITION)){
            mCardPosition = intent.getIntExtra(Constants.CARD_LIST_ITEM_POSITION,-1)
        }
        if(intent.hasExtra(Constants.BOARD_MEMBERS_LIST)){
            mMembersDetailList = intent.getParcelableArrayListExtra(Constants.BOARD_MEMBERS_LIST)!!
        }
    }

    private fun membersListDialog(){
        var cardAssignedMembersList = mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo

        if(cardAssignedMembersList.size > 0){

            // checking that members assigned to the board also assigned to the card or not?
            for (i in mMembersDetailList.indices){
                for(j in cardAssignedMembersList){
                    if(mMembersDetailList[i].id == j){
                        mMembersDetailList[i].selected = true
                    }
                }
            }
        } else {
            for (i in mMembersDetailList.indices){
                mMembersDetailList[i].selected = false
            }
        }

        val listDialog = object : MembersListDialog(
            this,
            mMembersDetailList,
            resources.getString(R.string.select_members)
        ){
            override fun onItemSelected(position : Int, user: User, action: String) {

                Log.d("action",action)
                val cardList = mBoardDetails.taskList[mTaskListPosition]
                    .cards[mCardPosition].assignedTo

                if (action == Constants.SELECT) {
                    for (i in mMembersDetailList.indices) {
                        if (mMembersDetailList[i].id == user.id) {
                            mMembersDetailList[i].selected = false
                        }
                    }
                    cardList.remove(user.id)

                    Log.d("action",action + "inside Constants.SELECT")


                } else {

                    Log.d("action",action + "inside Constants.UN_SELECT")
                    //user.selected = true

                    if (!cardList.contains(user.id)) {
                        cardList.add(user.id)
                    }

                    for (i in mMembersDetailList.indices) {
                        if (mMembersDetailList[i].id == user.id) {
                            mMembersDetailList[i].selected = true
                        }
                    }
                }

                // 🔧 Update mBoardDetails with the modified list
                mBoardDetails.taskList[mTaskListPosition]
                    .cards[mCardPosition].assignedTo = cardList


                for(i in cardList){
                    Log.d("cardListMembers", "$i ")
                }

                setupSelectedMembersList()

            }
        }
        listDialog.show()

    }


    private fun updateCardDetails(){
        val card = Card(
            findViewById<EditText>(R.id.et_name_card_details).text.toString(),
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].createdBy,
            mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].assignedTo,
            mSelectedColor,
            mSelectedDueDateMilliSeconds
        )


        mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition] = card

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity,mBoardDetails)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete_card,menu)
        return super.onCreateOptionsMenu(menu)
    }


    private fun colorList() : ArrayList<String>{
        val colorList : ArrayList<String> = ArrayList()
        colorList.add("#43C86F")
        colorList.add("#0C90F1")
        colorList.add("#F72400")
        colorList.add("#7A8089")
        colorList.add("#D57C1D")
        colorList.add("#770000")
        colorList.add("#0022F8")

        return colorList
    }

    private fun setColor(){
        val labelColor = findViewById<TextView>(R.id.tv_select_label_color)
        labelColor.text = ""
        labelColor.setBackgroundColor(Color.parseColor(mSelectedColor))
    }

    private fun labelColorsListDialog(){
        val colorsList : ArrayList<String> = colorList()

        val listDialog = object : LabelColorListDialog(
            this,
            colorsList,
            resources.getString(R.string.str_select_label_color),
            mSelectedColor){
            override fun onItemSelected(color: String) {
               mSelectedColor = color
                setColor()
            }
        }

        listDialog.show()
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_delete_card -> {
                alertDialogForDeleteCard(mBoardDetails.taskList[mTaskListPosition].cards[mCardPosition].name)
            }
        }
        return true
    }


    private fun deleteCard(){
        val cardsList : ArrayList<Card> = mBoardDetails.taskList[mTaskListPosition].cards
        cardsList.removeAt(mCardPosition)

        val taskList : ArrayList<Task> = mBoardDetails.taskList

        taskList[mTaskListPosition].cards = cardsList


        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().addUpdateTaskList(this@CardDetailsActivity,mBoardDetails)

    }

    private fun alertDialogForDeleteCard(cardName : String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.alert))
        builder.setMessage(
            resources.getString(
                R.string.confirmation_message_to_delete_card,
                cardName
            )
        )
        builder.setIcon(android.R.drawable.ic_dialog_alert)
        builder.setPositiveButton(resources.getString(R.string.yes)){ dialogInterface,_ ->
            dialogInterface.dismiss()
            deleteCard()
        }
        builder.setNegativeButton(resources.getString(R.string.no)){ dialogInterface,_ ->
            dialogInterface.dismiss()
        }

        val alertDialog : AlertDialog = builder.create()

        alertDialog.setCancelable(false)
        alertDialog.show()

    }


    // method to populate the Member list on Card details List
    private fun setupSelectedMembersList() {

        val cardAssignedMembersList =
            mBoardDetails.taskList[mTaskListPosition]
                .cards[mCardPosition].assignedTo

        for(i in cardAssignedMembersList){
            Log.d("cardAssignedMembersList", "$i ")
        }

        val selectedMembersList: ArrayList<SelectedMembers> = ArrayList()

        // Populate selectedMembersList
        for (i in mMembersDetailList.indices) {
            for (j in cardAssignedMembersList) {
                if (mMembersDetailList[i].id == j) {
                    val selectedMember = SelectedMembers(
                        mMembersDetailList[i].id,
                        mMembersDetailList[i].image
                    )
                    selectedMembersList.add(selectedMember)
                }
            }
        }

        // Add the "Add Member" placeholder
        if (selectedMembersList.isNotEmpty()) {
           selectedMembersList.add(SelectedMembers("", ""))

            findViewById<TextView>(R.id.tv_select_members).visibility = View.GONE
            val selectedMemberRecyclerView = findViewById<RecyclerView>(R.id.rvSelectdMembeList)
            selectedMemberRecyclerView.visibility = View.VISIBLE
            selectedMemberRecyclerView.layoutManager = GridLayoutManager(this, 6)

            val adapter = CardMemberListItemsAdapter(this, selectedMembersList,true)

            selectedMemberRecyclerView.adapter = adapter
            adapter.setOnClickListener(object : CardMemberListItemsAdapter.OnClickListener {
                    override fun onClick() {
                        membersListDialog()
                    }
                })

        } else {
            // Handle empty list
            findViewById<TextView>(R.id.tv_select_members).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.rvSelectdMembeList).visibility = View.GONE
        }
    }

    private fun showDatePicker(){
        val c = Calendar.getInstance()       // Calendar instance
        val year = c.get(Calendar.YEAR)          // year
        val month = c.get(Calendar.MONTH)        // month
        val day = c.get(Calendar.DAY_OF_MONTH)  // day
        val dpd = DatePickerDialog(                 // Datepicker Dialog
            this,
            DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                val sDayOfMonth = if(dayOfMonth < 10) "0$dayOfMonth" else "$dayOfMonth"
                val sMonthOfYear = if((monthOfYear + 1) < 10 ) "0${monthOfYear + 1}" else "${monthOfYear + 1}"
                val selectedDate = "$sDayOfMonth/$sMonthOfYear/$year"
                findViewById<TextView>(R.id.tv_select_due_date).text = selectedDate

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                val theDate = sdf.parse(selectedDate)
                mSelectedDueDateMilliSeconds = theDate!!.time
            },
            year,
            month,
            day
        )
        dpd.show()
    }


    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}