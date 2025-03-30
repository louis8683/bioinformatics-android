package com.louislu.pennbioinformatics.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.louislu.pennbioinformatics.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getLastLocation(): Location? {
        return suspendCoroutine { continuation ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location -> continuation.resume(location) }
                    .addOnFailureListener { continuation.resume(null) }
            } else {
                continuation.resume(null)
            }
        }
    }
}