package com.droidcon.global

import androidx.compose.runtime.Composable

/** Maps the Android system back button / predictive back to app navigation. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
