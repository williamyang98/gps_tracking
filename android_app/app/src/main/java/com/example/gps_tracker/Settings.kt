package com.example.gps_tracker

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager

private class Keys {
    companion object {
        const val USER_ID: String = "user_id";
        const val INTERVAL: String = "interval";
        const val AUTOSTART: String = "autostart";
        const val USER_NAME: String = "user_name";
        const val TIMELINE_LENGTH: String = "timeline_length";
    }
}

class Settings(context: Context) {
    private val prefs: SharedPreferences;

    init {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext);
    }

    var userId: Int
        get() {
            if (!prefs.contains(Keys.USER_ID)) {
                prefs.edit().putInt(Keys.USER_ID, 0).apply();
            }
            return prefs.getInt(Keys.USER_ID, 0);
        }
        set(value) {
            prefs.edit().putInt(Keys.USER_ID, value).apply();
        }

    var interval: Int
        get() {
            val defaultValue = 10;
            if (!prefs.contains(Keys.INTERVAL)) {
                prefs.edit().putInt(Keys.INTERVAL, defaultValue).apply();
            }
            return prefs.getInt(Keys.INTERVAL, defaultValue);
        }
        set(value) {
            prefs.edit().putInt(Keys.INTERVAL, value).apply();
        }

    var autostart: Boolean
        get() {
            val defaultValue = false;
            if (!prefs.contains(Keys.AUTOSTART)) {
                prefs.edit().putBoolean(Keys.AUTOSTART, defaultValue).apply();
            }
            return prefs.getBoolean(Keys.AUTOSTART, defaultValue);
        }
        set(value) {
            prefs.edit().putBoolean(Keys.AUTOSTART, value).apply();
        }

    var userName: String
        get() {
            if (!prefs.contains(Keys.USER_NAME)) {
                prefs.edit().putString(Keys.USER_NAME, "Unknown").apply();
            }
            return prefs.getString(Keys.USER_NAME, "");
        }
        set(value) {
            prefs.edit().putString(Keys.USER_NAME, value).apply();
        }

    var timelineLength: Int
        get() {
            val defaultValue = 100;
            if (!prefs.contains(Keys.TIMELINE_LENGTH)) {
                prefs.edit().putInt(Keys.TIMELINE_LENGTH, defaultValue).apply();
            }
            return prefs.getInt(Keys.TIMELINE_LENGTH, defaultValue);
        }
        set(value) {
            prefs.edit().putInt(Keys.TIMELINE_LENGTH, value).apply();
        }
}