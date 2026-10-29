package com.trading.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trading.app.domain.model.Portfolio
import com.trading.app.presentation.theme.*

@Composable
fun PortfolioSummaryCard(
    portfolio: Portfolio,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(20.dp)
    ) {
        Text("Portfolio Value", style = TradingTypography.label)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = Formatters.formatCurrency(portfolio.totalValue),
            style = TradingTypography.priceDisplay,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))

        PriceChangeIndicator(
            change = portfolio.dayPnL,
            changePercent = if (portfolio.totalValue != 0.0) (portfolio.dayPnL / portfolio.totalValue) * 100 else 0.0
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem("Cash", Formatters.formatCurrency(portfolio.cashBalance))
            SummaryItem("Invested", Formatters.formatCurrency(portfolio.totalCostBasis))
            SummaryItem("Market Value", Formatters.formatCurrency(portfolio.totalMarketValue))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SummaryItem("Unrealized P&L", Formatters.formatCurrency(portfolio.totalUnrealizedPnL),
                if (portfolio.totalUnrealizedPnL >= 0) ProfitGreen else LossRed)
            SummaryItem("Day P&L", Formatters.formatCurrency(portfolio.dayPnL),
                if (portfolio.dayPnL >= 0) ProfitGreen else LossRed)
            SummaryItem("Buying Power", Formatters.formatCurrency(portfolio.buyingPower))
        }

        if (portfolio.totalUsedMargin > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem("Used Margin", Formatters.formatCurrency(portfolio.totalUsedMargin), ChartPurple)
                SummaryItem("Free Margin", Formatters.formatCurrency(portfolio.freeMargin), PrimaryBlue)
                SummaryItem("Margin Level", "${String.format("%.1f", portfolio.marginLevel)}%")
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = TextPrimary
) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, fontSize = 10.sp, color = TextTertiary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
