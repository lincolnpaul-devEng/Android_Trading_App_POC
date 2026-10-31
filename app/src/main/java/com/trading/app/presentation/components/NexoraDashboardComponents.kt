package com.trading.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Segment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.presentation.theme.DarkBorder
import com.trading.app.presentation.theme.DarkSurface
import com.trading.app.presentation.theme.TextPrimary
import com.trading.app.presentation.theme.TextSecondary

/**
 * Replicates the Top App Bar from the reference image:
 * - Brand text "Cash Cage" with stepped tier logo icon
 * - Right-hand icons: Notification Bell + Hamburger Menu button
 */
@Composable
fun NexoraTopAppBar(
    onNotificationClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand: Stepped Tier Logo + Brand Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Stepped geometric tier logo
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(22.dp)
            ) {
                Box(modifier = Modifier.height(3.dp).width(12.dp).background(Color(0xFFCBD5E1), RoundedCornerShape(1.dp)))
                Box(modifier = Modifier.height(3.dp).width(18.dp).background(Color(0xFF94A3B8), RoundedCornerShape(1.dp)))
                Box(modifier = Modifier.height(3.dp).width(22.dp).background(Color(0xFF58A6FF), RoundedCornerShape(1.dp)))
            }

            Text(
                text = "Cash Cage",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        // Action Icons: Bell + Menu
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNotificationClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF162235))
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onMenuClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF162235))
            ) {
                Icon(
                    imageVector = Icons.Default.Segment,
                    contentDescription = "Menu",
                    tint = Color(0xFFCBD5E1),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Replicates the Hero Total Portfolio Value Card:
 * - Subtitle: "Your Total Portfolio Value"
 * - Large Bold Monetary Figure ($186,486.28)
 * - Return pills: +4.73% | +$1,286 - Today
 * - Three quick action buttons in bottom dark shelf:
 *   [Invest] [Add Fund] [Withdraw]
 */
@Composable
fun NexoraHeroPortfolioCard(
    totalValue: String = "$186,486.28",
    todayReturnPct: String = "+4.73%",
    todayDollarGain: String = "+$1,286 - Today",
    onInvestClick: () -> Unit = {},
    onAddFundClick: () -> Unit = {},
    onWithdrawClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Split e.g. "$186,486.28" into "$186,486" and ".28" to match reference visual weight
    val dotIndex = totalValue.indexOf('.')
    val mainPart = if (dotIndex != -1) totalValue.substring(0, dotIndex) else totalValue
    val decimalPart = if (dotIndex != -1) totalValue.substring(dotIndex) else ""

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF5A93DA), // Soft luminous sky blue
                        Color(0xFF285C9B), // Mid gradient transition
                        Color(0xFF0F2C55), // Deep rich blue
                        Color(0xFF061833)  // Midnight navy base
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x55FFFFFF),
                        Color(0x15FFFFFF),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(26.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // "Your Total Portfolio Value"
            Text(
                text = "Your Total Portfolio Value",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Large figure with translucent decimal part
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = mainPart,
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-0.5).sp
                )
                if (decimalPart.isNotEmpty()) {
                    Text(
                        text = decimalPart,
                        color = Color.White.copy(alpha = 0.50f),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Return pills row: [ ↗ +4.73% ] [ +$1,286 - Today ]
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x28FFFFFF))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = todayReturnPct,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x28FFFFFF))
                        .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = todayDollarGain,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons: [Invest] [Add Fund] [Withdraw] sitting directly on the gradient
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Invest
                NexoraActionButton(
                    title = "Invest",
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = onInvestClick,
                    modifier = Modifier.weight(1f)
                )

                // Button 2: Add Fund
                NexoraActionButton(
                    title = "Add Fund",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = onAddFundClick,
                    modifier = Modifier.weight(1f)
                )

                // Button 3: Withdraw
                NexoraActionButton(
                    title = "Withdraw",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = onWithdrawClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NexoraActionButton(
    title: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x2BFFFFFF))
            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

data class HoldingItemData(
    val badge: String,
    val name: String,
    val symbol: String,
    val value: String,
    val returnGain: String
)

/**
 * Replicates the Top Holdings Section with circular badge tokens and gain pills.
 */
@Composable
fun NexoraTopHoldingsSection(
    holdings: List<HoldingItemData>,
    onSeeAllClick: () -> Unit = {},
    onHoldingClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F1A2C))
            .border(1.dp, Color(0xFF1E2D44), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            // Header Row: Top Holdings | See All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Holdings",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "See All",
                    color = Color(0xFF7DD3FC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSeeAllClick() }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Holdings List
            holdings.forEachIndexed { index, holding ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onHoldingClick(holding.symbol) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Badge + Name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E314D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = holding.badge,
                                color = Color(0xFF7DD3FC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = holding.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = holding.symbol,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Right Value + Gain
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = holding.value,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "↗ ${holding.returnGain}",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (index < holdings.size - 1) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * Replicates the Stacked Monthly Inflow/Growth Bar Chart from the bottom of the left phone.
 */
@Composable
fun NexoraStackedMonthlyChart(
    modifier: Modifier = Modifier
) {
    val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN")
    // Pairs of (investedHeightFraction, profitHeightFraction)
    val bars = listOf(
        Pair(0.40f, 0.10f),
        Pair(0.65f, 0.12f),
        Pair(0.45f, 0.08f),
        Pair(0.85f, 0.15f),
        Pair(0.70f, 0.14f),
        Pair(0.35f, 0.09f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF0F1A2C))
            .border(1.dp, Color(0xFF1E2D44), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Performance",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "+$1.5k Max",
                    color = Color(0xFF7DD3FC),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bars row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEachIndexed { i, month ->
                    val (baseFraction, topFraction) = bars[i]

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Stacked Bar Container
                        Column(
                            modifier = Modifier
                                .width(18.dp)
                                .height(95.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            // Top grey/white profit cap
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((95 * topFraction).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFFE2E8F0))
                            )
                            // Bottom blue invested stem
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height((95 * baseFraction).dp)
                                    .background(Color(0xFF38BDF8))
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = month,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
