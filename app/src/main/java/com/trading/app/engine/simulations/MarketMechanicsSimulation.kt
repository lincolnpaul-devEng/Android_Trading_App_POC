package com.trading.app.engine.simulations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.engine.core.MoneyMath
import com.trading.app.engine.core.OrderType
import com.trading.app.engine.core.TradeDirection
import com.trading.app.engine.market.MarketDataEngine
import com.trading.app.engine.trading.TradingEngine
import java.math.BigDecimal

@Composable
fun MarketMechanicsSimulation(
    configJson: String?,
    onComplete: (earnedXp: Int) -> Unit,
    onBack: () -> Unit
) {
    val marketEngine = remember { MarketDataEngine(startingPrice = BigDecimal("150.00"), spreadBps = 10.0) }
    val tradingEngine = remember { TradingEngine(initialBalance = BigDecimal("10000.00")) }

    var lastExecutionMessage by remember { mutableStateOf<String?>(null) }
    var educationalFeedback by remember { mutableStateOf<String?>(null) }

    val orderBook = remember(marketEngine.currentPrice) { marketEngine.getOrderBook(depthLevels = 4) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Top Nav
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF161B22), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "MARKET MECHANICS ENGINE",
                    color = Color(0xFF5FE2D6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Order Book Depth & Spread",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .background(Color(0x335FE2D6), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF5FE2D6), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("+40 XP", color = Color(0xFF5FE2D6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Best Bid / Ask & Spread Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFF21262D), RoundedCornerShape(14.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("BEST BID (BUY)", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = MoneyMath.formatCurrency(marketEngine.bestBid),
                    color = Color(0xFF4ADE80),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BID-ASK SPREAD", color = Color(0xFF8B949E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "${MoneyMath.formatCurrency(orderBook.spread)} (${String.format("%.1f", orderBook.spreadBps)} bps)",
                    color = Color(0xFF5FE2D6),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("BEST ASK (SELL)", color = Color(0xFFF87171), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = MoneyMath.formatCurrency(marketEngine.bestAsk),
                    color = Color(0xFFF87171),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Level 2 Order Book Depth Visualizer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Column {
                Text(
                    text = "LEVEL 2 ORDER BOOK DEPTH",
                    color = Color(0xFF8B949E),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Asks (Red)
                orderBook.asks.reversed().forEach { ask ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = MoneyMath.formatCurrency(ask.price),
                            color = Color(0xFFF87171),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${ask.quantity.toInt()} shares (${ask.orderCount} orders)",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp
                        )
                    }
                }

                // Spread divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .background(Color(0xFF21262D), RoundedCornerShape(4.dp))
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "─── SPREAD: ${MoneyMath.formatCurrency(orderBook.spread)} ───",
                        color = Color(0xFF5FE2D6),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bids (Green)
                orderBook.bids.forEach { bid ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = MoneyMath.formatCurrency(bid.price),
                            color = Color(0xFF4ADE80),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${bid.quantity.toInt()} shares (${bid.orderCount} orders)",
                            color = Color(0xFF8B949E),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Order Execution Experiment Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val result = tradingEngine.executeOrder(
                        asset = "AAPL",
                        direction = TradeDirection.BUY,
                        orderType = OrderType.MARKET,
                        quantity = BigDecimal("20"),
                        bestBid = marketEngine.bestBid,
                        bestAsk = marketEngine.bestAsk
                    )
                    lastExecutionMessage = result.message
                    educationalFeedback = "Notice that your Market Order filled immediately at the Ask price (${MoneyMath.formatCurrency(result.fillPrice)}). In market mechanics, taking liquidity immediately costs the spread!"
                    onComplete(40)
                },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5FE2D6), contentColor = Color(0xFF0D1117)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Market Buy 20 Shares", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    val limitPrice = marketEngine.bestBid
                    lastExecutionMessage = "Limit Buy 20 AAPL placed at ${MoneyMath.formatCurrency(limitPrice)}. Order joined the resting bid queue."
                    educationalFeedback = "Limit orders provide liquidity to the market. You don't pay the spread, but execution is not guaranteed until a seller crosses down to your price."
                    onComplete(40)
                },
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Post Limit Bid", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        AnimatedVisibility(visible = lastExecutionMessage != null) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1F5FE2D6), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF5FE2D6), RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF5FE2D6), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ORDER EXECUTED (+40 XP)", color = Color(0xFF5FE2D6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(lastExecutionMessage.orEmpty(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(educationalFeedback.orEmpty(), color = Color(0xFFCBD5E1), fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }
        }
    }
}
