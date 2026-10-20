package gateway

import org.junit.jupiter.api.Test
import kotlin.test.*

fun signal(speed: Double = 60.0, battery: Double = 80.0, temperature: Double = 20.0) =
    Signal(speed, battery, temperature, Position(52.4, 10.8))

class RulesTest {
    @Test fun `speed above limit alerts`() { assertEquals("SPEED_LIMIT", SignalRule.SpeedLimit().evaluate(signal(speed = 131.0))?.rule) }
    @Test fun `speed at limit is safe`() { assertNull(SignalRule.SpeedLimit().evaluate(signal(speed = 130.0))) }
    @Test fun `speed below limit is safe`() { assertNull(SignalRule.SpeedLimit().evaluate(signal())) }
    @Test fun `custom speed limit works`() { assertNotNull(SignalRule.SpeedLimit(50.0).evaluate(signal())) }
    @Test fun `battery below threshold alerts`() { assertEquals("LOW_BATTERY", SignalRule.LowBattery().evaluate(signal(battery = 14.9))?.rule) }
    @Test fun `battery at threshold is safe`() { assertNull(SignalRule.LowBattery().evaluate(signal(battery = 15.0))) }
    @Test fun `battery above threshold is safe`() { assertNull(SignalRule.LowBattery().evaluate(signal())) }
    @Test fun `temperature below zero alerts`() { assertEquals("ICE_WARNING", SignalRule.IceWarning().evaluate(signal(temperature = -0.1))?.rule) }
    @Test fun `temperature at zero is safe`() { assertNull(SignalRule.IceWarning().evaluate(signal(temperature = 0.0))) }
    @Test fun `temperature above zero is safe`() { assertNull(SignalRule.IceWarning().evaluate(signal())) }
    @Test fun `engine returns every matching rule`() { assertEquals(3, RulesEngine().evaluate(signal(150.0, 10.0, -5.0)).size) }
    @Test fun `engine returns empty list for safe signal`() { assertTrue(RulesEngine().evaluate(signal()).isEmpty()) }
    @Test fun `alert preserves source signal`() { val input = signal(speed = 150.0); assertEquals(input, SignalRule.SpeedLimit().evaluate(input)?.signal) }
    @Test fun `invalid rule configuration is rejected`() { assertFailsWith<IllegalArgumentException> { SignalRule.LowBattery(101.0) } }
}
