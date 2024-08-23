package com.example.gps_tracker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private const val TAG: String = "gps_start_receiver";
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Starting background service after boot completed");
            BackgroundService.start(context);
        }
    }
}