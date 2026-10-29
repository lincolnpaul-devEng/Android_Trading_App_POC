package com.trading.app.data

import com.trading.app.domain.model.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controls whether the app uses live Finnhub data or the local simulator,
 * as well as whether the user is trading on a Demo Account or Live Account.
 */
enum class DataSourceMode {
    /** Use only the local market simulator — no network calls */
    SIMULATED,
    /** Use Finnhub API & WebSocket for market data — requires valid API key and network */
    LIVE,
    /** Stream live Finnhub WebSocket + REST and seamlessly fall back / bridge with simulator */
    HYBRID
}

/**
 * Global runtime data source and account mode configuration.
 * Defaults to HYBRID (live Finnhub market data + multi-asset fallback) and DEMO trading account.
 */
object DataSourceConfig {
    private val _modeFlow = MutableStateFlow(DataSourceMode.HYBRID)
    val modeFlow: StateFlow<DataSourceMode> = _modeFlow.asStateFlow()

    var mode: DataSourceMode
        get() = _modeFlow.value
        set(value) {
            _modeFlow.value = value
        }

    private val _accountTypeFlow = MutableStateFlow(AccountType.DEMO)
    val accountTypeFlow: StateFlow<AccountType> = _accountTypeFlow.asStateFlow()

    var accountType: AccountType
        get() = _accountTypeFlow.value
        set(value) {
            _accountTypeFlow.value = value
        }
}
