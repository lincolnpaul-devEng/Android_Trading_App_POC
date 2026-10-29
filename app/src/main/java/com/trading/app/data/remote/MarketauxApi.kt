package com.trading.app.data.remote

import com.trading.app.data.remote.model.MarketauxNewsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface MarketauxApi {

    @GET("v1/news/all")
    suspend fun getNews(
        @Query("api_token") apiToken: String,
        @Query("symbols") symbols: String? = null,
        @Query("filter_entities") filterEntities: Boolean = true,
        @Query("language") language: String = "en",
        @Query("limit") limit: Int = 15
    ): MarketauxNewsResponse
}
