package com.example.gps_tracker

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class LocationActivity: ComponentActivity() {
    private lateinit var gpsSenderContext: GpsSenderContext;

    private val listenGpsSender = object: GpsSenderListener {
        override fun onGpsData() {
            renderView();
        }
        override fun onGpsPostResponse() {
            renderView();
        }
        override fun onUserRegister(success: Boolean, name: String, id: Int) {
            if (success) {
                Toast.makeText(applicationContext, "Registered '$name' @ $id", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(applicationContext, "Failed to register username", Toast.LENGTH_SHORT).show();
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        gpsSenderContext = GpsSenderContext(this);
        val gpsSender = GpsSender.getInstance();
        gpsSender.listen(listenGpsSender);
        this.renderView();
    }

    override fun onDestroy() {
        super.onDestroy();
        val gpsSender = GpsSender.getInstance();
        gpsSender.unlisten(listenGpsSender);
    }

    fun renderView() {

    }

    @Composable
    fun render() {
        val gpsSender = GpsSender.getInstance();
        val col0 = 0.3f;
        val col1 = 0.7f;

        Row {
            Text(text="Statistics", style= MaterialTheme.typography.headlineSmall)
        }
        Row {
            LazyColumn {
                item {
                    Row {
                        TableCell(text="GPS Measured", weight=0.5f)
                        TableCell(text="${gpsSender.stats.measureSuccess}", weight=0.5f)
                    }
                }
                item {
                    Row {
                        TableCell(text="GPS Fails", weight=0.5f)
                        TableCell(text="${gpsSender.stats.measureFail}", weight=0.5f)
                    }
                }
                item {
                    Row {
                        TableCell(text="Server Sent", weight=0.5f)
                        TableCell(text="${gpsSender.stats.postDataPoints}", weight=0.5f)
                    }
                }
                item {
                    Row {
                        TableCell(text="Server Fails", weight=0.5f)
                        TableCell(text="${gpsSender.stats.postFail}", weight=0.5f)
                    }
                }
            }
        }
        Row {
            Text(text="GPS Location", style= MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                gpsSender.refreshLocation(gpsSenderContext);
            }) {
                Icon(Icons.Outlined.Refresh, contentDescription="Refresh")
            }
        }
        Row {
            LazyColumn {
                item {
                    Row {
                        TableCell(text="Time", weight=col0)
                        TableCell(text="${gpsSender.lastGpsData?.localDateTime}", weight=col1)
                    }
                }
                item {
                    Row {
                        TableCell(text="Longitude", weight=col0)
                        TableCell(text="${gpsSender.lastGpsData?.longitude}", weight=col1)
                    }
                }
                item {
                    Row {
                        TableCell(text="Latitude", weight=col0)
                        TableCell(text="${gpsSender.lastGpsData?.latitude}", weight=col1)
                    }
                }
                item {
                    Row {
                        TableCell(text="Altitude", weight=col0)
                        TableCell(text="${gpsSender.lastGpsData?.altitude}", weight=col1)
                    }
                }
            }
        }
        Row {
            Text(text="Server Status", style= MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { gpsSender.registerUserId(gpsSenderContext) }) {
                Icon(Icons.Outlined.Person, contentDescription="Register")
            }
            IconButton(onClick = { gpsSender.postGPS(gpsSenderContext) }) {
                Icon(Icons.Outlined.Send, contentDescription="Refresh")
            }
        }
        Row {
            LazyColumn(modifier= Modifier.fillMaxWidth()) {
                item {
                    Row {
                        TableCell(text="Time", weight=col0)
                        TableCell(text="${gpsSender.lastServerResponse?.startLocalDateTime}", weight=col1)
                    }
                }
                item {
                    Row {
                        TableCell(text="Status Code", weight=col0)
                        TableCell(text="${gpsSender.lastServerResponse?.statusCode}", weight=col1)
                    }
                }
                item {
                    Row {
                        TableCell(text="Response", weight=col0)
                        TableCell(text="${gpsSender.lastServerResponse?.responseBody}", weight=col1)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(
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