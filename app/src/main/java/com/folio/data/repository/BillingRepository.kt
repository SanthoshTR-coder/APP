package com.folio.data.repository

import android.app.Activity
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles Google Play Billing for "Remove Ads" ($4.99 one-time purchase).
 */
@Singleton
class BillingRepository @Inject constructor() {

    companion object {
        private const val TAG = "BillingRepository"
        const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
    }

    private var billingClient: BillingClient? = null

    private val _adsRemoved = MutableStateFlow(false)
    val adsRemoved: StateFlow<Boolean> = _adsRemoved.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    fun initialize(activity: Activity) {
        try {
            billingClient = BillingClient.newBuilder(activity)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases(
                    PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .build()
                )
                .build()

            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        checkExistingPurchases()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Retry connection on next operation
                    Log.d(TAG, "Billing service disconnected")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Billing initialization failed", e)
        }
    }

    private fun checkExistingPurchases() {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _adsRemoved.value = purchases.any {
                    it.products.contains(REMOVE_ADS_PRODUCT_ID) &&
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(REMOVE_ADS_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                productDetailsList.isNotEmpty()
            ) {
                val productDetails = productDetailsList[0]
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(
                        listOf(
                            BillingFlowParams.ProductDetailsParams.newBuilder()
                                .setProductDetails(productDetails)
                                .build()
                        )
                    )
                    .build()
                billingClient?.launchBillingFlow(activity, flowParams)
            }
        }
    }

    fun restorePurchases() {
        checkExistingPurchases()
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (purchase.products.contains(REMOVE_ADS_PRODUCT_ID)) {
                _adsRemoved.value = true
            }
            // Acknowledge the purchase
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { }
            }
        }
    }

    fun cleanup() {
        billingClient?.endConnection()
    }
}
