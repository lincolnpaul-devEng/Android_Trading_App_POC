package com.trading.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.trading.app.R
import com.trading.app.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_RISK = "trading_risk_channel"
        const val CHANNEL_ORDERS = "trading_orders_channel"
        const val CHANNEL_PRICE_ALERTS = "trading_price_alerts_channel"
        const val CHANNEL_NEWS = "trading_news_channel"
        const val CHANNEL_TRACKER = "trading_tracker_channel"

        private var nextNotificationId = 1000
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val riskChannel = NotificationChannel(
                CHANNEL_RISK,
                "Margin & Risk Protection",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts for margin calls, auto stop-outs, and liquidation protection."
                enableVibration(true)
            }

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "Order Executions & Fills",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Instant notifications when pending orders fill or Take Profit / Stop Loss triggers."
                enableVibration(true)
            }

            val priceAlertsChannel = NotificationChannel(
                CHANNEL_PRICE_ALERTS,
                "Price & Target Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when asset prices cross user-defined price targets."
            }

            val newsChannel = NotificationChannel(
                CHANNEL_NEWS,
                "Market News & Sentiment",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Breaking headlines and high-impact sentiment shifts from Marketaux."
            }

            val trackerChannel = NotificationChannel(
                CHANNEL_TRACKER,
                "Active Trade Progress",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Milestone P&L updates and trailing stop-loss warnings for active positions."
            }

            notificationManager.createNotificationChannels(
                listOf(riskChannel, ordersChannel, priceAlertsChannel, newsChannel, trackerChannel)
            )
        }
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createPendingIntent(symbol: String? = null, tabIndex: Int? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            symbol?.let { putExtra("EXTRA_SYMBOL", it) }
            tabIndex?.let { putExtra("EXTRA_TAB_INDEX", it) }
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, (symbol?.hashCode() ?: 0) + (tabIndex ?: 0), intent, flags)
    }

    // -------------------------------------------------------------
    // 1. Risk & Margin Alerts (High Priority)
    // -------------------------------------------------------------

    fun showMarginCallWarning(marginLevelPercent: Double) {
        if (!hasPermission()) return

        val title = "⚠️ Margin Call Warning (${String.format(Locale.US, "%.1f%%", marginLevelPercent)})"
        val message = "Your margin level has dropped below safety limits. Deposit funds or trim open positions to prevent liquidation."

        val builder = NotificationCompat.Builder(context, CHANNEL_RISK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(tabIndex = 2)) // Deep link to Portfolio

        notify(9001, builder)
    }

    fun showStopOutLiquidationAlert(symbol: String, quantity: Double) {
        if (!hasPermission()) return

        val title = "🚨 Margin Stop-Out Executed"
        val message = "Position for $symbol ($quantity units) was auto-closed at 50% margin level to preserve account solvency."

        val builder = NotificationCompat.Builder(context, CHANNEL_RISK)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(symbol = symbol, tabIndex = 2))

        notify(9002, builder)
    }

    // -------------------------------------------------------------
    // 2. Order Executions & Fills
    // -------------------------------------------------------------

    fun showOrderFilled(symbol: String, side: String, quantity: Double, price: Double, orderType: String) {
        if (!hasPermission()) return

        val title = "✅ Order Filled: $side $symbol"
        val message = "$orderType executed: $quantity units @ $${String.format(Locale.US, "%,.2f", price)}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(symbol = symbol, tabIndex = 2))

        notify(nextNotificationId++, builder)
    }

    fun showTakeProfitTriggered(symbol: String, realizedProfit: Double, roePercent: Double) {
        if (!hasPermission()) return

        val title = "🎯 Take-Profit Reached: $symbol"
        val message = "Position closed in profit: +$${String.format(Locale.US, "%,.2f", realizedProfit)} (ROE +${String.format(Locale.US, "%.1f%%", roePercent)})"

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(symbol = symbol, tabIndex = 2))

        notify(nextNotificationId++, builder)
    }

    fun showStopLossTriggered(symbol: String, realizedLoss: Double, roePercent: Double) {
        if (!hasPermission()) return

        val title = "🛑 Stop-Loss Hit: $symbol"
        val message = "Position closed at risk limit: -$${String.format(Locale.US, "%,.2f", Math.abs(realizedLoss))} (ROE ${String.format(Locale.US, "%.1f%%", roePercent)})"

        val builder = NotificationCompat.Builder(context, CHANNEL_ORDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(symbol = symbol, tabIndex = 2))

        notify(nextNotificationId++, builder)
    }

    // -------------------------------------------------------------
    // 3. User Price Alerts
    // -------------------------------------------------------------

    fun showPriceAlert(symbol: String, condition: String, targetPrice: Double, currentPrice: Double) {
        if (!hasPermission()) return

        val title = "🔔 Price Alert: $symbol"
        val message = "$symbol crossed $condition $${String.format(Locale.US, "%,.2f", targetPrice)} (Current: $${String.format(Locale.US, "%,.2f", currentPrice)})"

        val builder = NotificationCompat.Builder(context, CHANNEL_PRICE_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(symbol = symbol))

        notify(nextNotificationId++, builder)
    }

    // -------------------------------------------------------------
    // 4. Trade Progress & Milestone Tracking
    // -------------------------------------------------------------

    fun showTradeMilestone(symbol: String, roePercent: Double, unrealizedPnL: Double) {
        if (!hasPermission()) return

        val isProfit = roePercent > 0
        val icon = if (isProfit) "🚀" else "⚠️"
        val title = "$icon Trade Milestone: $symbol"
        val sign = if (isProfit) "+" else ""
        val message = "Current ROE: $sign${String.format(Locale.US, "%.1f%%", roePercent)} ($sign$${String.format(Locale.US, "%,.2f", unrealizedPnL)})"

        val builder = NotificationCompat.Builder(context, CHANNEL_TRACKER)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createPendingIntent(symbol = symbol, tabIndex = 2))

        notify(nextNotificationId++, builder)
    }

    // -------------------------------------------------------------
    // 5. Marketaux News & Sentiment Alerts
    // -------------------------------------------------------------

    fun showNewsAlert(title: String, source: String, sentimentScore: Double?, symbol: String?, articleUrl: String?) {
        if (!hasPermission()) return

        val sentimentBadge = when {
            sentimentScore != null && sentimentScore > 0.2 -> "🟢 Bullish"
            sentimentScore != null && sentimentScore < -0.2 -> "🔴 Bearish"
            else -> "⚪ Neutral"
        }

        val notifTitle = if (symbol != null) "📰 $symbol News ($sentimentBadge)" else "📰 Market News ($sentimentBadge)"
        val message = "$title — $source"

        val intent = if (!articleUrl.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
        } else {
            Intent(context, MainActivity::class.java).apply {
                symbol?.let { putExtra("EXTRA_SYMBOL", it) }
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            title.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_NEWS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notify(nextNotificationId++, builder)
    }

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Handled when permission is missing
        }
    }
}
