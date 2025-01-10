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
import com.example.flownix.adapters.LabelColorListItemsAdapter

abstract class LabelColorListDialog(
    context : Context,
    private var list : ArrayList<String>,
    private val title : String = "",
    private val mSelecteColor : String = ""
    ) : Dialog(context) {

    private var adapter : LabelColorListItemsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_list,null)
        setContentView(view)
        setCanceledOnTouchOutside(true)
        setCancelable(true)
        setupRecyclerView(view)
    }

    private fun setupRecyclerView(view : View){
        view.findViewById<TextView>(R.id.tvTitle).text = title
        val labelColorRecyclerView = view.findViewById<RecyclerView>(R.id.rvList)
        labelColorRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = LabelColorListItemsAdapter(context,list,mSelecteColor)
        labelColorRecyclerView.adapter = adapter

        adapter!!.setOnClickListener(object  : LabelColorListItemsAdapter.OnItemClickListener{
            override fun onClick(position: Int, color: String) {
                dismiss()
                onItemSelected(color)
            }

        })
    }

    protected abstract fun onItemSelected(color : String)
}