package com.example.gps_tracker

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.example.gps_tracker.ui.theme.Gps_trackerTheme



class SettingsActivity : ComponentActivity() {
    private lateinit var pref: SharedPreferences;
    private var textEditFields = mutableMapOf<String, String>();

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        pref = PreferenceManager.getDefaultSharedPreferences(applicationContext);
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
                        editInteger("User ID", "user_id")
                        editBoolean("Autostart", "autostart")
                    }
                }
            }
        }
    }

    private fun initTextEditField(field: String) {
        textEditFields.put(field, pref.getInt(field, 0).toString());
    }

    @Composable
    private fun editInteger(name: String, field: String) {
        Row {
            Text(text=name)
            Spacer(Modifier.weight(1f))
            TextField(
                value=textEditFields.get(field) ?: pref.getInt(field, 0).toString(),
                onValueChange={
                    textEditFields[field] = it;
                    it.toIntOrNull()?.apply {
                        pref.edit().putInt(field, this).apply();
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
    private fun editBoolean(name: String, field: String) {
        Row {
            Text(text=name)
            Spacer(Modifier.weight(1f))
            Checkbox(
                checked=pref.getBoolean(field, false),
                onCheckedChange={
                    pref.edit().putBoolean(field, it).apply();
                    renderView();
                }
            )
        }
    }
}