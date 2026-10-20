package gateway

import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.*

class ApiTest {
    @Test fun `health is ok`() = testApplication {
        application { module(simulate = false) }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"status\":\"ok\"}", response.bodyAsText())
    }
    @Test fun `latest is missing before first signal`() = testApplication {
        application { module(simulate = false) }
        assertEquals(HttpStatusCode.NotFound, client.get("/signals/latest").status)
    }
    @Test fun `alerts initially empty`() = testApplication {
        application { module(simulate = false) }
        assertEquals("[]", client.get("/alerts").bodyAsText())
    }
    @Test fun `posted signal becomes latest and saves alerts`() = testApplication {
        application { module(simulate = false) }
        val input = signal(150.0, 10.0, -2.0)
        val response = client.post("/signals") { contentType(ContentType.Application.Json); setBody(Json.encodeToString(input)) }
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(3, Json.decodeFromString<List<Alert>>(response.bodyAsText()).size)
        assertEquals(input, Json.decodeFromString<Signal>(client.get("/signals/latest").bodyAsText()))
        assertEquals(3, Json.decodeFromString<List<Alert>>(client.get("/alerts").bodyAsText()).size)
    }
    @Test fun `malformed json gets clear error`() = testApplication {
        application { module(simulate = false) }
        val response = client.post("/signals") { contentType(ContentType.Application.Json); setBody("{") }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid JSON"))
    }
    @Test fun `missing fields rejected`() = testApplication {
        application { module(simulate = false) }
        assertEquals(HttpStatusCode.BadRequest, client.post("/signals") { contentType(ContentType.Application.Json); setBody("{}") }.status)
    }
    @Test fun `out of range battery rejected without saving`() = testApplication {
        application { module(simulate = false) }
        val response = client.post("/signals") { contentType(ContentType.Application.Json); setBody(Json.encodeToString(signal(battery = 101.0))) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("batteryPercent"))
        assertEquals(HttpStatusCode.NotFound, client.get("/signals/latest").status)
    }
    @Test fun `invalid gps rejected`() = testApplication {
        application { module(simulate = false) }
        val response = client.post("/signals") { contentType(ContentType.Application.Json); setBody(Json.encodeToString(signal().copy(position = Position(91.0, 181.0)))) }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("latitude"))
        assertTrue(response.bodyAsText().contains("longitude"))
    }
    @Test fun `wrong content type rejected`() = testApplication {
        application { module(simulate = false) }
        assertEquals(HttpStatusCode.UnsupportedMediaType, client.post("/signals") { contentType(ContentType.Text.Plain); setBody("hello") }.status)
    }
    @Test fun `websocket streams new alert`() = testApplication {
        application { module(simulate = false) }
        val wsClient = createClient { install(WebSockets) }
        wsClient.webSocket("/alerts/live") {
            withTimeout(5000) {
                assertEquals("{\"status\":\"ready\"}", (incoming.receive() as Frame.Text).readText())
                client.post("/signals") { contentType(ContentType.Application.Json); setBody(Json.encodeToString(signal(speed = 150.0))) }
                val alert = Json.decodeFromString<Alert>((incoming.receive() as Frame.Text).readText())
                assertEquals("SPEED_LIMIT", alert.rule)
            }
            close()
        }
    }
}
