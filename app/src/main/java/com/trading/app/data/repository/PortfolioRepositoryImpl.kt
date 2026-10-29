package com.trading.app.data.repository

import com.trading.app.data.simulator.MarketSimulator
import com.trading.app.domain.model.AccountStatement
import com.trading.app.domain.model.AlertCondition
import com.trading.app.domain.model.EquityPoint
import com.trading.app.domain.model.Portfolio
import com.trading.app.domain.model.Position
import com.trading.app.domain.model.PriceAlert
import com.trading.app.domain.model.Transaction
import com.trading.app.domain.repository.PortfolioRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PortfolioRepositoryImpl @Inject constructor(
    private val simulator: MarketSimulator
) : PortfolioRepository {

    override fun getPortfolio(): Flow<Portfolio> = flow {
        while (true) {
            emit(simulator.getPortfolio())
            delay(1000)
        }
    }

    override fun getPositions(): Flow<List<Position>> = flow {
        while (true) {
            emit(simulator.getPositions())
            delay(1000)
        }
    }

    override fun getPosition(symbol: String): Flow<Position?> = flow {
        while (true) {
            emit(simulator.getPositions().find { it.symbol == symbol })
            delay(1000)
        }
    }

    override fun getTransactions(): Flow<List<Transaction>> = flow {
        while (true) {
            emit(simulator.getTransactions())
            delay(1000)
        }
    }

    override fun getAccountStatement(): Flow<AccountStatement> = flow {
        while (true) {
            emit(simulator.getAccountStatement())
            delay(1000)
        }
    }

    override fun getEquityHistory(): Flow<List<EquityPoint>> = flow {
        while (true) {
            emit(simulator.getEquityHistory())
            delay(1000)
        }
    }

    override fun getPriceAlerts(): Flow<List<PriceAlert>> = flow {
        while (true) {
            emit(simulator.getPriceAlerts())
            delay(1000)
        }
    }

    override suspend fun addPriceAlert(symbol: String, targetPrice: Double, condition: AlertCondition): PriceAlert =
        simulator.addPriceAlert(symbol, targetPrice, condition)

    override suspend fun removePriceAlert(alertId: String) =
        simulator.removePriceAlert(alertId)

    override suspend fun depositFunds(amount: Double): Portfolio = simulator.depositFunds(amount)

    override suspend fun withdrawFunds(amount: Double): Result<Portfolio> = simulator.withdrawFunds(amount)

    override suspend fun resetDemoAccount(startingBalance: Double): Portfolio =
        simulator.resetDemoAccount(startingBalance)
}
