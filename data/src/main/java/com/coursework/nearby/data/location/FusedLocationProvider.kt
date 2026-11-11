package com.coursework.nearby.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.coursework.nearby.domain.location.LocationProvider
import com.coursework.nearby.domain.model.GeoPoint
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

/**
 * Координаты от служб Google. Обновления идут, только пока поток собирают, то есть пока
 * приложение открыто и видимость включена. Разрешение на геолокацию запрашивает экран
 * до включения видимости.
 */
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override fun locations(): Flow<GeoPoint> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(GeoPoint(it.latitude, it.longitude)) }
            }
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(MIN_INTERVAL_MS)
            .build()
        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
                .addOnFailureListener { close(it) }
        } catch (e: SecurityException) {
            close(e)
        }
        awaitClose { client.removeLocationUpdates(callback) }
    }

    private companion object {
        const val UPDATE_INTERVAL_MS = 60_000L
        const val MIN_INTERVAL_MS = 15_000L
    }
}
