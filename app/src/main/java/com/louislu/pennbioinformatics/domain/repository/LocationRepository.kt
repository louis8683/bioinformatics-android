package com.louislu.pennbioinformatics.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getLastLocation(): Location?
}