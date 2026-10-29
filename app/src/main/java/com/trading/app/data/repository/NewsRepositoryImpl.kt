package com.trading.app.data.repository

import android.util.Log
import com.trading.app.BuildConfig
import com.trading.app.data.remote.MarketauxApi
import com.trading.app.data.remote.model.MarketauxArticleDto
import com.trading.app.domain.model.MarketNewsArticle
import com.trading.app.domain.model.MarketNewsEntity
import com.trading.app.domain.model.NewsSentiment
import com.trading.app.domain.repository.NewsRepository
import com.trading.app.notification.NotificationHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepositoryImpl @Inject constructor(
    private val marketauxApi: MarketauxApi,
    private val notificationHelper: NotificationHelper
) : NewsRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val cachedNewsFlow = MutableStateFlow<List<MarketNewsArticle>>(emptyList())
    private val notifiedArticleIds = ConcurrentHashMap.newKeySet<String>()

    override fun getNewsStream(symbols: List<String>): Flow<List<MarketNewsArticle>> {
        return cachedNewsFlow.asStateFlow()
    }

    override suspend fun refreshNews(symbols: List<String>): List<MarketNewsArticle> = withContext(ioDispatcher) {
        val apiKey = BuildConfig.MARKETAUX_API_KEY.trim()
        val symbolsParam = if (symbols.isNotEmpty()) symbols.take(6).joinToString(",") else "AAPL,TSLA,MSFT,BTC,ETH,NVDA"

        val liveArticles = if (apiKey.isNotBlank()) {
            try {
                val response = marketauxApi.getNews(
                    apiToken = apiKey,
                    symbols = symbolsParam,
                    filterEntities = true,
                    limit = 15
                )
                response.data?.map { it.toDomain() } ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Marketaux API call failed: ${e.message}")
                emptyList()
            }
        } else {
            Log.w(TAG, "Marketaux API key is not configured in secret.properties. No news loaded.")
            emptyList()
        }

        if (liveArticles.isNotEmpty()) {
            cachedNewsFlow.value = liveArticles
        }
        liveArticles
    }

    override suspend fun pollBreakingNewsAndNotify(watchlistSymbols: List<String>) = withContext(ioDispatcher) {
        val articles = refreshNews(watchlistSymbols)
        val upperWatchlist = watchlistSymbols.map { it.uppercase().trim() }.toSet()

        for (article in articles) {
            val symbolMatch = article.symbol?.uppercase()?.trim()
            val matchesWatchlist = symbolMatch != null && upperWatchlist.contains(symbolMatch)
            val isHighImpact = Math.abs(article.sentimentScore) >= 0.25

            if ((matchesWatchlist || isHighImpact) && !notifiedArticleIds.contains(article.id)) {
                notifiedArticleIds.add(article.id)
                notificationHelper.showNewsAlert(
                    title = article.title,
                    source = article.source,
                    sentimentScore = article.sentimentScore,
                    symbol = article.symbol,
                    articleUrl = article.url
                )
                // Limit to 1 alert per poll cycle to avoid flooding
                break
            }
        }
    }

    private fun MarketauxArticleDto.toDomain(): MarketNewsArticle {
        val matchedEntity = entities?.firstOrNull()
        val score = matchedEntity?.sentimentScore ?: sentiment?.score ?: 0.0
        val sentimentEnum = when {
            score > 0.15 -> NewsSentiment.BULLISH
            score < -0.15 -> NewsSentiment.BEARISH
            else -> NewsSentiment.NEUTRAL
        }

        val mappedEntities = entities?.map {
            MarketNewsEntity(
                symbol = it.symbol.substringBefore('.'),
                sentimentScore = it.sentimentScore,
                changePercent = it.sentimentScore?.let { s -> s * 2.2 }
            )
        }?.distinctBy { it.symbol } ?: listOfNotNull(
            matchedEntity?.let {
                MarketNewsEntity(
                    symbol = it.symbol.substringBefore('.'),
                    sentimentScore = score,
                    changePercent = score * 2.2
                )
            }
        )

        val cleanSource = formatSource(source)
        val relativeTime = formatRelativeTime(publishedAt)

        return MarketNewsArticle(
            id = uuid,
            title = title,
            summary = description ?: snippet ?: "",
            source = cleanSource,
            publishedAt = relativeTime,
            url = url,
            imageUrl = imageUrl,
            symbol = matchedEntity?.symbol?.substringBefore('.'),
            sentimentScore = score,
            sentiment = sentimentEnum,
            entities = mappedEntities
        )
    }

    private fun formatSource(rawSource: String?): String {
        if (rawSource.isNullOrBlank()) return "Financial News"
        val domain = rawSource.removePrefix("www.").substringBefore('/')
        return when (domain.lowercase()) {
            "businessinsider.com" -> "Business Insider"
            "seekingalpha.com" -> "Seeking Alpha"
            "theblock.co" -> "The Block"
            "bloomberg.com" -> "Bloomberg"
            "reuters.com" -> "Reuters"
            "cnbc.com" -> "CNBC"
            "coindesk.com" -> "CoinDesk"
            "marketwatch.com" -> "MarketWatch"
            "wsj.com" -> "Wall Street Journal"
            "ft.com" -> "Financial Times"
            else -> domain.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun formatRelativeTime(isoTimestamp: String?): String {
        if (isoTimestamp.isNullOrBlank()) return "1h ago"
        return try {
            val instant = Instant.parse(isoTimestamp)
            val diffSec = Duration.between(instant, Instant.now()).seconds
            when {
                diffSec < 60 -> "Just now"
                diffSec < 3600 -> "${diffSec / 60}m ago"
                diffSec < 86400 -> "${diffSec / 3600}h ago"
                else -> "${diffSec / 86400}d ago"
            }
        } catch (_: Exception) {
            "1h ago"
        }
    }

    companion object {
        private const val TAG = "NewsRepository"
    }
}
