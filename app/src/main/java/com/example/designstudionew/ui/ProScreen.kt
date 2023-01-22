package com.example.designstudionew.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.SkuDetails
import com.example.designstudionew.R
import com.example.designstudionew.billing.GBilling
import com.example.designstudionew.databinding.ActivityProScreenBinding
import com.example.designstudionew.model.CustomModel
import com.example.designstudionew.recyclerAdapter.ScrollCustomAdapter
import com.example.designstudionew.util.Utils
import java.util.ArrayList

class ProScreen : AppCompatActivity() {

    private var selectPrices: Int = 3

    private var workerHandler = Handler(Looper.getMainLooper())
    private lateinit var mainBinding: ActivityProScreenBinding

    private lateinit var imageArrayList: ArrayList<CustomModel>
    private lateinit var adapter: ScrollCustomAdapter

    private var bgLayoutButtonBar: ArrayList<CardView> = ArrayList()

    private var imageArray = listOf(
        R.drawable.banner_1,
        R.drawable.banner_2,
        R.drawable.banner_3,
        R.drawable.banner_1,
        R.drawable.banner_2,
        R.drawable.banner_3
    )

    //handle scroll count
    var scrollCount: Int = 0

    private lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityProScreenBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        updateBillingData()

        imageArrayList = eatFruits()

        initLayoutManager()

        updateUi()
    }

    private fun eatFruits(): ArrayList<CustomModel> {
        val list = ArrayList<CustomModel>()
        for (i in 0..5) {
            val fruitModel = CustomModel(imageArray[i])
            list.add(fruitModel)
        }
        return list
    }

    private fun updateUi() {

        bgLayoutButtonBar.add(mainBinding.cardView17)
        bgLayoutButtonBar.add(mainBinding.cardView16)
        bgLayoutButtonBar.add(mainBinding.cardView15)
        bgLayoutButtonBar.add(mainBinding.cardView6)

        mainBinding.textView52.text = getString(R.string._3_days_trial)
        mainBinding.textView14.text = "${getString(R.string.then)}${getString(R.string.year_after)}"
        mainBinding.textView14.visibility = View.VISIBLE

        mainBinding.cardView17.setOnClickListener {
            alphaManager(bgLayoutButtonBar, it.id)
            selectPrices = 4
            mainBinding.textView52.text = getString(R.string.purchase_start)
            mainBinding.textView14.visibility = View.GONE
        }

        mainBinding.cardView16.setOnClickListener {
            alphaManager(bgLayoutButtonBar, it.id)
            selectPrices = 3
            mainBinding.textView52.text = getString(R.string._3_days_trial)
            mainBinding.textView14.visibility = View.VISIBLE
        }

        mainBinding.cardView15.setOnClickListener {
            alphaManager(bgLayoutButtonBar, it.id)
            selectPrices = 2
            mainBinding.textView52.text = getString(R.string.subscription_start)
            mainBinding.textView14.visibility = View.GONE
        }

        mainBinding.cardView6.setOnClickListener {
            alphaManager(bgLayoutButtonBar, it.id)
            selectPrices = 1
            mainBinding.textView52.text = getString(R.string.subscription_start)
            mainBinding.textView14.visibility = View.GONE
        }

        mainBinding.btnBack3.setOnClickListener {
            finish()
        }

        mainBinding.freePlan.setOnClickListener {
            finish()
        }

        mainBinding.btnBuy.setOnClickListener {
            when (selectPrices) {
                1 -> {
                    if (GBilling.getConnectionStatus() && isNetworkAvailable()) {
                        GBilling.subscribe(this@ProScreen, Utils.inAppWeekly)
                    } else {
                        Utils.showToast(this, getString(R.string.internet_not_connected))
                    }
                }
                2 -> {
                    if (GBilling.getConnectionStatus() && isNetworkAvailable()) {
                        GBilling.subscribe(this@ProScreen, Utils.inAppMonthly)
                    } else {
                        Utils.showToast(this, getString(R.string.internet_not_connected))

                    }
                }
                3 -> {
                    if (GBilling.getConnectionStatus() && isNetworkAvailable()) {
                        GBilling.subscribe(this@ProScreen, Utils.inAppYearly)
                    } else {
                        Utils.showToast(this, getString(R.string.internet_not_connected))
                    }
                }
                4 -> {
                    if (GBilling.getConnectionStatus() && isNetworkAvailable()) {
                        GBilling.purchase(this@ProScreen, Utils.inAppPurchasedkey)
                    } else {
                        Utils.showToast(this, getString(R.string.internet_not_connected))
                    }
                }
                else -> {
                    Log.d("myBuy", "Select any Plan")
                }

            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
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

    private fun alphaManager(views: ArrayList<CardView>, view_id: Int) {
        for (i in views.indices) {
            if (views[i].id == view_id) {
                views[i].setCardBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.colorAccent
                    )
                )
            } else {
                views[i].setCardBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.grayColor
                    )
                )
            }
        }
    }

    private fun updateBillingData() {

        if (GBilling.getConnectionStatus()) {

            if (GBilling.isSubscribedOrPurchasedSaved) {

                mainBinding.textView52.text = getString(R.string.already_subscribed)
                mainBinding.btnBuy.isClickable = false

                mainBinding.tvWeekly.text = "------"
                mainBinding.tvMonthly.text = "------"
                mainBinding.tvYearly.text = "------"
                mainBinding.tvLifeTime.text = "------"

            } else {

                if (!(isDestroyed || isFinishing)) {

                    val inAppString = ArrayList<String>()
                    inAppString.add(Utils.inAppPurchasedkey)

                    GBilling.getInAppSkuDetails(
                        inAppString,
                        false,
                        this,
                        object : Observer<List<SkuDetails>> {
                            override fun onChanged(t: List<SkuDetails>?) {
                                t?.let {
                                    if (it.isNotEmpty()) {
                                        Log.d("error", it[0].price)
                                        mainBinding.tvLifeTime.text = it[0].price

                                    } else {
                                        mainBinding.tvLifeTime.text = "------"
                                    }
                                }
                            }
                        })


                    val inAppWeekly = ArrayList<String>()
                    inAppWeekly.add(Utils.inAppWeekly)

                    GBilling.getSubscriptionsSkuDetails(
                        inAppWeekly,
                        false,
                        this,
                        object : Observer<List<SkuDetails>> {
                            override fun onChanged(t: List<SkuDetails>?) {
                                t?.let {
                                    if (it.isNotEmpty()) {
                                        Log.d("error", it[0].price)
                                        mainBinding.tvWeekly.text = it[0].price

                                    } else {
                                        mainBinding.tvWeekly.text = "------"
                                    }
                                }
                            }
                        })


                    val inAppMonthly = ArrayList<String>()
                    inAppMonthly.add(Utils.inAppMonthly)

                    GBilling.getSubscriptionsSkuDetails(
                        inAppMonthly,
                        false,
                        this,
                        object : Observer<List<SkuDetails>> {
                            override fun onChanged(t: List<SkuDetails>?) {
                                t?.let {
                                    if (it.isNotEmpty()) {
                                        Log.d("error", it[0].price)
                                        mainBinding.tvMonthly.text = it[0].price

                                    } else {
                                        mainBinding.tvMonthly.text = "------"
                                    }
                                }
                            }
                        })

                    val inAppYearly = ArrayList<String>()
                    inAppYearly.add(Utils.inAppYearly)

                    GBilling.getSubscriptionsSkuDetails(
                        inAppYearly,
                        false,
                        this,
                        object : Observer<List<SkuDetails>> {
                            override fun onChanged(t: List<SkuDetails>?) {
                                t?.let {
                                    if (it.isNotEmpty()) {
                                        Log.d("error", it[0].price)
                                        mainBinding.tvYearly.text = it[0].price
                                        mainBinding.textView14.text = "${getString(R.string.then)} ${it[0].price}${getString(R.string.year_after)}"
                                        Log.d("myPrices","${getString(R.string.then)} ${it[0].price}${getString(R.string.year_after)}")
                                    } else {
                                        mainBinding.tvYearly.text = "------"
                                    }
                                }
                            }
                        })
                }
            }
        }

        GBilling.setOnPurchasedObserver(this,
            object : Observer<Purchase> {
                override fun onChanged(t: Purchase?) {
                    if (t != null) {
                        if (GBilling.isSubscribedOrPurchasedSaved) {
                            finish()
                        }
                    }
                }
            })

        GBilling.setOnErrorObserver(this,
            object : Observer<Int> {
                override fun onChanged(t: Int?) {
                    if (t != null) {
                        Log.d("myBilling", "${t}")
                    }
                }
            })

    }

    private fun initLayoutManager() {

        layoutManager = object : LinearLayoutManager(this@ProScreen) {
            override fun smoothScrollToPosition(
                recyclerView: RecyclerView,
                state: RecyclerView.State?,
                position: Int
            ) {
                val smoothScroller = object : LinearSmoothScroller(this@ProScreen) {
                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                        return 5.0f
                    }
                }
                smoothScroller.targetPosition = position
                startSmoothScroll(smoothScroller)
            }
        }
        adapter = object : ScrollCustomAdapter(this@ProScreen, imageArrayList) {
            override fun load() {
                if (layoutManager.findFirstVisibleItemPosition() > 1) {
                    adapter.notifyItemMoved(0, imageArrayList.size - 1)
                }
            }
        }

        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        mainBinding.recycler.layoutManager = layoutManager
        mainBinding.recycler.setHasFixedSize(true)
        mainBinding.recycler.adapter = adapter
        autoScroll()
    }

    private fun autoScroll() {
        scrollCount = 0
        val speedScroll: Long = 50
        val runnable = object : Runnable {
            override fun run() {
                if (layoutManager.findFirstVisibleItemPosition() >= imageArrayList.size / 2) {
                    adapter.load()

                }
                mainBinding.recycler.smoothScrollToPosition(scrollCount++)
                workerHandler.postDelayed(this, speedScroll)
            }
        }
        workerHandler.postDelayed(runnable, speedScroll)
    }
}