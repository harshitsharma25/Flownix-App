package com.example.flownix.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable.Orientation
import android.text.Layout.Alignment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flownix.R
import com.example.flownix.models.Card
import com.example.flownix.models.SelectedMembers
import com.example.flownix.screens.TaskListActivity
import org.intellij.lang.annotations.JdkConstants.HorizontalAlignment

open class CardListItemsAdapter(
    private var list : ArrayList<Card>,
    private val context : Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener : OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(LayoutInflater.from(context).inflate(R.layout.item_card,parent,false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
       val model = list[position]

        if(holder is MyViewHolder){

            if(model.labelColor.isNotEmpty()){
                holder.itemView.findViewById<View>(R.id.view_label_color).visibility = View.VISIBLE
                holder.itemView.findViewById<View>(R.id.view_label_color).setBackgroundColor(Color.parseColor(model.labelColor))
            }else {
                holder.itemView.findViewById<View>(R.id.view_label_color).visibility = View.GONE
            }

            if((context as TaskListActivity).mAssignedMemberDetailList.size > 0){
                val selectedMembersList : ArrayList<SelectedMembers> = ArrayList()

                // Those Board members which are also assigned to the card
                // In this double for-loop , we actually comparing user id of users which are assigned to the board to Selected members's id of card

                for(i in context.mAssignedMemberDetailList.indices){
                    for(j in model.assignedTo){
                        if(context.mAssignedMemberDetailList[i].id == j){
                            val selectedMembers = SelectedMembers(
                                 context.mAssignedMemberDetailList[i].id
                                ,context.mAssignedMemberDetailList[i].image
                            )
                            selectedMembersList.add(selectedMembers)
                        }
                    }
                }

                val memberRecyclerView = holder.itemView.findViewById<RecyclerView>(R.id.rvCardSelectedMembersList)
                if(selectedMembersList.size > 0){

                    memberRecyclerView.visibility = View.VISIBLE
                    val gridLayoutManager = GridLayoutManager(context, 4)
                    gridLayoutManager.reverseLayout = true  // This will populate items from right to left


                    memberRecyclerView.layoutManager = gridLayoutManager

                    // Ensure the RecyclerView is in RTL mode
                    memberRecyclerView.layoutDirection = View.LAYOUT_DIRECTION_RTL

                    val adapter = CardMemberListItemsAdapter(context,selectedMembersList,false)
                    memberRecyclerView.adapter = adapter

                    adapter.setOnClickListener(object  : CardMemberListItemsAdapter.OnClickListener{
                        override fun onClick() {
                            if(onClickListener != null){
                                onClickListener!!.onClick(position)
                            }
                        }
                    })
                }else{
                    memberRecyclerView.visibility = View.GONE
                }


            }

            holder.itemView.findViewById<TextView>(R.id.tv_card_name).text = model.name
            holder.itemView.setOnClickListener {
                if(onClickListener != null){
                    onClickListener!!.onClick(position)
                }
            }
        }
    }

    override fun getItemCount(): Int {
       return list.size
    }

    interface OnClickListener{
        fun onClick(position : Int)
    }

    fun setOnClickListener(onClickListener : OnClickListener){  // to make OnClickListner to be non null
        this.onClickListener = onClickListener
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}