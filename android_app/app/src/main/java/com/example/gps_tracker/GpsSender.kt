package com.example.gps_tracker

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
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
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.LinkedList
import java.util.Vector
import kotlin.experimental.or
import kotlin.properties.Delegates

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
    fun onUserRegister(success: Boolean, name: String, id: Int);
}

class GpsData {
    var unixTimeMillis: Long = 0;
    var batteryPercentage: Int = 0;
    var batteryCharging: Boolean = false;
    var latitude: Double = 0.0;
    var longitude: Double = 0.0;
    var accuracy: Float? = null;
    var altitude: Float? = null;
    var altitudeAccuracy: Float? = null;
    var mslAltitude: Double? = null;
    var mslAltitudeAccuracy: Float? = null;
    var speed: Float? = null;
    var speedAccuracy: Float? = null;
    var bearing: Float? = null;
    var bearingAccuracy: Float? = null;
    var locationExtras: Bundle? = null;
    var isSent: Boolean = false;

    val localDateTime: LocalDateTime get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.unixTimeMillis), ZoneId.systemDefault());

    fun encodeIntoBuffer(buffer: ByteBuffer) {
        buffer.putLong(this.unixTimeMillis);
        val batteryChargingFlag: Byte = if (this.batteryCharging) { 0x80.toByte() } else { 0x00.toByte() };
        val batteryPercentage = minOf(maxOf(this.batteryPercentage, 0), 100);
        buffer.put(batteryPercentage.toByte().or(batteryChargingFlag));
        buffer.putDouble(this.latitude);
        buffer.putDouble(this.longitude);

        // This has 9 optional fields, encode this into a u16 bitflag
        var flags: Short = 0;
        this.accuracy?.let              { flags = flags or (1 shl 0).toShort() }
        this.altitude?.let              { flags = flags or (1 shl 1).toShort() }
        this.altitudeAccuracy?.let      { flags = flags or (1 shl 2).toShort() }
        this.mslAltitude?.let           { flags = flags or (1 shl 3).toShort() }
        this.mslAltitudeAccuracy?.let   { flags = flags or (1 shl 4).toShort() }
        this.speed?.let                 { flags = flags or (1 shl 5).toShort() }
        this.speedAccuracy?.let         { flags = flags or (1 shl 6).toShort() }
        this.bearing?.let               { flags = flags or (1 shl 7).toShort() }
        this.bearingAccuracy?.let       { flags = flags or (1 shl 8).toShort() }
        buffer.putShort(flags);
        this.accuracy?.let { buffer.putFloat(it) }
        this.altitude?.let { buffer.putFloat(it.toFloat()) }
        this.altitudeAccuracy?.let { buffer.putFloat(it) }
        this.mslAltitude?.let { buffer.putFloat(it.toFloat()) }
        this.mslAltitudeAccuracy?.let { buffer.putFloat(it) }
        this.speed?.let { buffer.putFloat(it) }
        this.speedAccuracy?.let { buffer.putFloat(it) }
        this.bearing?.let { buffer.putFloat(it) }
        this.bearingAccuracy?.let { buffer.putFloat(it) }
    }

    fun getSizeBytes(): UInt {
        var size: UInt = 0U;
        // unixTimeMillis
        // batteryStatus | batteryPercentage
        // latitude
        // longitude
        // extensionFlags
        size += (8U + 1U + 8U + 8U + 2U);
        this.accuracy?.let              { size += 4U; }
        this.altitude?.let              { size += 4U; }
        this.altitudeAccuracy?.let      { size += 4U; }
        this.mslAltitude?.let           { size += 4U; }
        this.mslAltitudeAccuracy?.let   { size += 4U; }
        this.speed?.let                 { size += 4U; }
        this.speedAccuracy?.let         { size += 4U; }
        this.bearing?.let               { size += 4U; }
        this.bearingAccuracy?.let       { size += 4U; }
        return size;
    }
}

class ServerResponse {
    var startUnixTimeMillis: Long = 0;
    var endUnixTimeMillis: Long = 0;
    var statusCode: Int? = null;
    var responseBody: String? = null;
    var totalSent: Int? = null;

    val startLocalDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.startUnixTimeMillis), ZoneId.systemDefault());
    val endLocalDateTime: LocalDateTime
        get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.endUnixTimeMillis), ZoneId.systemDefault());
}

class GpsSenderStatistics {
    var measureAttempts: Int = 0;
    var measureSuccess: Int = 0;
    var measureFail: Int = 0;
    var postAttempts: Int = 0;
    var postSuccess: Int = 0;
    var postFail: Int = 0;
    var postDataPoints: Int = 0;
}

sealed class TimeLineEvent {
    data class MeasurementFail(val unixTimeMillis: Long): TimeLineEvent() {
        val localDateTime: LocalDateTime get() = LocalDateTime.ofInstant(Instant.ofEpochMilli(this.unixTimeMillis), ZoneId.systemDefault());
    }
    data class MeasurementSuccess(val gpsData: GpsData): TimeLineEvent()
    data class TransmissionFail(val serverResponse: ServerResponse): TimeLineEvent()
    data class TransmissionSuccess(val serverResponse: ServerResponse): TimeLineEvent()
}

class GpsSender private constructor() {
    private val listeners = mutableSetOf<GpsSenderListener>();
    var stats = GpsSenderStatistics();
    var gpsDataTimeline = LinkedList<TimeLineEvent>();
    var gpsDataQueue = LinkedList<GpsData>();
    var lastGpsData: GpsData? = null;
    var lastServerResponse: ServerResponse? = null;

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

    private fun pushTimelineEvent(context: GpsSenderContext, event: TimeLineEvent) {
        synchronized(this.gpsDataTimeline) {
            this.gpsDataTimeline.addFirst(event);
            val settings = Settings(context.parentContext);
            val maxLength = settings.timelineLength;
            val totalRemove = this.gpsDataTimeline.size - maxLength;
            if (totalRemove > 0) {
                for (_i in 0 until totalRemove) {
                    this.gpsDataTimeline.removeLast();
                }
            }
        }
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
        this.stats.measureAttempts++;
        context.fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener {
            it?.let { location ->
                val batteryManager = context.parentContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager;
                val data = GpsData();
                data.unixTimeMillis = location.time;
                data.batteryPercentage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                data.batteryCharging = batteryManager.isCharging;
                data.latitude = location.latitude;
                data.longitude = location.longitude;
                data.accuracy = if (location.hasAccuracy()) { location.accuracy } else { null };
                data.altitude = if (location.hasAltitude()) { location.altitude.toFloat() } else { null };
                data.altitudeAccuracy = if (location.hasVerticalAccuracy()) { location.verticalAccuracyMeters } else { null };
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    try {
                        data.mslAltitude = if (location.hasMslAltitude()) { location.mslAltitudeMeters } else { null };
                        data.mslAltitudeAccuracy = if (location.hasMslAltitudeAccuracy()) { location.mslAltitudeAccuracyMeters } else { null };
                    } catch (ex: Exception) {
                        Log.e(TAG, "Failed to get mean sea level information: $ex");
                    }
                }
                data.speed = if (location.hasSpeed()) { location.speed } else { null };
                data.speedAccuracy = if (location.hasSpeedAccuracy()) { location.speedAccuracyMetersPerSecond } else { null };
                data.bearing = if (location.hasBearing()) { location.bearing } else { null };
                data.bearingAccuracy = if (location.hasBearingAccuracy()) { location.bearingAccuracyDegrees } else { null };
                data.locationExtras = location.extras;
                this.lastGpsData = data;
                pushTimelineEvent(context, TimeLineEvent.MeasurementSuccess(data));
                synchronized(this.gpsDataQueue) {
                    this.gpsDataQueue.addFirst(data);
                }
                this.stats.measureSuccess++;
                this.postGPS(context);
            } ?: {
                this.lastGpsData = null;
                pushTimelineEvent(context, TimeLineEvent.MeasurementFail(System.currentTimeMillis()))
                this.stats.measureFail++;
            }
            listeners.forEach { listener -> listener.onGpsData(); }
        }
    }

    fun postGPS(context: GpsSenderContext) {
        if (ActivityCompat.checkSelfPermission(context.parentContext, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing INTERNET permission when posting GPS location");
            return;
        }
        synchronized(this.gpsDataQueue) {
            if (this.gpsDataQueue.isEmpty()) return;

            val settings = Settings(context.parentContext);
            val userId = settings.userId;
            // encode data
            val totalBytes = this.gpsDataQueue.sumOf { it.getSizeBytes() };
            val buffer = ByteBuffer.allocate(totalBytes.toInt());
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            this.gpsDataQueue.forEach { it.encodeIntoBuffer(buffer); }
            // keep track of which queued data points to remove after success
            val lastDataUnixTime: Long = this.gpsDataQueue.maxOf { it.unixTimeMillis };
            // attempt post
            val baseUrl = context.parentContext.resources.getString(R.string.server_url);
            val url = "$baseUrl/post-gps";

            val serverResponse = ServerResponse();
            serverResponse.startUnixTimeMillis = System.currentTimeMillis();
            this.stats.postAttempts++;
            val request = BinaryRequest(
                Request.Method.POST, "$url?user_id=$userId", buffer.array(),
                { statusCode ->
                    serverResponse.statusCode = statusCode;
                    serverResponse.endUnixTimeMillis = System.currentTimeMillis();
                },
                { response ->
                    serverResponse.responseBody = response;
                    serverResponse.endUnixTimeMillis = System.currentTimeMillis();
                    synchronized(this.gpsDataQueue) {
                        var totalSent = 0;
                        this.gpsDataQueue.filter { it.unixTimeMillis <= lastDataUnixTime }.forEach {
                            it.isSent = true;
                            totalSent++;
                        }
                        this.stats.postDataPoints += totalSent;
                        this.gpsDataQueue.removeIf { it.unixTimeMillis <= lastDataUnixTime }
                        serverResponse.totalSent = totalSent;
                    }
                    pushTimelineEvent(context, TimeLineEvent.TransmissionSuccess(serverResponse));
                    this.lastServerResponse = serverResponse;
                    this.stats.postSuccess++;
                    listeners.forEach { it.onGpsPostResponse() }
                },
                { error ->
                    error.networkResponse?.let { response ->
                        serverResponse.statusCode = response.statusCode;
                        serverResponse.responseBody = String(response.data, Charsets.UTF_8);
                        serverResponse.endUnixTimeMillis = System.currentTimeMillis();
                        this.stats.postFail++;
                    } ?: run {
                        serverResponse.statusCode = null;
                        serverResponse.responseBody = error.message;
                        serverResponse.endUnixTimeMillis = System.currentTimeMillis();
                        this.stats.postFail++;
                    }
                    pushTimelineEvent(context, TimeLineEvent.TransmissionFail(serverResponse));
                    this.lastServerResponse = serverResponse;
                    listeners.forEach { it.onGpsPostResponse() }
                }
            );
            context.requestQueue.add(request);
        }
    }

    fun registerUserId(context: GpsSenderContext) {
        val settings = Settings(context.parentContext);
        val userId = settings.userId;
        val userName = settings.userName;
        val baseUrl = context.parentContext.resources.getString(R.string.server_url);
        val url = "$baseUrl/register-user-name";
        val body = JSONObject();
        body.put("user_id", userId.toString());
        body.put("user_name", userName);
        val request = JsonRequest(
            Request.Method.POST, url, body,
            {
                Log.d(TAG, "Registering username got $it status code");
            },
            { response ->
                Log.d(TAG, "Successfully registered username: $response");
                listeners.forEach { it.onUserRegister(true, userName, userId) }
            },
            { error ->
                Log.e(TAG, "Failed to register username: $error");
                listeners.forEach { it.onUserRegister(false, userName, userId) }
            },
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

private class JsonRequest(
    method: Int, url: String,
    private val requestBody: JSONObject,
    private val statusListener: Response.Listener<Int>,
    successListener: Response.Listener<String>,
    errorListener: Response.ErrorListener
): StringRequest(method, url, successListener, errorListener) {
    override fun getBody(): ByteArray {
        return requestBody.toString(2).encodeToByteArray();
    }
    override fun getBodyContentType(): String {
        return "application/json";
    }
    override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
        statusListener.onResponse(response.statusCode);
        return super.parseNetworkResponse(response);
    }
}