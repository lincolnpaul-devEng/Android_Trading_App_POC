# Android Trading App POC (Cash Cage)

A high-performance Android trading terminal POC demonstrating modern mobile architecture for active trading workflows: real-time streaming market data, order entry, institutional paper trading, multi-asset margin accounting, push notifications, and live financial news.

## System Architecture: Simulation vs. Live Feeds

It is important to distinguish market data context from order execution in this application:

1. **Market Data (Network-Backed & Live)**:
   - **Finnhub REST & WebSocket**: Streams real-time market quotes, order book depth (L2), bid/ask spreads, and trade prints.
   - **Marketaux News API**: Live financial news and headlines with thumbnail previews, sentiment scores, and asset tags.
   - **Data Modes**:
     - `HYBRID` (Default): Uses live Finnhub WebSocket/REST quotes and Marketaux news, with automatic fallback to multi-asset simulators for non-equity synthetic pairs.
     - `LIVE`: Requires active Finnhub API key and internet connectivity.
     - `SIMULATED`: Fully isolated offline random-walk simulation engine.

2. **Order Execution & Portfolio (Institutional Paper Trading Simulator)**:
   - **100% Virtual Capital**: All order operations (`placeOrder`, `cancelOrder`, `closePosition`, `reversePosition`) route to `MarketSimulator`.
   - **No Real-Money Brokerage Execution**: The app does not transmit orders to external brokerages (e.g., Interactive Brokers, Alpaca).
   - **Account Tiers**:
     - `Demo Practice`: $100,000 virtual starter balance, full resetability.
     - `Paper Live Sandbox`: Paper trading executing against live Finnhub bid/ask pricing and simulated institutional liquidity.

3. **Terminal Demo Lockout**:
   - The PIN/Biometric lock (`1234` / `0000`) is a **simulated workstation lock** representing institutional terminal privacy features. It is explicitly labeled as a demo screen lock, not production banking credentials.

---

## Verified Simulator Accounting Behavior

The simulator's order matching and portfolio accounting have been audited and verified via automated unit and regression test suites:

- **Position Reversals (Excess Quantity Netting)**:
  - If a user holds 10 `SHORT` NVDA and submits a `BUY` order for 15 NVDA:
    1. The 10 `SHORT` units are closed at fill price, realizing trade P&L and returning locked margin to cash.
    2. A `ClosedTrade` and `TransactionType.REALIZED_PNL` record are created.
    3. The excess 5 units immediately open a new `LONG` position at the fill price, with required margin deducted.
    4. Exactly one parent `FILLED` order is recorded in order history.
- **Partial Position Closes**:
  - Selling fewer shares than held reduces the position quantity while keeping average entry cost intact.
  - Locked margin is returned proportionally, and realized P&L is credited/debited.
- **Margin & Buying Power Constraints**:
  - Closing orders (partial or full) never fail due to cash buying power shortages.
  - Excess margin is computed strictly on net opening quantities.
  - Multi-asset contract multipliers (e.g., S&P 500 E-mini `ESZ6` 50x, Commodities `XAU/USD` 100x) are factored into margin calculations and leverage scaling.
- **State Persistence Across Restarts**:
  - Full trading state—cash balance, positions, active working orders, order history, closed trades, transaction ledger, equity curve history, and price alerts—is serialized to JSON (`exportStateJson` / `restoreStateJson`) and saved to SharedPreferences. Verified to restore identically across process restarts.

---

## Core Implemented Features

- **Dashboard**: Market indices, translucent sector heatmap, hero video banner, top movers, and quick portfolio equity bar.
- **Watchlist**: Tracked symbols across Equities, Crypto, Forex, Commodities, and Futures with real-time flashing prices and sparkline charts.
- **Stock Detail & Charting**: Candlestick chart, interactive Level 2 Order Book, Bid/Ask depth chart, and instrument specifications.
- **Order Entry**: Market, Limit, Stop, Stop-Limit, and Trailing-Stop orders with leverage selector (1x to 50x), Take-Profit (TP), Stop-Loss (SL), and contract sizing.
- **Portfolio & Ledger**: Realized and unrealized P&L, margin utilization meter, closed trade history, downloadable account statements, and terminal demo lockout.
- **Live Marketaux News**: Real-time news feed (zero mock data), YouTube-style shimmer skeleton loading, Rive-powered empty/offline state animation (`orange_unsuccess_screen.riv`), and interactive modal drawer reader with linked asset tickers.
- **Firebase Cloud Messaging (FCM)**: Native Android notification channels for Risk Liquidation Alerts, Order Execution, Price Alerts, Trade Performance Tracking, and Breaking Market News.

---

## Planned Educational Roadmap

The verified paper-trading engine will serve as the execution backend for planned Brilliant-style interactive educational trading modules:
- *Interactive Order Mechanics*: Visual lessons on Bid/Ask spread dynamics, market impact, and limit order books.
- *Margin & Leverage Simulator*: Gamified lessons demonstrating liquidation risk, leverage decay, and maintenance margin.
- *Options & Multi-Leg Strategies*: Hands-on sandbox for spreads, straddles, and risk graphs.

---

## Tech Stack

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Clean Architecture + Repository Pattern
- **Async & Reactive**: Kotlin Coroutines + StateFlow / SharedFlow
- **Dependency Injection**: Dagger Hilt
- **Network & Realtime**: Retrofit 2 + OkHttp + WebSocket
- **Animation**: Rive Android Runtime (`.riv`) + Compose Animation
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Image Loading**: Coil Compose
- **Testing**: JUnit 4, Cash App Turbine, Coroutines Test

---

## Setup & Running

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 17
- Android SDK 34+
- `google-services.json` placed in `/app` (included for package `com.trading.app`)

### Build & Test Commands
```bash
# Run all unit tests (including simulator accounting & persistence regression suites)
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```
