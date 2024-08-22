package com.example.gps_tracker

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.gps_tracker.ui.theme.Gps_trackerTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.ZoneOffset

class BinaryRequest(
    method: Int, url: String,
    private val requestBody: ByteArray,
    private val statusListener: Response.Listener<Int>,
    successListener: Response.Listener<String>,
    errorListener: Response.ErrorListener
): StringRequest(method, url, successListener, errorListener) {
    override fun getBody(): ByteArray {
        return requestBody;
    }
    override fun getBodyContentType(): String {
        return "application/octet-stream";
    }
    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
        statusListener.onResponse(response.statusCode);
        return super.parseNetworkResponse(response);
    }
}

class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient;
    private lateinit var requestQueue: RequestQueue;
    private var locationTimestamp: LocalDateTime? = null;
    private var location: Location? = null;
    private var statusCode: Int? = null;
    private var responseBody: String? = null;
    private var responseTimestamp: LocalDateTime? = null;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);
        this.refreshLocation();
        this.renderView();
    }

    private fun refreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1);
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null) {
                this.location = location;
                this.locationTimestamp = LocalDateTime.now();
                this.postGPS();
            } else {
                this.location = null;
                this.locationTimestamp = LocalDateTime.now();
                this.postGPS();
            }
            this.renderView();
        }
    }

    private fun postGPS() {
        val location = this.location;
        val locationTimestamp = this.locationTimestamp;
        if (location == null || locationTimestamp == null) {
            return;
        }
        val unixTimeStamp = locationTimestamp.atZone(ZoneOffset.UTC).toEpochSecond();
        val user_id: Int = 0;
        val buffer = ByteBuffer.allocate(4 + 4 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(user_id);
        buffer.putInt(unixTimeStamp.toInt());
        buffer.putDouble(location.latitude);
        buffer.putDouble(location.longitude);
        buffer.putDouble(location.altitude);

        val url = "https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/post-gps";
        var request = BinaryRequest(
            Request.Method.POST, url, buffer.array(),
            { statusCode ->
                this.statusCode = statusCode;
                this.responseTimestamp = LocalDateTime.now();
                this.renderView();
            },
            { response ->
                this.responseBody = response;
                this.responseTimestamp = LocalDateTime.now();
                this.renderView();
            },
            { error ->
                error.networkResponse?.let { response ->
                    this.statusCode = response.statusCode;
                    this.responseBody = String(response.data, Charsets.UTF_8);
                    this.responseTimestamp = LocalDateTime.now();
                    this.renderView();
                } ?: run {
                    this.statusCode = null;
                    this.responseBody = error.message;
                    this.responseTimestamp = LocalDateTime.now();
                    this.renderView();
                }
            }
        );
        requestQueue.add(request);
    }

    private fun renderView() {
        var col0 = 0.3f;
        var col1 = 0.7f;
        setContent {
            Gps_trackerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column {
                        Text(text="GPS Location", style=MaterialTheme.typography.headlineSmall)
                        Button(onClick = { refreshLocation() }) {
                            Text(text="Refresh location")
                        }
                        Row {
                            LazyColumn {
                                item {
                                    Row {
                                        TableCell(text="Time", weight=col0)
                                        TableCell(text="$locationTimestamp", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Longitude", weight=col0)
                                        TableCell(text="${location?.longitude}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Latitude", weight=col0)
                                        TableCell(text="${location?.latitude}", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Altitude", weight=col0)
                                        TableCell(text="${location?.altitude}", weight=col1)
                                    }
                                }
                            }
                        }
                        Text(text="Server Status", style=MaterialTheme.typography.headlineSmall)
                        Button(onClick = { postGPS() }) {
                            Text(text="Submit GPS")
                        }
                        Row {
                            LazyColumn {
                                item {
                                    Row {
                                        TableCell(text="Time", weight=col0)
                                        TableCell(text="$responseTimestamp", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Status Code", weight=col0)
                                        TableCell(text="$statusCode", weight=col1)
                                    }
                                }
                                item {
                                    Row {
                                        TableCell(text="Response", weight=col0)
                                        TableCell(text="$responseBody", weight=col1)
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
    private fun getView() {

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