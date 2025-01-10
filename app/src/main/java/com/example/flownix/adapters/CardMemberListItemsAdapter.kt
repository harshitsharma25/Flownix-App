package com.example.flownix.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.flownix.R
import com.example.flownix.models.SelectedMembers

open class CardMemberListItemsAdapter(
    private val context: Context,
    private var list: ArrayList<SelectedMembers>,
    private val assignMembers : Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var onClickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_card_selected_member, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = list[position]

        if (holder is MyViewHolder) {
            if (position == list.size - 1  && assignMembers) {
                holder.itemView.findViewById<ImageView>(R.id.iv_add_member).visibility = View.VISIBLE
                holder.itemView.findViewById<ImageView>(R.id.ivSelectedMemberImage).visibility = View.GONE
            } else {
                holder.itemView.findViewById<ImageView>(R.id.iv_add_member).visibility = View.GONE
                holder.itemView.findViewById<ImageView>(R.id.ivSelectedMemberImage).visibility = View.VISIBLE

                Glide.with(context)
                    .load(model.image)
                    .centerCrop()
                    .placeholder(R.drawable.ic_user_place_holder)
                    .into(holder.itemView.findViewById(R.id.ivSelectedMemberImage))
            }
        }

        holder.itemView.setOnClickListener {
            if (onClickListener != null) {
                onClickListener!!.onClick()
            }
        }
    }

    interface OnClickListener {
        fun onClick()
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }


    class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)
}
