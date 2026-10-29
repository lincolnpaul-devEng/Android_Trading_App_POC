package com.trading.app.data

import com.trading.app.domain.model.AccountType
import org.junit.Assert.*
import org.junit.Test

class DataSourceConfigTest {

    @Test
    fun `default mode is HYBRID`() {
        assertEquals(DataSourceMode.HYBRID, DataSourceConfig.mode)
    }

    @Test
    fun `default accountType is DEMO`() {
        assertEquals(AccountType.DEMO, DataSourceConfig.accountType)
    }

    @Test
    fun `mode can be changed to SIMULATED and LIVE`() {
        val original = DataSourceConfig.mode
        DataSourceConfig.mode = DataSourceMode.SIMULATED
        assertEquals(DataSourceMode.SIMULATED, DataSourceConfig.mode)
        DataSourceConfig.mode = DataSourceMode.LIVE
        assertEquals(DataSourceMode.LIVE, DataSourceConfig.mode)
        DataSourceConfig.mode = original
    }

    @Test
    fun `all modes are defined`() {
        val modes = DataSourceMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(DataSourceMode.SIMULATED))
        assertTrue(modes.contains(DataSourceMode.LIVE))
        assertTrue(modes.contains(DataSourceMode.HYBRID))
    }
}
