package gateway

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.onSubscription
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ErrorResponse(val error: String)
@Serializable
data class HealthResponse(val status: String = "ok")

fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module(gateway: Gateway = Gateway(), simulate: Boolean = true) {
    install(ContentNegotiation) { json() }
    install(WebSockets)
    install(StatusPages) {
        exception<BadRequestException> { call, _ ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid JSON: supply all signal fields with numeric values"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Invalid signal"))
        }
        exception<UnsupportedMediaTypeException> { call, _ ->
            call.respond(HttpStatusCode.UnsupportedMediaType, ErrorResponse("Content-Type must be application/json"))
        }
    }
    if (simulate) {
        val simulatorJob = launch { SignalSimulator().signals().collect { gateway.accept(it) } }
        monitor.subscribe(ApplicationStopping) { simulatorJob.cancel() }
    }
    routing {
        get("/health") { call.respond(HealthResponse()) }
        get("/signals/latest") {
            val signal = gateway.repository.latest()
            if (signal == null) call.respond(HttpStatusCode.NotFound, ErrorResponse("No signal received yet"))
            else call.respond(signal)
        }
        get("/alerts") { call.respond(gateway.repository.alerts()) }
        post("/signals") {
            val signal = call.receive<Signal>()
            call.respond(HttpStatusCode.Created, gateway.accept(signal))
        }
        webSocket("/alerts/live") {
            // The ready message means collection has started before a client sends a signal.
            gateway.alerts.onSubscription { send(Frame.Text("{\"status\":\"ready\"}")) }
                .collect { send(Frame.Text(Json.encodeToString(it))) }
        }
    }
}
