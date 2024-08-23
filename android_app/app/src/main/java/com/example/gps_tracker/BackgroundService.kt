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
import android.os.PowerManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDateTime
import java.time.ZoneOffset

private const val TAG: String = "background_gps_service";

enum class BackgroundServiceActions {
    START,
    STOP
}

var gBackgroundService: BackgroundService? = null;
class BackgroundService: Service() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient;
    private lateinit var requestQueue: RequestQueue;
    var locationTimestamp: LocalDateTime? = null;
    var location: Location? = null;
    var statusCode: Int? = null;
    var responseBody: String? = null;
    var responseTimestamp: LocalDateTime? = null;
    var wakeLock: PowerManager.WakeLock? = null;
    var isServiceStarted = false;

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
        gBackgroundService = this;
        Log.d(TAG, "Created service");
        Toast.makeText(this, "Background GPS service has been created", Toast.LENGTH_SHORT).show();
    }

    override fun onDestroy() {
        super.onDestroy();
        gBackgroundService = null;
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
                this.postGPS();
            } else {
                this.location = null;
                this.locationTimestamp = LocalDateTime.now();
                this.postGPS();
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
        val user_id: Int = 2;
        val buffer = ByteBuffer.allocate(4 + 4 + 8 + 8 + 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(user_id);
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
            }
        );
        requestQueue.add(request);
    }
    override fun onBind(intent: Intent): IBinder? {
        return null;
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action;
            Log.d(TAG, "Using background service with action $action");
            when (action) {
                BackgroundServiceActions.START.name -> startService()
                BackgroundServiceActions.STOP.name -> stopService()
                else -> Log.e(TAG, "Unhandled start command $action")
            }
        } else {
            Log.d(TAG, "Started with null intent, service is probably being restarted");
        }
        return START_STICKY;
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, BackgroundService::class.java).also {
            it.setPackage(packageName)
        };
        val restartServicePendingIntent = PendingIntent.getService(this, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE);
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);
        Log.d(TAG, "Task was removed, attempting to restart");
        Toast.makeText(this, "Task was removed, attempting to restart", Toast.LENGTH_SHORT).show();
    }

    private fun startService() {
        if (this.isServiceStarted) {
            return;
        }
        this.isServiceStarted = true;
        Log.d(TAG, "Attempting to start background service");
        Toast.makeText(this, "Background GPS service is trying to start", Toast.LENGTH_SHORT).show();

        // avoid being disabled by doze mode
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager;
        wakeLock = powerManager.run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GPSTrackingService::wakelock").apply {
                acquire()
            }
        }

        // start infinite loop
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    refreshLocation();
                }
                // delay(10 * 60 * 1000);
                delay(1 * 60 * 1000);
            }
            Log.d(TAG, "Background service loop has closed");
        }
    }

    fun stopService() {
        Log.d(TAG, "Stopping background service");
        Toast.makeText(this, "GPS background service stopping", Toast.LENGTH_SHORT).show();
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release();
                }
            }
            stopForeground(true);
            stopSelf();
        } catch (ex: Exception) {
            Log.d(TAG, "Service stopped without being started: ${ex.message}")
        }
        setServiceState(this, ServiceState.STOPPED);
        isServiceStarted = false;
        gBackgroundService = null;
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
            it.lightColor = Color.RED;
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