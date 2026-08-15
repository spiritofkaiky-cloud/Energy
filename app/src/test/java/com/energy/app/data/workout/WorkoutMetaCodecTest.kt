package com.energy.app.data.workout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Storage codec tests (APP_SPEC §34/§35): versioning, round-trips,
 * malformed JSON, corrupt items, legacy migration.
 */
class WorkoutMetaCodecTest {

    private fun sampleWorkout(id: String = "w1"): SavedWorkout = SavedWorkout(
        id = id,
        type = WorkoutType.RUN,
        startMillis = 1_000L,
        endMillis = 2_000L,
        distanceMeters = 5_123.4,
        durationMillis = 1_800_000L,
        points = listOf(
            WorkoutPoint(35.0, 139.0, 1_000L, 10.5, alt = 12.0),
            WorkoutPoint(35.001, 139.0, 60_000L, 11.0)
        ),
        calories = 412,
        elevationGainMeters = 25.5,
        avgHeartRateBpm = 142,
        syncState = SyncState.SYNCED
    )

    @Test
    fun `metadata round-trips through encode-decode`() {
        val encoded = WorkoutMetaCodec.encode(listOf(sampleWorkout()))
        val decoded = WorkoutMetaCodec.decode(encoded)
        assertNotNull(decoded)
        val w = decoded!!.single()
        assertEquals("w1", w.id)
        assertEquals(WorkoutType.RUN, w.type)
        assertEquals(5_123.4, w.distanceMeters, 0.01)
        assertEquals(412, w.calories)
        assertEquals(25.5, w.elevationGainMeters, 0.01)
        assertEquals(142, w.avgHeartRateBpm)
        assertEquals(SyncState.SYNCED, w.syncState)
    }

    @Test
    fun `points round-trip keeps altitude`() {
        val encoded = WorkoutMetaCodec.encodePoints(sampleWorkout().points)
        val decoded = WorkoutMetaCodec.decodePoints(encoded)
        assertNotNull(decoded)
        assertEquals(2, decoded!!.size)
        assertEquals(12.0, decoded[0].alt!!, 0.01)
        assertNull(decoded[1].alt)
    }

    @Test
    fun `decodePoints clamps absurd speeds from legacy data`() {
        val json = """{"v":1,"points":[{"lat":35.0,"lng":139.0,"t":0,"s":88540.6},{"lat":35.001,"lng":139.0,"t":4000,"s":-3.0},{"lat":35.002,"lng":139.0,"t":8000,"s":12.5}]}"""
        val decoded = WorkoutMetaCodec.decodePoints(json)
        assertNotNull(decoded)
        assertEquals(130.0, decoded!![0].speedKmh, 0.001)
        assertEquals(0.0, decoded[1].speedKmh, 0.001)
        assertEquals(12.5, decoded[2].speedKmh, 0.001)
    }

    @Test
    fun `garbage input decodes to null or empty, never throws`() {
        assertNull(WorkoutMetaCodec.decode("not json at all"))
        assertNull(WorkoutMetaCodec.decode("{broken"))
        assertEquals(emptyList<SavedWorkout>(), WorkoutMetaCodec.decode(""))
        assertNull(WorkoutMetaCodec.decodePoints("garbage"))
        assertEquals(emptyList<SavedWorkout>(), WorkoutMetaCodec.decodeLegacy("garbage"))
    }

    @Test
    fun `wrong schema version returns null`() {
        assertNull(WorkoutMetaCodec.decode("{\"v\":0,\"items\":[]}"))
        assertNull(WorkoutMetaCodec.decode("{\"v\":99,\"items\":[]}"))
        assertNull(WorkoutMetaCodec.decodePoints("{\"v\":7,\"points\":[]}"))
    }

    @Test
    fun `one corrupt item is skipped, the rest survive`() {
        val good = WorkoutMetaCodec.encode(listOf(sampleWorkout("good")))
        // Build {"v":1,"items":[<corrupt>, <good>]} by splicing the encoded
        // good item inside the envelope's items array.
        val goodItem = good.removePrefix("""{"v":1,"items":[""").removeSuffix("]}")
        val json = """{"v":1,"items":[{"id":123,"type":"RUN","broken":true},$goodItem]}"""
        val decoded = WorkoutMetaCodec.decode(json)
        assertNotNull(decoded)
        assertEquals(listOf("good"), decoded!!.map { it.id })
    }

    @Test
    fun `unknown workout type and sync state degrade gracefully`() {
        val json = """{"v":1,"items":[{"id":"x","type":"KAYAK","start":1,"end":2,"distance":3,"duration":4,"sync":"WEIRD"}]}"""
        // Unknown TYPE makes the item unparseable → skipped (data survives elsewhere).
        val decoded = WorkoutMetaCodec.decode(json)
        assertNotNull(decoded)
        assertTrue(decoded!!.isEmpty())
    }

    @Test
    fun `legacy v0-4 blob migrates to full workouts with points`() {
        val legacy = """[{"id":"old1","type":"WALK","start":1000,"end":2000,"distance":800,"duration":900000,
            "points":[{"lat":35.0,"lng":139.0,"t":1000,"s":5.0},{"lat":35.001,"lng":139.0,"t":60000,"s":5.1}]}]"""
        val decoded = WorkoutMetaCodec.decodeLegacy(legacy)
        assertEquals(1, decoded.size)
        val w = decoded.single()
        assertEquals("old1", w.id)
        assertEquals(WorkoutType.WALK, w.type)
        assertEquals(2, w.points.size)
        assertTrue(w.calories > 0) // estimated during migration
    }

    @Test
    fun `empty list encodes and decodes`() {
        val encoded = WorkoutMetaCodec.encode(emptyList())
        assertEquals(emptyList<SavedWorkout>(), WorkoutMetaCodec.decode(encoded))
    }
}
