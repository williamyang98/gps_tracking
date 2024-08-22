package com.example.gps_tracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

private const val TAG: String = "gps_start_receiver";
class StartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && getServiceState(context) == ServiceState.STARTED) {
            Intent(context, BackgroundService::class.java).also {
                it.action = BackgroundServiceActions.START.name;
                context.startForegroundService(it);
            }
        }
    }
}