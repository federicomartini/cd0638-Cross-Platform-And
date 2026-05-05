package com.droidcon.global

import androidx.compose.ui.window.ComposeUIViewController
import com.droidcon.global.data.local.createIosDatabase
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    SharedGraph.init(createIosDatabase())
    return ComposeUIViewController {
        App()
    }
}