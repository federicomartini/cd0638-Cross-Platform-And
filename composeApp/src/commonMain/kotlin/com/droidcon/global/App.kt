package com.droidcon.global

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.droidcon.global.domain.model.Speaker
import com.droidcon.global.domain.model.SpeakerLoadState
import com.droidcon.global.domain.model.SpeakerSyncState
import org.jetbrains.compose.ui.tooling.preview.Preview
import com.droidcon.global.domain.model.Session

/** Row padding (16) + time column (108) + spacer (12) — keep in sync with SessionRow. */
private val ScheduleListDividerStart = 136.dp

/** Matches Material3 [WindowWidthSizeClass.Medium] lower bound (same as Android calculateWindowSizeClass). */
private val ExpandedLayoutMinWidth = 600.dp

@Composable
@Preview
fun App(isExpandedLayout: Boolean? = null) {
    CoilPlatformSetup()
    val vm: SessionsViewModel = viewModel { SessionsViewModel() }
    val state by vm.uiState.collectAsStateWithLifecycle()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useExpandedLayout = isExpandedLayout ?: (maxWidth >= ExpandedLayoutMinWidth)
                when (val current = state) {
                is SessionsUiState.Loading -> LoadingState()
                is SessionsUiState.Error -> ErrorState(current.message)
                is SessionsUiState.Success -> {
                    var selectedSessionId by rememberSaveable { mutableStateOf<String?>(null) }
                    var showDetail by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(current.sessions, selectedSessionId) {
                        if (current.sessions.isEmpty()) return@LaunchedEffect
                        if (selectedSessionId != null &&
                            current.sessions.none { it.id == selectedSessionId }
                        ) {
                            selectedSessionId = null
                            showDetail = false
                        }
                    }

                    if (useExpandedLayout) {
                        ExpandedSessionsLayout(
                            sessions = current.sessions,
                            speakersBySessionId = current.speakersBySessionId,
                            speakerSyncState = current.speakerSyncState,
                            selectedSessionId = selectedSessionId,
                            onSessionSelected = { selectedSessionId = it }
                        )
                    } else {
                        CompactSessionsLayout(
                            sessions = current.sessions,
                            speakersBySessionId = current.speakersBySessionId,
                            speakerSyncState = current.speakerSyncState,
                            selectedSessionId = selectedSessionId,
                            showDetail = showDetail,
                            onSessionSelected = { sessionId ->
                                selectedSessionId = sessionId
                                showDetail = true
                            },
                            onDismissDetail = {
                                showDetail = false
                                selectedSessionId = null
                            }
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Text("Unable to load sessions", style = MaterialTheme.typography.titleMedium)
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun CompactSessionsLayout(
    sessions: List<Session>,
    speakersBySessionId: Map<String, List<Speaker>>,
    speakerSyncState: SpeakerSyncState,
    selectedSessionId: String?,
    showDetail: Boolean,
    onSessionSelected: (String) -> Unit,
    onDismissDetail: () -> Unit
) {
    LaunchedEffect(showDetail, selectedSessionId, sessions) {
        if (!showDetail) return@LaunchedEffect
        if (selectedSessionId == null || sessions.none { it.id == selectedSessionId }) {
            onDismissDetail()
        }
    }

    if (showDetail) {
        val session = sessions.firstOrNull { it.id == selectedSessionId }
        if (session != null) {
            PlatformBackHandler(enabled = true) { onDismissDetail() }
            SessionDetail(
                session = session,
                speakers = speakersBySessionId[session.id].orEmpty(),
                speakerLoadState = speakerSyncState.loadStateBySessionId[session.id],
                speakerRefreshInProgress = speakerSyncState.refreshInProgress,
                onBack = onDismissDetail
            )
        }
    } else {
        ScheduleSessionList(
            sessions = sessions,
            speakersBySessionId = speakersBySessionId,
            speakerSyncState = speakerSyncState,
            onSessionClick = { onSessionSelected(it.id) },
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .displayCutoutPadding()
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun ExpandedSessionsLayout(
    sessions: List<Session>,
    speakersBySessionId: Map<String, List<Speaker>>,
    speakerSyncState: SpeakerSyncState,
    selectedSessionId: String?,
    onSessionSelected: (String) -> Unit
) {
    LaunchedEffect(sessions, selectedSessionId) {
        if (sessions.isEmpty()) return@LaunchedEffect
        if (selectedSessionId == null || sessions.none { it.id == selectedSessionId }) {
            onSessionSelected(sessions.first().id)
        }
    }

    val selected = sessions.firstOrNull { it.id == selectedSessionId } ?: sessions.firstOrNull()
    val selectedSpeakers = selected?.let { speakersBySessionId[it.id].orEmpty() }.orEmpty()
    Row(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .displayCutoutPadding()
            .navigationBarsPadding()
    ) {
        ScheduleSessionList(
            sessions = sessions,
            speakersBySessionId = speakersBySessionId,
            speakerSyncState = speakerSyncState,
            onSessionClick = { onSessionSelected(it.id) },
            modifier = Modifier.weight(1f).fillMaxHeight()
        )
        VerticalDivider(modifier = Modifier.fillMaxHeight())
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (selected != null) {
                SessionDetail(
                    session = selected,
                    speakers = selectedSpeakers,
                    speakerLoadState = speakerSyncState.loadStateBySessionId[selected.id],
                    speakerRefreshInProgress = speakerSyncState.refreshInProgress,
                    onBack = null,
                )
            } else {
                Text("No session", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ScheduleSessionList(
    sessions: List<Session>,
    speakersBySessionId: Map<String, List<Speaker>>,
    speakerSyncState: SpeakerSyncState,
    onSessionClick: (Session) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(
            count = sessions.size + 1,
            key = { index ->
                if (index == 0) "schedule_header" else sessions[index - 1].id
            }
        ) { index ->
            if (index == 0) {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                val sessionIndex = index - 1
                val session = sessions[sessionIndex]
                Column(modifier = Modifier.fillMaxWidth()) {
                    SessionRow(
                        session = session,
                        speakers = speakersBySessionId[session.id].orEmpty(),
                        speakerLoadState = speakerSyncState.loadStateBySessionId[session.id],
                        speakerRefreshInProgress = speakerSyncState.refreshInProgress,
                        onClick = { onSessionClick(session) }
                    )
                    if (sessionIndex < sessions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = ScheduleListDividerStart, end = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: Session,
    speakers: List<Speaker>,
    speakerLoadState: SpeakerLoadState?,
    speakerRefreshInProgress: Boolean,
    onClick: () -> Unit,
) {
    val timeLines = formatSessionTimeRangeForListLines(session)
    val room = session.room.trim()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.width(108.dp),
            horizontalAlignment = Alignment.End
        ) {
            if (timeLines.isNotEmpty()) {
                timeLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (session.isServiceSession) {
                Text(
                    text = "Break · service",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (room.isNotBlank()) {
                Text(
                    text = room,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = session.description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatSessionSpeakersListLine(
                    speakers = speakers,
                    loadState = speakerLoadState,
                    refreshInProgress = speakerRefreshInProgress,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = if (speakerLoadState == SpeakerLoadState.Pending ||
                    speakerLoadState == SpeakerLoadState.Loading ||
                    (speakers.isEmpty() && speakerRefreshInProgress && speakerLoadState == null)
                ) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun SessionDetail(
    session: Session,
    speakers: List<Speaker>,
    speakerLoadState: SpeakerLoadState?,
    speakerRefreshInProgress: Boolean,
    onBack: (() -> Unit)?,
) {
    val scrollState = rememberScrollState()
    val isPhoneFullscreen = onBack != null
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .then(
                if (isPhoneFullscreen) {
                    Modifier
                        .statusBarsPadding()
                        .displayCutoutPadding()
                        .navigationBarsPadding()
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back"
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
        }
        if (session.isServiceSession) {
            Text(
                text = "Break · service session",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        Text(
            text = session.title,
            style = MaterialTheme.typography.headlineSmall
        )
        val timeLabel = formatSessionTimeRangeForDetail(session)
        if (timeLabel.isNotBlank()) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 4
            )
        }
        Text(
            text = if (session.room.isBlank()) {
                "Room to be announced"
            } else {
                "Room: ${session.room}"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = session.description,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "Speakers",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        if (speakers.isEmpty()) {
            val speakersMessage = formatSessionSpeakersDetailMessage(
                speakers = speakers,
                loadState = speakerLoadState,
                refreshInProgress = speakerRefreshInProgress,
            )
            if (speakersMessage != null) {
                Text(speakersMessage, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            speakers.forEach { speaker ->
                SpeakerDetails(speaker)
            }
        }
    }
}

@Composable
private fun SpeakerDetails(speaker: Speaker) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        AvatarImage(avatarUrl = speaker.avatar, fallbackSeed = speaker.name)
        Spacer(modifier = Modifier.size(8.dp))
        Text(speaker.name, style = MaterialTheme.typography.titleSmall)
        if (speaker.company.isNotBlank()) {
            Text(speaker.company, style = MaterialTheme.typography.bodyMedium)
        }
        if (speaker.bio.isNotBlank()) {
            Text(speaker.bio, style = MaterialTheme.typography.bodySmall)
        }
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