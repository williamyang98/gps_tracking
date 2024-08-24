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
        set(value: Int) {
            prefs.edit().putInt(Keys.USER_ID, value).apply();
        }

    var interval: Int
        get() {
            if (!prefs.contains(Keys.INTERVAL)) {
                prefs.edit().putInt(Keys.INTERVAL, 10).apply();
            }
            return prefs.getInt(Keys.INTERVAL, 10);
        }
        set(value: Int) {
            prefs.edit().putInt(Keys.INTERVAL, value).apply();
        }

    var autostart: Boolean
        get() {
            if (!prefs.contains(Keys.AUTOSTART)) {
                prefs.edit().putBoolean(Keys.AUTOSTART, false).apply();
            }
            return prefs.getBoolean(Keys.AUTOSTART, false);
        }
        set(value: Boolean) {
            prefs.edit().putBoolean(Keys.AUTOSTART, value).apply();
        }

    var userName: String
        get() {
            if (!prefs.contains(Keys.USER_NAME)) {
                prefs.edit().putString(Keys.USER_NAME, "Unknown").apply();
            }
            return prefs.getString(Keys.USER_NAME, "");
        }
        set(value: String) {
            prefs.edit().putString(Keys.USER_NAME, value).apply();
        }
}