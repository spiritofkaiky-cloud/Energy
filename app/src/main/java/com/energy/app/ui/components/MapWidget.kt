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

/**
 * Strava-style map: draws the day's movement as a line.
 * MapLibre + OpenFreeMap = free forever, no API key. Dark style in dark mode.
 */
@Composable
fun MapWidget(
    points: List<DayPoint>,
    modifier: Modifier = Modifier
) {
    val dark = LocalDarkTheme.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapViewRef = remember { mutableStateOf<MapView?>(null) }
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }

    AndroidView(
        factory = { ctx ->
            MapLibre.getInstance(ctx)
            MapView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                getMapAsync { map ->
                    map.uiSettings.isRotateGesturesEnabled = false
                    map.setStyle(if (dark) DARK_STYLE else LIGHT_STYLE) { style ->
                        ensurePathLayer(style, points)
                    }
                    if (points.isEmpty()) {
                        map.cameraPosition = CameraPosition.Builder()
                            .target(LatLng(37.7749, -122.4194))
                            .zoom(12.0)
                            .build()
                    } else {
                        fitBounds(map, points)
                    }
                    mapRef = map
                }
                mapViewRef.value = this
            }
        },
        update = { _ ->
            mapRef?.let { map ->
                map.getStyle { style ->
                    ensurePathLayer(style, points)
                    if (points.size >= 2) fitBounds(map, points)
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

private fun ensurePathLayer(style: Style, points: List<DayPoint>) {
    val geometry = Feature.fromGeometry(
        LineString.fromLngLats(points.map { Point.fromLngLat(it.lng, it.lat) })
    )
    val existing = style.getSourceAs<GeoJsonSource>("day-path")
    if (existing == null) {
        style.addSource(GeoJsonSource("day-path", geometry))
        style.addLayer(
            LineLayer("day-path-layer", "day-path").withProperties(
                PropertyFactory.lineColor(ENERGY_ORANGE),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineOpacity(0.9f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
    } else {
        existing.setGeoJson(geometry)
    }
}

private fun fitBounds(map: MapLibreMap, points: List<DayPoint>) {
    if (points.size < 2) return
    val builder = LatLngBounds.Builder()
    points.forEach { builder.include(LatLng(it.lat, it.lng)) }
    val bounds = builder.build()
    val camera = map.getCameraForLatLngBounds(bounds, intArrayOf(48, 48, 48, 48)) ?: return
    map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), 700)
}
