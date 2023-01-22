package com.example.designstudionew.recyclerAdapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.designstudionew.R
import com.example.designstudionew.util.Utils


class ColorPickerAdapter(callBackItem: ColorPickerClick) :
    RecyclerView.Adapter<ColorPickerAdapter.ViewHolder>() {

    private var context: Context? = null
    private var callBack: ColorPickerClick = callBackItem

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        context = parent.context

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.svg_color_item, parent, false)

        return ViewHolder(view)

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        context?.let {
            holder.thumbNail.setColorFilter(
                ContextCompat.getColor(
                    it,
                    Utils.colorArray[position]
                )
            )
        }

    }


    override fun getItemCount(): Int {
        return Utils.colorArray.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var thumbNail: AppCompatImageView

        init {

            thumbNail = itemView.findViewById(R.id.imageView18)

            itemView.setOnClickListener {
                callBack.setOnColorClickListener(adapterPosition)
            }

        }

    }

    interface ColorPickerClick {
        fun setOnColorClickListener(position: Int)
    }
}