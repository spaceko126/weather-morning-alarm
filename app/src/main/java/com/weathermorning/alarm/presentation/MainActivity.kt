package com.weathermorning.alarm.presentation

import android.graphics.Color
import android.os.Bundle
import com.weathermorning.alarm.data.local.CrashLog
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        // auto() picks system-bar icon colours from the device theme, so the
        // bars stay readable in both light and dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            WeatherMorningApp()
        }
    }
}
