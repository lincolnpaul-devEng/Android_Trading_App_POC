package com.trading.app.domain.repository

import com.trading.app.domain.model.*
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    suspend fun placeOrder(order: Order): Result<Order>
    suspend fun cancelOrder(orderId: String): Result<Order>
    suspend fun closePosition(symbol: String, quantity: Int? = null): Result<Order>
    suspend fun reversePosition(symbol: String): Result<Order>
    suspend fun updatePositionSlTp(symbol: String, takeProfit: Double?, stopLoss: Double?): Result<Position>
    fun getOpenOrders(): Flow<List<Order>>
    fun getOrderHistory(): Flow<List<Order>>
    fun getClosedTrades(): Flow<List<ClosedTrade>>
    fun getOrderById(orderId: String): Flow<Order?>
}
