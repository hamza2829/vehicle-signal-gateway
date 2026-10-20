# Local verification

Verified on 21 October 2026 on Windows with Oracle JDK 17.0.12 and the included
Gradle 8.14.4 wrapper. These are local results, not GitHub Actions results.

The final build command was:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:GRADLE_USER_HOME = "$PWD\.gradle-home"
.\gradlew.bat clean build installDist --no-daemon --console=plain
```

[`build-output.txt`](build-output.txt) is the actual console output of that
command. It reports 33 tests, 33 passed, 0 failed, 0 skipped, and a successful
build. JUnit XML results confirm 14 rule tests, 6 simulator tests, 3 repository
tests, and 10 API tests.

The built distribution was started with Java 17 using:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -cp 'build/install/vehicle-signal-gateway/lib/*' gateway.ApplicationKt
```

The README's `gradlew.bat run --no-daemon --console=plain` command was also
started successfully, and `/health` returned 200 with `{"status":"ok"}`.
Both local verification servers were stopped after checking them.

In another terminal, curl returned:

```text
HTTP/1.1 200 OK
Content-Length: 15
Content-Type: application/json

{"status":"ok"}
```

The optional smoke script uses Node.js 22 built-in HTTP and WebSocket clients.
Node is needed only for this extra check, not for the Kotlin application or its
JUnit tests. With the service running on port 8080:

```powershell
node verification/smoke.mjs
```

It was executed with Node.js 22.19.0. Its actual output is saved in
[`local-smoke-output.txt`](local-smoke-output.txt). Assertions check health,
simulator activity, manual input, all three alerts, stored history, live
WebSocket delivery, and bad input responses.

Docker build and container execution were not verified. The user requested
local execution because of virtualization issues. These saved results describe
the local verification before publication. GitHub Actions results are available
in the repository's Actions tab after publication.
