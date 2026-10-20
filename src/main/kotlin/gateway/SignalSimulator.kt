package gateway

import kotlin.random.Random
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SignalSimulator(private val seed: Int = 42, private val intervalMillis: Long = 1000) {
    init { require(intervalMillis > 0) }

    fun signals(): Flow<Signal> = flow {
        val random = Random(seed)
        while (currentCoroutineContext().isActive) {
            emit(Signal(
                random.nextDouble(0.0, 180.0),
                random.nextDouble(0.0, 100.0),
                random.nextDouble(-15.0, 40.0),
                Position(random.nextDouble(48.0, 53.0), random.nextDouble(7.0, 14.0)),
            ))
            delay(intervalMillis)
        }
    }
}
