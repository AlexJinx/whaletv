package com.jing.whaletv.ui

import com.jing.whaletv.data.model.SyncSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SettingsDisplayModelTest {
    @Test
    fun effectiveEpgUrl_usesDiscoveredUrlOnly() {
        val discovered = SyncSummary(discoveredEpgUrl = "https://example.com/discovered.xml")

        assertEquals(
            "https://example.com/discovered.xml",
            effectiveEpgUrl(discovered),
        )
        assertNull(effectiveEpgUrl(SyncSummary()))
        assertNull(effectiveEpgUrl(SyncSummary(discoveredEpgUrl = "")))
    }
}
