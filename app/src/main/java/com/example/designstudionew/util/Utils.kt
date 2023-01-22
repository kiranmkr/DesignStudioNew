package com.example.designstudionew.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.widget.Toast
import com.example.designstudionew.R
import java.io.File

object Utils {

    val readPermissionPass = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    } else {
        arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    var stat = false

    @Suppress("DEPRECATION")
    @JvmField
    val BASE_LOCAL_PATH =
        "${Environment.getExternalStorageDirectory().absolutePath}/Download/DesignStudio/"

    val Base_External_Save = "${Environment.DIRECTORY_DOWNLOADS}/DesignStudio"

    @JvmStatic
    fun getRootPath(): String {

        val root = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Base_External_Save
        } else {
            BASE_LOCAL_PATH
        }

        val dirDest = File(BASE_LOCAL_PATH)

        if (!dirDest.exists()) {
            dirDest.mkdirs()
        }

        return root
    }

    fun showToast(c: Context, message: String) {
        try {
            if (!(c as Activity).isFinishing) {
                c.runOnUiThread { //show your Toast here..
                    Toast.makeText(c.applicationContext, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                //for other device how are able to connect with Ethernet
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                //for check internet over Bluetooth
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    fun getFileExt(): String {
        return if (mainCategory == "svg") {
            ".svg"
        } else {
            ".png"
        }
    }

    var mainCategory: String = "shapes"
    var subCategory: String = "abstract"
    var fileLabelNumber: Int = 1

    val request_read_permission = 2020220

    var feedBackDetails: String = "Report a Bug"
    var feedbackEmail: String = "couldcoding@gmail.com"
    var policyLink: String = "https://cloudcodingapp.blogspot.com/2023/01/privacy-policy.html"
    var termsCondition: String = "https://designstudioapp.blogspot.com/p/terms-conditions.html"

    var moreAppLink: String = "market://details?id=" + "com.print.svg.design.space"

    val inAppKeyArray: ArrayList<String> =
        arrayListOf("life_time")

    val subscriptionsKeyArray: ArrayList<String> =
        arrayListOf("weekly_plan", "monthly_plan", "yearly_plan")

    const val inAppPurchasedkey = "life_time"

    const val inAppWeekly = "weekly_plan"

    const val inAppMonthly = "monthly_plan"

    const val inAppYearly = "yearly_plan"

    var colorArray = arrayOf(
        R.color.black,
        R.color.red_100,
        R.color.red_300, R.color.red_500, R.color.red_700, R.color.blue_100,
        R.color.blue_300, R.color.blue_500, R.color.blue_700, R.color.green_100, R.color.green_300,
        R.color.green_500, R.color.green_700
    )


}