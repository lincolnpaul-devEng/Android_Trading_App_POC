package com.trading.app.domain.repository

import com.trading.app.domain.model.AccountStatement
import com.trading.app.domain.model.AlertCondition
import com.trading.app.domain.model.EquityPoint
import com.trading.app.domain.model.Portfolio
import com.trading.app.domain.model.Position
import com.trading.app.domain.model.PriceAlert
import com.trading.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface PortfolioRepository {
    fun getPortfolio(): Flow<Portfolio>
    fun getPositions(): Flow<List<Position>>
    fun getPosition(symbol: String): Flow<Position?>
    fun getTransactions(): Flow<List<Transaction>>
    fun getAccountStatement(): Flow<AccountStatement>
    fun getEquityHistory(): Flow<List<EquityPoint>>
    fun getPriceAlerts(): Flow<List<PriceAlert>>
    suspend fun addPriceAlert(symbol: String, targetPrice: Double, condition: AlertCondition): PriceAlert
    suspend fun removePriceAlert(alertId: String)
    suspend fun depositFunds(amount: Double): Portfolio
    suspend fun withdrawFunds(amount: Double): Result<Portfolio>
    suspend fun resetDemoAccount(startingBalance: Double = 100_000.0): Portfolio
}
