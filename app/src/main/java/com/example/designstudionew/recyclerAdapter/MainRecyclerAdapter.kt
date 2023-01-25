package com.example.designstudionew.recyclerAdapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.designstudionew.model.NewCategoryData
import com.example.designstudionew.R
import java.util.*
import kotlin.collections.ArrayList

class MainRecyclerAdapter : RecyclerView.Adapter<MainRecyclerAdapter.ViewHolder>() {

    private var totalCategory: ArrayList<NewCategoryData> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.main_recycler_row_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.title.text = totalCategory[position].categoryName
        setCatItemRecycler(
            holder.recyclerItemView,
            totalCategory[position].categoryName,
            totalCategory[position].size
        )
    }

    override fun getItemCount(): Int {
        return totalCategory.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var title: TextView
        var recyclerItemView: RecyclerView

        init {
            title = itemView.findViewById(R.id.cat_title)
            recyclerItemView = itemView.findViewById(R.id.item_recycler)

        }

    }

    private fun setCatItemRecycler(rec: RecyclerView, categoryName: String, totalCount: Int) {
        rec.apply {
            setHasFixedSize(true)
            adapter = MainSubRecyclerAdapter(totalCount, categoryName)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(list: java.util.ArrayList<NewCategoryData>) {
        totalCategory.clear()
        totalCategory.addAll(list)
        notifyDataSetChanged()

    }

}