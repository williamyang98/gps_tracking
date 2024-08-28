package com.example.gps_tracker

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.Binder
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
    companion object {
        fun getIsStarted(context: Context): Boolean {
            val intent = Intent(context, BackgroundService::class.java);
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE);
            return pendingIntent != null;
        }

        fun start(context: Context) {
            if (!getIsStarted(context)) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
                val intent = Intent(context, BackgroundService::class.java);
                val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);
                val settings = Settings(context);
                val interval = maxOf(settings.interval, 1).toDuration(DurationUnit.MINUTES);
                context.startForegroundService(intent); // required service.startForeground call is done in onCreate
                alarmManager.setInexactRepeating(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    interval.inWholeMilliseconds,
                    pendingIntent,
                );
                Log.d(TAG, "Starting $interval background service");
                Toast.makeText(context, "Starting $interval background service", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Could not start repeating background service since it already exists");
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BackgroundService::class.java);
            val pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE);
            when (pendingIntent) {
                null -> {
                    Log.e(TAG, "Could not cancel repeating background service since it was not started yet");
                };
                else -> {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                    context.stopService(intent)
                    Log.d(TAG, "Cancelled repeating background service");
                    Toast.makeText(context, "Cancelled repeating background service", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    lateinit var gpsSenderContext: GpsSenderContext;

    override fun onCreate() {
        super.onCreate();
        gpsSenderContext = GpsSenderContext(this);
        val notification = createNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.startForeground(FOREGROUND_SERVICE_TYPE_LOCATION, notification);
        } else {
            this.startForeground(1, notification);
        }
        Log.d(TAG, "Created service");
        Toast.makeText(this, "Background GPS service has been created", Toast.LENGTH_SHORT).show();
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "On start command: $intent");
        val gpsSender = GpsSender.getInstance();
        gpsSender.refreshLocation(gpsSenderContext);
        return START_STICKY;
    }

    override fun onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service has been destroyed");
        Toast.makeText(this, "Background GPS service has been destroyed", Toast.LENGTH_SHORT).show();
    }

    private val binder = LocalBinder();
    inner class LocalBinder: Binder() {
        fun getService(): BackgroundService = this@BackgroundService;
    }

    override fun onBind(intent: Intent): IBinder {
        return binder;
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
