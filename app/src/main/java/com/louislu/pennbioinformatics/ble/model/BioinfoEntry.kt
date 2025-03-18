package com.louislu.pennbioinformatics.ble.model

data class BioinfoEntry(
    val temperature: Float,       // Temperature in Celsius
    val humidity: Float,          // Humidity as a fraction (0.0 - 1.0)
    val pm25: Float,             // PM2.5 concentration in µg/m³
    val coConcentration: Float,  // CO concentration in PPM
    val lastUpdate: Int           // Last update timestamp in seconds
)
