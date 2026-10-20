package gateway

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.*

class RepositoryTest {
    @Test fun `history is bounded and keeps newest alerts`() = runTest {
        val repository = InMemorySignalRepository(2)
        val gateway = Gateway(repository)
        gateway.accept(signal(150.0, 10.0, -1.0))
        assertEquals(listOf("LOW_BATTERY", "ICE_WARNING"), repository.alerts().map { it.rule })
    }
    @Test fun `latest updates even without alerts`() = runTest {
        val gateway = Gateway()
        gateway.accept(signal(speed = 150.0))
        gateway.accept(signal())
        assertEquals(signal(), gateway.repository.latest())
        assertEquals(1, gateway.repository.alerts().size)
    }
    @Test fun `non finite and out of range values are rejected`() = runTest {
        for (input in listOf(signal(speed = Double.NaN), signal(speed = -1.0), signal(temperature = 81.0))) {
            assertFailsWith<IllegalArgumentException> { Gateway().accept(input) }
        }
    }
}
