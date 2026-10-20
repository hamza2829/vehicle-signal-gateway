package gateway

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface SignalRepository {
    suspend fun save(signal: Signal, alerts: List<Alert>)
    suspend fun latest(): Signal?
    suspend fun alerts(): List<Alert>
}

class InMemorySignalRepository(private val alertCapacity: Int = 1000) : SignalRepository {
    init { require(alertCapacity > 0) }
    private val mutex = Mutex()
    private var latestSignal: Signal? = null
    private val history = ArrayDeque<Alert>()

    override suspend fun save(signal: Signal, alerts: List<Alert>) = mutex.withLock {
        latestSignal = signal
        alerts.forEach {
            history.addLast(it)
            while (history.size > alertCapacity) history.removeFirst()
        }
    }
    override suspend fun latest(): Signal? = mutex.withLock { latestSignal }
    override suspend fun alerts(): List<Alert> = mutex.withLock { history.toList() }
}
