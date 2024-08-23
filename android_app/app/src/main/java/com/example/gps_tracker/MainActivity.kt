package com.example.gps_tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);

        this.renderView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION), 1);
            } else {
                BackgroundService.start(this);
            }
        } else {
            BackgroundService.start(this);
        }
    }

    private fun renderView() {
        val backgroundService = BackgroundService.getInstance();
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
                        Button(onClick = { renderView() }) {
                            Text("Refresh view");
                        }
                        Text(text="Background Service", style=MaterialTheme.typography.headlineSmall)
                        Row {
                            RadioButton(
                                selected=(BackgroundService.getIsStarted()),
                                onClick={
                                    if (!BackgroundService.getIsStarted()) {
                                        BackgroundService.start(self);
                                        self.renderView();
                                    }
                                },
                            )
                            Text(text="Service running")
                        }
                        Row {
                            Text(text="GPS Location", style=MaterialTheme.typography.headlineSmall)
                            Button(onClick = { backgroundService?.refreshLocation() }) {
                                Text(text="Refresh location")
                            }
                        }
                        Row {
                            LazyColumn {
                                item {
                                    Row {
                                        TableCell(text="Time", weight=col0)
                                        TableCell(text="${backgroundService?.locationTimestamp}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Longitude", weight=col0)
                                        TableCell(text="${backgroundService?.location?.longitude}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Latitude", weight=col0)
                                        TableCell(text="${backgroundService?.location?.latitude}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Altitude", weight=col0)
                                        TableCell(text="${backgroundService?.location?.altitude}", weight=col1)
                                    }
                                }
                            }
                        }
                        Row {
                            Text(text="Server Status", style=MaterialTheme.typography.headlineSmall)
                            Button(onClick = { backgroundService?.postGPS() }) {
                                Text(text="Submit GPS")
                            }
                        }
                        Row {
                            LazyColumn(modifier=Modifier.fillMaxWidth()) {
                                item {
                                    Row {
                                        TableCell(text="Time", weight=col0)
                                        TableCell(text="${backgroundService?.responseTimestamp}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Status Code", weight=col0)
                                        TableCell(text="${backgroundService?.statusCode}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Response", weight=col0)
                                        TableCell(text="${backgroundService?.responseBody}", weight=col1)
                                    }
                                }
                            }
                        }
                        Text(text="Permissions", style=MaterialTheme.typography.headlineSmall)
                        Row {
                            LazyColumn {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    item {
                                        Row {
                                            RadioButton(
                                                selected = (ActivityCompat.checkSelfPermission(
                                                    self,
                                                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
                                                ) == PackageManager.PERMISSION_GRANTED),
                                                onClick = {
                                                    ActivityCompat.requestPermissions(
                                                        self,
                                                        arrayOf(Manifest.permission.FOREGROUND_SERVICE_LOCATION),
                                                        1
                                                    )
                                                },
                                            )
                                            Text(text = "Foreground service location")
                                        }
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
                                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.SET_ALARM) == PackageManager.PERMISSION_GRANTED),
                                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.SET_ALARM), 1) },
                                        )
                                        Text(text="Set Alarm")
                                    }
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    item {
                                        Row {
                                            RadioButton(
                                                selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.SCHEDULE_EXACT_ALARM) == PackageManager.PERMISSION_GRANTED),
                                                onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.SCHEDULE_EXACT_ALARM), 1) },
                                            )
                                            Text(text="Schedule exact Alarm")
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
                                item {
                                    Row {
                                        RadioButton(
                                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED),
                                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.FOREGROUND_SERVICE), 1) },
                                        )
                                        Text(text="Foreground service")
                                    }
                                }
                                item {
                                    Row {
                                        RadioButton(
                                            selected=(ActivityCompat.checkSelfPermission(self, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED),
                                            onClick={ ActivityCompat.requestPermissions(self, arrayOf(Manifest.permission.WAKE_LOCK), 1) },
                                        )
                                        Text(text="Wake lock")
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