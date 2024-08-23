package com.example.gps_tracker

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
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
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val TAG: String = "background_gps_service";

class BackgroundService: Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient;
    private lateinit var requestQueue: RequestQueue;
    var locationTimestamp: LocalDateTime? = null;
    var location: Location? = null;
    var statusCode: Int? = null;
    var responseBody: String? = null;
    var responseTimestamp: LocalDateTime? = null;

    companion object {
        @Volatile private var instance: BackgroundService? = null;
        @Volatile private var pendingIntent: PendingIntent? = null;

        fun getInstance(): BackgroundService? { return instance; }
        fun getIsStarted(): Boolean { return pendingIntent != null; }

        fun start(context: Context, interval: Duration = 10.toDuration(DurationUnit.MINUTES)) {
            if (pendingIntent == null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
                val intent = Intent(context, BackgroundService::class.java);
                pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);
                alarmManager.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime(),
                    interval.inWholeMilliseconds,
                    pendingIntent
                );
                Log.d(TAG, "Starting repeating background service through alarm manager");
                Toast.makeText(context, "Starting repeating background service", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Could not start repeating background service since it already exists");
            }
        }
    }

    override fun onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestQueue = Volley.newRequestQueue(this);
        val notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.startForeground(FOREGROUND_SERVICE_TYPE_LOCATION, notification);
        } else {
            this.startForeground(1, notification);
        }
        Log.d(TAG, "Created service");
        Toast.makeText(this, "Background GPS service has been created", Toast.LENGTH_SHORT).show();
        instance = this;
    }

    override fun onDestroy() {
        super.onDestroy();
        instance = null;
        Log.d(TAG, "Service has been destroyed");
        Toast.makeText(this, "Background GPS service has been destroyed", Toast.LENGTH_SHORT).show();
    }

    fun refreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            if (location != null) {
                this.location = location;
                this.locationTimestamp = LocalDateTime.now();
                Log.d(TAG, "Refreshed GPS location");
                this.postGPS();
            } else {
                this.location = null;
                this.locationTimestamp = LocalDateTime.now();
                Log.e(TAG, "Failed to get GPS location");
            }
        }
    }

    fun postGPS() {
        val location = this.location;
        val locationTimestamp = this.locationTimestamp;
        if (location == null || locationTimestamp == null) {
            return;
        }
        val unixTimeStamp = locationTimestamp.atZone(ZoneOffset.systemDefault()).toEpochSecond();
        val userId: Int = 3;
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
                Log.e(TAG, "Post GPS failed: ${error.networkResponse}");
            }
        );
        requestQueue.add(request);
    }
    override fun onBind(intent: Intent): IBinder? {
        return null;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "On start command: $intent");
        refreshLocation();
        return START_STICKY;
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "GPS TRACKING SERVICE"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
        val channel = NotificationChannel(
            notificationChannelId,
            "GPS tracking notifications channel",
            NotificationManager.IMPORTANCE_HIGH
        ).let {
            it.description = "GPS tracking service channel";
            it.enableLights(true);
            it.lightColor = Color.WHITE;
            it.enableVibration(false);
            it.vibrationPattern = null;
            it
        }
        notificationManager.createNotificationChannel(channel);
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let {
            notificationIntent -> PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        }
        val builder: Notification.Builder = Notification.Builder(this, notificationChannelId);
        return builder
            .setContentTitle("Background GPS service")
            .setContentText("GPS tracking service running")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build();
    }
}

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