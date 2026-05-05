package com.droidcon.global.data.remote

import com.droidcon.global.data.remote.model.SpeakerDto
import com.droidcon.global.data.remote.model.SessionDto
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.decodeFromString
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
        parseListSafely(client.get("$BASE_URL/sessions").bodyAsText())

    suspend fun getSpeakers(sessionId: String): List<SpeakerDto> =
        parseListSafely(client.get("$BASE_URL/sessions/$sessionId/speakers").bodyAsText())

    private inline fun <reified T> parseListSafely(rawPayload: String): List<T> {
        val payload = rawPayload.trim()
        if (!payload.startsWith("[")) return emptyList()
        return runCatching { parser.decodeFromString<List<T>>(payload) }.getOrElse { emptyList() }
    }
}
