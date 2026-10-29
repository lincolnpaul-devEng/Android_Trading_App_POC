package com.trading.app.presentation.watchlist

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.trading.app.domain.model.AlertCondition
import com.trading.app.domain.model.MarketNewsArticle
import com.trading.app.domain.model.NewsSentiment
import com.trading.app.domain.model.PriceAlert
import com.trading.app.presentation.components.FlashingPriceText
import com.trading.app.presentation.components.Formatters
import com.trading.app.presentation.components.NewsDetailBottomSheet
import com.trading.app.presentation.components.NewsEmptyStateView
import com.trading.app.presentation.components.NewsShimmerList
import com.trading.app.presentation.components.SparklineChart
import com.trading.app.presentation.components.StockTickerCard
import com.trading.app.presentation.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    onStockClick: (String) -> Unit,
    onViewAllNews: () -> Unit = {},
    viewModel: WatchlistViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNewsArticle by remember { mutableStateOf<MarketNewsArticle?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChanged,
            placeholder = { Text("Search symbols...", color = TextTertiary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = DarkBorder,
                cursorColor = PrimaryBlue,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            ),
            singleLine = true
        )

        // Search results dropdown
        AnimatedVisibility(visible = state.isSearching && state.searchResults.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
            ) {
                state.searchResults.forEach { stock ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addToWatchlist(stock.symbol, stock.companyName)
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stock.symbol, style = TradingTypography.ticker, color = TextPrimary)
                            Text(stock.companyName, fontSize = 12.sp, color = TextSecondary)
                        }
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = PrimaryBlue)
                    }
                    if (stock != state.searchResults.last()) {
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    }
                }
            }
        }

        // Watchlist items
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Watchlist (${state.items.size})",
                    style = TradingTypography.sectionHeader,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(state.items, key = { it.symbol }) { item ->
                val hasAlert = state.priceAlerts.any { it.symbol == item.symbol && !it.isTriggered }
                WatchlistItemRow(
                    item = item,
                    hasAlert = hasAlert,
                    onStockClick = { onStockClick(item.symbol) },
                    onAlertClick = { viewModel.openSetAlert(item.symbol, item.lastPrice) },
                    onRemove = { viewModel.removeFromWatchlist(item.symbol) }
                )
            }

            // Active Price Alerts Section
            if (state.priceAlerts.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Target Price Alerts (${state.priceAlerts.size})",
                        style = TradingTypography.sectionHeader,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(state.priceAlerts, key = { it.id }) { alert ->
                    PriceAlertCard(
                        alert = alert,
                        onDelete = { viewModel.removePriceAlert(alert.id) }
                    )
                }
            }

            // Latest news section (Real-time Marketaux API)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Latest news",
                        style = TradingTypography.sectionHeader,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    if (state.marketNews.isNotEmpty()) {
                        Text(
                            text = "View all",
                            color = PrimaryBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable(onClick = onViewAllNews)
                                .padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (state.isNewsLoading) {
                item {
                    NewsShimmerList(count = 3)
                }
            } else if (state.newsError != null || state.marketNews.isEmpty()) {
                item {
                    NewsEmptyStateView(
                        message = state.newsError ?: "Error loading news or no internet connection",
                        onReload = { viewModel.reloadNews() }
                    )
                }
            } else {
                items(state.marketNews, key = { it.id }) { news ->
                    MarketauxNewsRowItem(
                        article = news,
                        onClick = { selectedNewsArticle = news }
                    )
                    HorizontalDivider(
                        color = DarkBorder.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Set Price Alert BottomSheet
    state.alertSettingSymbol?.let { symbol ->
        SetPriceAlertBottomSheet(
            symbol = symbol,
            currentPrice = state.alertSettingPrice,
            onDismiss = { viewModel.closeSetAlert() },
            onSave = { targetPrice, condition ->
                viewModel.addPriceAlert(symbol, targetPrice, condition)
            }
        )
    }

    // News Detail Drawer Sheet (matches uploaded design in app's dark theme)
    selectedNewsArticle?.let { article ->
        NewsDetailBottomSheet(
            article = article,
            onDismiss = { selectedNewsArticle = null },
            onStockClick = { symbol ->
                selectedNewsArticle = null
                onStockClick(symbol)
            }
        )
    }
}

@Composable
private fun WatchlistItemRow(
    item: com.trading.app.domain.model.WatchlistItem,
    hasAlert: Boolean,
    onStockClick: () -> Unit,
    onAlertClick: () -> Unit,
    onRemove: () -> Unit
) {
    val changeColor = if (item.isPositive) ProfitGreen else LossRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable(onClick = onStockClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(item.symbol, style = TradingTypography.ticker, color = TextPrimary)
                if (hasAlert) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alert Active",
                        tint = Color(0xFFF1E05A),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(item.companyName, fontSize = 11.sp, color = TextSecondary, maxLines = 1)
        }

        if (item.sparklineData.isNotEmpty()) {
            SparklineChart(
                data = item.sparklineData,
                color = changeColor,
                modifier = Modifier
                    .width(60.dp)
                    .height(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(horizontalAlignment = Alignment.End) {
            FlashingPriceText(
                price = item.lastPrice,
                formattedPrice = Formatters.formatPrice(item.lastPrice),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textColor = TextPrimary
            )
            Text(
                "${Formatters.formatChange(item.change)} (${Formatters.formatPercent(item.changePercent)})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = changeColor
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Set Alert button
        IconButton(
            onClick = onAlertClick,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = if (hasAlert) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                contentDescription = "Set Alert",
                tint = if (hasAlert) Color(0xFFF1E05A) else TextTertiary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun PriceAlertCard(
    alert: PriceAlert,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (alert.isTriggered) Icons.Default.CheckCircle else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (alert.isTriggered) ProfitGreen else Color(0xFFF1E05A),
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "${alert.symbol} ${alert.condition.label} ${Formatters.formatCurrency(alert.targetPrice)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (alert.isTriggered) "TRIGGERED" else "WAITING TO TRIGGER",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (alert.isTriggered) ProfitGreen else TextTertiary
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextTertiary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetPriceAlertBottomSheet(
    symbol: String,
    currentPrice: Double,
    onDismiss: () -> Unit,
    onSave: (targetPrice: Double, condition: AlertCondition) -> Unit
) {
    var targetText by remember { mutableStateOf(String.format(java.util.Locale.US, "%.2f", currentPrice)) }
    var selectedCondition by remember { mutableStateOf(AlertCondition.ABOVE) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Set Price Alert • $symbol",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Current Market Price: ${Formatters.formatCurrency(currentPrice)}",
                fontSize = 13.sp,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AlertCondition.entries.forEach { cond ->
                    val isSel = selectedCondition == cond
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) PrimaryBlue else DarkSurfaceVariant)
                            .clickable { selectedCondition = cond }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cond.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) DarkBackground else TextSecondary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                label = { Text("Target Price ($)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Button(
                onClick = {
                    val p = targetText.toDoubleOrNull() ?: currentPrice
                    onSave(p, selectedCondition)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Price Alert", fontWeight = FontWeight.Bold, color = DarkBackground)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MarketauxNewsRowItem(
    article: MarketNewsArticle,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Line 1: Source • Time (e.g., Business Insider • 1h ago)
        Text(
            text = "${article.source} • ${article.publishedAt}",
            color = Color(0xFF8B949E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )

        // Middle Row: Text on Left, Preview Image on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title (Bold, maxLines = 2)
                Text(
                    text = article.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle snippet (Subtle gray, maxLines = 2)
                if (article.summary.isNotBlank()) {
                    Text(
                        text = article.summary,
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom Ticker Chips Row: DIS -0.63% | MSFT +1.01% | OPEA —
                if (article.entities.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        article.entities.take(4).forEachIndexed { index, entity ->
                            if (index > 0) {
                                Text(
                                    text = "|",
                                    color = Color(0xFF484F58),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            val changeColor = when {
                                entity.changePercent != null && entity.changePercent > 0 -> ProfitGreen
                                entity.changePercent != null && entity.changePercent < 0 -> LossRed
                                else -> Color(0xFF8B949E)
                            }
                            val changeText = when {
                                entity.changePercent != null && entity.changePercent > 0 -> "+${String.format(Locale.US, "%.2f%%", entity.changePercent)}"
                                entity.changePercent != null && entity.changePercent < 0 -> "${String.format(Locale.US, "%.2f%%", entity.changePercent)}"
                                else -> "—"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = entity.symbol,
                                    color = if (entity.changePercent != null && entity.changePercent < 0) LossRed else changeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = changeText,
                                    color = changeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Right Thumbnail Preview Image
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                if (!article.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = article.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = PrimaryBlue.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
