package com.energy.app.data.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Personal records + insights tests (APP_SPEC §12, §35). */
class PersonalRecordsTest {

    /** A straight-line run at a constant pace — every km takes 360 s. */
    private fun runWorkout(
        id: String,
        km: Double,
        secPerKm: Double,
        startMillis: Long = 0L
    ): SavedWorkout {
        val pts = mutableListOf<WorkoutPoint>()
        val n = (km * 10).toInt().coerceAtLeast(2)
        for (i in 0..n) {
            val distKm = km * i / n
            pts += WorkoutPoint(
                lat = 35.0 + distKm / 111.0,
                lng = 139.0,
                timeMillis = (startMillis + distKm * secPerKm * 1000).toLong(),
                speedKmh = 3.6
            )
        }
        return SavedWorkout(
            id = id, type = WorkoutType.RUN,
            startMillis = startMillis,
            endMillis = startMillis + (km * secPerKm * 1000).toLong(),
            distanceMeters = km * 1000,
            durationMillis = (km * secPerKm * 1000).toLong(),
            points = pts,
            calories = 300
        )
    }

    @Test
    fun `bestEffortSeconds finds rolling 1km in a longer run`() {
        val w = runWorkout("a", km = 3.0, secPerKm = 360.0)
        val effort = PersonalRecords.bestEffortSeconds(w.points, 1_000.0)
        assertNotNull(effort)
        assertEquals(360.0, effort!!, 20.0)
    }

    @Test
    fun `bestEffortSeconds returns null when workout is shorter than target`() {
        val w = runWorkout("a", km = 0.4, secPerKm = 400.0)
        assertNull(PersonalRecords.bestEffortSeconds(w.points, 1_000.0))
    }

    @Test
    fun `bestEffortSeconds returns null for degenerate trails`() {
        assertNull(PersonalRecords.bestEffortSeconds(emptyList(), 1_000.0))
        assertNull(PersonalRecords.bestEffortSeconds(
            listOf(WorkoutPoint(35.0, 139.0, 0L, 0.0)), 1_000.0
        ))
    }

    @Test
    fun `fastest 1km record goes to the faster workout`() {
        val slow = runWorkout("slow", km = 1.2, secPerKm = 420.0)
        val fast = runWorkout("fast", km = 1.2, secPerKm = 300.0)
        val records = PersonalRecords.allRecords(listOf(slow, fast))
        val fastest = records.first { it.key == "fastest_1k" }
        assertEquals("fast", fastest.workoutId)
        assertTrue(fastest.valueText.startsWith("5:0") || fastest.valueText.startsWith("4:5"))
    }

    @Test
    fun `newRecordsFor flags only newly-set records`() {
        val first = runWorkout("first", km = 2.0, secPerKm = 400.0)
        val second = runWorkout("second", km = 5.0, secPerKm = 350.0, startMillis = 86_400_000L)
        // Records held before "second" exists.
        val before = PersonalRecords.newRecordsFor(listOf(first), first)
        assertTrue(before.isNotEmpty()) // first workout sets everything it can

        // "second" improves 5k (first couldn't run 5k) — should be flagged.
        val after = PersonalRecords.newRecordsFor(listOf(first, second), second)
        assertTrue(after.any { it.key == "fastest_5k" })
        assertTrue(after.any { it.key == "longest_distance" })
    }

    @Test
    fun `metadata records are present when history exists`() {
        val w = runWorkout("a", km = 6.0, secPerKm = 400.0)
        val records = PersonalRecords.allRecords(listOf(w))
        val keys = records.map { it.key }
        assertTrue("longest_distance" in keys)
        assertTrue("longest_time" in keys)
        assertTrue("best_day" in keys)
        assertTrue("fastest_1k" in keys)
    }
}

class WorkoutInsightsTest {

    private fun w(id: String, km: Double, paceSec: Double, start: Long): SavedWorkout =
        SavedWorkout(
            id = id, type = WorkoutType.RUN, startMillis = start,
            endMillis = start + (km * paceSec * 1000).toLong(),
            distanceMeters = km * 1000,
            durationMillis = (km * paceSec * 1000).toLong(),
            points = emptyList(), calories = 100
        )

    @Test
    fun `faster than recent average yields pace insight`() {
        val history = listOf(
            w("a", 5.0, 420.0, 1_000L), // 7:00/km
            w("b", 5.0, 400.0, 86_400_000L) // 6:40/km
        )
        val today = w("c", 5.0, 340.0, 2 * 86_400_000L) // 5:40/km
        val insights = WorkoutInsights.generate(history + today, today)
        assertTrue(insights.any { it.text.contains("faster") })
    }

    @Test
    fun `first workout of a type gets a welcome insight`() {
        val today = w("c", 5.0, 360.0, 2 * 86_400_000L)
        val insights = WorkoutInsights.generate(listOf(today), today)
        assertTrue(insights.any { it.text.contains("first") })
    }

    @Test
    fun `insights never exceed three`() {
        val history = (1..10).map { i ->
            w("w$i", 5.0, 400.0, i * 86_400_000L)
        }
        val today = w("today", 8.0, 320.0, 11 * 86_400_000L)
        val insights = WorkoutInsights.generate(history + today, today)
        assertTrue(insights.size <= 3)
    }
}
