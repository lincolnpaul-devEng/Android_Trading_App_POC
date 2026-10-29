package com.trading.app.presentation.stockdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trading.app.presentation.components.*
import com.trading.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    onBack: () -> Unit,
    onTrade: (String) -> Unit,
    viewModel: StockDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val intervals = listOf("1m", "5m", "15m", "1h", "1D", "1W", "1M")

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.symbol, style = TradingTypography.ticker, color = TextPrimary)
                        state.quote?.let {
                            Text(it.companyName, fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        bottomBar = {
            state.quote?.let {
                TradeButton(
                    symbol = viewModel.symbol,
                    quote = it,
                    onTrade = { onTrade(viewModel.symbol) }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Price header
            item {
                state.quote?.let { quote ->
                    PriceHeader(quote)
                }
            }

            // Active Open Position Card with instant Close button
            state.openPosition?.let { pos ->
                item {
                    OpenPositionBanner(
                        position = pos,
                        onClosePosition = { viewModel.closeOpenPosition() }
                    )
                }
            }

            // Interval selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervals.forEach { interval ->
                        IntervalChip(
                            label = interval,
                            isSelected = state.selectedInterval == interval,
                            onClick = { viewModel.selectInterval(interval) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Technical Indicator Chips (EMA 20, EMA 50, Bollinger Bands)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IndicatorChip(
                        label = "EMA 20",
                        isActive = state.showEma20,
                        activeColor = Color(0xFFF1E05A),
                        onClick = { viewModel.toggleEma20() },
                        modifier = Modifier.weight(1f)
                    )
                    IndicatorChip(
                        label = "EMA 50",
                        isActive = state.showEma50,
                        activeColor = Color(0xFF58A6FF),
                        onClick = { viewModel.toggleEma50() },
                        modifier = Modifier.weight(1f)
                    )
                    IndicatorChip(
                        label = "BOLL (20,2)",
                        isActive = state.showBollingerBands,
                        activeColor = Color(0xFF26A69A),
                        onClick = { viewModel.toggleBollingerBands() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Candlestick chart with live overlay lines & indicators
            item {
                CandlestickChart(
                    candles = state.candles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    openPosition = state.openPosition,
                    showEma20 = state.showEma20,
                    showEma50 = state.showEma50,
                    showBollingerBands = state.showBollingerBands
                )
            }

            // Quote details
            item {
                state.quote?.let { QuoteDetails(it) }
            }

            // Order book
            item {
                Text("Order Book (Level 2)", style = TradingTypography.sectionHeader, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                state.orderBook?.let { OrderBookView(it) }
            }

            // Depth chart
            item {
                Text("Market Depth", style = TradingTypography.sectionHeader, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                state.orderBook?.let { DepthChart(it) }
            }
        }
    }
}

@Composable
private fun OpenPositionBanner(
    position: com.trading.app.domain.model.Position,
    onClosePosition: () -> Unit
) {
    val pnlColor = if (position.isProfit) ProfitGreen else LossRed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "OPEN ${position.side.name} • ${position.quantity} @ ${Formatters.formatPrice(position.averageCost)} (${position.leverage}x)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "P&L: ${if (position.isProfit) "+" else ""}${Formatters.formatCurrency(position.unrealizedPnL)} (${String.format("%.2f", position.unrealizedPnLPercent)}%)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pnlColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onClosePosition,
            colors = ButtonDefaults.buttonColors(containerColor = LossRed),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Close Position",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
private fun PriceHeader(quote: com.trading.app.domain.model.StockQuote) {
    Column {
        FlashingPriceText(
            price = quote.lastPrice,
            formattedPrice = Formatters.formatCurrency(quote.lastPrice),
            textStyle = TradingTypography.priceDisplay,
            textColor = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        PriceChangeIndicator(change = quote.change, changePercent = quote.changePercent)
    }
}

@Composable
private fun IntervalChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) DarkBackground else TextSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun QuoteDetails(quote: com.trading.app.domain.model.StockQuote) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(16.dp)
    ) {
        Text("Quote Details", style = TradingTypography.sectionHeader, color = TextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        val items = listOf(
            "Open" to Formatters.formatPrice(quote.open),
            "High" to Formatters.formatPrice(quote.high),
            "Low" to Formatters.formatPrice(quote.low),
            "Volume" to Formatters.formatVolume(quote.volume),
            "Market Cap" to Formatters.formatMarketCap(quote.marketCap),
            "Bid" to "${Formatters.formatPrice(quote.bid)} x ${quote.bidSize}",
            "Ask" to "${Formatters.formatPrice(quote.ask)} x ${quote.askSize}",
            "Spread" to Formatters.formatPrice(quote.spread)
        )

        items.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                row.forEach { (label, value) ->
                    Column(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                        Text(label, fontSize = 11.sp, color = TextTertiary)
                        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun IndicatorChip(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) activeColor.copy(alpha = 0.2f) else DarkSurfaceVariant)
            .border(
                width = 1.dp,
                color = if (isActive) activeColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) activeColor else TextSecondary,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun TradeButton(
    symbol: String,
    quote: com.trading.app.domain.model.StockQuote,
    onTrade: () -> Unit
) {
    Surface(
        color = DarkSurface,
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Spread: ${Formatters.formatPrice(quote.spread)}",
                    fontSize = 11.sp,
                    color = TextTertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Sell @ Bid
                Button(
                    onClick = onTrade,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SELL", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color.White)
                        Text(Formatters.formatPrice(quote.bid), fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                }

                // Quick Buy @ Ask
                Button(
                    onClick = onTrade,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BUY", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = DarkBackground)
                        Text(Formatters.formatPrice(quote.ask), fontSize = 11.sp, color = DarkBackground.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}
