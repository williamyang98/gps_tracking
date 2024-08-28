package com.example.gps_tracker

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.gps_tracker.ui.theme.Gps_trackerTheme

private const val TAG: String = "main_activity";

class MainActivity : ComponentActivity() {
    private lateinit var gpsSenderContext: GpsSenderContext;
    private lateinit var settings: Settings;
    private var gpsService: BackgroundService? = null;

    private val gpsServiceConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName, ibinder: IBinder) {
            val binder = ibinder as BackgroundService.LocalBinder;
            gpsService = binder.getService();
            renderView();
            Toast.makeText(applicationContext, "Bound to gps service", Toast.LENGTH_SHORT).show();
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            gpsService = null;
            renderView();
            Toast.makeText(applicationContext, "Unbounded from gps service", Toast.LENGTH_SHORT).show();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        gpsSenderContext = GpsSenderContext(this);
        settings = Settings(this);
        if (settings.autostart) {
            BackgroundService.start(this);
        }
        this.renderView();
    }

    override fun onDestroy() {
        super.onDestroy();
    }

    override fun onStart() {
        super.onStart();
        Intent(this, BackgroundService::class.java).also {
            bindService(it, gpsServiceConnection, Context.BIND_FOREGROUND_SERVICE)
        }
    }

    override fun onStop() {
        super.onStop();
        unbindService(gpsServiceConnection);
    }

    private fun renderView() {
        val self = this;
        setContent {
            Gps_trackerTheme {
                // A surface container using the 'background' color from the theme
                Scaffold(
                    bottomBar={
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.Settings, contentDescription="Settings") },
                                label = { Text(text="Settings") },
                                selected = true,
                                onClick = {  }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.List, contentDescription="Measurements") },
                                label = { Text(text="Measurements") },
                                selected = false,
                                onClick = {  }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.LocationOn, contentDescription="Location") },
                                label = { Text(text="Location") },
                                selected = false,
                                onClick = {  }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Outlined.Lock, contentDescription="Permissions") },
                                label = { Text(text="Permissions") },
                                selected = false,
                                onClick = {  }
                            )
                        }
                    }
                ) { _it ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(1.dp)
                    ) {
                        Row {
                            Text(text="Background Service", style=MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { renderView() }) {
                                Icon(Icons.Outlined.Refresh, contentDescription="Refresh")
                            }
                            IconButton(
                                onClick = {
                                    val intent = Intent(self, SettingsActivity::class.java);
                                    self.startActivity(intent)
                                }
                            ) {
                                Icon(Icons.Outlined.Settings, contentDescription="Settings")
                            }
                        }
                        Row {
                            RadioButton(
                                selected=(BackgroundService.getIsStarted(self)),
                                onClick={
                                    if (!BackgroundService.getIsStarted(self)) {
                                        BackgroundService.start(self);
                                        self.renderView();
                                    } else {
                                        BackgroundService.stop(self);
                                        self.renderView();
                                    }
                                },
                            )
                            Text(text="Service repeating background scheduled")
                        }
                        Row {
                            RadioButton(
                                selected=(gpsService != null),
                                onClick={
                                    gpsService?.let {
                                        self.stopService(Intent(self, BackgroundService::class.java))
                                    }
                                },
                                enabled=(gpsService != null),
                            )
                            Text(text="Background service")
                        }


                    }

                }
            }
        }
    }
}