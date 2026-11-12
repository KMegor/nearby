package com.coursework.nearby.presentation.map

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.coursework.nearby.domain.location.GeoMath
import com.coursework.nearby.domain.model.GeoPoint
import com.coursework.nearby.domain.model.NearbyUser
import com.coursework.nearby.domain.model.Relation
import com.coursework.nearby.domain.model.SearchRadius
import com.coursework.nearby.presentation.theme.LocalDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import java.net.HttpURLConnection
import java.net.URL

// Бесплатные стили на данных OpenStreetMap, ключ не нужен.
private const val STYLE_LIGHT = "https://tiles.openfreemap.org/styles/positron"
private const val STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"

private const val SOURCE_AREA = "nearby-area"
private const val SOURCE_ME = "nearby-me"
private const val SOURCE_USERS = "nearby-users"
private const val LAYER_USERS = "nearby-users-layer"
private const val PROP_ID = "id"
private const val PROP_COLOR = "color"

/** Цвета слоёв карты для тёмного и светлого оформления. */
private class MapPalette(val accent: String, val markerStroke: String, val me: String, val background: String)

private val DARK_PALETTE = MapPalette(accent = "#E8B15A", markerStroke = "#0B1220", me = "#FFFFFF", background = "#0F1828")
private val LIGHT_PALETTE = MapPalette(accent = "#14213D", markerStroke = "#FFFFFF", me = "#FFFFFF", background = "#EFECE5")
private val DEFAULT_CENTER = LatLng(55.7522, 37.6156)

/** Цвет точки по отношению к человеку (таблица отображения из согласованных требований). */
fun Relation.markerColor(dark: Boolean): String = when (this) {
    Relation.NONE -> if (dark) "#E8B15A" else "#14213D"
    Relation.OUTGOING, Relation.INCOMING -> if (dark) "#FF8F6B" else "#D9541E"
    Relation.CHAT -> if (dark) "#6FD3B4" else "#1E8E6E"
    Relation.COOLDOWN, Relation.BLOCKED_BY_ME -> if (dark) "#62718C" else "#8A8F98"
}

/**
 * Карта MapLibre: круг радиуса поиска, собственная точка и люди рядом.
 * Круг и свою точку рисуем сами по огрублённым координатам — встроенный показ
 * местоположения использовал бы точные.
 */
@Composable
fun NearbyMap(
    myPoint: GeoPoint?,
    radius: SearchRadius,
    users: List<NearbyUser>,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val dark = LocalDarkTheme.current
    val palette = if (dark) DARK_PALETTE else LIGHT_PALETTE
    val currentOnUserClick by rememberUpdatedState(onUserClick)
    var layers by remember { mutableStateOf<MapLayers?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            layers = null
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView, dark) {
        layers = null
        // Подложку загружаем сами с коротким ожиданием: без сети карта сразу открывается на пустом
        // фоне, и круг с людьми видны без задержки.
        val styleJson = fetchStyle(if (dark) STYLE_DARK else STYLE_LIGHT) ?: fallbackStyle(palette)
        mapView.getMapAsync { map ->
            map.uiSettings.isRotateGesturesEnabled = false
            map.uiSettings.isTiltGesturesEnabled = false
            if (map.style == null) map.cameraPosition = CameraPosition.Builder().target(DEFAULT_CENTER).zoom(10.0).build()
            map.setStyle(Style.Builder().fromJson(styleJson)) { style -> layers = MapLayers(map, style, palette, dark) }
            map.addOnMapClickListener { latLng ->
                val point = map.projection.toScreenLocation(latLng)
                val touch = RectF(point.x - TOUCH_PX, point.y - TOUCH_PX, point.x + TOUCH_PX, point.y + TOUCH_PX)
                val id = map.queryRenderedFeatures(touch, LAYER_USERS).firstOrNull()?.getStringProperty(PROP_ID)
                if (id != null) currentOnUserClick(id)
                id != null
            }
        }
    }

    LaunchedEffect(layers, myPoint, radius) { layers?.showArea(myPoint, radius) }
    LaunchedEffect(layers, users) { layers?.showUsers(users) }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private const val TOUCH_PX = 36f

private const val STYLE_TIMEOUT_MS = 4_000L
private val styleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/** Описание стиля карты либо null, если сервер подложки недоступен за [STYLE_TIMEOUT_MS]. */
private suspend fun fetchStyle(url: String): String? {
    // Запрос идёт в отдельной области: ожидание DNS нельзя прервать, но можно перестать его ждать.
    val request = styleScope.async {
        runCatching {
            (URL(url).openConnection() as HttpURLConnection).run {
                connectTimeout = STYLE_TIMEOUT_MS.toInt()
                readTimeout = STYLE_TIMEOUT_MS.toInt()
                try {
                    if (responseCode == 200) inputStream.bufferedReader().use { it.readText() } else null
                } finally {
                    disconnect()
                }
            }
        }.getOrNull()
    }
    return withTimeoutOrNull(STYLE_TIMEOUT_MS) { request.await() }
}

private fun fallbackStyle(palette: MapPalette): String {
    val background = palette.background
    return """{"version":8,"sources":{},"layers":[{"id":"background","type":"background","paint":{"background-color":"$background"}}]}"""
}

private class MapLayers(private val map: MapLibreMap, style: Style, palette: MapPalette, private val dark: Boolean) {
    private val area = GeoJsonSource(SOURCE_AREA)
    private val me = GeoJsonSource(SOURCE_ME)
    private val users = GeoJsonSource(SOURCE_USERS)

    init {
        style.addSource(area)
        style.addSource(me)
        style.addSource(users)
        style.addLayer(FillLayer("nearby-area-fill", SOURCE_AREA).withProperties(fillColor(palette.accent), fillOpacity(0.10f)))
        style.addLayer(LineLayer("nearby-area-line", SOURCE_AREA).withProperties(lineColor(palette.accent), lineWidth(1.5f)))
        style.addLayer(
            CircleLayer("nearby-me-layer", SOURCE_ME).withProperties(
                circleRadius(6f), circleColor(palette.me), circleStrokeColor(palette.accent), circleStrokeWidth(4f),
            ),
        )
        style.addLayer(
            CircleLayer(LAYER_USERS, SOURCE_USERS).withProperties(
                circleRadius(11f),
                circleColor(Expression.toColor(Expression.get(PROP_COLOR))),
                circleStrokeColor(palette.markerStroke),
                circleStrokeWidth(3f),
            ),
        )
    }

    fun showArea(center: GeoPoint?, radius: SearchRadius) {
        if (center == null) {
            area.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            me.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        val ring = GeoMath.circle(center, radius.km * 1000.0)
        area.setGeoJson(Polygon.fromLngLats(listOf(ring.map { Point.fromLngLat(it.longitude, it.latitude) })))
        me.setGeoJson(Point.fromLngLat(center.longitude, center.latitude))
        val bounds = LatLngBounds.Builder().includes(ring.map { LatLng(it.latitude, it.longitude) }).build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, CAMERA_PADDING_PX))
    }

    fun showUsers(list: List<NearbyUser>) {
        users.setGeoJson(
            FeatureCollection.fromFeatures(
                list.map { user ->
                    Feature.fromGeometry(Point.fromLngLat(user.point.longitude, user.point.latitude)).apply {
                        addStringProperty(PROP_ID, user.id)
                        addStringProperty(PROP_COLOR, user.relation.markerColor(dark))
                    }
                },
            ),
        )
    }

    private companion object {
        const val CAMERA_PADDING_PX = 96
    }
}
