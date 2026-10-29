package com.trading.app.service

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.trading.app.notification.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TradingFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM Device Token: $token")
        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("fcm_device_token", token).apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: "DEFAULT"
            val symbol = data["symbol"] ?: ""
            val title = data["title"] ?: remoteMessage.notification?.title ?: "Trading Alert"
            val body = data["body"] ?: remoteMessage.notification?.body ?: ""

            when (type.uppercase()) {
                "MARGIN_CALL" -> {
                    val marginLevel = data["margin_level"]?.toDoubleOrNull() ?: 80.0
                    notificationHelper.showMarginCallWarning(marginLevel)
                }
                "STOP_OUT", "LIQUIDATION" -> {
                    val qty = data["quantity"]?.toDoubleOrNull() ?: 1.0
                    notificationHelper.showStopOutLiquidationAlert(symbol, qty)
                }
                "ORDER_FILLED" -> {
                    val side = data["side"] ?: "BUY"
                    val qty = data["quantity"]?.toDoubleOrNull() ?: 1.0
                    val price = data["price"]?.toDoubleOrNull() ?: 0.0
                    val orderType = data["order_type"] ?: "LIMIT"
                    notificationHelper.showOrderFilled(symbol, side, qty, price, orderType)
                }
                "PRICE_ALERT" -> {
                    val condition = data["condition"] ?: "ABOVE"
                    val targetPrice = data["target_price"]?.toDoubleOrNull() ?: 0.0
                    val currentPrice = data["current_price"]?.toDoubleOrNull() ?: 0.0
                    notificationHelper.showPriceAlert(symbol, condition, targetPrice, currentPrice)
                }
                "NEWS" -> {
                    val source = data["source"] ?: "Marketaux"
                    val sentiment = data["sentiment"]?.toDoubleOrNull()
                    val url = data["url"]
                    notificationHelper.showNewsAlert(title, source, sentiment, symbol.ifBlank { null }, url)
                }
                "TRADE_MILESTONE" -> {
                    val roe = data["roe_percent"]?.toDoubleOrNull() ?: 0.0
                    val pnl = data["unrealized_pnl"]?.toDoubleOrNull() ?: 0.0
                    notificationHelper.showTradeMilestone(symbol, roe, pnl)
                }
                else -> {
                    // Fallback to standard news or price alert
                    notificationHelper.showPriceAlert(symbol.ifBlank { "MARKET" }, "ALERT", 0.0, 0.0)
                }
            }
        } else {
            // Notification payload only
            remoteMessage.notification?.let {
                notificationHelper.showNewsAlert(
                    title = it.title ?: "Market Announcement",
                    source = "Trading Desk",
                    sentimentScore = null,
                    symbol = null,
                    articleUrl = null
                )
            }
        }
    }

    companion object {
        private const val TAG = "TradingFCMService"
    }
}
