package com.trading.app.data.remote.quiz

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRemoteDataSource @Inject constructor() {

    companion object {
        const val CURRENT_SERVER_VERSION = 3
        const val CURRENT_SERVER_ETAG = "financial_sim_manifest_2026_v3"
    }

    suspend fun fetchQuizManifest(clientEtag: String?): Result<QuizServerManifestResponse?> {
        return runCatching {
            delay(250) // network latency simulation

            if (clientEtag == CURRENT_SERVER_ETAG) {
                return@runCatching null
            }

            QuizServerManifestResponse(
                version = CURRENT_SERVER_VERSION,
                etag = CURRENT_SERVER_ETAG,
                publishedAt = 1793305000000L,
                items = listOf(
                    // 1. Interactive Risk Management Simulator (Drag-and-Drop)
                    QuizQuestionDto(
                        id = "risk_management_drag_drop",
                        lessonId = "risk_management",
                        title = "Risk Management: Interactive Stop Placement",
                        prompt = "Drag the Stop-Loss (red) and Take-Profit (green) lines directly on the candlestick chart to establish an optimal Risk/Reward ratio (> 1:1.8) while capping potential dollar risk under 2% of capital.",
                        type = "SIMULATION",
                        version = 3,
                        minXp = 25,
                        maxXp = 50,
                        publishedAt = 1793305000000L,
                        explanation = "Strict risk-to-reward architecture guarantees positive mathematical expectancy. When gains outsize losses 2:1, a trader remains profitable even with a 40% win rate.",
                        aiExplanationHint = "Formula: Risk/Reward = (TakeProfit - Entry) / (Entry - StopLoss). Never risk more than 1-2% of total portfolio balance on any single setup.",
                        options = null,
                        correctAnswer = null,
                        tolerance = null,
                        unit = "USD",
                        predictionConfig = null,
                        simulationType = "risk_management",
                        simulationConfigJson = "{\"max_risk_pct\":2.0,\"min_rr\":1.8}"
                    ),
                    // 2. Portfolio Asset Allocation & Shock Simulator
                    QuizQuestionDto(
                        id = "portfolio_management_shock",
                        lessonId = "portfolio_management",
                        title = "Portfolio Engine: Asset Allocation & Shocks",
                        prompt = "Adjust the allocation between Equities, Bonds, and Cash. Then trigger a -20% market crash shock to observe how diversification absorbs drawdown.",
                        type = "SIMULATION",
                        version = 3,
                        minXp = 20,
                        maxXp = 45,
                        publishedAt = 1793304000000L,
                        explanation = "Diversifying across low-correlation asset classes dampens volatility. Flight-to-safety bond rallies and cash stability offset equity crashes.",
                        aiExplanationHint = "Herfindahl-Hirschman Index (HHI) measures concentration risk. Portfolios with HHI < 4000 survive systemic equity shocks with minimal permanent impairment.",
                        options = null,
                        correctAnswer = null,
                        tolerance = null,
                        unit = "%",
                        predictionConfig = null,
                        simulationType = "portfolio_management",
                        simulationConfigJson = "{\"equity_shock\":-20.0,\"bond_shock\":5.0}"
                    ),
                    // 3. Market Mechanics & Order Book Depth Simulator
                    QuizQuestionDto(
                        id = "market_mechanics_orderbook",
                        lessonId = "market_mechanics",
                        title = "Market Mechanics: Order Book Depth & Spread",
                        prompt = "Inspect Level 2 order book bids and asks. Execute Market Orders vs. Limit Orders to experience how taking immediate liquidity crosses the spread.",
                        type = "SIMULATION",
                        version = 3,
                        minXp = 20,
                        maxXp = 40,
                        publishedAt = 1793303000000L,
                        explanation = "Market orders pay the bid-ask spread to take liquidity immediately, whereas limit orders provide liquidity to the book and capture favorable pricing.",
                        aiExplanationHint = "Bid-Ask Spread represents the compensation required by market makers for bearing inventory risk during price discovery.",
                        options = null,
                        correctAnswer = null,
                        tolerance = null,
                        unit = "USD",
                        predictionConfig = null,
                        simulationType = "market_mechanics",
                        simulationConfigJson = "{\"spread_bps\":10.0}"
                    ),
                    // 4. Money Flow Builder POC Simulation
                    QuizQuestionDto(
                        id = "money_flow_builder_poc",
                        lessonId = "weekly_spotlight",
                        title = "Money Flow Builder: Capital & Liquidity Shock",
                        prompt = "Construct your financial cashflow pipeline. Allocate salary between Living, Investing, Debt, and Savings, then withstand a surprise economic drawdown shock.",
                        type = "SIMULATION",
                        version = 3,
                        minXp = 50,
                        maxXp = 150,
                        publishedAt = 1793302000000L,
                        explanation = "Sustainable capital allocation requires maintaining positive net monthly delta and building emergency reserves before scaling volatile equity allocations.",
                        aiExplanationHint = "Sankey flow ribbon dynamics demonstrate where every dollar travels and visually exposes liquidity bottlenecks before risk events strike.",
                        options = null,
                        correctAnswer = null,
                        tolerance = null,
                        unit = "USD",
                        predictionConfig = null,
                        simulationType = "money_flow_builder",
                        simulationConfigJson = "{\"default_income\":5000,\"shock_penalty\":-1200}"
                    ),
                    // 5. Prediction & Experimentation Challenge
                    QuizQuestionDto(
                        id = "margin_call_prediction",
                        lessonId = "risk_management",
                        title = "Leverage & Margin Call Liquidation",
                        prompt = "Predict the adverse price move percentage that wipes out 100% of your equity under 10x leverage, then test it on the interactive slider.",
                        type = "PREDICTION_EXPERIMENT",
                        version = 3,
                        minXp = 15,
                        maxXp = 35,
                        publishedAt = 1793301000000L,
                        explanation = "With 10x leverage, initial margin is 10% (1/10). A 10% adverse drop in asset price produces a 100% loss of initial margin, triggering forced liquidation.",
                        aiExplanationHint = "Leverage multiplies both upside gains and downside risk. Formula: Liquidation Drop % = 100 / Leverage.",
                        options = null,
                        correctAnswer = "10.0",
                        tolerance = 1.0,
                        unit = "%",
                        predictionConfig = PredictionConfigDto(
                            variableName = "Leverage Factor",
                            minVal = 1f,
                            maxVal = 20f,
                            step = 1f,
                            defaultVal = 5f,
                            unit = "x",
                            targetMin = 9.5f,
                            targetMax = 10.5f,
                            predictionPrompt = "What % drop in underlying price wipes out your account at 10x leverage?",
                            experimentPrompt = "Scrub the leverage slider to 10x to simulate market drop against equity.",
                            revealInsight = "Notice how equity hits $0.00 exactly at a 10% drop when leverage = 10x! Formula: Liquidation Drop % = 100 / Leverage."
                        ),
                        simulationType = null,
                        simulationConfigJson = null
                    ),
                    // 6. Numerical Calculation Challenge
                    QuizQuestionDto(
                        id = "position_sizing_numerical",
                        lessonId = "risk_management",
                        title = "Position Sizing with 1% Risk Rule",
                        prompt = "Portfolio balance: $25,000. Entry price: $50.00. Stop-loss: $48.00 ($2.00 risk per share). Under the strict 1% risk rule ($250 max dollar loss), how many shares can you buy?",
                        type = "NUMERICAL",
                        version = 3,
                        minXp = 10,
                        maxXp = 30,
                        publishedAt = 1793300000000L,
                        explanation = "Max Dollar Risk = 1% * $25,000 = $250. Risk per share = $50.00 - $48.00 = $2.00. Number of shares = $250 / $2.00 = 125 shares.",
                        aiExplanationHint = "Formula: Position Size (shares) = (Account Capital * Risk%) / (Entry Price - Stop Loss Price). This keeps risk strictly constant regardless of volatility.",
                        options = null,
                        correctAnswer = "125",
                        tolerance = 0.5,
                        unit = "shares",
                        predictionConfig = null,
                        simulationType = null,
                        simulationConfigJson = null
                    )
                )
            )
        }
    }
}
