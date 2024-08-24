package com.example.gps_tracker

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gps_tracker.ui.theme.Gps_trackerTheme



class SettingsActivity : ComponentActivity() {
    private lateinit var settings: Settings;
    private var textEditFields = mutableMapOf<String, String>();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        settings = Settings(this);
        renderView();
    }

    private fun renderView() {
        setContent {
            Gps_trackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier=Modifier.fillMaxWidth()) {
                        Text(text="Settings", style=MaterialTheme.typography.headlineSmall)
                        editInteger("User ID", { -> settings.userId }, { v -> settings.userId = v });
                        editInteger("Interval (minutes)", { -> settings.interval }, { v -> settings.interval = v })
                        editBoolean("Autostart", { -> settings.autostart }, { v -> settings.autostart = v })
                    }
                }
            }
        }
    }

    @Composable
    private fun editInteger(name: String, get: () -> Int, set: (Int) -> Unit) {
        Row {
            NameLabel(text=name)
            TextField(
                value=textEditFields.get(name) ?: get().toString(),
                onValueChange={
                    textEditFields[name] = it;
                    it.toIntOrNull()?.apply {
                        set(this);
                        renderView();
                    }
                },
                keyboardOptions=KeyboardOptions.Default.copy(
                    keyboardType=KeyboardType.Number,
                    imeAction=ImeAction.Done,
                ),
            )
        }
    }

    @Composable
    private fun editBoolean(name: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        Row {
            NameLabel(text=name)
            Checkbox(
                checked=get(),
                onCheckedChange={
                    set(it);
                    renderView();
                }
            )
        }
    }
}

@Composable
private fun RowScope.NameLabel(text: String) {
    Text(
        text=text,
        Modifier
            .fillMaxWidth(1.0f)
            .weight(1f)
            .padding(8.dp)
    )
}