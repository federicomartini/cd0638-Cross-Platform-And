package com.droidcon.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidcon.global.domain.model.Session
import com.droidcon.global.domain.model.Speaker
import com.droidcon.global.domain.model.SpeakerSyncState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SessionsUiState {
    data object Loading : SessionsUiState
    data class Success(
        val sessions: List<Session>,
        val speakersBySessionId: Map<String, List<Speaker>>,
        val speakerSyncState: SpeakerSyncState,
    ) : SessionsUiState
    data class Error(val message: String) : SessionsUiState
}

class SessionsViewModel : ViewModel() {
    private val getSessions = SharedGraph.getSessionsUseCase
    private val getSpeakers = SharedGraph.getSpeakersUseCase
    private val observeSpeakerSyncState = SharedGraph.observeSpeakerSyncStateUseCase
    private val refreshConferenceData = SharedGraph.refreshConferenceDataUseCase
    private val networkMonitor = createNetworkMonitor()
    private val refreshFailureMessage = MutableStateFlow<String?>(null)
    private val refreshMutex = Mutex()
    private var refreshJob: Job? = null

    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                getSessions(),
                getSpeakers(),
                observeSpeakerSyncState(),
                refreshFailureMessage,
            ) { sessions, speakers, speakerSyncState, refreshError ->
                when {
                    sessions.isNotEmpty() -> SessionsUiState.Success(
                        sessions = sessions,
                        speakersBySessionId = speakers.groupBy { it.sessionId },
                        speakerSyncState = speakerSyncState,
                    )
                    refreshError != null -> SessionsUiState.Error(refreshError)
                    else -> SessionsUiState.Loading
                }
            }
                .onStart { _uiState.value = SessionsUiState.Loading }
                .catch { error ->
                    if (error is CancellationException) throw error
                    _uiState.value = SessionsUiState.Error(error.message ?: "Load failed")
                }
                .collect { _uiState.value = it }
        }

        viewModelScope.launch {
            networkMonitor.observeConnectivity()
                .distinctUntilChanged()
                .collect { online ->
                    val hasCache = getSessions().first().isNotEmpty()
                    when {
                        hasCache -> if (online) startRefresh()
                        online -> startRefresh()
                        else -> showOfflineWithoutCache()
                    }
                }
        }
    }

    fun retry() {
        startRefresh()
    }

    private fun showOfflineWithoutCache() {
        refreshJob?.cancel()
        refreshFailureMessage.value = OFFLINE_NO_CACHE_MESSAGE
    }

    private fun startRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val hasCache = getSessions().first().isNotEmpty()
            if (!hasCache && !networkMonitor.isOnline()) {
                showOfflineWithoutCache()
                return@launch
            }
            refreshFailureMessage.value = null
            if (!hasCache) {
                _uiState.value = SessionsUiState.Loading
            }
            refreshWithRetry(hasCache = hasCache)
        }
    }

    private suspend fun refreshWithRetry(hasCache: Boolean) {
        refreshMutex.withLock {
            val maxAttempts = if (hasCache) MAX_REFRESH_ATTEMPTS else MAX_REFRESH_ATTEMPTS_NO_CACHE
            var backoffMs = REFRESH_INITIAL_BACKOFF_MS
            repeat(maxAttempts) { attempt ->
                try {
                    refreshConferenceData()
                    refreshFailureMessage.value = if (getSessions().first().isEmpty()) {
                        EMPTY_SESSIONS_MESSAGE
                    } else {
                        null
                    }
                    return@withLock
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    if (getSessions().first().isNotEmpty()) {
                        refreshFailureMessage.value = null
                        return@withLock
                    }
                    if (!networkMonitor.isOnline()) {
                        refreshFailureMessage.value = OFFLINE_NO_CACHE_MESSAGE
                        return@withLock
                    }
                    if (attempt == maxAttempts - 1) {
                        refreshFailureMessage.value = error.message ?: "Refresh failed"
                        return@withLock
                    }
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(REFRESH_MAX_BACKOFF_MS)
                }
            }
        }
    }

    private companion object {
        const val MAX_REFRESH_ATTEMPTS = 5
        const val MAX_REFRESH_ATTEMPTS_NO_CACHE = 1
        const val REFRESH_INITIAL_BACKOFF_MS = 2_000L
        const val REFRESH_MAX_BACKOFF_MS = 30_000L
        const val EMPTY_SESSIONS_MESSAGE = "No sessions available from the server"
        const val OFFLINE_NO_CACHE_MESSAGE =
            "No internet connection. The schedule will load when you are back online."
    }
}
