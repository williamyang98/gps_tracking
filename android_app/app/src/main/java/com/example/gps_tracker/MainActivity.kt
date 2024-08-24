package com.example.gps_tracker

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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

    private val listenGpsSender = object: GpsSenderListener {
        override fun onGpsData() {
            renderView();
        }
        override fun onGpsPostResponse() {
            renderView();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        gpsSenderContext = GpsSenderContext(this);
        val gpsSender = GpsSender.getInstance();
        gpsSender.listen(listenGpsSender);
        BackgroundService.start(this);
        this.renderView();
    }

    override fun onDestroy() {
        super.onDestroy();
        val gpsSender = GpsSender.getInstance();
        gpsSender.unlisten(listenGpsSender);
    }

    private fun renderView() {
        val gpsSender = GpsSender.getInstance();
        val self = this;
        val col0 = 0.3f;
        val col1 = 0.7f;
        setContent {
            Gps_trackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(1.dp)
                    ) {
                        Row {
                            Text(text="Background Service", style=MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.weight(1f))
                            Button(onClick = { renderView() }) {
                                Text("Refresh");
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
                                selected=(BackgroundService.getInstance() != null),
                                onClick={},
                                enabled=false,
                            )
                            Text(text="Background service")
                        }
                        Row {
                            Text(text="GPS Location", style=MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.weight(1f))
                            Button(onClick = { gpsSender.refreshLocation(gpsSenderContext) }) {
                                Text(text="Refresh")
                            }
                        }
                        Row {
                            LazyColumn {
                                item {
                                    Row {
                                        TableCell(text="Time", weight=col0)
                                        TableCell(text="${gpsSender.locationTimestamp}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Longitude", weight=col0)
                                        TableCell(text="${gpsSender.location?.longitude}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Latitude", weight=col0)
                                        TableCell(text="${gpsSender.location?.latitude}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Altitude", weight=col0)
                                        TableCell(text="${gpsSender.location?.altitude}", weight=col1)
                                    }
                                }
                            }
                        }
                        Row {
                            Text(text="Server Status", style=MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.weight(1f))
                            Button(onClick = { gpsSender.postGPS(gpsSenderContext) }) {
                                Text(text="Submit")
                            }
                        }
                        Row {
                            LazyColumn(modifier=Modifier.fillMaxWidth()) {
                                item {
                                    Row {
                                        TableCell(text="Time", weight=col0)
                                        TableCell(text="${gpsSender.responseTimestamp}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Status Code", weight=col0)
                                        TableCell(text="${gpsSender.statusCode}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Response", weight=col0)
                                        TableCell(text="${gpsSender.responseBody}", weight=col1)
                                    }
                                }
                            }
                        }
                        Text(text="Permissions", style=MaterialTheme.typography.headlineSmall)
                        Row {
                            LazyColumn {
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
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float
) {
    Text(
        text=text,
        Modifier
            .border(1.dp, Color.Black)
            .weight(weight)
            .padding(8.dp)
    )
}