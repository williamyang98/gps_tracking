package com.example.gps_tracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val TAG: String = "gps_sender";

class GpsSenderContext(context: Context) {
    val parentContext: Context;
    val fusedLocationClient: FusedLocationProviderClient;
    val requestQueue: RequestQueue;
    init {
        parentContext = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        requestQueue = Volley.newRequestQueue(context);
    }
}

interface GpsSenderListener {
    fun onGpsData();
    fun onGpsPostResponse();
}

class GpsSender private constructor() {
    private val listeners = mutableSetOf<GpsSenderListener>();
    private val userId: Int = 2;
    var locationTimestamp: LocalDateTime? = null;
    var location: Location? = null;
    var statusCode: Int? = null;
    var responseBody: String? = null;
    var responseTimestamp: LocalDateTime? = null;

    companion object {
        @Volatile private var instance: GpsSender? = null // Volatile modifier is necessary
        fun getInstance() = instance ?: synchronized(this) { // synchronized to avoid concurrency problem
            instance ?: GpsSender().also { instance = it }
        }
    }

    fun listen(listener: GpsSenderListener) {
        listeners.add(listener);
    }

    fun unlisten(listener: GpsSenderListener) {
        listeners.remove(listener);
    }

    fun refreshLocation(context: GpsSenderContext) {
        if (ActivityCompat.checkSelfPermission(context.parentContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing ACCESS_FINE_LOCATION permission when refreshing location");
            return;
        }
        if (ActivityCompat.checkSelfPermission(context.parentContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing ACCESS_COARSE_LOCATION permission when refreshing location");
            return;
        }
        context.fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null) {
                this.location = location;
                this.locationTimestamp = LocalDateTime.now();
                Log.d(TAG, "Refreshed GPS location");
                this.postGPS(context);
            } else {
                this.location = null;
                this.locationTimestamp = LocalDateTime.now();
                Log.e(TAG, "Failed to get GPS location");
            }
            listeners.forEach { listener -> listener.onGpsData(); }
        }
    }

    fun postGPS(context: GpsSenderContext) {
        if (ActivityCompat.checkSelfPermission(context.parentContext, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing INTERNET permission when posting GPS location");
            return;
        }

        val location = this.location;
        val locationTimestamp = this.locationTimestamp;
        if (location == null || locationTimestamp == null) {
            return;
        }
        val unixTimeStamp = locationTimestamp.atZone(ZoneOffset.systemDefault()).toEpochSecond();
        val buffer = ByteBuffer.allocate(4 + 4 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(userId);
        buffer.putInt(unixTimeStamp.toInt());
        buffer.putDouble(location.latitude);
        buffer.putDouble(location.longitude);
        buffer.putDouble(location.altitude);

        val url = "https://australia-southeast1-gps-tracking-433211.cloudfunctions.net/post-gps";
        val request = BinaryRequest(
            Request.Method.POST, url, buffer.array(),
            { statusCode ->
                this.statusCode = statusCode;
                this.responseTimestamp = LocalDateTime.now();
            },
            { response ->
                this.responseBody = response;
                this.responseTimestamp = LocalDateTime.now();
                listeners.forEach { listener -> listener.onGpsPostResponse(); }
                Log.d(TAG, "Post GPS succeeded");
            },
            { error ->
                error.networkResponse?.let { response ->
                    this.statusCode = response.statusCode;
                    this.responseBody = String(response.data, Charsets.UTF_8);
                    this.responseTimestamp = LocalDateTime.now();
                } ?: run {
                    this.statusCode = null;
                    this.responseBody = error.message;
                    this.responseTimestamp = LocalDateTime.now();
                }
                listeners.forEach { listener -> listener.onGpsPostResponse(); }
                Log.e(TAG, "Post GPS failed: ${error.networkResponse}");
            }
        );
        context.requestQueue.add(request);
    }
}


private class BinaryRequest(
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