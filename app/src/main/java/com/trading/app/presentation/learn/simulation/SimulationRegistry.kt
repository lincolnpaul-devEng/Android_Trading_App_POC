package com.trading.app.presentation.learn.simulation

import androidx.compose.runtime.Composable
import com.trading.app.engine.simulations.MarketMechanicsSimulation
import com.trading.app.engine.simulations.PortfolioManagementSimulation
import com.trading.app.engine.simulations.RiskManagementSimulation
import com.trading.app.presentation.learn.flowbuilder.MoneyFlowBuilderScreen

typealias SimulationComposable = @Composable (
    configJson: String?,
    onComplete: (earnedXp: Int) -> Unit,
    onBack: () -> Unit
) -> Unit

object SimulationRegistry {

    private val simulations = mutableMapOf<String, SimulationComposable>()
    private var isInitialized = false

    fun initDefaults() {
        if (isInitialized) return
        isInitialized = true

        // 1. Money Flow Builder POC simulation
        register("money_flow_builder") { _, onComplete, onBack ->
            MoneyFlowBuilderScreen(
                onExit = onBack,
                onComplete = { earnedXp -> onComplete(earnedXp) }
            )
        }

        // 2. Risk Management Simulation with Drag-and-Drop chart
        register("risk_management") { configJson, onComplete, onBack ->
            RiskManagementSimulation(
                configJson = configJson,
                onComplete = onComplete,
                onBack = onBack
            )
        }

        // 3. Portfolio Management Simulation
        register("portfolio_management") { configJson, onComplete, onBack ->
            PortfolioManagementSimulation(
                configJson = configJson,
                onComplete = onComplete,
                onBack = onBack
            )
        }

        // 4. Market Mechanics Simulation
        register("market_mechanics") { configJson, onComplete, onBack ->
            MarketMechanicsSimulation(
                configJson = configJson,
                onComplete = onComplete,
                onBack = onBack
            )
        }
    }

    fun register(type: String, composable: SimulationComposable) {
        simulations[type] = composable
    }

    fun get(type: String): SimulationComposable? {
        if (!isInitialized) {
            initDefaults()
        }
        return simulations[type]
    }

    fun has(type: String): Boolean {
        if (!isInitialized) {
            initDefaults()
        }
        return simulations.containsKey(type)
    }

    fun registeredTypes(): Set<String> {
        if (!isInitialized) {
            initDefaults()
        }
        return simulations.keys
    }
}
