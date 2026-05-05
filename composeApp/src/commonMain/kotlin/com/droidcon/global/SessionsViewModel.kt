package com.droidcon.global

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidcon.global.domain.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

sealed interface SessionsUiState {
    data object Loading : SessionsUiState
    data class Success(val sessions: List<Session>) : SessionsUiState
    data class Error(val message: String) : SessionsUiState
}

class SessionsViewModel : ViewModel() {
    private val getSessions = SharedGraph.getSessionsUseCase
    private val refreshConferenceData = SharedGraph.refreshConferenceDataUseCase

    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { refreshConferenceData() }
                .onFailure { _uiState.value = SessionsUiState.Error(it.message ?: "Refresh failed") }
        }

        viewModelScope.launch {
            getSessions()
                .onStart { _uiState.value = SessionsUiState.Loading }
                .catch { _uiState.value = SessionsUiState.Error(it.message ?: "Load failed") }
                .collect { _uiState.value = SessionsUiState.Success(it) }
        }
    }
}
