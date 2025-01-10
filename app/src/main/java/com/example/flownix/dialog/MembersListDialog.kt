package com.example.flownix.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flownix.R
import com.example.flownix.adapters.MembersListItemAdapter
import com.example.flownix.models.User

abstract class MembersListDialog(
    mContext : Context,
    private var list : ArrayList<User>,
    private val title : String = "",
    ) : Dialog(mContext) {

    var adapter : MembersListItemAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list,null)
        setContentView(view)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setUpRecyclerView(view)
    }

    private fun setUpRecyclerView(view : View){
        view.findViewById<TextView>(R.id.tvTitle).text = title

        if (list.size > 0){
            view.findViewById<TextView>(R.id.tvTitle).text = title
            val memberListDialogRecyclerView = view.findViewById<RecyclerView>(R.id.rvList)
            memberListDialogRecyclerView.layoutManager = LinearLayoutManager(context)

            adapter = MembersListItemAdapter(context,list)
            memberListDialogRecyclerView.adapter = adapter

            adapter!!.setOnClickListener(object : MembersListItemAdapter.OnClickListener{
                override fun onClick(position: Int, user: User, action: String) {
                    dismiss()
                    onItemSelected(position,user,action)
                }
            })
        }
    }
    protected abstract fun onItemSelected(position : Int,user: User,action : String)
}