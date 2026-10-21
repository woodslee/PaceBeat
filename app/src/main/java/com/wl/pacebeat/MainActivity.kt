package com.wl.pacebeat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wl.pacebeat.service.PlaybackBus
import com.wl.pacebeat.ui.HomeScreen
import com.wl.pacebeat.ui.RunScreen
import com.wl.pacebeat.ui.theme.Background
import com.wl.pacebeat.ui.theme.PaceBeatTheme
import com.wl.pacebeat.viewmodel.AppScreen
import com.wl.pacebeat.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.WHITE,
                android.graphics.Color.WHITE
            )
        )
        setContent {
            PaceBeatTheme {
                PaceBeatApp()
            }
        }
    }
}

@Composable
private fun PaceBeatApp(vm: AppViewModel = viewModel()) {
    val playback by PlaybackBus.state.collectAsStateWithLifecycle()

    BackHandler(enabled = vm.screen == AppScreen.RUN) {
        vm.backToHome()
    }

    LaunchedEffect(playback.active, playback.completed) {
        if (!playback.active && !playback.completed && vm.screen == AppScreen.RUN) {
            vm.backToHome()
        }
        if (playback.completed && vm.screen == AppScreen.HOME) {
            vm.openRun()
        }
    }

    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        when (vm.screen) {
            AppScreen.HOME -> HomeScreen(vm = vm, playback = playback)
            AppScreen.RUN -> RunScreen(vm = vm, playback = playback)
        }
    }
}
