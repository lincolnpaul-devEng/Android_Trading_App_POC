package com.trading.app.di

import com.trading.app.BuildConfig
import com.trading.app.data.remote.FinnhubApi
import com.trading.app.data.remote.FinnhubWebSocket
import com.trading.app.data.remote.HistoricalDataClient
import com.trading.app.data.remote.MarketauxApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val FINNHUB_BASE_URL = "https://finnhub.io/api/v1/"
    private const val MARKETAUX_BASE_URL = "https://api.marketaux.com/"

    @Provides
    @Singleton
    @Named("finnhubApiKey")
    fun provideFinnhubApiKey(): String = BuildConfig.FINNHUB_API_KEY.ifBlank { "demo" }

    @Provides
    @Singleton
    fun provideAuthInterceptor(@Named("finnhubApiKey") apiKey: String): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val url = original.url.newBuilder()
                .addQueryParameter("token", apiKey)
                .build()
            chain.proceed(original.newBuilder().url(url).build())
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: Interceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideHistoricalDataClient(): HistoricalDataClient {
        val plainClient = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
        return HistoricalDataClient(plainClient)
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(FINNHUB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideFinnhubApi(retrofit: Retrofit): FinnhubApi {
        return retrofit.create(FinnhubApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFinnhubWebSocket(
        okHttpClient: OkHttpClient,
        @Named("finnhubApiKey") apiKey: String
    ): FinnhubWebSocket {
        return FinnhubWebSocket(okHttpClient, apiKey)
    }

    @Provides
    @Singleton
    fun provideMarketauxApi(): MarketauxApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(MARKETAUX_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MarketauxApi::class.java)
    }
}
