package com.trading.app.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.model.MarketNewsArticle
import com.trading.app.domain.model.NewsSentiment
import com.trading.app.domain.model.StockQuote
import com.trading.app.presentation.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsDetailBottomSheet(
    article: MarketNewsArticle,
    relatedQuotes: List<StockQuote> = emptyList(),
    onDismiss: () -> Unit,
    onStockClick: (String) -> Unit
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }

    val sentimentColor = when (article.sentiment) {
        NewsSentiment.BULLISH -> ProfitGreen
        NewsSentiment.BEARISH -> LossRed
        NewsSentiment.NEUTRAL -> Color(0xFF8B949E)
    }

    val primaryCategory = when {
        article.symbol in listOf("BTC", "ETH", "SOL", "DOGE") -> "Digital Assets & Crypto"
        article.symbol in listOf("EUR/USD", "GBP/USD", "USD/JPY") -> "Foreign Exchange (FX)"
        article.symbol in listOf("AAPL", "MSFT", "NVDA", "GOOGL") -> "Technology & Semiconductors"
        else -> "Market Intelligence & Equities"
    }

    val sentimentPercentText = when {
        article.sentimentScore > 0 -> "+${String.format(Locale.US, "%.2f%%", (article.sentimentScore * 5.0).coerceIn(0.5, 9.5))}"
        article.sentimentScore < 0 -> "${String.format(Locale.US, "%.2f%%", (article.sentimentScore * 5.0).coerceIn(-9.5, -0.5))}"
        else -> "0.00%"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkBackground,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = Color(0xFF484F58))
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // -------------------------------------------------------------
            // Top Drawer Header (Category, Subtitle, Sentiment %, Metric)
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = primaryCategory,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${article.source} • ${article.publishedAt}",
                        color = Color(0xFF8B949E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = sentimentPercentText,
                        color = sentimentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when (article.sentiment) {
                            NewsSentiment.BULLISH -> "Sentiment: Bullish"
                            NewsSentiment.BEARISH -> "Sentiment: Bearish"
                            NewsSentiment.NEUTRAL -> "Sentiment: Neutral"
                        },
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // -------------------------------------------------------------
            // Main Narrative Card (AI Summary, Key Drivers, Tickers)
            // -------------------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13171F)),
                border = BorderStroke(1.dp, Color(0xFF21262D))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Headline with collapse/expand caret
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = article.title,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF8B949E)
                        )
                    }

                    // Byline
                    Text(
                        text = "Marketaux Intelligence & Agent Analysis • ${article.publishedAt} ✨",
                        color = Color(0xFF8B949E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    if (isExpanded) {
                        // Opening overview paragraph
                        Text(
                            text = article.summary.ifBlank {
                                "Key market drivers indicate notable institutional repositioning and active liquidity participation following this development."
                            },
                            color = Color(0xFFC9D1D9),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        HorizontalDivider(
                            color = Color(0xFF21262D),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // 3 Structured Institutional Driver Points
                        val primaryTicker = article.symbol ?: "Market"
                        val drivers = listOf(
                            "1. Catalyst & Price Action" to "Institutional desks are observing elevated bid-ask depth around $primaryTicker with volume clusters absorbing supply.",
                            "2. Sector & Peer Breadth" to "Sympathy moves across correlated assets indicate heightened momentum and multi-asset capital allocation.",
                            "3. Macro Backdrop & Risk" to "Volatility expectations remain anchored as options skew reflects demand for upside participation."
                        )

                        drivers.forEach { (title, desc) ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = desc,
                                    color = Color(0xFF8B949E),
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Concluding Takeaway
                        if (article.entities.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryBlue.copy(alpha = 0.08f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Impacted Assets:",
                                    color = Color(0xFF8B949E),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                article.entities.take(4).forEach { entity ->
                                    Text(
                                        text = entity.symbol,
                                        color = PrimaryBlue,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            onDismiss()
                                            onStockClick(entity.symbol)
                                        }
                                    )
                                }
                            }
                        }

                        // Read Full Web Article Button
                        if (article.url.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF30363D)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Read full report on ${article.source}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // Assets in this Category / Related Assets Section
            // -------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Assets in this category",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "1D change",
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Price",
                            color = Color(0xFF8B949E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Render related assets
                val displayAssets = if (relatedQuotes.isNotEmpty()) {
                    relatedQuotes.take(4).map {
                        RelatedAssetItem(
                            symbol = it.symbol,
                            name = it.companyName.ifBlank { it.symbol },
                            changePercent = it.changePercent,
                            price = it.lastPrice
                        )
                    }
                } else {
                    article.entities.take(4).map {
                        RelatedAssetItem(
                            symbol = it.symbol,
                            name = it.symbol,
                            changePercent = it.changePercent,
                            price = null
                        )
                    }
                }

                displayAssets.forEach { asset ->
                    RelatedAssetRow(
                        item = asset,
                        onClick = {
                            onDismiss()
                            onStockClick(asset.symbol)
                        }
                    )
                }
            }
        }
    }
}

private data class RelatedAssetItem(
    val symbol: String,
    val name: String,
    val changePercent: Double?,
    val price: Double?
)

@Composable
private fun RelatedAssetRow(
    item: RelatedAssetItem,
    onClick: () -> Unit
) {
    val changeColor = when {
        item.changePercent != null && item.changePercent > 0 -> ProfitGreen
        item.changePercent != null && item.changePercent < 0 -> LossRed
        else -> Color(0xFF8B949E)
    }
    val changeText = when {
        item.changePercent != null && item.changePercent > 0 -> "+${String.format(Locale.US, "%.2f%%", item.changePercent)}"
        item.changePercent != null && item.changePercent < 0 -> String.format(Locale.US, "%.2f%%", item.changePercent)
        else -> "—"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF13171F))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.symbol.take(2),
                    color = PrimaryBlue,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = item.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.symbol,
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = changeText,
                color = changeColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
            item.price?.let { p ->
                Text(
                    text = Formatters.formatCurrency(p),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false
                )
            } ?: Text(
                text = "—",
                color = Color(0xFF6E7681),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
