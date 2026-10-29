package com.trading.app.domain.model

enum class NewsSentiment {
    BULLISH,
    BEARISH,
    NEUTRAL
}

data class MarketNewsEntity(
    val symbol: String,
    val changePercent: Double? = null,
    val sentimentScore: Double? = null
)

data class MarketNewsArticle(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val publishedAt: String,
    val url: String,
    val imageUrl: String? = null,
    val symbol: String? = null,
    val sentimentScore: Double = 0.0,
    val sentiment: NewsSentiment = NewsSentiment.NEUTRAL,
    val entities: List<MarketNewsEntity> = emptyList()
)
