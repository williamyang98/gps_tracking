package com.example.gps_tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat

class PermissionsActivity: ComponentActivity() {

    @Composable
    fun render() {
        val self = this;
        Text(text="Permissions", style= MaterialTheme.typography.headlineSmall)
        Row {
            LazyColumn(modifier= Modifier.weight(1.0f)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    item {
                        Row {
                            RadioButton(
                                selected = (ActivityCompat.checkSelfPermission(self, Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED),
                                onClick = { ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION), 1) },
                            )
                            Text(text = "Foreground service location")
                        }
                    }
                }
                item {
                    Row {
                        RadioButton(
                            selected = (ActivityCompat.checkSelfPermission(self, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED),
                            onClick = { ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.FOREGROUND_SERVICE), 1) },
                        )
                        Text(text = "Start foreground service")
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    item {
                        Row {
                            RadioButton(
                                selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED),
                                onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1) },
                            )
                            Text(text="Post notifications")
                        }
                    }
                }
                item {
                    Row {
                        RadioButton(
                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED),
                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.SET_ALARM), 1) },
                        )
                        Text(text="Set Alarm")
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    item {
                        Row {
                            RadioButton(
                                selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED),
                                onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION), 1) },
                            )
                            Text(text="Background location")
                        }
                    }
                }
                item {
                    Row {
                        RadioButton(
                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED),
                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1) },
                        )
                        Text(text="Coarse location")
                    }
                }
                item {
                    Row {
                        RadioButton(
                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED),
                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1) },
                        )
                        Text(text="Precise location")
                    }
                }
                item {
                    Row {
                        RadioButton(
                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED),
                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.INTERNET), 1) },
                        )
                        Text(text="Internet")
                    }
                }
                item {
                    Row {
                        RadioButton(
                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.RECEIVE_BOOT_COMPLETED) == PackageManager.PERMISSION_GRANTED),
                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.RECEIVE_BOOT_COMPLETED), 1) },
                        )
                        Text(text="Receive boot complete")
                    }
                }
            }
        }
    }
}