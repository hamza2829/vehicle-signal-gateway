# vehicle-signal-gateway

A small Kotlin learning project for connected vehicle signals. A Ktor service
receives speed, battery level, outside temperature, and GPS coordinates. It
checks each signal against three rules, stores the latest signal and recent
alerts, and streams new alerts over WebSocket.

This is an independent learning project. It has no connection to Volkswagen
systems or real vehicles.

## Stack

Kotlin on JVM 17, Gradle Kotlin DSL, Ktor, kotlinx.serialization, coroutines,
Flow, JUnit 5, and the Ktor test host. Dependency versions are pinned in
`build.gradle.kts`. The Gradle wrapper is included.

## Run locally

Install a JDK 17 and set `JAVA_HOME` to its directory. The first Gradle run
needs internet access to download Gradle and dependencies. No Docker is needed.

On Windows PowerShell:

```powershell
.\gradlew.bat run
```

On Linux or macOS:

```sh
bash ./gradlew run
```

The service listens on port 8080. Set the `PORT` environment variable to use
another port. Stop it with Ctrl+C. It binds to `0.0.0.0`; use a trusted local
environment because this demo has no authentication.

The simulator starts automatically. It emits the first signal immediately,
then one per second. Every collection starts a new `Random(42)` sequence.
Manual signals and simulated signals use the same validation, rules, and
repository. The next simulated signal can replace a manually submitted signal
as the latest value.

## API examples

These examples use curl. In Windows PowerShell, use `curl.exe` to avoid the
Windows PowerShell curl alias. All responses use JSON.

```sh
curl http://localhost:8080/health
curl http://localhost:8080/signals/latest
curl http://localhost:8080/alerts
curl -i -X POST http://localhost:8080/signals \
  -H 'Content-Type: application/json' \
  -d '{"speedKmh":145,"batteryPercent":10,"outsideTemperatureC":-2,"position":{"latitude":52.42,"longitude":10.79}}'
```

For a manual POST in PowerShell:

```powershell
$body = '{"speedKmh":145,"batteryPercent":10,"outsideTemperatureC":-2,"position":{"latitude":52.42,"longitude":10.79}}'
Invoke-RestMethod http://localhost:8080/signals -Method Post -ContentType application/json -Body $body
```

| Endpoint | Result |
| --- | --- |
| `GET /health` | 200 with `{"status":"ok"}` |
| `GET /signals/latest` | 200 with the most recently accepted signal; 404 before the first signal |
| `GET /alerts` | 200 with an array of stored alerts, oldest first |
| `POST /signals` | 201 with an array of alerts for this signal; an empty array if no rules match |
| `GET /alerts/live` | WebSocket upgrade for new alerts |

A signal is one complete snapshot. All five numeric values are required.
Speed must be 0 to 300 km/h, battery 0 to 100 percent, temperature -80 to
80 degrees C, latitude -90 to 90, and longitude -180 to 180. Non-finite values,
missing fields, wrong types, malformed JSON, and unknown JSON fields are
rejected. Validation bounds include both endpoints.

Invalid JSON or invalid values return 400 with an `error` field. For example:

```json
{"error":"batteryPercent must be between 0 and 100"}
```

A POST with the wrong content type returns 415. Rejected signals do not change
the repository or produce alerts.

## Rules and live stream

| Rule | Trigger |
| --- | --- |
| `SPEED_LIMIT` | Speed strictly above 130 km/h |
| `LOW_BATTERY` | Battery strictly below 15 percent |
| `ICE_WARNING` | Outside temperature strictly below 0 degrees C |

The thresholds are constructor parameters in the sealed `SignalRule` class.
Each matching rule returns an `Alert` data class with its code, message, and
source signal. A non-matching rule returns null. `RulesEngine` combines the
matching alerts in rule order.

Connect a WebSocket client to `ws://localhost:8080/alerts/live`. The first
frame is `{"status":"ready"}`. It confirms that the subscription is active.
Each later text frame is one JSON alert. For example, run this in a browser
developer console while the service is running:

```js
const socket = new WebSocket('ws://localhost:8080/alerts/live');
socket.onmessage = event => console.log(JSON.parse(event.data));
// When finished: socket.close();
```

The stream sends future alerts only. Use `GET /alerts` for stored history.
The gateway serializes acceptance with a coroutine Mutex. The repository uses
its own Mutex for atomic reads and writes. A SharedFlow broadcasts alerts with
a 64-alert buffer; a slow consumer can miss older events. No subscriber is
required for signal processing to continue.

## Tests and build

```powershell
.\gradlew.bat clean build
```

```sh
bash ./gradlew clean build
```

To run only tests, replace `clean build` with `test`. The test report is
`build/reports/tests/test/index.html`, and JUnit XML files are in
`build/test-results/test/`.

The tests cover every rule and its boundary, repeatable simulator sequences,
virtual-time emission and cancellation, repository retention, input validation,
API success and failure responses, and live WebSocket alert delivery. API tests
disable the simulator so manual input can be checked without background writes.

On 21 October 2026, `clean build installDist` passed all 33 tests on local
Java 17. The running service also passed real HTTP and WebSocket checks.
Actual output and the commands used are in [`verification/`](verification/README.md).

GitHub Actions is configured to build and test on each push and pull request
with JDK 17. The workflow has not been run on GitHub yet.

## Docker

A multi-stage Dockerfile is provided. Its build stage runs tests and creates an
application distribution, and its runtime stage uses a Java 17 JRE and a
non-root user. Docker build and container execution have not been verified.
Local Java execution is the verified setup. Docker verification was skipped at
the user's request because of local virtualization issues.

## Project structure

```text
src/main/kotlin/gateway/
  Application.kt       Ktor routes, JSON, errors, WebSocket, simulator lifecycle
  Signal.kt            Signal models and validation
  Rules.kt             Sealed rules, alerts, rules engine
  SignalSimulator.kt   Seeded cold Flow
  Repository.kt        Storage interface and bounded in-memory implementation
  Gateway.kt           Validation, evaluation, storage, and alert broadcast
src/test/kotlin/gateway/
  RulesTest.kt         Rule and boundary tests
  SimulatorTest.kt     Seed, ranges, timing, cancellation tests
  RepositoryTest.kt    Retention and service validation tests
  ApiTest.kt           REST and WebSocket integration tests
.github/workflows/ci.yml
Dockerfile
```

## Limitations

- All generated data is simulated. There is no vehicle, CAN bus, or production
  telemetry integration.
- Storage is in memory. Restarting loses all data. Only the latest signal and
  the newest 1000 alerts are retained.
- Alerts are emitted for every matching signal. There is no deduplication,
  acknowledgement, timestamp, vehicle ID, or durable event replay.
- The service has no authentication, TLS termination, rate limiting, or
  production monitoring. `/health` is a liveness check.
- Slow live clients can lose events. Reconnecting does not replay events.
- This project is for learning. It is not suitable for vehicle safety decisions.

## License

MIT. See `LICENSE`.
