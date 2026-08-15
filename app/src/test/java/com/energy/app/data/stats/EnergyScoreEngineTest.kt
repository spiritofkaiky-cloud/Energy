package com.energy.app.data.stats

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Energy Score engine tests (APP_SPEC §35): categories, factors, trends. */
class EnergyScoreEngineTest {

    private val today = "2026-08-15"

    @Test
    fun `category thresholds follow the product spec`() {
        assertEquals("Excellent", EnergyScoreEngine.categoryFor(100))
        assertEquals("Excellent", EnergyScoreEngine.categoryFor(85))
        assertEquals("Good", EnergyScoreEngine.categoryFor(84))
        assertEquals("Good", EnergyScoreEngine.categoryFor(70))
        assertEquals("Fair", EnergyScoreEngine.categoryFor(69))
        assertEquals("Fair", EnergyScoreEngine.categoryFor(60))
        assertEquals("Recover", EnergyScoreEngine.categoryFor(59))
        assertEquals("Recover", EnergyScoreEngine.categoryFor(0))
    }

    @Test
    fun `fully active day scores high with heavy-load recovery adjust`() {
        val history = (1..7).associate {
            EnergyScoreEngine.lastNDays(7, today)[it - 1] to 55
        }
        val score = EnergyScoreEngine.compute(
            steps = 10_000, stepGoal = 10_000,
            workoutMinutes = 50.0, workoutKm = 8.0, pathKm = 2.0,
            history = history, today = today,
            todayLoadMinutes = 74.0, recentLoadMinutes = 50.0
        )
        // Steps 40 + minutes 30 + distance 20 = 90; heavy load −10 → 80.
        assertEquals(80, score.value)
        assertEquals("Good", score.category)
        assertTrue(score.factors.any { it.label == "Recovery" && it.points == -10 })
    }

    @Test
    fun `idle day scores at the bottom and recommends movement`() {
        val score = EnergyScoreEngine.compute(
            steps = 0, stepGoal = 10_000,
            workoutMinutes = 0.0, workoutKm = 0.0, pathKm = 0.0,
            history = emptyMap(), today = today,
            todayLoadMinutes = 0.0, recentLoadMinutes = 0.0
        )
        assertEquals(0, score.value)
        assertEquals("Recover", score.category)
        assertTrue(score.recommendation.text.contains("walk"))
    }

    @Test
    fun `trend compares against the previous 7 days`() {
        val history = mapOf(
            "2026-08-14" to 50, "2026-08-13" to 50, "2026-08-12" to 50,
            "2026-08-11" to 50, "2026-08-10" to 50, "2026-08-09" to 50,
            "2026-08-08" to 50
        )
        val score = EnergyScoreEngine.compute(
            steps = 10_000, stepGoal = 10_000,
            workoutMinutes = 45.0, workoutKm = 8.0, pathKm = 0.0,
            history = history, today = today,
            todayLoadMinutes = 40.0, recentLoadMinutes = 40.0
        )
        // 40 + 30 + 20 = 90, no recovery adjust (similar load).
        assertEquals(90, score.value)
        assertEquals(40, score.trendVs7Day)
    }

    @Test
    fun `step-goal progress drives the steps factor proportionally`() {
        val half = EnergyScoreEngine.compute(
            steps = 5_000, stepGoal = 10_000,
            workoutMinutes = 0.0, workoutKm = 0.0, pathKm = 0.0,
            history = emptyMap(), today = today,
            todayLoadMinutes = 0.0, recentLoadMinutes = 0.0
        )
        assertEquals(20, half.factors.first { it.label == "Steps" }.points)
    }

    @Test
    fun `high recent load with no workout today recommends recovery`() {
        val rec = EnergyScoreEngine.recommend(
            steps = 2_000, stepGoal = 10_000,
            workoutCount = 0, todayLoad = 10.0, recentLoad = 120.0,
            hasMovement = true
        )
        assertTrue(rec.text.contains("Recovery"))
    }

    @Test
    fun `near-goal steps with no workout recommends finishing the goal`() {
        val rec = EnergyScoreEngine.recommend(
            steps = 9_000, stepGoal = 10_000,
            workoutCount = 0, todayLoad = 10.0, recentLoad = 30.0,
            hasMovement = true
        )
        assertTrue(rec.text.contains("step goal"))
    }

    @Test
    fun `lastNDays returns the 7 previous calendar days oldest-first`() {
        val days = EnergyScoreEngine.lastNDays(7, "2026-08-15")
        assertEquals(7, days.size)
        assertEquals("2026-08-08", days.first())
        assertEquals("2026-08-14", days.last())
        assertTrue("2026-08-15" !in days)
    }
}
