package com.example.designstudionew.billing

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.SkuType
import com.android.billingclient.api.Purchase.PurchaseState.PURCHASED

object GBilling {
    private var savedApplicationContext: Context? = null
    private var isConnecting = false
    const val IsSubscribedUserConst = "isSubscribedUser"
    const val IsPurchasedUserConst = "IsPurchasedUserConst"
    const val IsSubscribedOrPurchasedUserConst = "IsSubscribedOrPurchasedUserConst"
    private var preferences: SharedPreferences? = null
    var isSubscribedSaved: Boolean
        get() = preferences?.getBoolean(IsSubscribedUserConst, false) ?: false
        set(value) = if (preferences != null) preferences!!.edit()
            .putBoolean(IsSubscribedUserConst, value).apply() else {
        }
    var isPurchasedSaved: Boolean
        get() = preferences?.getBoolean(IsPurchasedUserConst, false) ?: false
        set(value) = if (preferences != null) preferences!!.edit()
            .putBoolean(IsPurchasedUserConst, value).apply() else {
        }
    var isSubscribedOrPurchasedSaved: Boolean
        get() = preferences?.getBoolean(IsSubscribedOrPurchasedUserConst, false) ?: false
        set(value) = if (preferences != null) preferences!!.edit().putBoolean(
            IsSubscribedOrPurchasedUserConst, value
        ).apply() else {
        }

    @JvmStatic
    private val onErrorObserver = MutableLiveData<Int>()
    private fun setOnError(errorCodeLocal: Int) {
        onErrorObserver.postValue(errorCodeLocal)
    }

    fun getOnErrorValue(): Int = onErrorObserver.value ?: 0
    fun setOnErrorObserver(owner: LifecycleOwner, observer: Observer<Int>) {
        onErrorObserver.observe(owner, observer)
    }

    @JvmStatic
    private val connectionStatusObserver = MutableLiveData<Boolean>()
    private fun setConnectionStatus(isConnectedLocal: Boolean) {
        connectionStatusObserver.postValue(isConnectedLocal)
    }

    fun getConnectionStatus(): Boolean {
        val value = connectionStatusObserver.value ?: false
        if (!value && savedApplicationContext != null) startConnection(savedApplicationContext!!)
        return value
    }

    fun setConnectionStatusObserver(owner: LifecycleOwner, observer: Observer<Boolean>) {
        connectionStatusObserver.observe(owner, observer)
    }

    @JvmStatic
    private val onPurchasedObserver = MutableLiveData<Purchase>()
    private fun setOnPurchased(onPurchasedLocal: Purchase) {
        onPurchasedObserver.postValue(onPurchasedLocal)
    }

    fun getOnPurchasedValue(): Purchase? = onPurchasedObserver.value
    fun setOnPurchasedObserver(owner: LifecycleOwner, observer: Observer<Purchase>) {
        onPurchasedObserver.observe(owner, observer)
    }

    private var autoAcknowledgePurchase = true
    private var autoAcknowledgeSubscription = true
    private var billingClient: BillingClient? = null

    @JvmStatic
    private var isConnected = false
    private var purchaseType = ""
    private var savedProductId = ""

    @JvmStatic
    var TAG = "BillingClass"

    @JvmStatic
    private var isSubscriptionCached = false

    @JvmStatic
    private var isInAppCached = false

    @JvmStatic
    private var cachedSubscriptionStatusList: ArrayList<String> = ArrayList()

    @JvmStatic
    private var cachedInAppStatusList: ArrayList<String> = ArrayList()

    @JvmStatic
    private var cachedInAppAcknowledgedStatusList: ArrayList<String> = ArrayList()

    @JvmStatic
    private var cachedSubscriptionAcknowledgedStatusList: ArrayList<String> = ArrayList()

    @JvmStatic
    fun getPriceValueFromMicros(value: Long): Double {
        return value.toDouble() / 1000000.0
    }

    private const val CALL_BACK = "CALL_BACK"
    private const val NO_CALL_BACK = "NO_CALL_BACK"

    object ResponseCodes {
        const val SERVICE_TIMEOUT = -3
        const val FEATURE_NOT_SUPPORTED = -2
        const val SERVICE_DISCONNECTED = -1
        const val OK = 0
        const val USER_CANCELED = 1
        const val SERVICE_UNAVAILABLE = 2
        const val BILLING_UNAVAILABLE = 3
        const val ITEM_UNAVAILABLE = 4
        const val DEVELOPER_ERROR = 5
        const val ERROR = 6
        const val ITEM_ALREADY_OWNED = 7
        const val ITEM_NOT_OWNED = 8
        const val NOT_PURCHASED_STATE = 109
        const val NOT_CONNECTED = 110
        const val ALREADY_ACKNOWLEDGED = 111
    }

    @JvmStatic
    fun initializeInAppClass(context: Context) {
        savedApplicationContext = context
        val purchaseUpdateListener = PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases?.forEach { itPurchase ->
                    if (itPurchase.skus.contains(savedProductId)) {
                        Log.e(TAG, " PurchaseUpdatedListener Called")
                        if (purchaseType == SkuType.SUBS) {
                            if (!autoAcknowledgeSubscription) {
                                if (itPurchase.purchaseState == PURCHASED) {
                                    isSubscribedSaved = true
                                    isSubscribedOrPurchasedSaved = true
                                    setOnPurchased(itPurchase)
//                                    billingHandler?.onPurchased(itPurchase)
                                }
                            } else {
                                Log.e(TAG, "acknowledge Called1")
                                acknowledgePurchase(itPurchase, CALL_BACK)
                            }
                        }
                        if (purchaseType == SkuType.INAPP) {
                            if (!autoAcknowledgePurchase) {
                                if (itPurchase.purchaseState == PURCHASED) {
                                    isPurchasedSaved = true
                                    isSubscribedOrPurchasedSaved = true
                                    setOnPurchased(itPurchase)
//                                    billingHandler?.onPurchased(itPurchase)
                                }
                            } else {
                                Log.e(TAG, "acknowledge Called2")
                                acknowledgePurchase(itPurchase, CALL_BACK)
                            }
                        }
                    }
                }
            } else {
                Log.e(TAG, "ErrorPoint1")
                setOnError(billingResult.responseCode)
//                billingHandler?.onBillingError(billingResult.responseCode)
            }
        }
        billingClient = BillingClient.newBuilder(context)
            .setListener(purchaseUpdateListener)
            .enablePendingPurchases()
            .build()
        startConnection(context)
    }

    @JvmStatic
    fun isSubscribedOrPurchased(
        subscriptionList: ArrayList<String>?,
        inAppList: ArrayList<String>?,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Boolean>
    ) {
        val subscribeAndPurchasedLiveData = MutableLiveData<Boolean>()
        subscribeAndPurchasedLiveData.observe(lifecycleOwner, observer)
        var subResult: Boolean? = null
        var inAppResult: Boolean? = null
        subscriptionList?.let { itList ->
            isSubscribedAnyPrivate(itList) { isSubscribedLocal ->
                subResult = isSubscribedLocal
                if (inAppList != null && inAppList.size > 0) {
                    if (inAppResult != null) {
                        isSubscribedSaved = subResult!!
                        isPurchasedSaved = inAppResult!!
                        isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                        subscribeAndPurchasedLiveData.postValue(isSubscribedOrPurchasedSaved)
                    }
                } else {
                    isSubscribedSaved = subResult!!
                    isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                    subscribeAndPurchasedLiveData.postValue(isSubscribedOrPurchasedSaved)

                }

            }
            Log.e(TAG, "Sub $subResult")
        }
        inAppList?.let { itList ->
            isPurchasedAnyPrivate(itList) { isPurchased ->
                inAppResult = isPurchased
                if (subscriptionList != null && subscriptionList.size > 0) {
                    if (subResult != null) {
                        isSubscribedSaved = subResult!!
                        isPurchasedSaved = inAppResult!!
                        isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                        subscribeAndPurchasedLiveData.postValue(isSubscribedOrPurchasedSaved)
                    }
                } else {
                    isPurchasedSaved = inAppResult!!
                    isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                    subscribeAndPurchasedLiveData.postValue(isSubscribedOrPurchasedSaved)
                }
            }
            Log.e(TAG, "InApp $inAppResult")
        }
    }

    @JvmStatic
    fun isSubscribed(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Boolean>
    ) {
        val isSubscribedObserver = MutableLiveData<Boolean>()
        isSubscribedObserver.observe(lifecycleOwner, observer)
        if (isSubscriptionCached) {
            Log.e(TAG, "Cached Result $cachedSubscriptionStatusList")
            val result = cachedSubscriptionStatusList.contains(productId)
            isSubscribedObserver.postValue(result)
            isSubscribedSaved = result
            isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
        } else {
            isSubscribedRealtime(productId) { isSubscribed ->
                isSubscribedObserver.postValue(isSubscribed)
                isSubscribedSaved = isSubscribed
                isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                Log.e(TAG, "Not Cached Result $isSubscribed")
            }
        }
    }

    @JvmStatic
    fun isPurchased(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Boolean>
    ) {
        val isPurchasedObserver = MutableLiveData<Boolean>()
        isPurchasedObserver.observe(lifecycleOwner, observer)
        if (isInAppCached) {
            Log.e(TAG, "Cached")
            val result = cachedInAppStatusList.contains(productId)
            isPurchasedObserver.postValue(result)
            isPurchasedSaved = result
            isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
        } else {
            isPurchasedRealtime(productId) { isPurchased ->
                isPurchasedObserver.postValue(isPurchased)
                isPurchasedSaved = isPurchased
                isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                Log.e(TAG, "Not Cached")
            }
        }
    }

    @JvmStatic
    fun isPurchasedAndAcknowledged(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Boolean>
    ) {
        val isPurchasedAndAcknowledgedObserver = MutableLiveData<Boolean>()
        isPurchasedAndAcknowledgedObserver.observe(lifecycleOwner, observer)
        if (isInAppCached) {
            Log.e(TAG, "Cached")
            val result = cachedInAppAcknowledgedStatusList.contains(productId)
            isPurchasedAndAcknowledgedObserver.postValue(result)
        } else {
            Log.e(TAG, "Not Cached")
            isPurchasedAndAcknowledgedRealtime(productId) {
                isPurchasedAndAcknowledgedObserver.postValue(it)
            }
        }
    }

    @JvmStatic
    fun isSubscribedAndAcknowledged(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Boolean>
    ) {
        val isSubscribedAndAcknowledgedObserver = MutableLiveData<Boolean>()
        isSubscribedAndAcknowledgedObserver.observe(lifecycleOwner, observer)
        if (isSubscriptionCached) {
            Log.e(TAG, "Cached")
            isSubscribedAndAcknowledgedObserver.postValue(
                cachedSubscriptionAcknowledgedStatusList.contains(
                    productId
                )
            )
        } else {
            Log.e(TAG, "Not Cached")
            isSubscribedAndAcknowledgedRealtime(productId) {
                isSubscribedAndAcknowledgedObserver.postValue(it)
            }
        }
    }

    private fun isSubscribedAnyPrivate(
        productIdList: ArrayList<String>,
        callback: (isSubscribed: Boolean) -> Unit
    ) {
        if (isSubscriptionCached) {
            var check = false
            productIdList.forEach { itProductId ->
                Log.e(TAG, "Cached Any Result  $cachedSubscriptionStatusList  $ ")
                if (cachedSubscriptionStatusList.contains(itProductId)) {
                    if (!check) {
                        check = true
                        isSubscribedSaved = check
                        isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                    }
                }
            }
            callback(check)
        } else {
            isSubscribedAnyRealtime(productIdList) {
                isSubscribedSaved = it
                isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                callback(it)
                Log.e(TAG, "Not Cached Any Result $it")
            }
        }
    }

    @JvmStatic
    fun isSubscribedAny(
        productIdList: ArrayList<String>,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Boolean>
    ) {
        val isSubscribedAnyObserver = MutableLiveData<Boolean>()
        isSubscribedAnyObserver.observe(lifecycleOwner, observer)
        if (isSubscriptionCached) {
            var check = false
            productIdList.forEach { itProductId ->
                Log.e(TAG, "Cached Any Result  $cachedSubscriptionStatusList  $ ")
                if (cachedSubscriptionStatusList.contains(itProductId)) {
                    if (!check) {
                        check = true
                        isSubscribedSaved = check
                        isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                    }
                }
            }
            isSubscribedAnyObserver.postValue(check)
        } else {
            isSubscribedAnyRealtime(productIdList) {
                isSubscribedSaved = it
                isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                isSubscribedAnyObserver.postValue(it)
                Log.e(TAG, "Not Cached Any Result $it")
            }
        }
    }

    private fun isPurchasedAnyPrivate(
        productIdList: ArrayList<String>,
        callback: (isSubscribed: Boolean) -> Unit
    ) {
        if (isInAppCached) {
            var check = false
            Log.e(TAG, "Cached")
            productIdList.forEach { itProductId ->
                if (cachedInAppStatusList.contains(itProductId)) {
                    if (!check) {
                        check = true
                        isPurchasedSaved = check
                        isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                    }
                }
            }
            callback(check)
        } else {
            Log.e(TAG, "Not Cached")
            isPurchasedAnyRealtime(productIdList) {
                isPurchasedSaved = it
                isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                callback(it)
            }
        }
    }

    @JvmStatic
    fun isPurchasedAny(
        productIdList: ArrayList<String>,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Boolean>
    ) {
        val isPurchasedAnyObserver = MutableLiveData<Boolean>()
        isPurchasedAnyObserver.observe(lifecycleOwner, observer)
        if (isInAppCached) {
            var check = false
            Log.e(TAG, "Cached")
            productIdList.forEach { itProductId ->
                if (cachedInAppStatusList.contains(itProductId)) {
                    if (!check) {
                        check = true
                        isPurchasedSaved = check
                        isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                    }
                }
            }
            isPurchasedAnyObserver.postValue(check)
        } else {
            Log.e(TAG, "Not Cached")
            isPurchasedAnyRealtime(productIdList) {
                isPurchasedSaved = it
                isSubscribedOrPurchasedSaved = isSubscribedSaved || isPurchasedSaved
                isPurchasedAnyObserver.postValue(it)
            }
        }
    }

    private fun startConnection(context: Context) {
        if (!isConnecting) {
            isConnecting = true
            preferences = context.getSharedPreferences(
                context.packageName + "_billing_preferences",
                ContextWrapper.MODE_PRIVATE
            )
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    isConnecting = false
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "ConnectedBillingA")
                        isConnected = true
                        Log.e(TAG, "ConnectedBillingB")
                        setConnectionStatus(true)
                        Log.e(TAG, "ConnectedBillingC")
                        // The BillingClient is ready. You can query purchases here.
                        if (isInAppCached && isSubscriptionCached) {
                            isConnected = true
//                        billingHandler?.onBillingInitialized()
                        } else {
                            if (!isInAppCached) {
                                reloadInAppCache()
                            }
                            if (!isSubscriptionCached) {
                                reloadSubscriptionCache()
                            }
                            if (isInAppCached && isInAppCached) {
                                isConnected = true

//                            billingHandler?.onBillingInitialized()
                            }
                        }
                    } else {
                        Log.e(TAG, "ErrorPoint2")
                        setOnError(billingResult.responseCode)
//                    billingHandler?.onBillingError(billingResult.responseCode)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Try to restart the connection on the next request to
                    // Google Play by calling the startConnection() method.
                    isConnected = false
                    Log.e(TAG, "Disconnected")
                    setConnectionStatus(false)
                    startConnection(context)
//                billingHandler?.onBillingServiceDisconnected()
                }
            })
        }

    }

    @JvmStatic
    fun getSubscriptionsSkuDetails(
        productIdList: ArrayList<String>,
        isToSort: Boolean = false,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<List<SkuDetails>>
    ) {
        val subscriptionSkuListObserver = MutableLiveData<List<SkuDetails>>()
        subscriptionSkuListObserver.observe(lifecycleOwner, observer)
        getSkuDetails(
            SkuType.SUBS,
            productIdList,
            isToSort
        ) { error: Int?, list: List<SkuDetails>? ->
            if (error == null && list != null) subscriptionSkuListObserver.postValue(
                list
            )
        }
    }

    @JvmStatic
    fun getInAppSkuDetails(
        productIdList: ArrayList<String>,
        isToSort: Boolean = false,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<List<SkuDetails>>
    ) {
        val purchaseSkuListObserver = MutableLiveData<List<SkuDetails>>()
        purchaseSkuListObserver.observe(lifecycleOwner, observer)
        getSkuDetails(
            SkuType.INAPP,
            productIdList,
            isToSort
        ) { error: Int?, list: List<SkuDetails>? ->
            if (error == null && list != null) purchaseSkuListObserver.postValue(
                list
            )
        }
    }

    @JvmStatic
    fun purchase(activity: Activity, productId: String) {
        subscribeOrPurchase(activity, productId, SkuType.INAPP)
    }

    @JvmStatic
    fun subscribe(activity: Activity, productId: String) {
        subscribeOrPurchase(activity, productId, SkuType.SUBS)
    }

    @JvmStatic
    fun purchaseWithCustomParams(
        activity: Activity,
        productId: String,
        obfuscatedProfileId: String,
        obfuscatedAccountId: String
    ) {
        subscribeOrPurchase(
            activity,
            productId,
            SkuType.INAPP,
            true,
            obfuscatedProfileId,
            obfuscatedAccountId
        )
    }

    @JvmStatic
    fun subscribeWithCustomParams(
        activity: Activity,
        productId: String,
        obfuscatedProfileId: String,
        obfuscatedAccountId: String
    ) {
        subscribeOrPurchase(
            activity,
            productId,
            SkuType.SUBS,
            true,
            obfuscatedProfileId,
            obfuscatedAccountId
        )
    }

    private fun isSubscribedRealtime(productId: String, callback: (isSubscribed: Boolean) -> Unit) {
        var check = false
        getSubscriptionPurchaseList { itPurchaseList ->
            Log.e(TAG, "SubscribedList Size" + itPurchaseList!!.size)
            cachedSubscriptionStatusList.clear()
            cachedSubscriptionAcknowledgedStatusList.clear()
            isSubscriptionCached = true
            itPurchaseList.forEach { itPurchase ->
                if (!check) {
                    check = itPurchase.skus.contains(productId)
                }
                cachedSubscriptionStatusList.add(productId)
                if (!itPurchase.isAcknowledged && autoAcknowledgeSubscription && itPurchase.purchaseState == PURCHASED) {
                    Log.e(TAG, "acknowledge Called3")
                    acknowledgePurchase(itPurchase, NO_CALL_BACK)
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    cachedSubscriptionAcknowledgedStatusList.add(productId)
                }
                Log.e(
                    TAG,
                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString() + "skuSize" + cachedSubscriptionStatusList.size
                )
            }
            Log.e(TAG, "skuDetails $cachedSubscriptionStatusList")
            callback(check)
        }
    }

    @JvmStatic
    fun getSubscriptionReceiptDetails(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Purchase>
    ) {
        val purchaseObserverLocalLiveData = MutableLiveData<Purchase>()
        purchaseObserverLocalLiveData.observe(lifecycleOwner, observer)
        var check = false
        var purchase1: Purchase? = null
        getSubscriptionPurchaseList { itPurchaseList ->
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            cachedSubscriptionStatusList.clear()
            cachedSubscriptionAcknowledgedStatusList.clear()
            isSubscriptionCached = true
            itPurchaseList?.forEach { itPurchase ->
                if (!check) {
                    check = itPurchase.skus.contains(productId)
                    if (check) {
                        purchaseObserverLocalLiveData.postValue(itPurchase)
                    }
                }
                cachedSubscriptionStatusList.add(productId)
                if (!itPurchase.isAcknowledged && autoAcknowledgeSubscription && itPurchase.purchaseState == PURCHASED) {
                    Log.e(TAG, "acknowledge Called4")
                    acknowledgePurchase(itPurchase, NO_CALL_BACK)
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    cachedSubscriptionAcknowledgedStatusList.add(productId)
                }
                Log.e(
                    TAG,
                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
            }
        }
    }

    private fun isPurchasedRealtime(productId: String, callback: (isSubscribed: Boolean) -> Unit) {
        var check = false
        getInAppPurchaseList { itPurchaseList ->
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            cachedInAppStatusList.clear()
            cachedInAppAcknowledgedStatusList.clear()
            isInAppCached = true
            itPurchaseList?.forEach { itPurchase ->
                if (!check) {
                    check =
                        itPurchase.skus.contains(productId) && itPurchase.purchaseState == PURCHASED
                }
                if (itPurchase.purchaseState == PURCHASED) {
                    cachedInAppStatusList.add(productId)
                    if (!itPurchase.isAcknowledged && autoAcknowledgePurchase) {
                        Log.e(TAG, "acknowledge Called5")
                        acknowledgePurchase(itPurchase, NO_CALL_BACK)
                    }
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    cachedInAppAcknowledgedStatusList.add(productId)
                }
                Log.e(
                    TAG,
                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
            }
            callback(check)
        }
    }

    private fun isPurchasedAndAcknowledgedRealtime(
        productId: String,
        callback: (isSubscribed: Boolean) -> Unit
    ) {
        var check = false
        getInAppPurchaseList { itPurchaseList ->
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            cachedInAppStatusList.clear()
            cachedInAppAcknowledgedStatusList.clear()
            isInAppCached = true
            itPurchaseList?.forEach { itPurchase ->
                if (!check) {
                    check =
                        itPurchase.skus.contains(productId) && itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged
                }
                if (itPurchase.purchaseState == PURCHASED) {
                    cachedInAppStatusList.add(productId)
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    cachedInAppAcknowledgedStatusList.add(productId)
                }
                Log.e(
                    TAG,
                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
            }
            callback(check)
        }
    }

    private fun isSubscribedAndAcknowledgedRealtime(
        productId: String,
        callback: (isSubscribed: Boolean) -> Unit
    ) {
        var check = false
        getInAppPurchaseList { itPurchaseList ->
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            cachedSubscriptionStatusList.clear()
            cachedSubscriptionAcknowledgedStatusList.clear()
            isSubscriptionCached = true
            itPurchaseList?.forEach { itPurchase ->
                if (!check) {
                    check =
                        itPurchase.skus.contains(productId) && itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged
                }
                cachedSubscriptionStatusList.add(productId)
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    cachedSubscriptionAcknowledgedStatusList.add(productId)
                }
                Log.e(
                    TAG,
                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
            }
            callback(check)
        }
    }

    @JvmStatic
    fun getInAppReceiptDetails(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        observer: Observer<Purchase>
    ) {
        val purchaseObserverLocalLiveData = MutableLiveData<Purchase>()
        purchaseObserverLocalLiveData.observe(lifecycleOwner, observer)
        var check = false
        getInAppPurchaseList { itPurchaseList ->
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            cachedInAppStatusList.clear()
            cachedInAppAcknowledgedStatusList.clear()
            isInAppCached = true
            itPurchaseList?.forEach { itPurchase ->
                if (!check) {
                    check =
                        itPurchase.skus.contains(productId) && itPurchase.purchaseState == PURCHASED
                    if (check) {
                        purchaseObserverLocalLiveData.postValue(itPurchase)
                    }
                }
                if (itPurchase.purchaseState == PURCHASED) {
                    cachedInAppStatusList.add(productId)
                    if (!itPurchase.isAcknowledged && autoAcknowledgePurchase) {
                        Log.e(TAG, "acknowledge Called6")
                        acknowledgePurchase(itPurchase, NO_CALL_BACK)
                    }
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    cachedInAppAcknowledgedStatusList.add(productId)
                }
                Log.e(
                    TAG,
                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
            }
        }
    }

    @JvmStatic
    fun acknowledgePurchase(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        acknowledgeObserver: Observer<Boolean>
    ) {
        val acknowledgeObserverLocalLiveData = MutableLiveData<Boolean>()
        acknowledgeObserverLocalLiveData.observe(lifecycleOwner, acknowledgeObserver)
        var purchase1: Purchase? = null
        if (isConnected) {
            billingClient?.queryPurchasesAsync(SkuType.INAPP, object : PurchasesResponseListener {
                override fun onQueryPurchasesResponse(
                    p0: BillingResult,
                    p1: MutableList<Purchase>
                ) {
                    Log.e(TAG, p0.responseCode.toString())
                    if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "SubscribedList Size" + p1.size)
                        cachedInAppStatusList.clear()
                        cachedInAppAcknowledgedStatusList.clear()
                        isInAppCached = true
                        p1.forEach { itPurchase ->
                            if (itPurchase.skus.contains(productId)) {
                                purchase1 = itPurchase
                                Log.e(
                                    TAG,
                                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                                )
                            }
                            if (itPurchase.purchaseState == PURCHASED) {
                                cachedInAppStatusList.add(productId)
                            }
                            if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                                cachedInAppAcknowledgedStatusList.add(productId)
                            }
                        }
                        if (purchase1 != null) {
                            if (purchase1!!.purchaseState == PURCHASED) {
                                if (!purchase1!!.isAcknowledged) {
                                    val acknowledgePurchaseParams =
                                        AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase1!!.purchaseToken)
                                            .build()
                                    Log.e(TAG, "acknowledge Called7")
                                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { itBillingResult ->
                                        if (itBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                            Log.e(TAG, "Acknowledged")
                                            acknowledgeObserverLocalLiveData.postValue(true)
                                        } else {
//                                            callback(itBillingResult.responseCode)
                                            acknowledgeObserverLocalLiveData.postValue(false)
                                            Log.e(TAG, "ErrorPoint3")
                                            setOnError(itBillingResult.responseCode)
//                                            billingHandler?.onBillingError(itBillingResult.responseCode)
                                        }
                                    }
                                } else {
                                    acknowledgeObserverLocalLiveData.postValue(true)
                                    Log.e(TAG, "ErrorPoint4")
                                    setOnError(ResponseCodes.ALREADY_ACKNOWLEDGED)
//                                    billingHandler?.onBillingError(ResponseCodes.ALREADY_ACKNOWLEDGED)
                                }
                            } else {
                                acknowledgeObserverLocalLiveData.postValue(false)
                                Log.e(TAG, "ErrorPoint5")
                                setOnError(ResponseCodes.NOT_PURCHASED_STATE)
//                                billingHandler?.onBillingError(ResponseCodes.NOT_PURCHASED_STATE)
                            }
                        } else {
                            acknowledgeObserverLocalLiveData.postValue(false)
                            Log.e(TAG, "ErrorPoint6")
                            setOnError(ResponseCodes.NOT_PURCHASED_STATE)
//                            billingHandler?.onBillingError(ResponseCodes.NOT_PURCHASED_STATE)
                        }
                    } else {
                        acknowledgeObserverLocalLiveData.postValue(false)
                        Log.e(TAG, "ErrorPoint7")
                        setOnError(p0.responseCode)
//                        billingHandler?.onBillingError(p0.responseCode)
                    }
                }
            })
        } else {
            acknowledgeObserverLocalLiveData.postValue(false)
            Log.e(TAG, "ErrorPoint8")
            setOnError(ResponseCodes.NOT_CONNECTED)
//            billingHandler?.onBillingError(ResponseCodes.NOT_CONNECTED)
        }
    }

    @JvmStatic
    fun acknowledgeSubscription(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        acknowledgeObserver: Observer<Boolean>
    ) {
        val acknowledgeObserverLocalLiveData = MutableLiveData<Boolean>()
        acknowledgeObserverLocalLiveData.observe(lifecycleOwner, acknowledgeObserver)
        var purchase1: Purchase? = null
        if (isConnected) {
            billingClient?.queryPurchasesAsync(
                SkuType.SUBS,
                PurchasesResponseListener { billingResult, mutableList ->
                    Log.e(TAG, billingResult.responseCode.toString())
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "SubscribedList Size" + mutableList.size)
                        cachedSubscriptionStatusList.clear()
                        cachedSubscriptionAcknowledgedStatusList.clear()
                        isInAppCached = true
                        mutableList.forEach { itPurchase ->
                            if (itPurchase.skus.contains(productId)) {
                                purchase1 = itPurchase
                                Log.e(
                                    TAG,
                                    productId + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                                )
                            }
                            cachedSubscriptionStatusList.add(productId)
                            if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                                cachedSubscriptionAcknowledgedStatusList.add(productId)
                            }
                        }
                        if (purchase1 != null) {
                            if (purchase1!!.purchaseState == PURCHASED) {
                                if (!purchase1!!.isAcknowledged) {
                                    val acknowledgePurchaseParams =
                                        AcknowledgePurchaseParams.newBuilder()
                                            .setPurchaseToken(purchase1!!.purchaseToken)
                                            .build()
                                    Log.e(TAG, "acknowledge Called8")
                                    billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { itBillingResult ->
                                        if (itBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                            Log.e(TAG, "Acknowledged")
                                            acknowledgeObserverLocalLiveData.postValue(true)
                                        } else {
                                            acknowledgeObserverLocalLiveData.postValue(false)
                                            Log.e(TAG, "ErrorPoint9")
                                            setOnError(itBillingResult.responseCode)
//                                        billingHandler?.onBillingError(itBillingResult.responseCode)
                                        }
                                    }
                                } else {
                                    acknowledgeObserverLocalLiveData.postValue(true)
                                    Log.e(TAG, "ErrorPoint10")
                                    setOnError(ResponseCodes.ALREADY_ACKNOWLEDGED)
//                                billingHandler?.onBillingError(ResponseCodes.ALREADY_ACKNOWLEDGED)
                                }
                            } else {
                                acknowledgeObserverLocalLiveData.postValue(false)
                                Log.e(TAG, "ErrorPoint11")
                                setOnError(ResponseCodes.NOT_PURCHASED_STATE)
//                            billingHandler?.onBillingError(ResponseCodes.NOT_PURCHASED_STATE)
                            }
                        } else {
                            acknowledgeObserverLocalLiveData.postValue(false)
                            Log.e(TAG, "ErrorPoint12")
                            setOnError(ResponseCodes.NOT_PURCHASED_STATE)
//                        billingHandler?.onBillingError(ResponseCodes.NOT_PURCHASED_STATE)
                        }
                    } else {
                        acknowledgeObserverLocalLiveData.postValue(false)
                        Log.e(TAG, "ErrorPoint13")
                        setOnError(billingResult.responseCode)
//                    billingHandler?.onBillingError(billingResult.responseCode)
                    }
                })

        } else {
            acknowledgeObserverLocalLiveData.postValue(false)
            Log.e(TAG, "ErrorPoint14")
            setOnError(ResponseCodes.NOT_CONNECTED)
//            billingHandler?.onBillingError(ResponseCodes.NOT_CONNECTED)
        }
    }

    @JvmStatic
    fun consumePurchase(
        productId: String,
        lifecycleOwner: LifecycleOwner,
        consumeObserver: Observer<Boolean>
    ) {
        val consumeObserverLocalLiveData = MutableLiveData<Boolean>()
        consumeObserverLocalLiveData.observe(lifecycleOwner, consumeObserver)
        if (isConnected) {
            billingClient?.queryPurchasesAsync(SkuType.INAPP, object : PurchasesResponseListener {
                override fun onQueryPurchasesResponse(
                    p0: BillingResult,
                    p1: MutableList<Purchase>
                ) {
                    if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                        p1.forEach { itPurchase ->
                            if (itPurchase.skus.contains(productId)) {
                                val consumeParams = ConsumeParams.newBuilder()
                                    .setPurchaseToken(itPurchase.purchaseToken)
                                    .build()
                                billingClient?.consumeAsync(consumeParams) { billingResult, outToken ->
                                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                        isInAppCached = false
                                        consumeObserverLocalLiveData.postValue(true)
//                                            callback(null, outToken)
                                    } else {
                                        consumeObserverLocalLiveData.postValue(false)
//                                            callback(billingResult.responseCode, outToken)
                                        Log.e(TAG, "ErrorPoint15")
                                        setOnError(billingResult.responseCode)
                                    }
                                }
                            }
                        }
                    } else {
                        consumeObserverLocalLiveData.postValue(false)
//                            callback(p0.responseCode, null)
                        Log.e(TAG, "ErrorPoint16")
                        setOnError(p0.responseCode)
                    }
                }

            })
        } else {
            consumeObserverLocalLiveData.postValue(false)
            Log.e(TAG, "ErrorPoint17")
            setOnError(ResponseCodes.NOT_CONNECTED)
//            callback(ResponseCodes.NOT_CONNECTED, null)
        }
    }

    @JvmStatic
    fun endConnection() {
        billingClient?.endConnection()
    }

    private fun isSubscribedAnyRealtime(
        productId: ArrayList<String>,
        callback: (isSubscribed: Boolean) -> Unit
    ) {
        var check = false
        getSubscriptionPurchaseList { itPurchaseList ->
            cachedSubscriptionStatusList.clear()
            cachedSubscriptionAcknowledgedStatusList.clear()
            isSubscriptionCached = true
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            itPurchaseList?.forEach { itPurchase ->
                itPurchase.skus.forEach { itPurchaseSku ->
                    if (!check) check = productId.contains(itPurchaseSku)
                }
                itPurchase.skus.forEach { cachedSubscriptionStatusList.add(it) }
                if (!itPurchase.isAcknowledged && autoAcknowledgeSubscription && itPurchase.purchaseState == PURCHASED) {
                    Log.e(TAG, "acknowledge Called9")
                    acknowledgePurchase(itPurchase, NO_CALL_BACK)
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    itPurchase.skus.forEach { cachedSubscriptionAcknowledgedStatusList.add(it) }
                }
                Log.e(
                    TAG,
                    "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString() + " SkuSize: " + cachedSubscriptionStatusList.size
                )
            }
            callback(check)
        }
        Log.e(TAG, "Check Returned")
    }

    private fun isPurchasedAnyRealtime(
        productId: ArrayList<String>,
        callback: (isSubscribed: Boolean) -> Unit
    ) {
        var check = false
        getInAppPurchaseList { itPurchaseList ->
            cachedInAppStatusList.clear()
            cachedInAppAcknowledgedStatusList.clear()
            isInAppCached = true
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            itPurchaseList?.forEach { itPurchase ->
                itPurchase.skus.forEach { itPurchaseSku ->
                    if (!check) check =
                        productId.contains(itPurchaseSku) && itPurchase.purchaseState == PURCHASED
                }

                if (itPurchase.purchaseState == PURCHASED) {
                    itPurchase.skus.forEach { cachedInAppStatusList.add(it) }
                    if (!itPurchase.isAcknowledged && autoAcknowledgePurchase) {
                        Log.e(TAG, "acknowledge Called10")
                        acknowledgePurchase(itPurchase, NO_CALL_BACK)
                    }
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    itPurchase.skus.forEach { cachedInAppAcknowledgedStatusList.add(it) }
                }
                Log.e(
                    TAG,
                    itPurchase.skus.toString() + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
            }
            callback(check)
        }
    }

    @JvmStatic
    fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            ResponseCodes.SERVICE_TIMEOUT -> {
                "Service Timeout"
            }
            ResponseCodes.FEATURE_NOT_SUPPORTED -> {
                "Feature Not Supported"
            }
            ResponseCodes.SERVICE_DISCONNECTED -> {
                "Service Disconnected"
            }
            ResponseCodes.OK -> {
                "OK"
            }
            ResponseCodes.USER_CANCELED -> {
                "User Canceled"
            }
            ResponseCodes.SERVICE_UNAVAILABLE -> {
                "Service Unavailable"
            }
            ResponseCodes.BILLING_UNAVAILABLE -> {
                "Billing Unavailable"
            }
            ResponseCodes.ITEM_UNAVAILABLE -> {
                "Item Unavailable"
            }
            ResponseCodes.DEVELOPER_ERROR -> {
                "Developer Error"
            }
            ResponseCodes.ERROR -> {
                "Error"
            }
            ResponseCodes.ITEM_ALREADY_OWNED -> {
                "Item Already Owned"
            }
            ResponseCodes.ITEM_NOT_OWNED -> {
                "Item Not Owned"
            }
            ResponseCodes.NOT_PURCHASED_STATE -> {
                "purchase State is not Purchased"
            }
            ResponseCodes.NOT_CONNECTED -> {
                "Not Connected"
            }
            else -> {
                return ""
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase, type: String) {
        Log.e(TAG, "1")
        if (purchase.purchaseState == PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { itBillingResult ->
                    if (itBillingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.e(TAG, "Acknowledged Purchase")
                        if (type == CALL_BACK) {
                            when (purchaseType) {
                                SkuType.INAPP -> {
                                    isInAppCached = false
                                    isPurchasedSaved = true
                                }
                                SkuType.SUBS -> {
                                    isSubscriptionCached = false
                                    isSubscribedSaved = true
                                }
                                else -> {
                                    isInAppCached = false
                                    isSubscriptionCached = false
                                }
                            }
                            isSubscribedOrPurchasedSaved = true
                            setOnPurchased(purchase)
//                            billingHandler?.onPurchased(purchase)
                        }
                    } else {
                        Log.e(TAG, "ErrorPoint18")
                        setOnError(itBillingResult.responseCode)
                    }
                }
            } else {
                setOnPurchased(purchase)
//                billingHandler?.onPurchased(purchase)
            }
        } else {
            Log.e(TAG, "ErrorPoint19")
            setOnError(ResponseCodes.NOT_PURCHASED_STATE)
        }
    }

    private fun getInAppPurchaseList(callback: (purchaseList: List<Purchase>?) -> Unit) {
        getPurchaseList(SkuType.INAPP) { itPurchaseList ->
            callback(itPurchaseList)
        }
    }

    private fun getSubscriptionPurchaseList(callback: (purchaseList: List<Purchase>?) -> Unit) {
        getPurchaseList(SkuType.SUBS) { itPurchaseList ->
            callback(itPurchaseList)
        }
    }

    private fun reloadSubscriptionCache() {
        getPurchaseListWithoutConnectedCheck(SkuType.SUBS) { itPurchaseList ->
            Log.e(TAG, "SubscribedList Size" + itPurchaseList?.size)
            cachedSubscriptionStatusList.clear()
            cachedSubscriptionAcknowledgedStatusList.clear()
            isSubscriptionCached = true
            itPurchaseList?.forEach { itPurchase ->
                Log.e(
                    TAG,
                    itPurchase.skus.toString() + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
                itPurchase.skus.forEach { cachedSubscriptionStatusList.add(it) }
                if (!itPurchase.isAcknowledged && autoAcknowledgeSubscription && itPurchase.purchaseState == PURCHASED) {
                    Log.e(TAG, "acknowledge Called12")
                    acknowledgePurchase(itPurchase, NO_CALL_BACK)
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    itPurchase.skus.forEach { cachedSubscriptionAcknowledgedStatusList.add(it) }
                }
                Log.e(TAG, "SkuSize " + cachedSubscriptionStatusList.size.toString())
            }
        }
    }

    private fun reloadInAppCache() {
        getPurchaseListWithoutConnectedCheck(SkuType.INAPP) { itPurchaseList ->
            Log.e(TAG, "PurchasedList Size" + itPurchaseList?.size)
            cachedInAppStatusList.clear()
            cachedInAppAcknowledgedStatusList.clear()
            isInAppCached = true
            itPurchaseList?.forEach { itPurchase ->
                Log.e(
                    TAG,
                    itPurchase.skus.toString() + "==" + itPurchase.skus.toString() + " && " + itPurchase.purchaseState.toString() + "==" + PURCHASED.toString() + " && " + itPurchase.isAcknowledged.toString()
                )
                if (itPurchase.purchaseState == PURCHASED) {
                    itPurchase.skus.forEach { cachedInAppStatusList.add(it) }
                    if (!itPurchase.isAcknowledged && autoAcknowledgePurchase) {
                        Log.e(TAG, "acknowledge Called13")
                        acknowledgePurchase(itPurchase, NO_CALL_BACK)
                    }
                }
                if (itPurchase.purchaseState == PURCHASED && itPurchase.isAcknowledged) {
                    itPurchase.skus.forEach { cachedInAppAcknowledgedStatusList.add(it) }
                }
            }
        }
    }

    private fun getSkuDetails(
        type: String,
        productIdList: ArrayList<String>,
        isToSort: Boolean = false,
        callback: (error: Int?, skuList: List<SkuDetails>?) -> Unit
    ) {
        Log.d(TAG, productIdList.toString())
        if (isConnected) {
            val params = SkuDetailsParams.newBuilder()
            params.setSkusList(productIdList).setType(type)
            billingClient?.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    val skuList = if (isToSort) sortSkuDetailsArray(
                        productIdList,
                        skuDetailsList
                    ) else skuDetailsList
                    callback(null, skuList)
                } else {
                    callback(billingResult.responseCode, skuDetailsList)
                    Log.e(TAG, "ErrorPoint20")
                    setOnError(billingResult.responseCode)
//                    billingHandler?.onBillingError(billingResult.responseCode)
                }
            }
        } else {
            callback(ResponseCodes.NOT_CONNECTED, null)
            Log.e(TAG, "ErrorPoint21")
            setOnError(ResponseCodes.NOT_CONNECTED)
//            billingHandler?.onBillingError(ResponseCodes.NOT_CONNECTED)
        }
    }

    private fun subscribeOrPurchase(
        activity: Activity,
        productId: String,
        type: String,
        sendCustomParams: Boolean = false,
        obfuscatedProfileId: String = "",
        obfuscatedAccountId: String = ""
    ) {
        if (isConnected) {
            purchaseType = type
            savedProductId = productId
            var skuDetails: SkuDetails? = null
            getSkuDetails(
                type,
                arrayListOf(productId)
            ) { errorCode: Int?, skuList: List<SkuDetails>? ->
                if (errorCode == null) {
                    skuList?.forEach { itSkuDetail ->
                        if (itSkuDetail.sku == productId) {
                            skuDetails = itSkuDetail
                        }
                    }
                    skuDetails?.let {
                        val billingFlowParams = if (sendCustomParams) {
                            BillingFlowParams.newBuilder()
                                .setSkuDetails(it)
                                .setObfuscatedProfileId(obfuscatedProfileId)
                                .setObfuscatedAccountId(obfuscatedAccountId)
                                .build()
                        } else {
                            BillingFlowParams.newBuilder()
                                .setSkuDetails(it)
                                .build()
                        }
                        val response = billingClient?.launchBillingFlow(
                            activity,
                            billingFlowParams
                        )?.responseCode
                        if (response != BillingClient.BillingResponseCode.OK) {
                            if (type == SkuType.SUBS) {
                                isSubscriptionCached = false
                            }
                            if (type == SkuType.INAPP) {
                                isInAppCached = false
                            }
                            Log.e(TAG, "ErrorPoint22")
                            setOnError(response!!)
//                            billingHandler?.onBillingError(response)
                        }
                    }
                } else {
                    Log.e(TAG, "ErrorPoint23")
                    setOnError(errorCode)
                }
            }
        } else {
            Log.e(TAG, "fun subscribe")
            Log.e(TAG, "ErrorPoint24")
            setOnError(ResponseCodes.NOT_CONNECTED)
        }
    }

    private fun getPurchaseList(type: String, callback: (purchaseList: List<Purchase>?) -> Unit) {
        if (isConnected) {
            billingClient?.queryPurchasesAsync(type, object : PurchasesResponseListener {
                override fun onQueryPurchasesResponse(
                    p0: BillingResult,
                    p1: MutableList<Purchase>
                ) {
                    Log.e(TAG, p0.responseCode.toString())
                    if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                        callback(p1)

                    } else {
                        Log.e(TAG, "ErrorPoint25")
//                        billingHandler?.onBillingError(p0.responseCode)
                        setOnError(p0.responseCode)
                    }
                }
            })
        } else {
            Log.e(TAG, "ErrorPoint26")
            setOnError(ResponseCodes.NOT_CONNECTED)
//            billingHandler?.onBillingError(ResponseCodes.NOT_CONNECTED)
        }
    }

    private fun getPurchaseListWithoutConnectedCheck(
        type: String,
        callback: (purchaseList: List<Purchase>?) -> Unit
    ) {
        billingClient?.queryPurchasesAsync(type, object : PurchasesResponseListener {
            override fun onQueryPurchasesResponse(p0: BillingResult, p1: MutableList<Purchase>) {
                Log.e(TAG, p0.responseCode.toString())
                if (p0.responseCode == BillingClient.BillingResponseCode.OK) {
                    callback(p1)
                } else {
                    Log.e(TAG, "ErrorPoint27")
                    setOnError(p0.responseCode)
                }
            }
        })

    }

    private fun sortSkuDetailsArray(
        productsList: ArrayList<String>,
        skuDetailsList: List<SkuDetails>?
    ): List<SkuDetails> {
        val newSkuDetailsList = ArrayList<SkuDetails>()
        productsList.forEach { itProductId ->
            skuDetailsList?.forEach { itSkuDetail ->
                if (itProductId == itSkuDetail.sku) newSkuDetailsList.add(itSkuDetail)
            }
        }
        return newSkuDetailsList
    }
}