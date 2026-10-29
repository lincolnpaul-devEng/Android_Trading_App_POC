package com.trading.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.StockQuote
import com.trading.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstrumentSpecsBottomSheet(
    quote: StockQuote,
    onDismiss: () -> Unit
) {
    val specs = quote.specs
    val badgeColor = when (quote.assetClass) {
        AssetClass.FOREX -> PrimaryBlue
        AssetClass.INDEX -> PrimaryBlue
        AssetClass.CRYPTO -> ChartPurple
        AssetClass.COMMODITY -> ChartYellow
        AssetClass.FUTURES -> ChartCyan
        AssetClass.CFD -> ChartOrange
        AssetClass.EQUITY -> ProfitGreen
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = quote.assetClass.badge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                    Column {
                        Text(
                            text = quote.symbol,
                            style = TradingTypography.ticker,
                            color = TextPrimary
                        )
                        Text(
                            text = quote.companyName,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Contract & Instrument Specifications",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextTertiary,
                letterSpacing = 0.5.sp
            )

            // Specs Table Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SpecRow("Asset Category", quote.assetClass.displayName, badgeColor)
                SpecRow("Base Unit / Measure", specs.unitOfMeasure, TextPrimary)

                when (quote.assetClass) {
                    AssetClass.FOREX -> {
                        SpecRow("Standard Lot Size", "100,000 Units", TextPrimary)
                        SpecRow("Min Lot Size", "${specs.minLotSize} Lot", TextPrimary)
                        SpecRow("Pip Decimal", String.format("%.4f", specs.pipSize), PrimaryBlue)
                        SpecRow("Spread", "${String.format("%.1f", quote.spreadPips)} pips", TextPrimary)
                        specs.overnightSwapLong?.let {
                            SpecRow("Overnight Swap (Long)", "${String.format("%.3f", it * 100)}% / day", LossRed)
                        }
                        specs.overnightSwapShort?.let {
                            SpecRow("Overnight Swap (Short)", "${String.format("%.3f", it * 100)}% / day", ProfitGreen)
                        }
                    }
                    AssetClass.FUTURES -> {
                        SpecRow("Contract Multiplier", "$${specs.contractMultiplier.toInt()} / point", ChartCyan)
                        SpecRow("Expiration Cycle", specs.expiryDate ?: "Quarterly", TextPrimary)
                        SpecRow("Tick Size", "${specs.tickSize} pts", TextPrimary)
                        SpecRow("Tick Value", "$${String.format("%.2f", specs.tickSize * specs.contractMultiplier)}", TextPrimary)
                        SpecRow("Settlement Type", "Financial (Cash)", TextSecondary)
                    }
                    AssetClass.CRYPTO -> {
                        SpecRow("Contract Type", "Perpetual Swap (Linear)", ChartPurple)
                        SpecRow("24h Funding Rate", "${String.format("%.4f", (specs.fundingRate24h ?: 0.0) * 100)}%", ProfitGreen)
                        SpecRow("Trading Hours", "24/7/365 Continuous", TextPrimary)
                        SpecRow("Index Price", Formatters.formatCurrency(quote.lastPrice), TextSecondary)
                    }
                    AssetClass.COMMODITY -> {
                        SpecRow("Pricing Unit", "USD per ${specs.unitOfMeasure}", ChartYellow)
                        SpecRow("Trading Hours", "23 hrs / day (Sun-Fri)", TextPrimary)
                        specs.overnightSwapLong?.let {
                            SpecRow("Daily Storage/Swap (Long)", "${String.format("%.3f", it * 100)}%", LossRed)
                        }
                        specs.overnightSwapShort?.let {
                            SpecRow("Daily Storage/Swap (Short)", "${String.format("%.3f", it * 100)}%", ProfitGreen)
                        }
                    }
                    AssetClass.CFD, AssetClass.INDEX -> {
                        SpecRow("Underlying Index", quote.companyName, ChartOrange)
                        SpecRow("Contract Size", "1 Contract (${specs.unitOfMeasure})", TextPrimary)
                        specs.overnightSwapLong?.let {
                            SpecRow("Overnight Financing (Long)", "${String.format("%.3f", it * 100)}% / day", LossRed)
                        }
                        specs.overnightSwapShort?.let {
                            SpecRow("Overnight Financing (Short)", "${String.format("%.3f", it * 100)}% / day", ProfitGreen)
                        }
                    }
                    AssetClass.EQUITY -> {
                        SpecRow("Market Cap", Formatters.formatMarketCap(quote.marketCap), TextPrimary)
                        SpecRow("Exchange Hours", "09:30 - 16:00 EST", TextPrimary)
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                SpecRow("Max Permitted Leverage", "${specs.maxLeverage}x", ChartPurple)
                SpecRow("Margin Mode", "Cross / Isolated Margin", TextSecondary)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
