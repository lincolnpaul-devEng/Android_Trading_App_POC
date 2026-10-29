package com.trading.app.presentation.dashboard

import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trading.app.R
import com.trading.app.domain.model.AssetClass
import com.trading.app.domain.model.MarketIndex
import com.trading.app.domain.model.SectorPerformance
import com.trading.app.presentation.components.FlashingPriceText
import com.trading.app.presentation.components.Formatters
import com.trading.app.presentation.components.HeroVideoBanner
import com.trading.app.presentation.components.InstrumentSpecsBottomSheet
import com.trading.app.presentation.components.MultiAssetQuoteRow
import com.trading.app.presentation.game.MiniGameCarouselSection
import com.trading.app.presentation.theme.*

@Composable
fun DashboardScreen(
    onStockClick: (String) -> Unit,
    onGameClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Brand & Profile Header Bar (Logo on Top-Left + Circular Profile Avatar)
            item {
                DashboardTopHeader(
                    onProfileClick = onProfileClick
                )
            }

            // Real-time Equity & Margin Exposure Pill
            item {
                DashboardAccountEquityStrip(
                    totalEquity = state.totalEquity,
                    availableMargin = state.availableMargin,
                    unrealizedPnL = state.unrealizedPnL,
                    onClick = onProfileClick
                )
            }

            // Homescreen Hero Video Banner with live translucent Sector Performance overlay
            item {
                HeroVideoBanner(
                    sectors = state.sectors
                )
            }

            // Market Status
            item {
                MarketStatusBanner()
            }

            // Market Indices (Tradable — tap any index to view chart & trade)
            item {
                Text("Market Overview (Tap to Trade)", style = TradingTypography.sectionHeader, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.indices) { index ->
                        MarketIndexCard(
                            index = index,
                            onClick = { onStockClick(index.symbol) }
                        )
                    }
                }
            }

            // Interactive Mini-Games Horizontal Viewport Carousel (Positioned just above Multi-Asset Terminal)
            item {
                MiniGameCarouselSection(
                    onGameClick = { game -> onGameClick(game.id) }
                )
            }

            // Multi-Asset Category Filter
            item {
                Text("Multi-Asset Terminal", style = TradingTypography.sectionHeader, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AssetFilterChip(
                            label = "All Markets",
                            isSelected = state.selectedAssetClass == null,
                            onClick = { viewModel.selectAssetClass(null) }
                        )
                    }
                    items(AssetClass.entries) { assetClass ->
                        AssetFilterChip(
                            label = assetClass.displayName,
                            isSelected = state.selectedAssetClass == assetClass,
                            onClick = { viewModel.selectAssetClass(assetClass) }
                        )
                    }
                }
            }

            // Multi-Asset Live Quotes List
            items(state.multiAssetQuotes, key = { it.symbol }) { quote ->
                MultiAssetQuoteRow(
                    quote = quote,
                    onClick = { onStockClick(quote.symbol) },
                    onInfoClick = { viewModel.showSpecs(quote) }
                )
            }


        }

        // Instrument Specifications Bottom Sheet
        state.selectedSpecsQuote?.let { quote ->
            InstrumentSpecsBottomSheet(
                quote = quote,
                onDismiss = viewModel::dismissSpecs
            )
        }
    }
}

@Composable
private fun DashboardTopHeader(
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Top-Left: Transparent Cash Cage Logo
        Image(
            painter = painterResource(id = R.drawable.ic_cash_cage_logo),
            contentDescription = "Cash Cage Logo",
            modifier = Modifier
                .height(40.dp)
                .wrapContentWidth()
        )

        // Top-Right: Circular Profile Avatar (PNG)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F1412))
                .border(1.5.dp, Color(0xFFBFAF8D), CircleShape)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.profile_avatar),
                contentDescription = "Profile Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun AssetFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) DarkBackground else TextSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MarketStatusBanner() {
    val mode by com.trading.app.data.DataSourceConfig.modeFlow.collectAsStateWithLifecycle()
    val accountType by com.trading.app.data.DataSourceConfig.accountTypeFlow.collectAsStateWithLifecycle()
    val isLiveFeed = mode != com.trading.app.data.DataSourceMode.SIMULATED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(ProfitGreenDark.copy(alpha = 0.15f))
            .clickable {
                com.trading.app.data.DataSourceConfig.mode =
                    if (isLiveFeed) com.trading.app.data.DataSourceMode.SIMULATED
                    else com.trading.app.data.DataSourceMode.HYBRID
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isLiveFeed) ProfitGreen else ChartYellow)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLiveFeed) "Live Market Stream • ${accountType.name}" else "Simulator • ${accountType.name}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isLiveFeed) ProfitGreen else ChartYellow,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "FX · Indices · Crypto · Futures · CFD",
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MarketIndexCard(index: MarketIndex, onClick: () -> Unit = {}) {
    val changeColor = if (index.isPositive) ProfitGreen else LossRed
    val bg = if (index.isPositive) ProfitGreen.copy(alpha = 0.06f) else LossRed.copy(alpha = 0.06f)

    Column(
        modifier = Modifier
            .width(168.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(
            text = index.name,
            fontSize = 12.sp,
            color = TextSecondary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        FlashingPriceText(
            price = index.value,
            formattedPrice = Formatters.formatPrice(index.value),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            textColor = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${Formatters.formatChange(index.change)} (${Formatters.formatPercent(index.changePercent)})",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = changeColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DashboardAccountEquityStrip(
    totalEquity: Double,
    availableMargin: Double,
    unrealizedPnL: Double,
    onClick: () -> Unit
) {
    val pnlColor = if (unrealizedPnL >= 0) ProfitGreen else LossRed

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, Color(0xFF21262D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ProfitGreen)
                    )
                    Text("Demo Balance", fontSize = 11.sp, color = Color(0xFF8B949E))
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Formatters.formatCurrency(totalEquity),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Avail. Margin", fontSize = 10.sp, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = Formatters.formatCurrency(availableMargin),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC9D1D9)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Floating P&L", fontSize = 10.sp, color = Color(0xFF8B949E))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = (if (unrealizedPnL >= 0) "+" else "") + Formatters.formatCurrency(unrealizedPnL),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = pnlColor
                )
            }
        }
    }
}
