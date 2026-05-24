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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.random.Random

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

  /** Ensures only one in-flight request at a time to reduce mock API 429 responses. */
    private val requestMutex = Mutex()

    suspend fun getSessions(): List<SessionDto> =
        requestWithRateLimitRetry("$BASE_URL/sessions") { response ->
            parseListResponse<SessionDto>(response)
        }

    suspend fun getSpeakers(sessionId: String): List<SpeakerDto> {
        val url = "$BASE_URL/sessions/$sessionId/speakers"
        return requestWithRateLimitRetry(
            url = url,
            notFoundValue = emptyList(),
        ) { response ->
            parseListResponse<SpeakerDto>(response)
        }
    }

    private suspend fun <T> requestWithRateLimitRetry(
        url: String,
        notFoundValue: T? = null,
        parseSuccess: suspend (HttpResponse) -> T,
    ): T {
        var backoffMs = INITIAL_BACKOFF_MS
        repeat(MAX_ATTEMPTS) { attempt ->
            val response = try {
                requestMutex.withLock { client.get(url) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (attempt == MAX_ATTEMPTS - 1) {
                    throw ConferenceApiException("Network error requesting $url", error)
                }
                backoffMs = delayBeforeRetry(backoffMs, retryAfterMs = null)
                return@repeat
            }

            when {
                response.status.isSuccess() -> return parseSuccess(response)
                response.status.value == 404 && notFoundValue != null -> return notFoundValue
                response.status.value == 429 || response.status.value in RETRYABLE_SERVER_ERRORS -> {
                    if (attempt == MAX_ATTEMPTS - 1) {
                        throw rateLimitException(response, url)
                    }
                    val retryAfterMs = response.headers["Retry-After"]
                        ?.toLongOrNull()
                        ?.times(1_000L)
                    backoffMs = delayBeforeRetry(backoffMs, retryAfterMs)
                }
                else -> throw ConferenceApiException(
                    "Request failed (${response.status.value} ${response.status.description}) for $url",
                )
            }
        }
        throw ConferenceApiException("Rate limit retries exhausted for $url")
    }

    private suspend fun delayBeforeRetry(currentBackoffMs: Long, retryAfterMs: Long?): Long {
        delay((retryAfterMs ?: currentBackoffMs) + Random.nextLong(0, JITTER_MS))
        return (currentBackoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
    }

    private fun rateLimitException(response: HttpResponse, url: String): ConferenceApiException =
        ConferenceApiException(
            "Request failed (${response.status.value} ${response.status.description}) for $url",
        )

    private suspend inline fun <reified T> parseListResponse(response: HttpResponse): List<T> {
        val url = response.call.request.url.toString()
        if (!response.status.isSuccess()) {
            throw ConferenceApiException(
                "Request failed (${response.status.value} ${response.status.description}) for $url",
            )
        }

        val payload = response.bodyAsText().trim()
        if (!payload.startsWith("[")) {
            val preview = payload.take(120).ifEmpty { "<empty body>" }
            throw ConferenceApiException(
                "Expected JSON array from $url, got: $preview",
            )
        }

        return try {
            parser.decodeFromString<List<T>>(payload)
        } catch (e: SerializationException) {
            throw ConferenceApiException(
                "Failed to parse JSON array from $url",
                e,
            )
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
        const val INITIAL_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 20_000L
        const val JITTER_MS = 500L
        val RETRYABLE_SERVER_ERRORS = 500..599
    }
}
