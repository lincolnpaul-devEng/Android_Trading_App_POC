package com.trading.app.di

import com.trading.app.data.repository.MarketDataRepositoryImpl
import com.trading.app.data.repository.NewsRepositoryImpl
import com.trading.app.data.repository.OrderRepositoryImpl
import com.trading.app.data.repository.PortfolioRepositoryImpl
import com.trading.app.data.repository.WatchlistRepositoryImpl
import com.trading.app.domain.repository.MarketDataRepository
import com.trading.app.domain.repository.NewsRepository
import com.trading.app.domain.repository.OrderRepository
import com.trading.app.domain.repository.PortfolioRepository
import com.trading.app.domain.repository.WatchlistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindMarketDataRepository(impl: MarketDataRepositoryImpl): MarketDataRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindPortfolioRepository(impl: PortfolioRepositoryImpl): PortfolioRepository

    @Binds
    @Singleton
    abstract fun bindWatchlistRepository(impl: WatchlistRepositoryImpl): WatchlistRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository
}
