package com.example.expensemanager.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles Google Play Billing for Walletly Pro subscription.
 *
 * Play Console setup:
 *  - Subscription product ID : walletly_pro
 *  - Monthly base plan ID    : monthly
 *  - Yearly base plan ID     : yearly
 *
 * Usage:
 *  1. Call connect() in MainActivity.onCreate()
 *  2. Call queryPurchases() in MainActivity.onResume()
 *  3. Call launchBillingFlow(activity, planId) with PLAN_MONTHLY or PLAN_YEARLY
 *  4. Call restorePurchases() from "Restore Purchase" button
 */
class BillingManager(
    context    : Context,
    private val proManager: ProManager
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG        = "BillingManager"
        const val PRODUCT_ID         = "smartspend_pro"
        const val PLAN_MONTHLY       = "monthly"
        const val PLAN_YEARLY        = "yearly"
    }

    // Subscription product details — observed by ProUpgradeScreen to show real prices
    private val _productDetails = MutableStateFlow<ProductDetails?>(null)
    val productDetails: StateFlow<ProductDetails?> = _productDetails.asStateFlow()

    // true while connecting or loading product details
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // non-null when something goes wrong (cleared after connect retry)
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() { _errorMessage.value = null }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    // ── Connection ────────────────────────────────────────────────────────────

    fun connect() {
        _isLoading.value = true
        _errorMessage.value = null
        if (billingClient.isReady) {
            queryProductDetails()
            queryPurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    queryProductDetails()
                    queryPurchases()
                } else {
                    _isLoading.value = false
                    _errorMessage.value = "Could not connect to Play Store. Please try again."
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }
            override fun onBillingServiceDisconnected() {
                _isLoading.value = false
                Log.w(TAG, "Billing disconnected — will retry on next connect()")
            }
        })
    }

    // ── Product details (prices, offer tokens) ────────────────────────────────

    private fun queryProductDetails() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            _isLoading.value = false
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = detailsList.firstOrNull()
                if (_productDetails.value == null) {
                    _errorMessage.value = "Subscription not set up yet. Please check back soon."
                }
                Log.d(TAG, "Product details loaded: ${_productDetails.value?.title}")
            } else {
                _errorMessage.value = "Could not load subscription info. Please try again."
                Log.w(TAG, "queryProductDetails failed: ${result.debugMessage}")
            }
        }
    }

    /**
     * Returns the formatted price string for a given base plan ID.
     * e.g. "PKR 299.00/mo" or "PKR 1,999.00/yr"
     * Returns null if product details haven't loaded yet.
     */
    fun getPriceForPlan(planId: String): String? {
        val details = _productDetails.value ?: return null
        val offer = details.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == planId }
            ?: return null
        // Use the first pricing phase (the recurring charge, not a free trial)
        return offer.pricingPhases.pricingPhaseList
            .lastOrNull()
            ?.formattedPrice
    }

    // ── Query existing subscriptions (call on every resume) ───────────────────

    fun queryPurchases() {
        if (!billingClient.isReady) { connect(); return }

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasActiveSub = purchases.any {
                    it.products.contains(PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasActiveSub) proManager.grantPro() else proManager.revokePro()
                Log.d(TAG, "queryPurchases — isPro=$hasActiveSub")
            }
        }
    }

    // ── Launch billing flow ───────────────────────────────────────────────────

    /**
     * Opens the Play subscription sheet for the given plan.
     * @param planId use BillingManager.PLAN_MONTHLY or BillingManager.PLAN_YEARLY
     */
    fun launchBillingFlow(activity: Activity, planId: String = PLAN_MONTHLY) {
        val details = _productDetails.value
        if (details == null) {
            Log.w(TAG, "Product details not loaded yet — reconnecting")
            _errorMessage.value = "Still connecting to Play Store. Please wait a moment and try again."
            connect()
            return
        }

        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull { it.basePlanId == planId }
            ?.offerToken

        if (offerToken == null) {
            Log.w(TAG, "No offer token found for planId=$planId")
            _errorMessage.value = "Subscription plan unavailable. Please try again later."
            return
        }

        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, billingFlowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow failed: ${result.debugMessage}")
        }
    }

    // ── Restore purchases ─────────────────────────────────────────────────────

    fun restorePurchases() = queryPurchases()

    // ── PurchasesUpdatedListener ──────────────────────────────────────────────

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED ->
                Log.d(TAG, "User cancelled the purchase")
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                proManager.grantPro()
                Log.d(TAG, "Already subscribed — granting Pro")
            }
            else ->
                Log.w(TAG, "onPurchasesUpdated error: ${result.debugMessage}")
        }
    }

    // ── Acknowledge & grant ───────────────────────────────────────────────────

    private fun handlePurchase(purchase: Purchase) {
        if (!purchase.products.contains(PRODUCT_ID)) return

        if (purchase.isAcknowledged) {
            proManager.grantPro()
            return
        }

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { ackResult ->
            if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                proManager.grantPro()
                Log.d(TAG, "Subscription acknowledged — Pro granted")
            } else {
                Log.w(TAG, "Acknowledge failed: ${ackResult.debugMessage}")
            }
        }
    }
}
