package com.example.designstudionew.recyclerAdapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.designstudionew.customCallBack.PopularClickListener
import com.example.designstudionew.databinding.NewBottomMenuItemsLayoutBinding
import com.example.designstudionew.model.RecyclerItemsModel

class BottomMenuAdapter(
    var listItems: ArrayList<RecyclerItemsModel>
) :
    RecyclerView.Adapter<BottomMenuAdapter.ViewHolder>() {

    var callBack: PopularClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding =
            NewBottomMenuItemsLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.thumbnailShow.setImageResource(listItems[position].image)
        holder.tvTitle.text = listItems[position].title

    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    inner class ViewHolder(view: NewBottomMenuItemsLayoutBinding) :
        RecyclerView.ViewHolder(view.root) {

        var thumbnailShow: ImageView
        var tvTitle: TextView

        init {
            thumbnailShow = view.imageView5
            tvTitle = view.textView2

            view.root.setOnClickListener {
                callBack?.onPopularClick(listItems[adapterPosition].callBackValue)
            }
        }
    }

    fun upDateCallBack(callBackClick: PopularClickListener) {
        this.callBack = callBackClick
    }

    @SuppressLint("NotifyDataSetChanged")
    fun upIconList(newList: ArrayList<RecyclerItemsModel>) {
        this.listItems = newList
        notifyDataSetChanged()
    }

}