package com.trading.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.StockQuote
import com.trading.app.presentation.theme.*

@Composable
fun MultiAssetQuoteRow(
    quote: StockQuote,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pnlColor = if (quote.isPositive) ProfitGreen else LossRed
    val badgeColor = when (quote.assetClass) {
        AssetClass.FOREX -> PrimaryBlue
        AssetClass.INDEX -> PrimaryBlue
        AssetClass.CRYPTO -> ChartPurple
        AssetClass.COMMODITY -> ChartYellow
        AssetClass.FUTURES -> ChartCyan
        AssetClass.CFD -> ChartOrange
        AssetClass.EQUITY -> ProfitGreen
    }

    val formattedPrice = when (quote.assetClass) {
        AssetClass.FOREX -> if (quote.symbol.contains("JPY") || quote.symbol.contains("TRY") || quote.symbol.contains("ZAR") || quote.symbol.contains("MXN")) String.format("%.3f", quote.lastPrice) else String.format("%.5f", quote.lastPrice)
        AssetClass.COMMODITY -> String.format("%.2f", quote.lastPrice)
        AssetClass.FUTURES -> String.format("%.2f", quote.lastPrice)
        AssetClass.CFD, AssetClass.INDEX -> String.format("%.2f", quote.lastPrice)
        else -> Formatters.formatPrice(quote.lastPrice)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Symbol, Badge, & Asset Specific Subtitle
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = quote.symbol,
                    style = TradingTypography.ticker,
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.18f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = quote.assetClass.badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Specs",
                        tint = TextTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            val metadataText = when (quote.assetClass) {
                AssetClass.FOREX -> "${quote.specs.subCategory ?: "FX"} • Spread: ${String.format("%.1f", quote.spreadPips)} pips • 100k lot"
                AssetClass.INDEX -> "${quote.companyName} • Bid ${String.format("%.1f", quote.bid)} / Ask ${String.format("%.1f", quote.ask)}"
                AssetClass.CRYPTO -> "Funding: ${String.format("%.3f", (quote.specs.fundingRate24h ?: 0.0) * 100)}% • 24/7"
                AssetClass.FUTURES -> "Exp: ${quote.specs.expiryDate ?: "DEC 26"} • Mult: $${quote.specs.contractMultiplier.toInt()}"
                AssetClass.COMMODITY -> "${quote.companyName} • Per ${quote.specs.unitOfMeasure}"
                AssetClass.CFD -> "${quote.companyName} • Swap: ${String.format("%.3f", (quote.specs.overnightSwapLong ?: 0.0) * 100)}%/d"
                AssetClass.EQUITY -> "${quote.companyName} • Bid $${String.format("%.2f", quote.bid)}"
            }

            Text(
                text = metadataText,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Right Column: Live Price & Ticker Change
        Column(horizontalAlignment = Alignment.End) {
            FlashingPriceText(
                price = quote.lastPrice,
                formattedPrice = if (quote.assetClass == AssetClass.FOREX) formattedPrice else "$$formattedPrice",
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                textColor = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${if (quote.isPositive) "+" else ""}${String.format("%.2f", quote.changePercent)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = pnlColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
