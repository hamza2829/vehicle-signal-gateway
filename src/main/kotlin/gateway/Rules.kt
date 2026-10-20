package gateway

import kotlinx.serialization.Serializable

@Serializable
data class Alert(val rule: String, val message: String, val signal: Signal)

sealed class SignalRule {
    abstract fun evaluate(signal: Signal): Alert?

    data class SpeedLimit(val limitKmh: Double = 130.0) : SignalRule() {
        init { require(limitKmh.isFinite() && limitKmh >= 0) }
        override fun evaluate(signal: Signal): Alert? =
            if (signal.speedKmh > limitKmh) Alert("SPEED_LIMIT", "Speed exceeds $limitKmh km/h", signal) else null
    }

    data class LowBattery(val thresholdPercent: Double = 15.0) : SignalRule() {
        init { require(thresholdPercent.isFinite() && thresholdPercent in 0.0..100.0) }
        override fun evaluate(signal: Signal): Alert? =
            if (signal.batteryPercent < thresholdPercent) Alert("LOW_BATTERY", "Battery below $thresholdPercent percent", signal) else null
    }

    data class IceWarning(val thresholdC: Double = 0.0) : SignalRule() {
        init { require(thresholdC.isFinite()) }
        override fun evaluate(signal: Signal): Alert? =
            if (signal.outsideTemperatureC < thresholdC) Alert("ICE_WARNING", "Temperature below $thresholdC C", signal) else null
    }
}

class RulesEngine(private val rules: List<SignalRule> = listOf(
    SignalRule.SpeedLimit(), SignalRule.LowBattery(), SignalRule.IceWarning(),
)) {
    fun evaluate(signal: Signal): List<Alert> = rules.mapNotNull { it.evaluate(signal) }
}
