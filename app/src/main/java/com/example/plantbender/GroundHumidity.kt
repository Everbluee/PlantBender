package com.example.plantbender

import InstantSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class GroundHumidity(
    @SerialName("humidity")
    val humidity: Int?,
    @Serializable(with = InstantSerializer::class)
    @SerialName("measurement_time")
    val measurement_time: Instant,
) {
    val humidityValue: String
        get() = humidity
            ?.div(1023.0)
            ?.times(100)
            ?.toBigDecimal()?.setScale(2, RoundingMode.UP)
            ?.toString() ?: "No data"

    val date: String
        get() = measurement_time.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    val time: String
        get() = measurement_time.atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
}