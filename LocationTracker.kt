package com.taxivn.dispatch.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.taxivn.dispatch.data.model.GpsCoordinate
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class LocationTracker(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(intervalMs: Long = 5000L): Flow<GpsCoordinate> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(2500L)
            .setMinUpdateDistanceMeters(5f)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(
                    GpsCoordinate(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        speedKmh = (loc.speed * 3.6f),
                        bearing = loc.bearing,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }

        fusedClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) {
                trySend(
                    GpsCoordinate(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        speedKmh = (loc.speed * 3.6f),
                        bearing = loc.bearing,
                        updatedAtEpochMs = System.currentTimeMillis()
                    )
                )
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        awaitClose {
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
