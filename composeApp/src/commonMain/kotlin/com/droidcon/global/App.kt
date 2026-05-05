package com.droidcon.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.droidcon.global.domain.model.Session

@Composable
@Preview
fun App(isExpandedLayout: Boolean = false) {
    val vm: SessionsViewModel = viewModel { SessionsViewModel() }
    val state by vm.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val current = state) {
                is SessionsUiState.Loading -> LoadingState()
                is SessionsUiState.Error -> ErrorState(current.message)
                is SessionsUiState.Success -> {
                    if (isExpandedLayout) {
                        ExpandedSessionsLayout(current.sessions)
                    } else {
                        CompactSessionsLayout(current.sessions)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text("Unable to load sessions", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CompactSessionsLayout(sessions: List<Session>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sessions) { session ->
            SessionRow(session = session, onClick = {})
        }
    }
}

@Composable
private fun ExpandedSessionsLayout(sessions: List<Session>) {
    var selected by remember(sessions) { mutableStateOf(sessions.firstOrNull()) }
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(sessions) { session ->
                SessionRow(
                    session = session,
                    onClick = { selected = session }
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            Text("Selected session", style = MaterialTheme.typography.titleMedium)
            Text(selected?.title ?: "No session")
            Text(selected?.description ?: "")
        }
    }
}

@Composable
private fun SessionRow(session: Session, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(session.title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = session.description,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}