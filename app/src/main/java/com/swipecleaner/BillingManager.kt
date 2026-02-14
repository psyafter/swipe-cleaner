package com.swipecleaner

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResponseListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

class BillingManager(context: Context) : PurchasesResponseListener {
    private val billingClient = BillingClient.newBuilder(context)
        .enablePendingPurchases()
        .setListener { _, purchases ->
            handlePurchases(purchases.orEmpty())
        }
        .build()

    private var proProductDetails: ProductDetails? = null
    private var productAvailabilityMessage: String? = null
    private var onProStatusChanged: (Boolean) -> Unit = {}
    private var onMessage: (String) -> Unit = {}

    fun setCallbacks(
        onProStatusChanged: (Boolean) -> Unit,
        onMessage: (String) -> Unit,
    ) {
        this.onProStatusChanged = onProStatusChanged
        this.onMessage = onMessage
    }

    fun connect() {
        if (billingClient.isReady) {
            queryPurchases()
            queryProductDetails()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPurchases()
                    queryProductDetails()
                } else {
                    onMessage("Billing unavailable: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                onMessage("Billing disconnected")
            }
        })
    }

    fun launchPurchaseFlow(activity: Activity): Boolean {
        val details = proProductDetails ?: return false
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun productAvailabilityMessage(): String? = productAvailabilityMessage

    fun queryPurchases() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            this,
        )
    }

    override fun onQueryPurchasesResponse(result: BillingResult, purchases: MutableList<Purchase>) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onMessage("Restore failed: ${result.debugMessage}")
            return
        }
        handlePurchases(purchases)
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        ) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                proProductDetails = detailsList.firstOrNull()
                productAvailabilityMessage = if (proProductDetails == null) {
                    "Product not available in this build / check Play Console product id"
                } else {
                    null
                }
                if (productAvailabilityMessage != null) {
                    onMessage(productAvailabilityMessage!!)
                }
            } else {
                productAvailabilityMessage = "Billing unavailable: ${result.debugMessage}"
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        val proPurchase = purchases.firstOrNull { purchase ->
            purchase.products.contains(PRO_PRODUCT_ID) &&
                (purchase.purchaseState == Purchase.PurchaseState.PURCHASED)
        }

        if (proPurchase == null) {
            onProStatusChanged(false)
            return
        }

        if (!proPurchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(proPurchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { ackResult ->
                if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onProStatusChanged(true)
                } else {
                    onMessage("Purchase acknowledge failed")
                }
            }
        } else {
            onProStatusChanged(true)
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
