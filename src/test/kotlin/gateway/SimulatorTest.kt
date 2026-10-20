package gateway

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SimulatorTest {
    @Test fun `same seed repeats sequence`() = runTest {
        assertEquals(SignalSimulator(42, 10).signals().take(10).toList(), SignalSimulator(42, 10).signals().take(10).toList())
    }
    @Test fun `different seeds change sequence`() = runTest {
        assertNotEquals(SignalSimulator(1, 10).signals().take(3).toList(), SignalSimulator(2, 10).signals().take(3).toList())
    }
    @Test fun `generated values are valid`() = runTest {
        SignalSimulator(intervalMillis = 1).signals().take(1000).toList().forEach { assertTrue(it.validationErrors().isEmpty()) }
    }
    @Test fun `collection is cold and repeatable`() = runTest {
        val flow = SignalSimulator(intervalMillis = 10).signals()
        assertEquals(flow.take(3).toList(), flow.take(3).toList())
    }
    @Test fun `flow respects interval and stops with take`() = runTest {
        SignalSimulator(intervalMillis = 100).signals().take(3).toList()
        assertEquals(200, testScheduler.currentTime)
    }
    @Test fun `zero interval rejected`() { assertFailsWith<IllegalArgumentException> { SignalSimulator(intervalMillis = 0) } }
}
