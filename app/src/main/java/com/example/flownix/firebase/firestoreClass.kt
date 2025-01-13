package com.example.flownix.firebase

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.example.flownix.models.Board
import com.example.flownix.models.User
import com.example.flownix.screens.CardDetailsActivity
import com.example.flownix.screens.CreateBoardActivity
import com.example.flownix.screens.MainActivity
import com.example.flownix.screens.MembersActivity
import com.example.flownix.screens.MyProfileActivity
import com.example.flownix.screens.SignUpActivity
import com.example.flownix.screens.SigninActivity
import com.example.flownix.screens.TaskListActivity
import com.example.flownix.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class FirestoreClass {
    private val mFireStore = FirebaseFirestore.getInstance()

    fun registerUser(activity: SignUpActivity,userInfo: User){

        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId()).set(userInfo, SetOptions.merge())
            .addOnSuccessListener {
                activity.userRegisteredSuccess()
            }.addOnFailureListener{
                e ->
                Log.e(activity.javaClass.simpleName,"Error")
            }
    }

    fun getBoardDetails(activity: TaskListActivity,documentId : String){
        mFireStore.collection(Constants.BOARDS)
            .document(documentId)
            .get()
            .addOnSuccessListener {
                    document ->
                Log.i(activity.javaClass.simpleName,document.toString())

                activity.boardDetails(document.toObject(Board::class.java)!!)

            }.addOnFailureListener{
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating a Board")
            }
    }


    fun createBoard(activity: CreateBoardActivity, board: Board) {
        //activity.showProgressDialog("Please Wait")
        val documentReference = mFireStore.collection(Constants.BOARDS).document()
        board.documentId = documentReference.id // Assign the generated document ID to the Board object

        documentReference
            .set(board, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(activity, "Board created Successfully", Toast.LENGTH_SHORT).show()
                activity.boardCreatedSuccessfully()
            }
            .addOnFailureListener { exception ->
                activity.hideProgressDialog()
                Log.e(
                    activity.javaClass.simpleName,
                    "Error while creating the board",
                    exception
                )
            }
    }


    fun getBoardsList(activity: MainActivity){
        mFireStore.collection(Constants.BOARDS)
            .whereArrayContains(Constants.ASSIGNED_TO,getCurrentUserId())
            .get()
            .addOnSuccessListener {
                    documentsAssignedToMe ->
                Log.i(activity.javaClass.simpleName,    documentsAssignedToMe.documents.toString())
                val boardList : ArrayList<Board> = ArrayList()

                for(i in     documentsAssignedToMe.documents){
                    val board = i.toObject(Board::class.java)!!
                    board.documentId = i.id
                    boardList.add(board)
                }

                // populating the board list to the Main Activity
                activity.populateBoardsListToUI(boardList)

            }.addOnFailureListener{
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating a Board")
            }
    }


    fun addUpdateTaskList(activity: Activity,board: Board){

        // for storing the multiple tasks list
        val taskListHashMap = HashMap<String,Any>()
        taskListHashMap[Constants.TASK_LIST] = board.taskList

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(taskListHashMap)
            .addOnSuccessListener {

                if(activity is TaskListActivity)
                    activity.addUpdateTaskListSuccess()
                else if(activity is CardDetailsActivity)
                    activity.addUpdateTaskListSuccess()
            }
            .addOnFailureListener {
                if(activity is TaskListActivity)
                    activity.hideProgressDialog()
                else if(activity is CardDetailsActivity)
                    activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating the task")
            }
    }

    fun updateUserProfileData(activity: Activity,
                              userHashMap: HashMap<String,Any>){
        mFireStore.collection(Constants.USERS)
            .document(getCurrentUserId())
            .update(userHashMap)
            .addOnSuccessListener {
                Log.i(activity.javaClass.simpleName,"Profile data Updated")

                when(activity){
                    is MainActivity -> {
                        activity.tokenUpdateSuccess()
                    }
                    is MyProfileActivity -> {
                        Toast.makeText(activity,"Profile updated Successfully",Toast.LENGTH_SHORT).show()
                        activity.profileUpdateSuccess()
                    }
                }

            }.addOnFailureListener{
                e ->
                when(activity){
                    is MainActivity -> {
                        activity.hideProgressDialog()
                    }
                    is MyProfileActivity -> {
                        activity.hideProgressDialog()
                    }
                }
                Toast.makeText(activity,"Error when updating the profile",Toast.LENGTH_SHORT).show()
            }
    }


    fun loadUserData(activity: Activity , readBoardsList : Boolean = false ) {
        val currentUserId = getCurrentUserId()

        if (currentUserId.isEmpty()) {
            Log.e("signInUser", "Current user ID is empty. No user is signed in.")
            when (activity) {
                is SigninActivity -> activity.hideProgressDialog()
                is MainActivity -> activity.hideProgressDialog()
            }
            return // Exit the function early if no user is signed in.
        }

        mFireStore.collection(Constants.USERS)
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                val loggedInUser = document.toObject(User::class.java)!!

                when (activity) {
                    is SigninActivity -> activity.signInSuccess(loggedInUser)
                    is MainActivity -> activity.updateNavigationUserDetails(loggedInUser,readBoardsList)
                    is MyProfileActivity -> activity.setUserDataInUI(loggedInUser)
                }
            }
            .addOnFailureListener { e ->
                when (activity) {
                    is SigninActivity -> activity.hideProgressDialog()
                    is MainActivity -> activity.hideProgressDialog()
                }
                Log.e("signInUser", "Error retrieving document", e)
            }
    }

    fun getAssignedMembersListDetails(
        activity : Activity,
        assignedTo : ArrayList<String>
    ){
        mFireStore.collection(Constants.USERS)
            .whereIn(Constants.ID,assignedTo)
            .get()
            .addOnSuccessListener {
                document ->
                Log.e(activity.javaClass.simpleName,document.documents.toString())

                val usersList :ArrayList<User> = ArrayList()

                for(i in document.documents){
                    val user = i.toObject(User::class.java)!!
                    usersList.add(user)
                }

                if(activity is MembersActivity)
                   activity.setupMembersList(usersList)
                else if(activity is TaskListActivity)
                    activity.boardMembersDetailsList(usersList)
            }.addOnFailureListener {e ->
                if(activity is MembersActivity)
                  activity.hideProgressDialog()
                if(activity is TaskListActivity)
                  activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error while creating the board",
                    e
                )
            }

    }

    fun getMemberDetails(activity: MembersActivity,email : String){
        mFireStore.collection(Constants.USERS)
            .whereEqualTo(Constants.EMAIL,email)
            .get()
            .addOnSuccessListener {
                document ->
                if(document.documents.size > 0){
                    // user which is assigned a new task
                    val user = document.documents[0].toObject(User::class.java)!!
                    activity.memberDetails(user)
                } else {
                    activity.hideProgressDialog()
                    activity.showErrorSnackBar("No such Member Found")
                }
            }.addOnFailureListener {e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,
                    "Error while getting the user details",e)
            }
    }

    // updating the "assigned To" of mBoardDetails in firebase
    fun assignMemberToBoard(activity: MembersActivity,board: Board,user: User){
        val assignedToHashMap = HashMap<String,Any>()
        assignedToHashMap[Constants.ASSIGNED_TO] = board.assignedTo

        mFireStore.collection(Constants.BOARDS)
            .document(board.documentId)
            .update(assignedToHashMap)
            .addOnSuccessListener {
                activity.memberAssignSuccess(user)
            }.addOnFailureListener {e ->
                activity.hideProgressDialog()
                Log.e(activity.javaClass.simpleName,"Error while creating the board",e,)
            }
    }


    fun getCurrentUserId() : String {
        var currentUser = FirebaseAuth.getInstance().currentUser
        var currentUserID = ""
        if(currentUser != null){
            currentUserID = currentUser.uid
        }

        return currentUserID
    }
}