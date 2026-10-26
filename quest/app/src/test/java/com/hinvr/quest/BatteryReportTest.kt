package com.hinvr.quest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryReportTest {
    @Test
    fun firstReadingIsSent() {
        assertTrue(BatteryReport.worthSending(80, false, null, null, 0L))
    }

    @Test
    fun smallDriftIsNot() {
        assertFalse(BatteryReport.worthSending(77, false, 80, false, 60_000L))
    }

    @Test
    fun fivePointsOrChargingOrTenMinutesIs() {
        assertTrue(BatteryReport.worthSending(75, false, 80, false, 60_000L))
        assertTrue(BatteryReport.worthSending(80, true, 80, false, 1_000L))
        assertTrue(BatteryReport.worthSending(80, false, 80, false, BatteryReport.EveryMs))
    }
}
