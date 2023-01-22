package com.example.designstudionew.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.example.designstudionew.R
import java.util.*

object FeedbackUtils {

    private fun getDeviceInfo(context: Context): String {
        val sdk = Build.VERSION.SDK_INT      // API Level
        val model = Build.MODEL            // Model
        val brand = Build.BRAND          // Product
        var infoString = ""
        val locale: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0).country
        } else {
            Locale.getDefault().country.lowercase(Locale.ROOT)
        }
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = pInfo.versionName
            infoString += "Application Version: $version\n"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        infoString += "Brand: " + brand + " (" + model + ")\n" +
                "Android API: " + getSDKName(sdk)

        infoString += if (locale.isNotEmpty()) {
            "\nCountry: $locale \n" +
                    " FeedBack: \n" +
                    " ${Utils.feedBackDetails}"
        } else {
            "\n FeedBack: \n ${Utils.feedBackDetails}"
        }
        return infoString
    }

    private var sdkName = "Android  5"

    private fun getSDKName(sdk: Int): String {

        when (sdk) {
            21, 22 -> {
                sdkName = "Android 5"
            }
            23 -> {
                sdkName = "Android 6"
            }
            24, 25 -> {
                sdkName = "Android 7"
            }
            26, 27 -> {
                sdkName = "Android 8"
            }
            28 -> {
                sdkName = "Android 9"
            }
            29 -> {
                sdkName = "Android 10"
            }
            30 -> {
                sdkName = "Android 11"
            }
            31 -> {
                sdkName = "Android 12"
            }
            32 -> {
                sdkName = "Android 12"
            }
            else -> {
                sdkName = "Android 13"
            }
        }

        return sdkName
    }

    private fun emailSubject(context: Context): String {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val version = pInfo.versionName
            val name = context.getString(R.string.app_name)//getApplicationName(context)
            return "$name - $version"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return context.getString(R.string.app_name)
    }

    fun startFeedbackEmail(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(Utils.feedbackEmail))
        intent.putExtra(Intent.EXTRA_SUBJECT, emailSubject(context))
        intent.putExtra(
            Intent.EXTRA_TEXT,
            "\n\n--Please write your question above this--\n${getDeviceInfo(context)}"
        )
        context.startActivity(Intent.createChooser(intent, "Email via..."))
    }

}