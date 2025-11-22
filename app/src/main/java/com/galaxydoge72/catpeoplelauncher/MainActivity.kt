package com.galaxydoge72.catpeoplelauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxydoge72.catpeoplelauncher.ui.theme.CatPeopleLauncherTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.delay

// ----------------------------------------------------------------------
// CONSTANTS & DATA MODEL & NAVIGATION ENUM
// ----------------------------------------------------------------------

// 🚨 NAVIGATION ENUM
enum class Screen {
    HOME,
    SETTINGS
}

// 🚨 Define the set of target package names for the special wallpaper
val TARGET_APP_PACKAGES = setOf(
    "com.google.android.youtube",
    "com.google.android.chrome",
    "com.spotify.music"
)

// Define the wallpaper and timeout settings
val SPECIAL_BACKGROUND_RES_ID = R.drawable.silly_cat
const val DEFAULT_BACKGROUND_RES_ID = 0
const val WALLPAPER_TIMEOUT_MS = 15 * 60 * 1000L // 15 minutes

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    val launchIntent: Intent
)

// ----------------------------------------------------------------------
// MAIN ACTIVITY
// ----------------------------------------------------------------------

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatPeopleLauncherTheme {
                // 🚨 State for managing screen navigation
                var currentScreen by remember { mutableStateOf(Screen.HOME) }

                // Simple Navigation Handler
                when (currentScreen) {
                    Screen.HOME -> LauncherScreen(
                        appContext = this,
                        onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                    )
                    Screen.SETTINGS -> SettingsScreen(
                        onNavigateBack = { currentScreen = Screen.HOME }
                    )
                }
            }
        }
    }

    // Function to load installed apps remains the same
    fun loadInstalledApps(): List<AppInfo> {
        val packageManager = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolvedInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)

        return resolvedInfoList
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)

                if (launchIntent != null) {
                    AppInfo(
                        appName = resolveInfo.loadLabel(packageManager).toString(),
                        packageName = packageName,
                        icon = resolveInfo.loadIcon(packageManager),
                        launchIntent = launchIntent
                    )
                } else {
                    null
                }
            }
            .sortedBy { it.appName.lowercase() }
    }
}
// ----------------------------------------------------------------------
// SETTINGS SCREEN COMPOSABLE
// ----------------------------------------------------------------------

@Composable
fun SettingsScreen(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Using the theme's background color
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Launcher Settings",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Current Dynamic Wallpaper Apps:",
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        // Placeholder to show target apps
        TARGET_APP_PACKAGES.forEach { packageName ->
            Text(
                text = packageName,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(onClick = onNavigateBack) {
            Text("Go Back to Home")
        }

        // You can add logic here to open the system settings to set default launcher
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* TODO: Implement prompt to set default home app */ }) {
            Text("Set Default Home App")
        }
    }
}

// ----------------------------------------------------------------------
// HOME SCREEN COMPONENTS (App Icon)
// ----------------------------------------------------------------------

@Composable
fun AppIconItem(
    appInfo: AppInfo,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clickable {
                onLaunch()
                context.startActivity(appInfo.launchIntent)
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = rememberDrawablePainter(drawable = appInfo.icon),
            contentDescription = appInfo.appName,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = appInfo.appName,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color.White
        )
    }
}

// ----------------------------------------------------------------------
// HOME SCREEN COMPOSABLE
// ----------------------------------------------------------------------

@Composable
fun LauncherScreen(appContext: Context, onNavigateToSettings: () -> Unit) {
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var currentBackgroundResId by remember { mutableStateOf(DEFAULT_BACKGROUND_RES_ID) }
    var resetTime by remember { mutableStateOf(0L) }

    // 1. Initial App Loading
    LaunchedEffect(Unit) {
        (appContext as? MainActivity)?.let { activity ->
            appList = activity.loadInstalledApps()
        }
    }

    // 2. THE 15-MINUTE TIMER
    if (resetTime > 0) {
        LaunchedEffect(resetTime) {
            val remainingTime = (resetTime + WALLPAPER_TIMEOUT_MS) - System.currentTimeMillis()

            if (remainingTime > 0) {
                delay(remainingTime)
            }

            currentBackgroundResId = DEFAULT_BACKGROUND_RES_ID
            resetTime = 0L
        }
    }

    // Use Box to stack the background image and the content layers
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Display the special background image
        if (currentBackgroundResId != DEFAULT_BACKGROUND_RES_ID) {
            Image(
                painter = painterResource(id = currentBackgroundResId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Status/Search Bar Placeholder (with Settings Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(Color(0x33FFFFFF)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CatPeople Launcher 🐈",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    )

                    // 🚨 Settings Button
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onNavigateToSettings)
                    )
                }

                // 2. Main App Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(appList) { appInfo ->
                        val launchAction = {
                            if (appInfo.packageName in TARGET_APP_PACKAGES) {
                                currentBackgroundResId = SPECIAL_BACKGROUND_RES_ID
                                resetTime = System.currentTimeMillis()
                            } else {
                                currentBackgroundResId = DEFAULT_BACKGROUND_RES_ID
                                resetTime = 0L
                            }
                        }
                        AppIconItem(appInfo = appInfo, onLaunch = launchAction)
                    }
                }

                // 3. Dock (Persistent Bottom Row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(Color(0x55000000)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    appList.take(4).forEach { appInfo ->
                        val launchAction = {
                            if (appInfo.packageName in TARGET_APP_PACKAGES) {
                                currentBackgroundResId = SPECIAL_BACKGROUND_RES_ID
                                resetTime = System.currentTimeMillis()
                            } else {
                                currentBackgroundResId = DEFAULT_BACKGROUND_RES_ID
                                resetTime = 0L
                            }
                        }
                        AppIconItem(
                            appInfo = appInfo,
                            onLaunch = launchAction,
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }
        }
    }
}