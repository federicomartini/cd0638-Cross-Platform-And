package com.droidcon.global.data.remote

import com.droidcon.global.data.remote.model.SpeakerDto
import com.droidcon.global.data.remote.model.SessionDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://694e2d80b5bc648a93bf8dd0.mockapi.io/api/v1"

class ConferenceApi {
    private val parser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(parser)
        }
    }

    suspend fun getSessions(): List<SessionDto> =
        parseListResponse(client.get("$BASE_URL/sessions"))

    suspend fun getSpeakers(sessionId: String): List<SpeakerDto> {
        val url = "$BASE_URL/sessions/$sessionId/speakers"
        repeat(4) { attempt ->
            val response = client.get(url)
            when {
                response.status.value == 404 -> return emptyList()
                response.status.isSuccess() -> return parseListResponse(response)
                response.status.value == 429 && attempt < 3 -> delay(400L * (attempt + 1))
                else -> throw ConferenceApiException(
                    "Request failed (${response.status.value} ${response.status.description}) for $url"
                )
            }
        }
        return emptyList()
    }

    private suspend inline fun <reified T> parseListResponse(response: HttpResponse): List<T> {
        val url = response.call.request.url.toString()
        if (!response.status.isSuccess()) {
            throw ConferenceApiException(
                "Request failed (${response.status.value} ${response.status.description}) for $url"
            )
        }

        val payload = response.bodyAsText().trim()
        if (!payload.startsWith("[")) {
            val preview = payload.take(120).ifEmpty { "<empty body>" }
            throw ConferenceApiException(
                "Expected JSON array from $url, got: $preview"
            )
        }

        return try {
            parser.decodeFromString<List<T>>(payload)
        } catch (e: SerializationException) {
            throw ConferenceApiException(
                "Failed to parse JSON array from $url",
                e
            )
        }
    }
}
