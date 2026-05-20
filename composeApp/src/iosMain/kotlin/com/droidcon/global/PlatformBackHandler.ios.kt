package com.droidcon.global

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS navigation is typically driven by navigation stack / gestures, not this API.
}
