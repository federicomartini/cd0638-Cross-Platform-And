package com.droidcon.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.droidcon.global.domain.model.Speaker
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
                        ExpandedSessionsLayout(current.sessions, current.speakersBySessionId)
                    } else {
                        CompactSessionsLayout(current.sessions, current.speakersBySessionId)
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
private fun CompactSessionsLayout(
    sessions: List<Session>,
    speakersBySessionId: Map<String, List<Speaker>>
) {
    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDetail by rememberSaveable { mutableStateOf(false) }

    if (showDetail) {
        val session = sessions.firstOrNull { it.id == selectedSessionId }
        if (session == null) {
            showDetail = false
        } else {
            SessionDetail(
                session = session,
                speakers = speakersBySessionId[session.id].orEmpty(),
                onBack = { showDetail = false }
            )
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sessions) { session ->
                SessionRow(
                    session = session,
                    speakers = speakersBySessionId[session.id].orEmpty(),
                    onClick = {
                        selectedSessionId = session.id
                        showDetail = true
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpandedSessionsLayout(
    sessions: List<Session>,
    speakersBySessionId: Map<String, List<Speaker>>
) {
    var selectedSessionId by rememberSaveable { mutableStateOf(sessions.firstOrNull()?.id) }
    val selected = sessions.firstOrNull { it.id == selectedSessionId } ?: sessions.firstOrNull()
    val selectedSpeakers = selected?.let { speakersBySessionId[it.id].orEmpty() }.orEmpty()
    Row(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(sessions) { session ->
                SessionRow(
                    session = session,
                    speakers = speakersBySessionId[session.id].orEmpty(),
                    onClick = { selectedSessionId = session.id }
                )
            }
        }
        Box(modifier = Modifier.weight(1f).padding(16.dp)) {
            if (selected != null) {
                SessionDetail(session = selected, speakers = selectedSpeakers, onBack = null)
            } else {
                Text("No session", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session, speakers: List<Speaker>, onClick: () -> Unit) {
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
        Text(
            text = if (speakers.isEmpty()) "Speakers: not available" else "Speakers: ${speakers.joinToString { it.name }}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SessionDetail(session: Session, speakers: List<Speaker>, onBack: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        if (onBack != null) {
            Button(onClick = onBack) { Text("Back") }
            Spacer(modifier = Modifier.size(8.dp))
        }
        Text("Selected session", style = MaterialTheme.typography.titleMedium)
        Text(session.title)
        Text(session.description)
        Text("Room: ${session.room}")
        Text("Session ID: ${session.id}", style = MaterialTheme.typography.bodySmall)
        Text("Speakers:", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
        if (speakers.isEmpty()) {
            Text("No speakers available", style = MaterialTheme.typography.bodySmall)
        } else {
            speakers.forEach { speaker ->
                SpeakerDetails(speaker)
            }
        }
    }
}

@Composable
private fun SpeakerDetails(speaker: Speaker) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        AvatarImage(avatarUrl = speaker.avatar, fallbackSeed = speaker.name)
        Text("• Name: ${speaker.name}", style = MaterialTheme.typography.bodySmall)
        Text("  Company: ${speaker.company}", style = MaterialTheme.typography.bodySmall)
        Text(
            "  Bio: ${speaker.bio}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "  Avatar: ${speaker.avatar}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text("  Speaker ID: ${speaker.id}", style = MaterialTheme.typography.bodySmall)
        Text("  Session ID: ${speaker.sessionId}", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AvatarImage(avatarUrl: String, fallbackSeed: String) {
    if (avatarUrl.isBlank()) {
        DummyAvatar(fallbackSeed)
    } else {
        SubcomposeAsyncImage(
            model = avatarUrl,
            contentDescription = "Speaker avatar",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            loading = { DummyAvatar(fallbackSeed) },
            error = { DummyAvatar(fallbackSeed) }
        )
    }
}

@Composable
private fun DummyAvatar(seed: String) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        Text(
            text = seed.take(1).uppercase(),
            modifier = Modifier.padding(18.dp),
            color = Color.White
        )
    }
}