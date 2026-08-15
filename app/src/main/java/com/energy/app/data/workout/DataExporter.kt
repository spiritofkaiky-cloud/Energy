package com.energy.app.data.workout

import com.energy.app.data.settings.RoutePrivacy
import java.io.File

/**
 * Data export (§14): JSON (full fidelity), CSV (per-km splits) and GPX
 * (routes). Pure string builders — unit-testable. Route privacy trimming
 * happens HERE so exports always respect the user's setting.
 */
object DataExporter {

    /** Drop the first/last ~150 m of a route so home/work aren't exposed. */
    fun trimForPrivacy(points: List<WorkoutPoint>, meters: Double = 150.0): List<WorkoutPoint> {
        if (points.size < 4) return points
        var cutStart = 0
        var acc = 0.0
        for (i in 1 until points.size) {
            acc += GpsFilter.haversineMeters(
                points[i - 1].lat, points[i - 1].lng, points[i].lat, points[i].lng
            )
            if (acc >= meters) { cutStart = i; break }
        }
        var cutEnd = points.size - 1
        acc = 0.0
        for (i in points.size - 2 downTo 1) {
            acc += GpsFilter.haversineMeters(
                points[i + 1].lat, points[i + 1].lng, points[i].lat, points[i].lng
            )
            if (acc >= meters) { cutEnd = i; break }
        }
        if (cutEnd - cutStart < 2) return points // route too short — keep it intact
        return points.slice(cutStart..cutEnd)
    }

    fun applyPrivacy(points: List<WorkoutPoint>, privacy: RoutePrivacy): List<WorkoutPoint> =
        when (privacy) {
            RoutePrivacy.EXACT, RoutePrivacy.PRIVATE -> points
            RoutePrivacy.APPROXIMATE -> trimForPrivacy(points)
        }

    fun buildGpx(points: List<WorkoutPoint>, name: String, startMillis: Long): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<gpx version=\"1.1\" creator=\"Energy\" " +
            "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 " +
            "http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")
        sb.append("  <trk><name>${escape(name)}</name><trkseg>\n")
        points.forEach { p ->
            sb.append(
                "    <trkpt lat=\"%.6f\" lon=\"%.6f\"><time>%s</time></trkpt>\n"
                    .format(
                        java.util.Locale.US, p.lat, p.lng,
                        java.time.Instant.ofEpochMilli(startMillis + p.timeMillis).toString()
                    )
            )
        }
        sb.append("  </trkseg></trk>\n</gpx>\n")
        return sb.toString()
    }

    fun buildCsv(workout: SavedWorkout): String {
        val splits = WorkoutMath.splits(WorkoutMath.cumulativeDistanceTime(workout.points))
        val sb = StringBuilder("km,time_s,pace_s_per_km\n")
        splits.forEachIndexed { i, s ->
            sb.append("${i + 1},${String.format(java.util.Locale.US, "%.1f", s * 60)},$s\n")
        }
        return sb.toString()
    }

    fun buildJson(workout: SavedWorkout): String =
        WorkoutRepository.toCloudJson(workout)

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}

/** Export one workout into cacheDir/export for sharing via FileProvider. */
class ExportManager(private val cacheDir: File) {

    fun exportWorkout(workout: SavedWorkout, privacy: RoutePrivacy, format: String): File {
        val dir = File(cacheDir, "export").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // one export at a time
        val safeName = "energy-${workout.id.take(8)}"
        val points = DataExporter.applyPrivacy(workout.points, privacy)
        val exportable = workout.copy(points = points)
        val file = when (format) {
            "gpx" -> File(dir, "$safeName.gpx")
            "csv" -> File(dir, "$safeName.csv")
            else -> File(dir, "$safeName.json")
        }
        file.writeText(
            when (format) {
                "gpx" -> DataExporter.buildGpx(points, "Energy ${workout.type.label}", workout.startMillis)
                "csv" -> DataExporter.buildCsv(exportable)
                else -> DataExporter.buildJson(exportable)
            }
        )
        return file
    }
}
