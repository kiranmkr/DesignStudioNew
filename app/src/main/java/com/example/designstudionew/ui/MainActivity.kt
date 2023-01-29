package com.example.designstudionew.ui

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.transition.TransitionManager
import com.android.billingclient.api.Purchase
import com.example.designstudionew.BuildConfig
import com.example.designstudionew.R
import com.example.designstudionew.billing.GBilling
import com.example.designstudionew.customCallBack.PopularClickListener
import com.example.designstudionew.customCallBack.TemplateClickCallBack
import com.example.designstudionew.databinding.*
import com.example.designstudionew.model.NewCategoryData
import com.example.designstudionew.model.NewDataModelJson
import com.example.designstudionew.model.RecyclerItemsModel
import com.example.designstudionew.recyclerAdapter.BottomMenuAdapter
import com.example.designstudionew.recyclerAdapter.MainRecyclerAdapter
import com.example.designstudionew.util.FeedbackUtils
import com.example.designstudionew.util.Utils
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import org.json.JSONArray
import pub.devrel.easypermissions.EasyPermissions
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TemplateClickCallBack,
    EasyPermissions.PermissionCallbacks {

    private lateinit var mainBinding: ActivityMainBinding
    private var workerHandler = Handler(Looper.getMainLooper())
    private var workerThread: ExecutorService = Executors.newCachedThreadPool()
    private lateinit var homeRoot: CustomHomeUiBinding
    private lateinit var settingRoot: SettingScreenBinding
    private lateinit var saveRoot: ButtomSheetLayoutBinding
    private lateinit var dialogRoot: DownloadDialogBinding
    private var cardListView: ArrayList<CardView> = ArrayList()

    private var newAssetsList: ArrayList<NewDataModelJson> = ArrayList()
    private var categoryList: ArrayList<NewCategoryData> = ArrayList()
    private var mainListAdapter: MainRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        homeRoot = CustomHomeUiBinding.bind(mainBinding.homeRoot.root)
        settingRoot = SettingScreenBinding.bind(mainBinding.settingRoot.root)
        saveRoot = ButtomSheetLayoutBinding.bind(mainBinding.savingRoot.root)
        dialogRoot = DownloadDialogBinding.bind(mainBinding.dialogRoot.root)

        FirebaseApp.initializeApp(this@MainActivity)

        updateBillingData()

        updateUi()

        updateUiClick()

    }

    private fun updateBillingData() {

        GBilling.setOnPurchasedObserver(this,
            object : Observer<Purchase> {
                override fun onChanged(t: Purchase?) {
                    if (t != null) {
                        if (GBilling.isSubscribedOrPurchasedSaved) {
                            proUser()
                        }
                    }
                }
            })

        GBilling.isSubscribedOrPurchased(
            Utils.subscriptionsKeyArray,
            Utils.inAppKeyArray,
            this,
            object : Observer<Boolean> {
                override fun onChanged(t: Boolean?) {
                    if (t != null) {
                        if (t) {
                            Log.d("myBilling", "Billing is buy")
                            proUser()
                        } else {
                            Log.d("myBilling", "Billing  is not  buy")
                            workerHandler.postDelayed({
                                startActivity(Intent(this@MainActivity, ProScreen::class.java))
                            }, 500)

                        }
                    }
                }

            }
        )

        GBilling.setOnErrorObserver(this,
            object : Observer<Int> {
                override fun onChanged(t: Int?) {

                    if (t != null) {
                        Log.d("myBillingError", "${t}")
                    }
                }
            })

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun proUser() {

        if (categoryList.isNotEmpty()) {
            Log.d("myList", "${categoryList.size}")
            mainListAdapter?.updateList(categoryList)
        }

        updateSettingListBilling()

        homeRoot.goPro.visibility = View.GONE

    }

    private fun updateUi() {

        homeSelection()
        settingScreenUi()

        cardListView.add(homeRoot.cardSvg)
        cardListView.add(homeRoot.cardSticker)
        cardListView.add(homeRoot.cardMono)
        cardListView.add(homeRoot.cardWaterColor)
        cardListView.add(homeRoot.cardShape)

        mainListAdapter = MainRecyclerAdapter()

        homeRoot.tvTitle.setOnClickListener {
            if (BuildConfig.DEBUG && GBilling.getConnectionStatus() && GBilling.isSubscribedOrPurchasedSaved) {
                GBilling.consumePurchase(Utils.inAppPurchasedkey, this) {
                    if (it) {
                        startActivity(Intent(this@MainActivity, MainActivity::class.java))
                        finish()
                    }
                }
            }
        }

        homeRoot.mainRecycler.adapter = mainListAdapter

        readJsonData()
    }

    private var listItems: ArrayList<RecyclerItemsModel> = ArrayList()
    private var newAdapter: BottomMenuAdapter? = null

    private fun settingScreenUi() {

        updateSettingList()

        newAdapter = BottomMenuAdapter(listItems)

        newAdapter?.upDateCallBack(object : PopularClickListener {
            override fun onPopularClick(position: String) {

                Log.d("myCallBack", position)

                when (position) {
                    "premium" -> {
                        if (GBilling.isSubscribedOrPurchasedSaved) {
                            Utils.showToast(
                                this@MainActivity,
                                getString(R.string.already_subscribed)
                            )
                            Log.d("myBilling", "billing is buy")
                        } else {
                            startActivity(Intent(this@MainActivity, ProScreen::class.java))
                        }
                    }
                    "purchase" -> {
                        Log.d("restore", "This is log to restore purchase")
                    }
                    "bug" -> {
                        Utils.feedBackDetails = "Report a Bug"
                        FeedbackUtils.startFeedbackEmail(this@MainActivity)
                    }
                    "feature" -> {
                        Utils.feedBackDetails = "Request a Feature"
                        FeedbackUtils.startFeedbackEmail(this@MainActivity)
                    }
                    "policy" -> {
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(Utils.policyLink)
                                )
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                    "service" -> {
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(Utils.termsCondition)
                                )
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }

                    }
                    "rate" -> {
                        try {

                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW, Uri
                                        .parse("market://details?id=$packageName")
                                )
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                    "share" -> {
                        try {

                            val i = Intent(Intent.ACTION_SEND)
                            i.type = "text/plain"
                            i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                            var sAux = "\nLet me recommend you this application\n\n"
                            sAux = """
                    ${sAux}https://play.google.com/store/apps/details?id=$packageName
                    """.trimIndent()
                            i.putExtra(Intent.EXTRA_TEXT, sAux)

                            startActivity(
                                Intent.createChooser(
                                    i,
                                    resources.getString(R.string.choose_one)
                                )
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }

                    }
                    "other_apps" -> {
                        try {
                            startActivity(
                                Intent(
                                    Intent.ACTION_VIEW, Uri
                                        .parse(Utils.moreAppLink)
                                )
                            )
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }

                    }
                    else -> {
                        Log.d("myPopularClick", "not Match above of this")
                    }
                }

            }

        })

        settingRoot.reMain.setHasFixedSize(true)
        settingRoot.reMain.adapter = newAdapter

        settingRoot.btnBack.setOnClickListener {
            showHomeRoot()
        }

    }

    private fun updateSettingList() {

        listItems.clear()

        if (GBilling.isSubscribedOrPurchasedSaved) {
            Log.d("myBilling", "billing is buy")
        } else {
            Log.d("myBilling", "billing is not  buy")
            listItems.add(RecyclerItemsModel(R.drawable.pro_icon, "Go Premium", "premium"))
        }

        listItems.add(RecyclerItemsModel(R.drawable.bug_icon, "Report a Bug", "bug"))
        listItems.add(RecyclerItemsModel(R.drawable.feature_icon, "Request a Feature", "feature"))
        listItems.add(RecyclerItemsModel(R.drawable.policy_icon, "Privacy policy", "policy"))
        listItems.add(RecyclerItemsModel(R.drawable.terms_icon, "Terms of Service", "service"))
        listItems.add(RecyclerItemsModel(R.drawable.rate_us_icon, "Rate this App", "rate"))
        listItems.add(RecyclerItemsModel(R.drawable.share_icon, "Share", "share"))
        listItems.add(RecyclerItemsModel(R.drawable.other_app_icon, "Other Apps", "other_apps"))

        Log.d("myListSize", "${listItems.size}")

    }

    private fun updateSettingListBilling() {

        listItems.clear()

        if (GBilling.isSubscribedOrPurchasedSaved) {
            Log.d("myBilling", "billing is buy")
        } else {
            Log.d("myBilling", "billing is not  buy")
            listItems.add(RecyclerItemsModel(R.drawable.pro_icon, "Go Premium", "premium"))
        }

        listItems.add(RecyclerItemsModel(R.drawable.bug_icon, "Report a Bug", "bug"))
        listItems.add(RecyclerItemsModel(R.drawable.feature_icon, "Request a Feature", "feature"))
        listItems.add(RecyclerItemsModel(R.drawable.policy_icon, "Privacy policy", "policy"))
        listItems.add(RecyclerItemsModel(R.drawable.terms_icon, "Terms of Service", "service"))
        listItems.add(RecyclerItemsModel(R.drawable.rate_us_icon, "Rate this App", "rate"))
        listItems.add(RecyclerItemsModel(R.drawable.share_icon, "Share", "share"))
        listItems.add(RecyclerItemsModel(R.drawable.other_app_icon, "Other Apps", "other_apps"))

        Log.d("myListSize", "${listItems.size}")

        newAdapter?.upIconList(listItems)

    }

    override fun onResume() {
        super.onResume()
        if (Utils.stat) {
            proUser()
        }
    }

    private fun readJsonData() {

        workerThread.execute {

            val readJsonList: String? = loadJSONFromAsset()

            if (readJsonList != null) {

                val jsonArrayAssets = JSONArray(readJsonList)
                newAssetsList.clear()

                for (i in 0 until jsonArrayAssets.length()) {
                    val dataModel = Gson().fromJson(
                        jsonArrayAssets[i].toString(), NewDataModelJson::class.java
                    )
                    newAssetsList.addAll(listOf(dataModel))
                }

                updateIndexList(1)

            } else {
                Log.d("readJsonData", "data is null")
                workerHandler.post {
                    Utils.showToast(this@MainActivity, getString(R.string.something_went_wrong))
                }
            }
        }
    }

    private fun updateIndexList(position: Int) {

        var categorySelection = "SVG"

        when (position) {
            1 -> {
                categorySelection = "SVG"
            }
            2 -> {
                categorySelection = "Water Color"
            }
            3 -> {
                categorySelection = "Monograms"
            }
            4 -> {
                categorySelection = "Stickers"
            }
            5 -> {
                categorySelection = "Shapes"
            }

        }

        Utils.mainCategory = categorySelection

        if (newAssetsList.isNotEmpty()) {

            for (i in 0 until newAssetsList.size) {

                if (newAssetsList[i].category == categorySelection) {

                    Log.d("myList", categorySelection)

                    categoryList.clear()

                    newAssetsList[i].totalCategory.forEachIndexed { index, newCategoryData ->
                        categoryList.add(index, newCategoryData)
                    }

                    workerHandler.post {

                        if (categoryList.isNotEmpty()) {

                            Log.d("myList", "${categoryList.size}")

                            mainListAdapter?.updateList(categoryList)
                        }

                    }

                }
            }
        }
    }

    //*******************This method return the Json String *********************//
    private fun loadJSONFromAsset(): String? {
        val json: String = try {
            val `is`: InputStream = assets.open("new_assets.json")
            val size = `is`.available()
            val buffer = ByteArray(size)
            `is`.read(buffer)
            `is`.close()
            String(buffer, StandardCharsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    private fun homeSelection() {

        mainBinding.imHome.isSelected = true

        mainBinding.tvHome.setTextColor(
            ContextCompat.getColor(
                this, R.color.colorAccent
            )
        )


        mainBinding.imSetting.isSelected = false
        mainBinding.tvSetting.setTextColor(
            ContextCompat.getColor(
                this, R.color.grayColor
            )
        )
    }

    private fun settingSelection() {

        mainBinding.imHome.isSelected = false
        mainBinding.tvHome.setTextColor(
            ContextCompat.getColor(
                this, R.color.grayColor
            )
        )

        mainBinding.imSetting.isSelected = true
        mainBinding.tvSetting.setTextColor(
            ContextCompat.getColor(
                this, R.color.colorAccent
            )
        )
    }

    private fun updateUiClick() {

        homeIconClick()

        saveSheetClick()

        homeRoot.root.setOnClickListener {
            Log.d("myEmptyClick", "empty click")
        }
        settingRoot.root.setOnClickListener {
            Log.d("myEmptyClick", "empty click")
        }
        saveRoot.root.setOnClickListener {
            Log.d("myEmptyClick", "empty click")
        }
        dialogRoot.root.setOnClickListener {
            Log.d("myEmptyClick", "empty click")
        }

        mainBinding.btnHome.setOnClickListener {
            showHomeRoot()
        }

        mainBinding.btnCustom.setOnClickListener {
            Utils.showToast(this, "calling Go To Custom")
        }

        saveRoot.savingWinRoot.setOnClickListener {
            disMissSavingRoot()
        }

        mainBinding.btnSetting.setOnClickListener {
            showSettingRoot()
        }

    }

    private fun showHomeRoot() {
        if (settingRoot.root.visibility == View.VISIBLE) {
            showAnimation()
            settingRoot.root.visibility = View.GONE
        }
        homeRoot.root.visibility = View.VISIBLE

        homeSelection()
    }

    private fun showSettingRoot() {
        if (homeRoot.root.visibility == View.VISIBLE) {
            showAnimation()
            homeRoot.root.visibility = View.GONE
        }
        settingRoot.root.visibility = View.VISIBLE
        settingSelection()
    }

    private fun saveSheetClick() {

        saveRoot.cardCustomize.setOnClickListener {
            saveRoot.root.visibility = View.GONE
            startActivity(Intent(this@MainActivity, EditingScreen::class.java))
        }

        saveRoot.cardSave.setOnClickListener {

            disMissSavingRoot()

            dialogRoot.root.visibility = View.VISIBLE

            try {

                Log.d(
                    "myFileName", "${Utils.mainCategory} --  " +
                            "${Utils.subCategory} -- ${Utils.fileLabelNumber}" +
                            " -- ${Utils.getFileExt()}"
                )

                val mRef = FirebaseStorage.getInstance().reference

                val completePath =
                    "/${Utils.mainCategory}/${Utils.subCategory}/${Utils.fileLabelNumber}${Utils.getFileExt()}"

                val islandRef = mRef.child(completePath)

                val tenMegabyte: Long = (1024 * 1024) * 10

                if (Utils.isNetworkAvailable(this)) {

                    islandRef.getBytes(tenMegabyte).addOnSuccessListener {

                        workerThread.execute {

                            val filePath = saveMediaToStorage(it)

                            Log.d("myLocalPath", "${filePath}")

                            workerHandler.postDelayed({

                                if (filePath != null) {
                                    showAnimation()
                                    dialogRoot.root.visibility = View.GONE
                                    Utils.showToast(this, "File is save this path ${filePath}")
                                } else {
                                    showAnimation()
                                    dialogRoot.root.visibility = View.GONE
                                }

                            }, 1000)

                        }

                    }.addOnFailureListener {
                        // Handle any errors
                        Log.d("myLocalPath", "byte is not download")
                        showAnimation()
                        dialogRoot.root.visibility = View.GONE
                    }
                } else {
                    showAnimation()
                    dialogRoot.root.visibility = View.GONE
                    Utils.showToast(this, getString(R.string.internet_not_connected))
                }


            } catch (ex: IOException) {
                ex.printStackTrace()
                showAnimation()
                dialogRoot.root.visibility = View.GONE
                Utils.showToast(this, getString(R.string.something_went_wrong))
            }

        }
    }

    private fun saveMediaToStorage(bytes: ByteArray): String? {

        var filePath: String? = null

        //Generating a file name
        val filename = "${Utils.fileLabelNumber}${Utils.getFileExt()}"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            applicationContext?.contentResolver?.also { resolver ->

                val dirDest =
                    File(Utils.getRootPath(), "${Utils.mainCategory}/${Utils.subCategory}")

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {
                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "$dirDest")
                }

                //MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                //Inserting the contentValues to contentResolver and getting the Uri
                // val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                val imageUri: Uri? = resolver.insert(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    contentValues
                )

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }

                filePath = dirDest.toString()

            }

        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val dirDest = File(Utils.getRootPath(), "${Utils.mainCategory}/${Utils.subCategory}")
            if (!dirDest.exists()) {
                dirDest.mkdirs()
            }
            val image = File(dirDest, filename)

            Log.e("myFilePath", "$image")

            fos = FileOutputStream(image)

            filePath = image.toString()

        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened

            it.write(bytes)

            it.flush()
            it.close()

            Log.e("myFileFos", "Saved to Photos Main")

        }

        return filePath
    }

    private fun disMissSavingRoot() {
        showAnimation()
        saveRoot.root.visibility = View.GONE
    }

    private fun homeIconClick() {

        homeRoot.goPro.setOnClickListener {
            gotoProScreen()
        }

        homeRoot.cardSticker.setOnClickListener {
            cardSelectionForAll(cardListView, it.id)
            updateIndexList(4)
        }

        homeRoot.cardSvg.setOnClickListener {
            cardSelectionForAll(cardListView, it.id)
            updateIndexList(1)

        }

        homeRoot.cardMono.setOnClickListener {
            cardSelectionForAll(cardListView, it.id)
            updateIndexList(3)
        }
        homeRoot.cardWaterColor.setOnClickListener {
            cardSelectionForAll(cardListView, it.id)
            updateIndexList(2)
        }
        homeRoot.cardShape.setOnClickListener {
            cardSelectionForAll(cardListView, it.id)
            updateIndexList(5)
        }
    }

    private fun cardSelectionForAll(views: ArrayList<CardView>, view_id: Int) {

        for (i in views.indices) {

            if (views[i].id == view_id) {
                views[i].setCardBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity, R.color.cardBack
                    )
                )
            } else {
                Log.d("myCard", "Card is disable ")
                views[i].setCardBackgroundColor(
                    ContextCompat.getColor(
                        this@MainActivity, R.color.colorAccent
                    )
                )
            }

        }
    }

    override fun onItemClickListener(labelStatus: Boolean) {

        if (labelStatus) {
            gotoProScreen()
        } else {
            goToEditingScreen()
        }
    }

    private fun goToEditingScreen() {

        if (EasyPermissions.hasPermissions(this@MainActivity, *Utils.readPermissionPass)) {
            Log.d("myPermission", "hasPermissions allow")

            if (saveRoot.root.visibility == View.GONE) {
                showAnimation()
                saveRoot.root.visibility = View.VISIBLE
            }

//            startActivity(Intent(this@MainActivity, EditingScreen::class.java))
        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity, "Please allow permissions to proceed further",
                Utils.request_read_permission, *Utils.readPermissionPass
            )
        }

    }

    private fun gotoProScreen() {
        startActivity(Intent(this@MainActivity, ProScreen::class.java))
    }

    private fun showAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TransitionManager.beginDelayedTransition(mainBinding.mainroot)
        }
    }

    override fun onBackPressed() {

        if (saveRoot.root.visibility == View.VISIBLE) {
            showAnimation()
            saveRoot.root.visibility = View.GONE
            return
        }

        if (settingRoot.root.visibility == View.VISIBLE) {
            showHomeRoot()
            return
        }

        super.onBackPressed()
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {

        when (requestCode) {
            Utils.request_read_permission -> {
                if (perms.size == Utils.readPermissionPass.size) {
                    goToEditingScreen()
                } else {
                    Log.d("myPermissionsGranted", "not all Permission allow")
                }
            }
            else -> {
                Log.d("myPermissionsGranted", "no any  Permission allow")
            }
        }

    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }
}