package gateway

import kotlinx.serialization.Serializable

@Serializable
data class Position(val latitude: Double, val longitude: Double)

@Serializable
data class Signal(
    val speedKmh: Double,
    val batteryPercent: Double,
    val outsideTemperatureC: Double,
    val position: Position,
)

fun Signal.validationErrors(): List<String> = buildList {
    if (!speedKmh.isFinite() || speedKmh !in 0.0..300.0) add("speedKmh must be between 0 and 300")
    if (!batteryPercent.isFinite() || batteryPercent !in 0.0..100.0) add("batteryPercent must be between 0 and 100")
    if (!outsideTemperatureC.isFinite() || outsideTemperatureC !in -80.0..80.0) add("outsideTemperatureC must be between -80 and 80")
    if (!position.latitude.isFinite() || position.latitude !in -90.0..90.0) add("latitude must be between -90 and 90")
    if (!position.longitude.isFinite() || position.longitude !in -180.0..180.0) add("longitude must be between -180 and 180")
}
