package com.droidcon.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidcon.global.domain.model.Session
import com.droidcon.global.domain.model.Speaker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

sealed interface SessionsUiState {
    data object Loading : SessionsUiState
    data class Success(
        val sessions: List<Session>,
        val speakersBySessionId: Map<String, List<Speaker>>
    ) : SessionsUiState
    data class Error(val message: String) : SessionsUiState
}

class SessionsViewModel : ViewModel() {
    private val getSessions = SharedGraph.getSessionsUseCase
    private val getSpeakers = SharedGraph.getSpeakersUseCase
    private val refreshConferenceData = SharedGraph.refreshConferenceDataUseCase
    private val refreshFailureMessage = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { refreshConferenceData() }
                .onFailure { refreshFailureMessage.value = it.message ?: "Refresh failed" }
        }

        viewModelScope.launch {
            combine(getSessions(), getSpeakers(), refreshFailureMessage) { sessions, speakers, refreshError ->
                when {
                    sessions.isNotEmpty() -> SessionsUiState.Success(
                        sessions = sessions,
                        speakersBySessionId = speakers.groupBy { it.sessionId }
                    )
                    refreshError != null -> SessionsUiState.Error(
                        "No network data available and no cached sessions found."
                    )
                    else -> SessionsUiState.Loading
                }
            }
                .onStart { _uiState.value = SessionsUiState.Loading }
                .catch { _uiState.value = SessionsUiState.Error(it.message ?: "Load failed") }
                .collect { _uiState.value = it }
        }
    }
}
