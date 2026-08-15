package com.energy.app.ui.components

import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.energy.app.data.location.DayPoint
import com.energy.app.ui.theme.LocalDarkTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val LIGHT_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DARK_STYLE = "https://tiles.openfreemap.org/styles/dark"
private const val ENERGY_ORANGE = "#FF7A1A"
private const val ENERGY_CORAL = "#FF5F6D"

/**
 * Strava-style map: draws the route as a line, optional speed coloring
 * (coral → orange → yellow by segment speed), optional pulsing
 * current-position marker, full gesture support when [interactive].
 * MapLibre + OpenFreeMap = free, no API key. Dark style in dark mode.
 */
@Composable
fun MapWidget(
    points: List<DayPoint>,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    currentPosition: DayPoint? = null,
    /** Per-segment speeds (km/h, size = points.size) → speed-colored route. */
    speeds: List<Float>? = null
) {
    val dark = LocalDarkTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var fittedOnce by remember { mutableStateOf(false) }
    // Delta guards: skip redundant native layer updates (they leak on
    // software renderers when called every recomposition).
    var lastPointsHash by remember { mutableStateOf(points.hashCode()) }
    var lastSpeedsHash by remember { mutableStateOf(speeds?.hashCode() ?: 0) }
    var lastPosition by remember { mutableStateOf(currentPosition) }

    AndroidView(
        factory = { ctx ->
            MapLibre.getInstance(ctx)
            MapView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                getMapAsync { map ->
                    map.uiSettings.isRotateGesturesEnabled = interactive
                    map.uiSettings.isTiltGesturesEnabled = interactive
                    map.uiSettings.isScrollGesturesEnabled = interactive
                    map.uiSettings.isZoomGesturesEnabled = interactive
                    map.uiSettings.isCompassEnabled = interactive
                    map.setStyle(if (dark) DARK_STYLE else LIGHT_STYLE) { style ->
                        ensurePathLayer(style, points, speeds)
                        ensurePositionLayer(style)
                    }
                    if (points.isEmpty() && currentPosition == null) {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(37.7749, -122.4194))
                            .zoom(12.0)
                            .build()
                    } else {
                        fitBounds(map, points, currentPosition)
                    }
                    mapRef = map
                }
                mapViewRef.value = this
            }
        },
        update = { _ ->
            mapRef?.let { map ->
                val pointsChanged = points.hashCode() != lastPointsHash
                val speedsChanged = (speeds?.hashCode() ?: 0) != lastSpeedsHash
                val positionChanged = currentPosition != lastPosition
                if (!pointsChanged && !speedsChanged && !positionChanged && fittedOnce) return@let
                lastPointsHash = points.hashCode()
                lastSpeedsHash = speeds?.hashCode() ?: 0
                lastPosition = currentPosition
                map.getStyle { style ->
                    if (pointsChanged || speedsChanged) ensurePathLayer(style, points, speeds)
                    if (positionChanged) updatePositionLayer(style, currentPosition)
                    if (!fittedOnce && (points.size >= 2 || currentPosition != null)) {
                        fittedOnce = true
                        fitBounds(map, points, currentPosition)
                    }
                }
            }
        },
        modifier = modifier
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapViewRef.value?.onStart()
                Lifecycle.Event.ON_STOP -> mapViewRef.value?.onStop()
                Lifecycle.Event.ON_DESTROY -> mapViewRef.value?.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

private fun ensurePathLayer(style: Style, points: List<DayPoint>, speeds: List<Float>?) {
    val sourceName = if (speeds != null) "day-path-speed" else "day-path"
    val layerName = if (speeds != null) "day-path-layer-speed" else "day-path-layer"

    val geometryJson = if (speeds == null || points.size < 2) {
        Feature.fromGeometry(
            LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) })
        ).toJson()
    } else {
        val features = (1 until points.size).map { i ->
            Feature.fromGeometry(
                LineString.fromLngLats(
                    listOf(
                        Point.fromLngLat(points[i - 1].lng, points[i - 1].lat),
                        Point.fromLngLat(points[i].lng, points[i].lat)
                    )
                )
            ).apply { addNumberProperty("speed", speeds[i].coerceAtLeast(0f)) }
        }
        org.maplibre.geojson.FeatureCollection.fromFeatures(features).toJson()
    }

    val existing = style.getSourceAs<GeoJsonSource>(sourceName)
    if (existing == null) {
        style.addSource(GeoJsonSource(sourceName, geometryJson))
        val props = arrayOf(
            PropertyFactory.lineWidth(4f),
            PropertyFactory.lineOpacity(0.9f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
        )
        val color = if (speeds != null) speedColorExpression() else null
        style.addLayer(
            LineLayer(layerName, sourceName).withProperties(
                *props,
                if (color != null) PropertyFactory.lineColor(color)
                else PropertyFactory.lineColor(ENERGY_ORANGE)
            )
        )
    } else {
        existing.setGeoJson(geometryJson)
    }
}

/** Slow → coral, cruising → orange, fast → yellow (data-driven, GPU-side). */
private fun speedColorExpression(): org.maplibre.android.style.expressions.Expression =
    org.maplibre.android.style.expressions.Expression.interpolate(
        org.maplibre.android.style.expressions.Expression.exponential(1f),
        org.maplibre.android.style.expressions.Expression.get("speed"),
        org.maplibre.android.style.expressions.Expression.stop(0f, "#FF5F6D"),
        org.maplibre.android.style.expressions.Expression.stop(8f, "#FF7A1A"),
        org.maplibre.android.style.expressions.Expression.stop(16f, "#FFB84D"),
        org.maplibre.android.style.expressions.Expression.stop(30f, "#FFE28A")
    )

private fun ensurePositionLayer(style: Style) {
    if (style.getSourceAs<GeoJsonSource>("current-pos") != null) return
    style.addSource(GeoJsonSource("current-pos"))
    style.addLayer(
        CircleLayer("current-pos-halo", "current-pos").withProperties(
            PropertyFactory.circleRadius(16f),
            PropertyFactory.circleColor(ENERGY_ORANGE),
            PropertyFactory.circleOpacity(0.35f)
        )
    )
    style.addLayer(
        CircleLayer("current-pos-dot", "current-pos").withProperties(
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleColor("#FFFFFF"),
            PropertyFactory.circleStrokeColor(ENERGY_ORANGE),
            PropertyFactory.circleStrokeWidth(3f)
        )
    )
}

private fun updatePositionLayer(style: Style, currentPosition: DayPoint?) {
    val source = style.getSourceAs<GeoJsonSource>("current-pos") ?: return
    source.setGeoJson(
        if (currentPosition == null) {
            Feature.fromGeometry(LineString.fromLngLats(emptyList()))
        } else {
            Feature.fromGeometry(Point.fromLngLat(currentPosition.lng, currentPosition.lat))
        }
    )
}

private fun fitBounds(map: MapLibreMap, points: List<DayPoint>, currentPosition: DayPoint?) {
    val all = buildList {
        addAll(points)
        currentPosition?.let { add(it) }
    }
    if (all.isEmpty()) return
    if (all.size == 1) {
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(all[0].lat, all[0].lng))
                    .zoom(15.0)
                    .build()
            ),
            700
        )
        return
    }
    val builder = LatLngBounds.Builder()
    all.forEach { builder.include(LatLng(it.lat, it.lng)) }
    val camera = map.getCameraForLatLngBounds(builder.build(), intArrayOf(64, 64, 64, 64)) ?: return
    map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 700)
}
