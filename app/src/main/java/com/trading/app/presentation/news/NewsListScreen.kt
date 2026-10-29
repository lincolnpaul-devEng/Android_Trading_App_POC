package com.trading.app.presentation.news

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.trading.app.domain.model.MarketNewsArticle
import com.trading.app.presentation.components.NewsDetailBottomSheet
import com.trading.app.presentation.components.NewsEmptyStateView
import com.trading.app.presentation.components.NewsShimmerList
import com.trading.app.presentation.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    onBack: () -> Unit,
    onStockClick: (String) -> Unit,
    viewModel: NewsListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Market News & Intelligence",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshNews() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PrimaryBlue
                        )
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
                .background(DarkBackground)
        ) {
            // Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search tickers, companies, topics...", color = Color(0xFF6E7681), fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF8B949E))
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF8B949E))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NewsFilterCategory.entries.forEach { filter ->
                    val isSelected = state.selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectFilter(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryBlue,
                            containerColor = DarkSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = if (isSelected) PrimaryBlue else DarkBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Article List / Shimmer Loading / Empty State
            if (state.isLoading) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        NewsShimmerList(count = 5)
                    }
                }
            } else if (state.loadError != null || state.filteredArticles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NewsEmptyStateView(
                        message = state.loadError ?: "Error loading news or no internet connection",
                        onReload = { viewModel.refreshNews() }
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.filteredArticles, key = { it.id }) { article ->
                        NewsRowItem(
                            article = article,
                            onClick = { viewModel.selectArticle(article) }
                        )
                        HorizontalDivider(
                            color = DarkBorder.copy(alpha = 0.5f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    // News Detail BottomSheet
    state.selectedArticle?.let { article ->
        NewsDetailBottomSheet(
            article = article,
            relatedQuotes = state.relatedQuotes,
            onDismiss = { viewModel.dismissArticle() },
            onStockClick = { symbol ->
                viewModel.dismissArticle()
                onStockClick(symbol)
            }
        )
    }
}

@Composable
private fun NewsRowItem(
    article: MarketNewsArticle,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Line 1: Source • Time (e.g., Business Insider • 1h ago)
        Text(
            text = "${article.source} • ${article.publishedAt}",
            color = Color(0xFF8B949E),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )

        // Middle Row: Text on Left, Preview Image on Right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Title (Bold, maxLines = 2)
                Text(
                    text = article.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Subtitle snippet (Subtle gray, maxLines = 2)
                if (article.summary.isNotBlank()) {
                    Text(
                        text = article.summary,
                        color = Color(0xFF8B949E),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Bottom Ticker Chips Row: DIS -0.63% | MSFT +1.01% | OPEA —
                if (article.entities.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        article.entities.take(4).forEachIndexed { index, entity ->
                            if (index > 0) {
                                Text(
                                    text = "|",
                                    color = Color(0xFF484F58),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                            val changeColor = when {
                                entity.changePercent != null && entity.changePercent > 0 -> ProfitGreen
                                entity.changePercent != null && entity.changePercent < 0 -> LossRed
                                else -> Color(0xFF8B949E)
                            }
                            val changeText = when {
                                entity.changePercent != null && entity.changePercent > 0 -> "+${String.format(Locale.US, "%.2f%%", entity.changePercent)}"
                                entity.changePercent != null && entity.changePercent < 0 -> "${String.format(Locale.US, "%.2f%%", entity.changePercent)}"
                                else -> "—"
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = entity.symbol,
                                    color = if (entity.changePercent != null && entity.changePercent < 0) LossRed else changeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = changeText,
                                    color = changeColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            // Right Thumbnail Preview Image
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF21262D)),
                contentAlignment = Alignment.Center
            ) {
                if (!article.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = article.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = PrimaryBlue.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
