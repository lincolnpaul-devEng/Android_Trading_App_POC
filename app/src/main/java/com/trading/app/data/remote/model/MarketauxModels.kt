package com.trading.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class MarketauxNewsResponse(
    @SerializedName("meta") val meta: MarketauxMeta?,
    @SerializedName("data") val data: List<MarketauxArticleDto>?
)

data class MarketauxMeta(
    @SerializedName("found") val found: Int?,
    @SerializedName("returned") val returned: Int?,
    @SerializedName("limit") val limit: Int?,
    @SerializedName("page") val page: Int?
)

data class MarketauxArticleDto(
    @SerializedName("uuid") val uuid: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("snippet") val snippet: String?,
    @SerializedName("url") val url: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("published_at") val publishedAt: String?,
    @SerializedName("source") val source: String?,
    @SerializedName("entities") val entities: List<MarketauxEntityDto>?,
    @SerializedName("sentiment") val sentiment: MarketauxSentimentDto?
)

data class MarketauxEntityDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String?,
    @SerializedName("sentiment_score") val sentimentScore: Double?
)

data class MarketauxSentimentDto(
    @SerializedName("score") val score: Double?
)
