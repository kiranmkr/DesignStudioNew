package com.example.designstudionew.util

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.designstudionew.R

fun ImageView.loadThumbnail(path: String, callback: LoadCallback<Drawable>?) {

    Glide.with(App.context)
        .load(path)
        .dontAnimate()
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .placeholder(R.drawable.image_placeholder)
        .error(R.drawable.image_placeholder)
        .listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                callback?.onLoaded(null, e)
                e?.logRootCauses("ImageView:loadThumbnail")
                Log.e("ImageView:loadThumbnail", "failed: ${e?.localizedMessage}")
                return false
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                callback?.onLoaded(resource, null)
                invalidate()
                Log.e("ImageView:loadThumbnail", "success")
                return false
            }
        })
        .into(this)
    //this.invalidate()
    this.visibility = View.VISIBLE

}

fun ImageView.loadDrawable(drawable: Int) {
    Glide.with(App.context)
        .load(drawable)
        .dontAnimate()
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .skipMemoryCache(true)
        .into(this)
}

interface LoadCallback<T> {
    fun onLoaded(data: T?, exception: Exception?)
}
