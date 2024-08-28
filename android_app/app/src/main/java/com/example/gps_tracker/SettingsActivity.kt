package com.example.gps_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gps_tracker.ui.theme.Gps_trackerTheme


class SettingsActivity : ComponentActivity() {
    private lateinit var settings: Settings;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        settings = Settings(this);
        renderView();
    }

    private fun renderView() {
        val settingsComposable = SettingsComposable.getInstance();
        setContent {
            Gps_trackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    settingsComposable.Render(settings, onChange = { renderView() });
                }
            }
        }
    }

}