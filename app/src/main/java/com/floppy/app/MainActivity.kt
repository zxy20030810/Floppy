package com.floppy.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.floppy.app.playback.ExoPlaybackController
import com.floppy.app.ui.FloppyApp
import com.floppy.app.ui.FloppyViewModel
import com.floppy.app.ui.theme.FloppyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        // 让内容延伸到系统栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // 状态栏/导航栏透明，并关闭系统强加的半透明 scrim（白底的来源）
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            // 深色背景下图标用浅色
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        val app = application as FloppyApplication
        val playbackController = ExoPlaybackController(this)
        setContent {
            FloppyTheme {
                val viewModel: FloppyViewModel = viewModel(
                    factory = FloppyViewModel.Factory(app.repository, playbackController)
                )
                FloppyApp(viewModel = viewModel)
            }
        }
    }
}
