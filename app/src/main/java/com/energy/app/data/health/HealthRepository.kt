package com.energy.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyHealth(
    val steps: Int = 0,
    val avgHeartRateBpm: Int? = null,
    val distanceMeters: Double? = null
)

/**
 * Health Connect gateway (APP_SPEC M2). Fully graceful: if Health Connect
 * isn't installed (e.g. this emulator), `available=false` and the dashboard
 * keeps its skeleton placeholders.
 */
class HealthRepository(context: Context) {

    private val client: HealthConnectClient? = runCatching {
        HealthConnectClient.getOrCreate(context)
    }.getOrNull()

    val available: Boolean =
        client != null &&
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    private val _daily = MutableStateFlow<DailyHealth?>(null)
    val daily: StateFlow<DailyHealth?> = _daily.asStateFlow()

    /** Read today's steps + average heart rate. No-op when unavailable. */
    suspend fun refreshToday() {
        if (!available) return
        withContext(Dispatchers.IO) {
            runCatching {
                val granted = client!!.permissionController.getGrantedPermissions()
                val stepsPerm = HealthPermission.getReadPermission(StepsRecord::class)
                val hrPerm = HealthPermission.getReadPermission(HeartRateRecord::class)
                if (stepsPerm !in granted && hrPerm !in granted) return@runCatching

                // "Today" means the LOCAL day — using UTC skewed daily totals
                // for anyone east/west of the prime meridian.
                val startOfDay = LocalDate.now()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                val range = TimeRangeFilter.between(startOfDay, Instant.now())
                val metrics = buildList {
                    if (stepsPerm in granted) add(StepsRecord.COUNT_TOTAL)
                    if (hrPerm in granted) add(HeartRateRecord.BPM_AVG)
                }
                if (metrics.isEmpty()) return@runCatching
                val response = client!!.aggregate(
                    AggregateRequest(
                        metrics = metrics.toSet(),
                        timeRangeFilter = range
                    )
                )
                _daily.value = DailyHealth(
                    steps = response[StepsRecord.COUNT_TOTAL]?.toInt() ?: 0,
                    avgHeartRateBpm = response[HeartRateRecord.BPM_AVG]?.toInt()
                )
            }
        }
    }
}
