package com.energy.app.data.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GPS quality gate tests (APP_SPEC §35): accuracy, impossible jumps,
 * spikes, duplicates, null island.
 */
class GpsFilterTest {

    private fun filter() = GpsFilter(
        maxAccuracyMeters = 100.0,
        maxSpeedKmh = 90.0,
        minDistanceMeters = 2.0,
        minTimeMillis = 1_500L
    )

    @Test
    fun `first fix is always accepted`() {
        val f = filter()
        assertTrue(f.accept(35.0, 139.0, 1_000L, accuracyMeters = 5.0))
    }

    @Test
    fun `rejects null island glitch`() {
        val f = filter()
        assertTrue(f.accept(35.0, 139.0, 1_000L, 5.0))
        assertFalse(f.accept(0.0, 0.0, 3_000L, 5.0))
    }

    @Test
    fun `rejects out-of-world coordinates`() {
        val f = filter()
        assertFalse(f.accept(91.0, 139.0, 1_000L, null))
        assertFalse(f.accept(35.0, 181.0, 1_000L, null))
        assertFalse(f.accept(Double.NaN, 139.0, 1_000L, null))
    }

    @Test
    fun `rejects fixes with poor accuracy`() {
        val f = filter()
        assertFalse(f.accept(35.0, 139.0, 1_000L, accuracyMeters = 150.0))
        // Unknown accuracy (0/null) is accepted — many devices report 0.
        assertTrue(f.accept(35.0, 139.0, 1_000L, accuracyMeters = 0.0))
        assertTrue(f.accept(35.001, 139.0, 11_000L, null))
    }

    @Test
    fun `rejects impossible jumps`() {
        val f = filter()
        assertTrue(f.accept(35.0, 139.0, 0L, 5.0))
        // 0.05° lat ≈ 5.5 km in 3 s ≈ 6,600 km/h — impossible.
        assertFalse(f.accept(35.05, 139.0, 3_000L, 5.0))
    }

    @Test
    fun `rejects speed spikes but accepts sustained speed`() {
        val f = filter()
        // Steady ~30 km/h segments (2.5 m/0.3 s would be huge... use realistic):
        assertTrue(f.accept(35.0, 139.0, 0L, 5.0))
        // 250 m in 30 s = 30 km/h — normal running/cycling segment.
        assertTrue(f.accept(35.0 + 0.00225, 139.0, 30_000L, 5.0))
        // Spike: 180 m in 3 s = 216 km/h — above 90 ceiling, rejected outright.
        assertFalse(f.accept(35.0 + 0.00225 + 0.00162, 139.0, 33_000L, 5.0))
        // And the trail survives: a normal next fix is accepted.
        assertTrue(f.accept(35.0 + 0.00225 + 0.00162 + 0.00225, 139.0, 63_000L, 5.0))
    }

    @Test
    fun `rejects duplicate spam but keeps time anchors`() {
        val f = filter()
        assertTrue(f.accept(35.0, 139.0, 0L, 5.0))
        // 1 m away, 0.5 s later — spam.
        assertFalse(f.accept(35.0 + 0.000009, 139.0, 500L, 5.0))
        // 1 m away but 10 s later — anchor fix, accepted.
        assertTrue(f.accept(35.0 + 0.000009, 139.0, 10_000L, 5.0))
    }

    @Test
    fun `rejects backwards clock`() {
        val f = filter()
        assertTrue(f.accept(35.0, 139.0, 5_000L, 5.0))
        assertFalse(f.accept(35.001, 139.0, 4_000L, 5.0))
    }

    @Test
    fun `reset clears state`() {
        val f = filter()
        assertTrue(f.accept(35.0, 139.0, 0L, 5.0))
        f.reset()
        // First fix after reset is accepted regardless of distance from before.
        assertTrue(f.accept(40.0, 145.0, 1_000L, 5.0))
    }
}
