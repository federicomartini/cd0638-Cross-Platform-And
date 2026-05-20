package com.droidcon.global

import androidx.compose.runtime.Composable

/** Platform-specific Coil network setup (required on iOS; Android uses OkHttp via Gradle). */
@Composable
expect fun CoilPlatformSetup()
