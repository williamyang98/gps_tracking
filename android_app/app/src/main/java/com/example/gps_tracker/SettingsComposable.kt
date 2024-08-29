package com.example.gps_tracker

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class SettingsComposable private constructor() {
    private var integerEditFields = mutableMapOf<String, String>();
    //private var stringEditFields = mutableMapOf<String, String>();

    companion object {
        @Volatile
        private var instance: SettingsComposable? = null // Volatile modifier is necessary
        fun getInstance() =
            instance ?: synchronized(this) { // synchronized to avoid concurrency problem
                instance ?: SettingsComposable().also { instance = it }
            }
    }

    @Composable
    fun Render(settings: Settings, onChange: () -> Unit = {}) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            EditInteger(
                "User ID",
                Icons.Filled.Face,
                { settings.userId },
                { v -> settings.userId = v },
                onChange
            );
            EditString(
                "User Name",
                Icons.Filled.AccountBox,
                { settings.userName },
                { v -> settings.userName = v },
                onChange
            );
            EditInteger(
                "Interval (minutes)",
                Icons.Filled.DateRange,
                { settings.interval },
                { v -> settings.interval = v },
                onChange
            )
            EditInteger(
                "Timeline Length",
                Icons.Filled.List,
                { settings.timelineLength },
                { v -> settings.timelineLength = v },
                onChange
            )
            EditString(
                "Server URL",
                Icons.Filled.Home,
                { settings.serverUrl },
                { v -> settings.serverUrl = v },
                onChange
            );
            EditBoolean(
                "Autostart",
                "Start background service on open",
                Icons.Filled.PlayArrow,
                { settings.autostart },
                { v -> settings.autostart = v },
                onChange,
            )
        }
    }

    @Composable
    private fun EditString(
        name: String,
        icon: ImageVector?,
        get: () -> String,
        set: (String) -> Unit,
        onChange: () -> Unit
    ) {
        val focusManager = LocalFocusManager.current;
        OutlinedTextField(
            singleLine = true,
            label = { Text(text = name) },
            value = get(),
            onValueChange = { set(it); onChange(); },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus();
                }
            ),
            leadingIcon = {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    @Composable
    private fun EditInteger(
        name: String,
        icon: ImageVector?,
        get: () -> Int,
        set: (Int) -> Unit,
        onChange: () -> Unit
    ) {
        val focusManager = LocalFocusManager.current;
        OutlinedTextField(
            singleLine = true,
            label = { Text(text = name) },
            value = integerEditFields[name] ?: get().toString(),
            onValueChange = {
                integerEditFields[name] = it;
                it.toIntOrNull()?.apply {
                    set(this);
                    onChange();
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus();
                }
            ),
            leadingIcon = {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    @Composable
    private fun EditBoolean(
        name: String,
        description: String?,
        icon: ImageVector?,
        get: () -> Boolean,
        set: (Boolean) -> Unit,
        onChange: () -> Unit
    ) {
        val isChecked = get();
        ListItem(
            leadingContent = {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                    )
                }
            },
            headlineContent = { Text(text = name) },
            supportingContent = { description?.let { Text(text = it) } },
            trailingContent = {
                Switch(
                    checked = isChecked,
                    onCheckedChange = {
                        set(it);
                        onChange();
                    },
                    thumbContent = if (isChecked) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }
        )
    }
}