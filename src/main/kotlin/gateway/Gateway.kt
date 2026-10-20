package gateway

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Gateway(
    val repository: SignalRepository = InMemorySignalRepository(),
    private val engine: RulesEngine = RulesEngine(),
) {
    private val mutex = Mutex()
    private val events = MutableSharedFlow<Alert>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val alerts = events.asSharedFlow()

    suspend fun accept(signal: Signal): List<Alert> = mutex.withLock {
        val errors = signal.validationErrors()
        require(errors.isEmpty()) { errors.joinToString("; ") }
        val alerts = engine.evaluate(signal)
        repository.save(signal, alerts)
        alerts.forEach { events.emit(it) }
        alerts
    }
}
