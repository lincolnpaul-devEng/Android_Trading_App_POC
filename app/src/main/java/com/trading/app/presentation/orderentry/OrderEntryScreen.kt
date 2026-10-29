package com.trading.app.presentation.orderentry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trading.app.domain.model.*
import com.trading.app.presentation.components.AutoCloseBottomSheet
import com.trading.app.presentation.components.Formatters
import com.trading.app.presentation.components.InstrumentSpecsBottomSheet
import com.trading.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEntryScreen(
    onBack: () -> Unit,
    viewModel: OrderEntryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSpecsSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Trade ${state.symbol}", style = TradingTypography.ticker, color = TextPrimary)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(PrimaryBlue.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = state.specs.assetClass.badge,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                        Text(state.companyName, fontSize = 12.sp, color = TextSecondary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showSpecsSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Contract Specs", tint = TextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Prominent Header Card: Collateral/Margin & Current Price (Matching Reference Screens)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (state.requiredMargin > 0) Formatters.formatCurrency(state.requiredMargin) else Formatters.formatCurrency(state.currentPrice),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                val posDescription = if (state.quantity.isNotEmpty() && state.quantity.toIntOrNull() != null) {
                    "${if (state.side == OrderSide.BUY) "Long" else "Short"} ${state.quantity} ${state.symbol} • Current Price ${Formatters.formatCurrency(state.currentPrice)}"
                } else {
                    "Current Price ${Formatters.formatCurrency(state.currentPrice)}"
                }
                Text(
                    text = posDescription,
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                if (state.bidPrice > 0.0 || state.askPrice > 0.0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bid: ${Formatters.formatCurrency(state.bidPrice)}", fontSize = 11.sp, color = LossRed, fontWeight = FontWeight.SemiBold)
                        Text("• Spread: ${Formatters.formatCurrency(state.spread)} •", fontSize = 10.sp, color = TextTertiary)
                        Text("Ask: ${Formatters.formatCurrency(state.askPrice)}", fontSize = 11.sp, color = ProfitGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Buy/Sell toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(4.dp)
            ) {
                OrderSide.entries.forEach { side ->
                    val isSelected = state.side == side
                    val color = when {
                        isSelected && side == OrderSide.BUY -> ProfitGreen
                        isSelected && side == OrderSide.SELL -> LossRed
                        else -> DarkCard
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color)
                            .clickable { viewModel.setSide(side) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (side == OrderSide.BUY) "BUY / LONG" else "SELL / SHORT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Leverage Selector (Matching Screenshot 0 & 3)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Leverage", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, softWrap = false)
                    Text(
                        "${state.leverage}x • Up to ${state.specs.maxLeverage}x available",
                        fontSize = 12.sp,
                        color = ChartPurple,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 5, 10, 20).forEach { lev ->
                        val isSelected = state.leverage == lev
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ChartPurple else DarkSurfaceVariant)
                                .clickable { viewModel.setLeverage(lev) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${lev}x",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DarkBackground else TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Auto Close Row (Matching Screenshot 0 & 3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .clickable { viewModel.openAutoCloseSheet() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.hasAutoClose) "Auto Close" else "Add Auto Close",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    if (state.hasAutoClose) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val tpText = state.calculatedTpPrice?.let { "TP ${Formatters.formatCurrency(it)}" } ?: ""
                        val slText = state.calculatedSlPrice?.let { "SL ${Formatters.formatCurrency(it)}" } ?: ""
                        val summaryText = listOf(tpText, slText).filter { it.isNotEmpty() }.joinToString(" • ")
                        Text(
                            text = summaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = PrimaryBlue,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Take Profit & Stop Loss targets",
                            fontSize = 12.sp,
                            color = TextTertiary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (state.hasAutoClose) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.clearAutoClose() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = TextTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Edit Auto Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Auto Close",
                        tint = ChartPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Order Type (Horizontally scrollable chip bar so multi-word types never wrap vertically)
            Text("Order Type", style = TradingTypography.label)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OrderType.entries.forEach { type ->
                    val isSelected = state.type == type
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
                            .clickable { viewModel.setType(type) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.name.replace("_", " "),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Quantity
            TradingTextField(
                label = "Quantity",
                value = state.quantity,
                onValueChange = viewModel::setQuantity,
                keyboardType = KeyboardType.Number
            )

            // Quick quantity buttons (Compact horizontal padding prevents 100% from wrapping vertically)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.25, 0.50, 0.75, 1.0).forEach { pct ->
                    OutlinedButton(
                        onClick = { viewModel.setQuantityPercent(pct) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = androidx.compose.ui.graphics.SolidColor(DarkBorder)
                        )
                    ) {
                        Text(
                            text = "${(pct * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Size by Account Risk (Stop Loss Distance Model)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant.copy(alpha = 0.5f))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Size by Risk (% Equity)", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    if (state.calculatedSlPrice == null) {
                        Text("(Requires SL)", fontSize = 10.sp, color = TextTertiary)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0.5 to "0.5% Risk", 1.0 to "1.0% Risk", 2.0 to "2.0% Risk").forEach { (pct, label) ->
                        OutlinedButton(
                            onClick = { viewModel.setQuantityByRiskPercent(pct) },
                            modifier = Modifier.weight(1f).height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ChartPurple),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(ChartPurple.copy(alpha = 0.5f))
                            )
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Limit price (conditional)
            if (state.type in listOf(OrderType.LIMIT, OrderType.STOP_LIMIT)) {
                TradingTextField(
                    label = "Limit Price",
                    value = state.limitPrice,
                    onValueChange = viewModel::setLimitPrice,
                    keyboardType = KeyboardType.Decimal
                )
            }

            // Stop price (conditional)
            if (state.type in listOf(OrderType.STOP, OrderType.STOP_LIMIT, OrderType.TRAILING_STOP)) {
                TradingTextField(
                    label = "Stop Price",
                    value = state.stopPrice,
                    onValueChange = viewModel::setStopPrice,
                    keyboardType = KeyboardType.Decimal
                )
            }

            // Time in Force
            Text("Time in Force", style = TradingTypography.label)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeInForce.entries.forEach { tif ->
                    val isSelected = state.timeInForce == tif
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryBlue else DarkSurfaceVariant)
                            .clickable { viewModel.setTimeInForce(tif) }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tif.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) DarkBackground else TextSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Order summary
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Text("Order Summary", style = TradingTypography.sectionHeader, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                SummaryRow("Side", if (state.side == OrderSide.BUY) "BUY / LONG" else "SELL / SHORT", if (state.side == OrderSide.BUY) ProfitGreen else LossRed)
                SummaryRow("Leverage", "${state.leverage}x", ChartPurple)
                SummaryRow("Quantity", state.quantity.ifEmpty { "—" }, TextPrimary)
                SummaryRow("Notional Value", Formatters.formatCurrency(state.estimatedCost), TextPrimary)
                SummaryRow("Required Margin", Formatters.formatCurrency(state.requiredMargin), ProfitGreen)
                SummaryRow("Est. Fee (0.095%)", Formatters.formatCurrency(state.estimatedFee), TextSecondary)
                SummaryRow("Buying Power", Formatters.formatCurrency(state.buyingPower), TextSecondary)

                state.calculatedTpPrice?.let { tp ->
                    SummaryRow("Take Profit Trigger", Formatters.formatCurrency(tp), ProfitGreen)
                }
                state.calculatedSlPrice?.let { sl ->
                    SummaryRow("Stop Loss Trigger", Formatters.formatCurrency(sl), LossRed)
                }

                state.estimatedLiquidationPrice?.let { liq ->
                    SummaryRow("Est. Liquidation Price", Formatters.formatCurrency(liq), Color(0xFFD29922))
                }
                state.riskRewardRatioDisplay?.let { rr ->
                    SummaryRow("Risk : Reward (R:R)", rr, ProfitGreen)
                }
                state.totalRiskDollars?.let { risk ->
                    SummaryRow("Max Risk ($)", "-${Formatters.formatCurrency(risk)}", LossRed)
                }
                state.totalRewardDollars?.let { reward ->
                    SummaryRow("Target Profit ($)", "+${Formatters.formatCurrency(reward)}", ProfitGreen)
                }

                if (state.currentPosition > 0) {
                    SummaryRow("Current Position", "${state.currentPosition} shares", TextSecondary)
                }
            }

            // Error
            state.error?.let { error ->
                Text(
                    error,
                    color = LossRed,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LossRed.copy(alpha = 0.1f))
                        .padding(12.dp)
                )
            }

            // Success
            state.orderResult?.let { result ->
                Text(
                    result,
                    color = ProfitGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ProfitGreen.copy(alpha = 0.1f))
                        .padding(12.dp)
                )
            }

            // Submit button (Matching Screenshot 0 & 3 Confirm Button)
            Button(
                onClick = { viewModel.showConfirmation() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ChartPurple
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Confirm ${state.side.name} Order",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Auto Close Bottom Sheet Modal
        if (state.showAutoCloseSheet) {
            AutoCloseBottomSheet(
                isShort = state.isShort,
                currentPrice = state.effectivePrice,
                leverage = state.leverage,
                initialMode = state.autoCloseMode,
                initialTp = state.takeProfit,
                initialSl = state.stopLoss,
                onDismiss = viewModel::dismissAutoCloseSheet,
                onSave = viewModel::saveAutoClose
            )
        }

        // Confirmation dialog
        if (state.showConfirmation) {
            AlertDialog(
                onDismissRequest = viewModel::dismissConfirmation,
                title = { Text("Confirm Order", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${state.side} ${state.quantity} ${state.symbol} (${state.leverage}x leverage) @ ${if (state.type == OrderType.MARKET) "MARKET" else state.limitPrice}",
                            color = TextPrimary, fontWeight = FontWeight.SemiBold
                        )
                        Text("Required Margin: ${Formatters.formatCurrency(state.requiredMargin)}", color = TextSecondary)
                        Text("Est. Fee: ${Formatters.formatCurrency(state.estimatedFee)}", color = TextSecondary)
                        if (state.hasAutoClose) {
                            state.calculatedTpPrice?.let {
                                Text("Take Profit: ${Formatters.formatCurrency(it)}", color = ProfitGreen)
                            }
                            state.calculatedSlPrice?.let {
                                Text("Stop Loss: ${Formatters.formatCurrency(it)}", color = LossRed)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::submitOrder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChartPurple
                        )
                    ) { Text("Confirm", color = DarkBackground) }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissConfirmation) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = DarkSurface
            )
        }

        // Contract Specs Modal
        if (showSpecsSheet) {
            val dummyQuote = StockQuote(
                symbol = state.symbol,
                companyName = state.companyName,
                lastPrice = state.currentPrice,
                change = 0.0,
                changePercent = 0.0,
                open = state.currentPrice,
                high = state.currentPrice,
                low = state.currentPrice,
                volume = 0L,
                marketCap = 0L,
                bid = state.currentPrice - state.specs.pipSize,
                ask = state.currentPrice + state.specs.pipSize,
                bidSize = 100,
                askSize = 100,
                specs = state.specs
            )
            InstrumentSpecsBottomSheet(
                quote = dummyQuote,
                onDismiss = { showSpecsSheet = false }
            )
        }
    }
}

@Composable
private fun TradingTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
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
}

@Composable
private fun SummaryRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = TextSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}
