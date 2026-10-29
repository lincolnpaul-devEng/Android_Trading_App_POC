package com.trading.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.presentation.theme.*

enum class AutoCloseMode {
    PERCENT_PNL,
    PRICE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCloseBottomSheet(
    isShort: Boolean,
    currentPrice: Double,
    leverage: Int,
    initialMode: AutoCloseMode,
    initialTp: String,
    initialSl: String,
    onDismiss: () -> Unit,
    onSave: (mode: AutoCloseMode, tp: String, sl: String) -> Unit
) {
    var mode by remember { mutableStateOf(initialMode) }
    var tpInput by remember { mutableStateOf(initialTp) }
    var slInput by remember { mutableStateOf(initialSl) }
    var showModeDropdown by remember { mutableStateOf(false) }

    // Helper calculations
    val lev = maxOf(1, leverage)

    fun calculateTpPrice(): Double? {
        val tpVal = tpInput.toDoubleOrNull() ?: return null
        return if (mode == AutoCloseMode.PERCENT_PNL) {
            val deltaPct = tpVal / (100.0 * lev)
            if (isShort) currentPrice * (1.0 - deltaPct) else currentPrice * (1.0 + deltaPct)
        } else {
            tpVal
        }
    }

    fun calculateSlPrice(): Double? {
        val slVal = slInput.toDoubleOrNull() ?: return null
        return if (mode == AutoCloseMode.PERCENT_PNL) {
            val deltaPct = kotlin.math.abs(slVal) / (100.0 * lev)
            if (isShort) currentPrice * (1.0 + deltaPct) else currentPrice * (1.0 - deltaPct)
        } else {
            slVal
        }
    }

    val calculatedTp = calculateTpPrice()
    val calculatedSl = calculateSlPrice()

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
            // Header: Close icon, Title, Confirm icon button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Auto Close",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = {
                        onSave(mode, tpInput.trim(), slInput.trim())
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChartPurple)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = DarkBackground,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Mode Selector Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .clickable { showModeDropdown = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Use",
                    fontSize = 15.sp,
                    color = TextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (mode == AutoCloseMode.PERCENT_PNL) "% P&L" else "Price ($)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.UnfoldMore,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = showModeDropdown,
                    onDismissRequest = { showModeDropdown = false },
                    modifier = Modifier.background(DarkCard)
                ) {
                    DropdownMenuItem(
                        text = { Text("% P&L", color = TextPrimary) },
                        onClick = {
                            mode = AutoCloseMode.PERCENT_PNL
                            showModeDropdown = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Price ($)", color = TextPrimary) },
                        onClick = {
                            mode = AutoCloseMode.PRICE
                            showModeDropdown = false
                        }
                    )
                }
            }

            // Take Profit Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Take Profit",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == AutoCloseMode.PERCENT_PNL) "+ " else "$ ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                        OutlinedTextField(
                            value = tpInput,
                            onValueChange = { tpInput = it },
                            placeholder = { Text("0.0", color = TextTertiary, fontSize = 20.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = ProfitGreen,
                                focusedTextColor = ProfitGreen,
                                unfocusedTextColor = ProfitGreen,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (tpInput.isNotEmpty()) {
                        IconButton(onClick = { tpInput = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (calculatedTp != null) {
                    Text(
                        text = "Trigger Price: ${Formatters.formatCurrency(calculatedTp)}",
                        fontSize = 12.sp,
                        color = ProfitGreen.copy(alpha = 0.8f)
                    )
                }
            }

            // Stop Loss Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Stop Loss",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == AutoCloseMode.PERCENT_PNL) "- " else "$ ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LossRed
                        )
                        OutlinedTextField(
                            value = slInput,
                            onValueChange = { slInput = it },
                            placeholder = { Text("0.0", color = TextTertiary, fontSize = 20.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                cursorColor = LossRed,
                                focusedTextColor = LossRed,
                                unfocusedTextColor = LossRed,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (slInput.isNotEmpty()) {
                        IconButton(onClick = { slInput = "" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextTertiary, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                if (calculatedSl != null) {
                    Text(
                        text = "Trigger Price: ${Formatters.formatCurrency(calculatedSl)}",
                        fontSize = 12.sp,
                        color = LossRed.copy(alpha = 0.8f)
                    )
                }
            }

            // Quick Preset Pills (e.g., Stop Loss presets: -5%, -10%, -25%, -50%)
            if (mode == AutoCloseMode.PERCENT_PNL) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Quick Stop Loss Presets", fontSize = 12.sp, color = TextTertiary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 25, 50).forEach { pct ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .clickable { slInput = pct.toString() }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-$pct%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (slInput == pct.toString()) LossRed else TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
