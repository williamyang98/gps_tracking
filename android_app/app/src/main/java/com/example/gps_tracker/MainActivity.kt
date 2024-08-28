package com.example.gps_tracker

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.gps_tracker.ui.theme.Gps_trackerTheme
import java.time.format.DateTimeFormatter

private const val TAG: String = "main_activity";

private data class NavRoute(val icon: ImageVector, val label: String, val route: String)
private sealed class NavRouteNames {
    companion object {
        const val HOME = "home";
        const val TIMELINE = "timeline";
        const val LOCATION = "location";
        const val PERMISSIONS = "permissions";
        const val SETTINGS = "settings";
    }
}

private val navRoutes = arrayOf(
    NavRoute(Icons.Outlined.Home, "Home", NavRouteNames.HOME),
    NavRoute(Icons.Outlined.List, "Timeline", NavRouteNames.TIMELINE),
    NavRoute(Icons.Outlined.LocationOn, "Location", NavRouteNames.LOCATION),
    NavRoute(Icons.Outlined.Lock, "Permission", NavRouteNames.PERMISSIONS),
    NavRoute(Icons.Outlined.Settings, "Settings", NavRouteNames.SETTINGS),
);


private val FORMAT_DATETIME = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm:ss")

private sealed class TimelineEventData {
    data class Measurement(val gpsData: GpsData) : TimelineEventData()
    data class Transmission(val serverResponse: ServerResponse) : TimelineEventData()
}

class MainActivity : ComponentActivity() {
    private lateinit var gpsSenderContext: GpsSenderContext;
    private lateinit var settings: Settings;
    private var gpsService: BackgroundService? = null;

    private val listenGpsSender = object : GpsSenderListener {
        override fun onGpsData() {
            renderView();
        }

        override fun onGpsPostResponse() {
            renderView();
        }

        override fun onUserRegister(success: Boolean, name: String, id: Int) {
            if (success) {
                Toast.makeText(applicationContext, "Registered '$name' @ $id", Toast.LENGTH_SHORT)
                    .show();
            } else {
                Toast.makeText(
                    applicationContext,
                    "Failed to register username",
                    Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private val gpsServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, ibinder: IBinder) {
            val binder = ibinder as BackgroundService.LocalBinder;
            gpsService = binder.getService();
            renderView();
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            gpsService = null;
            renderView();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        gpsSenderContext = GpsSenderContext(this);
        val gpsSender = GpsSender.getInstance();
        gpsSender.listen(listenGpsSender);
        settings = Settings(this);
        if (settings.autostart) {
            BackgroundService.start(this);
        }
        this.renderView();
    }

    override fun onDestroy() {
        super.onDestroy();
        val gpsSender = GpsSender.getInstance();
        gpsSender.unlisten(listenGpsSender);
    }

    override fun onStart() {
        super.onStart();
        Intent(this, BackgroundService::class.java).also {
            bindService(it, gpsServiceConnection, Context.BIND_FOREGROUND_SERVICE)
        }
    }

    override fun onStop() {
        super.onStop();
        unbindService(gpsServiceConnection);
    }

    private fun renderView() {
        Intent(this, BackgroundService::class.java).also {
            bindService(it, gpsServiceConnection, Context.BIND_FOREGROUND_SERVICE)
        }
        setContent {
            Gps_trackerTheme {
                Render();
            }
        }
    }

    @Composable
    private fun Render() {
        val navController = rememberNavController();
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // https://developer.android.com/develop/ui/compose/navigation#bottom-nav
        Scaffold(
            bottomBar = {
                NavigationBar {
                    navRoutes.forEach { navRoute ->
                        val isSelected =
                            currentDestination?.hierarchy?.any { it.route == navRoute.route } == true
                        NavigationBarItem(
                            icon = { Icon(navRoute.icon, contentDescription = navRoute.label) },
                            // label = { Text(text=navRoute.label) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(navRoute.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true;
                                    }
                                    launchSingleTop = true;
                                    restoreState = true;
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(navController, startDestination = "home", Modifier.padding(innerPadding)) {
                composable(NavRouteNames.HOME) { RenderHome() }
                composable(NavRouteNames.TIMELINE) { RenderTimeline() }
                composable(NavRouteNames.LOCATION) { RenderLocation() }
                composable(NavRouteNames.PERMISSIONS) { RenderPermissions() }
                composable(NavRouteNames.SETTINGS) { RenderSettings() }
            }
        }
    }

    @Composable
    private fun RenderHome() {
        val self = this;
        val gpsSender = GpsSender.getInstance();
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Home",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            Row {
                Text(
                    text = "Background Service",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            ListItem(
                headlineContent = {
                    Text(text = "Run in background")
                },
                supportingContent = {
                    Text(text = "GPS updates will be sent in background")
                },
                trailingContent = {
                    val isEnabled = BackgroundService.getIsStarted(self);
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {
                            if (!isEnabled) {
                                BackgroundService.start(self);
                                self.renderView();
                            } else {
                                BackgroundService.stop(self);
                                self.renderView();
                            }
                        },
                        thumbContent = if (isEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        }
                    )
                }
            )
            ListItem(
                headlineContent = {
                    Text(text = "Service status")
                },
                supportingContent = {
                    Text(text = "Service is active and bound to application")
                },
                trailingContent = {
                    val isEnabled = (gpsService != null);
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = {},
                        thumbContent = if (isEnabled) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        },
                        enabled = false,
                    )
                }
            )
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            Row {
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            Row {
                LazyColumn {
                    val pushRow: (String, String) -> Unit = { label, body ->
                        item {
                            ListItem(
                                headlineContent = {
                                    Text(text = label)
                                },
                                trailingContent = {
                                    Text(
                                        text = body,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                            )
                        }
                    }
                    pushRow("GPS success", "${gpsSender.stats.measureSuccess}");
                    pushRow("GPS failed", "${gpsSender.stats.measureFail}");
                    pushRow("GPS submitted", "${gpsSender.stats.postDataPoints}");
                    pushRow("Requests success", "${gpsSender.stats.postSuccess}");
                    pushRow("Requests failed", "${gpsSender.stats.postFail}");
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun RenderTimeline() {
        val gpsSender = GpsSender.getInstance();
        val timeline = gpsSender.gpsDataTimeline;
        val sheetState = rememberModalBottomSheetState()
        var timelineEventData by remember { mutableStateOf<TimelineEventData?>(null) }

        Column(modifier = Modifier.fillMaxWidth()) {
            // LineItem adds in an annoying 16dp between heading and first item that we don't want
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Timeline",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                timeline.forEach {
                    item {
                        when (it) {
                            is TimeLineEvent.MeasurementFail -> {
                                val ev = it;
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = ev.localDateTime.format(
                                                FORMAT_DATETIME
                                            )
                                        )
                                    },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = null,
                                            tint = Color.Red,
                                        )
                                    },
                                )
                            }

                            is TimeLineEvent.MeasurementSuccess -> {
                                val ev = it.gpsData;
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = ev.localDateTime.format(
                                                FORMAT_DATETIME
                                            )
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${
                                                String.format(
                                                    "%.6f",
                                                    ev.latitude
                                                )
                                            },${String.format("%.6f", ev.longitude)}"
                                        )
                                    },
                                    trailingContent = {
                                        if (ev.isSent) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = Color.Green,
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Outlined.Refresh,
                                                contentDescription = null,
                                                tint = Color.Yellow,
                                            )
                                        }
                                    },
                                    modifier = Modifier.clickable(onClick = {
                                        timelineEventData = TimelineEventData.Measurement(ev);
                                    }),
                                )
                            }

                            is TimeLineEvent.TransmissionSuccess -> {
                                val ev = it.serverResponse;
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Filled.Send,
                                            contentDescription = null,
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = ev.endLocalDateTime.format(
                                                FORMAT_DATETIME
                                            )
                                        )
                                    },
                                    supportingContent = {
                                        Text(text = "${ev.statusCode} ${ev.responseBody}")
                                    },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = Color.Green,
                                        )
                                    },
                                    modifier = Modifier.clickable(onClick = {
                                        timelineEventData = TimelineEventData.Transmission(ev);
                                    }),
                                )
                            }

                            is TimeLineEvent.TransmissionFail -> {
                                val ev = it.serverResponse;
                                ListItem(
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Filled.Send,
                                            contentDescription = null,
                                        )
                                    },
                                    headlineContent = {
                                        Text(
                                            text = ev.endLocalDateTime.format(
                                                FORMAT_DATETIME
                                            )
                                        )
                                    },
                                    supportingContent = {
                                        Text(text = "${ev.statusCode ?: "Request not sent"}")
                                    },
                                    trailingContent = {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = null,
                                            tint = Color.Red,
                                        )
                                    },
                                    modifier = Modifier.clickable(onClick = {
                                        timelineEventData = TimelineEventData.Transmission(ev);
                                    }),
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
                if (timeline.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No events to display",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            timelineEventData?.let {
                ModalBottomSheet(
                    onDismissRequest = {
                        timelineEventData = null;
                    },
                    sheetState = sheetState,
                    modifier = Modifier.fillMaxHeight(),
                ) {
                    when (it) {
                        is TimelineEventData.Measurement -> {
                            val ev = it.gpsData;
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                val pushRow: (String, String) -> Unit = { label, body ->
                                    item {
                                        ListItem(
                                            headlineContent = {
                                                Text(text = label)
                                            },
                                            trailingContent = {
                                                Text(
                                                    text = body,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                )
                                            },
                                        )
                                    }
                                }
                                pushRow("Time", ev.localDateTime.format(FORMAT_DATETIME))
                                val batteryCharging = if (ev.batteryCharging) {
                                    "Charging"
                                } else {
                                    "Discharging"
                                };
                                pushRow("Battery", "${ev.batteryPercentage}% (${batteryCharging})")
                                pushRow("Latitude", "${ev.latitude}")
                                pushRow("Longitude", "${ev.longitude}")
                                pushRow("Accuracy", "${ev.accuracy ?: "?"} m")
                                pushRow(
                                    "Altitude",
                                    "${ev.altitude ?: "?"} ± ${ev.altitudeAccuracy ?: "?"} m"
                                )
                                pushRow(
                                    "MSL Altitude",
                                    "${ev.mslAltitude ?: "?"} ± ${ev.mslAltitudeAccuracy ?: "?"} m"
                                )
                                pushRow(
                                    "Speed",
                                    "${ev.speed ?: "?"} ± ${ev.speedAccuracy ?: "?"} m/s"
                                )
                                pushRow(
                                    "Bearing",
                                    "${ev.bearing ?: "?"} ± ${ev.bearingAccuracy ?: "?"} °"
                                )
                                pushRow("Is Submitted", "${ev.isSent}")
                            }
                        }

                        is TimelineEventData.Transmission -> {
                            val ev = it.serverResponse;
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                val pushRow: (String, String) -> Unit = { label, body ->
                                    item {
                                        ListItem(
                                            headlineContent = {
                                                Text(text = label)
                                            },
                                            trailingContent = {
                                                Text(
                                                    text = body,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                )
                                            },
                                        )
                                    }
                                }
                                pushRow("Start Time", ev.startLocalDateTime.format(FORMAT_DATETIME))
                                pushRow("End Time", ev.endLocalDateTime.format(FORMAT_DATETIME))
                                pushRow("Total GPS Points Sent", "${ev.totalSent}")
                                pushRow("Status Code", "${ev.statusCode}")
                                val responseLength = ev.responseBody?.length ?: 4;
                                if (responseLength > 10) {
                                    item {
                                        Text(
                                            text = "Response Body",
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 16.dp
                                            )
                                        )
                                    }
                                    item {
                                        Text(
                                            text = "${ev.responseBody}",
                                            modifier = Modifier.padding(
                                                horizontal = 16.dp,
                                                vertical = 0.dp
                                            ),
                                        )
                                    }
                                } else {
                                    pushRow("Response Body", "${ev.responseBody}");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun RenderLocation() {
        val gpsSender = GpsSender.getInstance();

        Column(modifier = Modifier.fillMaxWidth()) {
            // LineItem adds in an annoying 16dp between heading and first item that we don't want
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Location",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val pushRow: (String, String) -> Unit = { label, body ->
                    item {
                        ListItem(
                            headlineContent = {
                                Text(text = label)
                            },
                            trailingContent = {
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                        )
                    }
                }
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "GPS Sensor",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = {
                                gpsSender.refreshLocation(gpsSenderContext);
                            }) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                            }
                        },
                    )
                }
                pushRow("Time", "${gpsSender.lastGpsData?.localDateTime?.format(FORMAT_DATETIME)}")
                pushRow("Latitude", "${gpsSender.lastGpsData?.latitude}")
                pushRow("Longitude", "${gpsSender.lastGpsData?.longitude}")
                item { HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp)) }
                item {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = "Server Response",
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { gpsSender.registerUserId(gpsSenderContext) }) {
                                    Icon(Icons.Outlined.Person, contentDescription = "Register")
                                }
                                IconButton(onClick = { gpsSender.postGPS(gpsSenderContext) }) {
                                    Icon(Icons.Outlined.Send, contentDescription = "Refresh")
                                }
                            }
                        },
                    )
                }
                pushRow(
                    "Time",
                    "${gpsSender.lastServerResponse?.endLocalDateTime?.format(FORMAT_DATETIME)}"
                )
                pushRow("Status Code", "${gpsSender.lastServerResponse?.statusCode}")
                pushRow("Total Sent", "${gpsSender.lastServerResponse?.totalSent}")
                val responseBodyLength = gpsSender.lastServerResponse?.responseBody?.length ?: 4;
                if (responseBodyLength > 10) {
                    item {
                        Text(
                            text = "Response Body",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                        )
                    }
                    item {
                        Text(
                            text = "${gpsSender.lastServerResponse?.responseBody}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp),
                        )
                    }
                } else {
                    pushRow("Response Body", "${gpsSender.lastServerResponse?.responseBody}");
                }
            }
        }
    }


    @Composable
    private fun RenderPermissions() {
        val self = this;
        Column(modifier = Modifier.fillMaxWidth()) {
            // LineItem adds in an annoying 16dp between heading and first item that we don't want
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 0.dp),
                )
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val createRow: (label: String, permission: String) -> Unit = { label, permission ->
                    val isEnabled = ActivityCompat.checkSelfPermission(
                        self,
                        permission
                    ) == PackageManager.PERMISSION_GRANTED;
                    item {
                        ListItem(
                            headlineContent = { Text(text = label) },
                            supportingContent = { Text(text = permission.split('.').last()) },
                            trailingContent = {
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = {
                                        if (it) {
                                            ActivityCompat.requestPermissions(
                                                self,
                                                arrayOf(permission),
                                                1
                                            )
                                        }
                                    },
                                    thumbContent = if (isEnabled) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    } else {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Clear,
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        }
                                    }
                                )
                            }
                        )
                        HorizontalDivider()
                    }
                }
                createRow("Start foreground service", Manifest.permission.FOREGROUND_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    createRow(
                        "Allow foreground service to get location",
                        Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    createRow("Post notifications", Manifest.permission.POST_NOTIFICATIONS);
                }
                createRow("Set alarm", Manifest.permission.SET_ALARM);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    createRow(
                        "Read location in background",
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    );
                }
                createRow("Allow coarse location", Manifest.permission.ACCESS_COARSE_LOCATION);
                createRow("Allow precise location", Manifest.permission.ACCESS_FINE_LOCATION);
                createRow("Permit internet access", Manifest.permission.INTERNET);
                createRow("Launch service on boot", Manifest.permission.RECEIVE_BOOT_COMPLETED);
            }
        }
    }

    @Composable
    private fun RenderSettings() {
        val settingsComposable = SettingsComposable.getInstance();
        settingsComposable.Render(this.settings, onChange = { this.renderView() })
    }
}