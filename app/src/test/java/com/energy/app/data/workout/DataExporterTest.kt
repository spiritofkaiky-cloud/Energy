package com.energy.app.data.workout

import com.energy.app.data.settings.RoutePrivacy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Data exporter + route privacy tests (§14, §22): GPX validity, privacy
 * trimming, and that privacy never shrinks a tiny route below 2 points.
 */
class DataExporterTest {

    private fun route(count: Int): List<WorkoutPoint> {
        // ~100 m per step, moving north — 150 m trim ≈ 1.5 steps.
        return (0 until count).map { i ->
            WorkoutPoint(
                lat = 35.0 + i * 0.0009,
                lng = 139.0,
                timeMillis = i * 4_000L,
                speedKmh = 8.0,
                alt = 10.0 + i
            )
        }
    }

    @Test
    fun `gpx output is valid XML with one point per input`() {
        val pts = route(4)
        val gpx = DataExporter.buildGpx(pts, "Morning Run", 1_000_000L)
        assertTrue(gpx.startsWith("<?xml"))
        assertTrue(gpx.contains("<trkpt"))
        assertEquals(4, "<trkpt".toRegex().findAll(gpx).count())
        assertTrue(gpx.contains("35.000000"))
    }

    @Test
    fun `approximate privacy trims start and end by about 150 m`() {
        val pts = route(10) // ~900 m
        val trimmed = DataExporter.trimForPrivacy(pts)
        // 900 m route minus 2×150 m ≈ 6 points remain
        assertEquals(6, trimmed.size)
        // First/last original points must be gone
        assertTrue(trimmed.first().timeMillis > pts.first().timeMillis)
        assertTrue(trimmed.last().timeMillis < pts.last().timeMillis)
    }

    @Test
    fun `tiny routes are never destroyed by privacy trimming`() {
        val pts = route(3) // ~200 m — trimming would leave < 2 points
        val trimmed = DataExporter.trimForPrivacy(pts)
        assertEquals(3, trimmed.size)
    }

    @Test
    fun `exact privacy keeps the full route`() {
        val pts = route(6)
        assertEquals(pts.size, DataExporter.applyPrivacy(pts, RoutePrivacy.EXACT).size)
        assertEquals(pts.size, DataExporter.applyPrivacy(pts, RoutePrivacy.PRIVATE).size)
    }

    @Test
    fun `csv produces one row per km split`() {
        val w = workoutWithPoints(route(25)) // ~2.4 km → 2 full splits
        val csv = DataExporter.buildCsv(w)
        val lines = csv.trim().lines()
        assertEquals("km,time_s,pace_s_per_km", lines.first())
        assertTrue(lines.size >= 3)
    }

    private fun workoutWithPoints(points: List<WorkoutPoint>) = SavedWorkout(
        id = "test1",
        type = WorkoutType.RUN,
        startMillis = 1_000L,
        endMillis = 100_000L,
        distanceMeters = 900.0,
        durationMillis = 90_000L,
        points = points,
        calories = 100
    )
}
