package com.energy.app.data.workout

import com.energy.app.data.stats.StatsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutMathTest {

    private val eps = 0.01

    @Test
    fun `haversine - same point is zero`() {
        assertEquals(0.0, StatsRepository.haversineKm(35.0, 139.0, 35.0, 139.0), eps)
    }

    @Test
    fun `haversine - known distance ~1km per 0 01 degree latitude`() {
        // 0.01 deg latitude ≈ 1.11 km
        val d = StatsRepository.haversineKm(35.0, 139.0, 35.01, 139.0)
        assertEquals(1.11, d, 0.03)
    }

    @Test
    fun `speedKmh - 1km in 6 minutes is 10 kmh`() {
        val speed = WorkoutMath.speedKmh(35.0, 139.0, 35.009, 139.0, 6 * 60_000L)
        assertEquals(10.0, speed, 0.2)
    }

    @Test
    fun `speedKmh - zero dt returns zero not infinity`() {
        assertEquals(0.0, WorkoutMath.speedKmh(35.0, 139.0, 35.01, 139.0, 0L), eps)
    }

    @Test
    fun `pace - 10 kmh means 6 min per km`() {
        val pace = WorkoutMath.paceSecondsPerKm(1000.0, 6 * 60_000L)
        assertEquals(360.0, pace!!, 1.0)
    }

    @Test
    fun `pace - too short distance returns null`() {
        assertEquals(null, WorkoutMath.paceSecondsPerKm(10.0, 60_000L))
    }

    @Test
    fun `splits - two km at constant pace produce two equal splits`() {
        val points = listOf(
            0.0 to 0L,
            1000.0 to 300_000L,
            2000.0 to 600_000L
        )
        val splits = WorkoutMath.splits(points)
        assertEquals(2, splits.size)
        assertEquals(300.0, splits[0], 1.0)
        assertEquals(300.0, splits[1], 1.0)
    }

    @Test
    fun `calories - run burns more than walk`() {
        val run = WorkoutMath.calories(WorkoutType.RUN, 30 * 60_000L)
        val walk = WorkoutMath.calories(WorkoutType.WALK, 30 * 60_000L)
        assertTrue("run ${run} should exceed walk ${walk}", run > walk)
    }

    @Test
    fun `maxSpeed - picks the peak`() {
        assertEquals(25.0, WorkoutMath.maxSpeedKmh(listOf(5.0, 25.0, 12.0)), eps)
    }

    @Test
    fun `movingTime - excludes slow GPS drift segments`() {
        val points = listOf(
            WorkoutPoint(35.0, 139.0, 0L, 0.0),
            WorkoutPoint(35.009, 139.0, 300_000L, 12.0),   // ~11 km/h — moving
            WorkoutPoint(35.00901, 139.0, 600_000L, 0.05)  // drift — excluded
        )
        val moving = WorkoutMath.movingTimeMillis(points)
        assertEquals(300_000L, moving)
    }

    @Test
    fun `formatPace - 360 seconds is 6-00`() {
        assertEquals("6:00 /km", WorkoutMath.formatPace(360.0))
    }

    @Test
    fun `formatDistance - switches to km at 1000m`() {
        assertEquals("500 m", WorkoutMath.formatDistance(500.0))
        assertEquals("1.50 km", WorkoutMath.formatDistance(1500.0))
    }

    @Test
    fun `dayPathDistanceKm - sums consecutive segments`() {
        val points = listOf(
            com.energy.app.data.location.DayPoint(35.0, 139.0, 0L),
            com.energy.app.data.location.DayPoint(35.009, 139.0, 1L),
            com.energy.app.data.location.DayPoint(35.018, 139.0, 2L)
        )
        val km = StatsRepository.dayPathDistanceKm(points)
        assertEquals(2.0, km, 0.05)
    }

    // ── v0.5 additions ────────────────────────────────────────────────────

    @Test
    fun `elevationGain - sums only meaningful climbs`() {
        val points = listOf(
            WorkoutPoint(35.0, 139.0, 0L, 0.0, alt = 10.0),
            WorkoutPoint(35.001, 139.0, 1L, 0.0, alt = 15.0),   // +5
            WorkoutPoint(35.002, 139.0, 2L, 0.0, alt = 14.0),   // -1 (ignored)
            WorkoutPoint(35.003, 139.0, 3L, 0.0, alt = 14.4),   // +0.4 (<0.5 ignored)
            WorkoutPoint(35.004, 139.0, 4L, 0.0, alt = 20.0),   // +5.6
            WorkoutPoint(35.005, 139.0, 5L, 0.0, alt = 90.0)    // +70 (>30 ignored)
        )
        assertEquals(10.6, WorkoutMath.elevationGainMeters(points), 0.01)
    }

    @Test
    fun `elevationGain - empty and null altitudes`() {
        assertEquals(0.0, WorkoutMath.elevationGainMeters(emptyList()), 0.001)
        assertEquals(
            0.0,
            WorkoutMath.elevationGainMeters(listOf(WorkoutPoint(35.0, 139.0, 0L, 0.0))),
            0.001
        )
    }

    @Test
    fun `calories - heavier runner burns more`() {
        val light = WorkoutMath.calories(WorkoutType.RUN, 30 * 60_000L, 50.0)
        val heavy = WorkoutMath.calories(WorkoutType.RUN, 30 * 60_000L, 90.0)
        assertTrue("heavy ($heavy) should exceed light ($light)", heavy > light)
    }

    @Test
    fun `formatDistance - imperial conversion`() {
        assertEquals("3.11 mi", WorkoutMath.formatDistance(5_000.0, imperial = true))
        assertEquals("1,640 ft".replace(",", ""), WorkoutMath.formatDistance(500.0, imperial = true).replace(",", ""))
        assertEquals("5.00 km", WorkoutMath.formatDistance(5_000.0, imperial = false))
    }

    @Test
    fun `formatPace - imperial converts minutes per mile`() {
        // 360 s/km ≈ 579.4 s/mi ≈ 9:39
        assertEquals("9:39 /mi", WorkoutMath.formatPace(360.0, imperial = true))
    }

    @Test
    fun `formatSpeed - imperial converts to mph`() {
        assertEquals("6.2 mph", WorkoutMath.formatSpeed(10.0, imperial = true))
        assertEquals("10.0 km/h", WorkoutMath.formatSpeed(10.0, imperial = false))
    }

    @Test
    fun `formatDuration - handles hours`() {
        assertEquals("1:02:03", WorkoutMath.formatDuration(3_723_000L))
        assertEquals("05:06", WorkoutMath.formatDuration(306_000L))
    }

    @Test
    fun `cumulativeDistanceTime - starts at zero and only counts moving`() {
        val points = listOf(
            WorkoutPoint(35.0, 139.0, 0L, 0.0),
            WorkoutPoint(35.009, 139.0, 300_000L, 12.0),  // ~1 km moving
            WorkoutPoint(35.00902, 139.0, 600_000L, 0.0)  // drift — excluded
        )
        val cum = WorkoutMath.cumulativeDistanceTime(points)
        assertEquals(3, cum.size)
        assertEquals(0.0, cum[0].first, 0.001)
        assertEquals(0L, cum[0].second)
        // Final cumulative distance ≈ 1 km (drift segment not counted).
        assertEquals(1.0, cum[2].first / 1000.0, 0.05)
        assertEquals(600_000L, cum[2].second)
    }
}
