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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay // Import for the delay function

// ----------------------------------------------------------------------
// CONSTANTS & DATA MODEL
// ----------------------------------------------------------------------
val TARGET_APP_PACKAGES = setOf(
    "com.discord",
    "cocobo1.pupu.app"
)

val SPECIAL_BACKGROUND_RES_ID = R.drawable.silly_cat
const val DEFAULT_BACKGROUND_RES_ID = 0 // Represents the default state (no special image)

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
                LauncherScreen(appContext = this)
            }
        }
    }

    /**
     * Finds all applications installed on the device that have a LAUNCHER category
     */
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

                // Only create AppInfo if a launch Intent is found
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
// COMPOSE UI COMPONENTS
// ----------------------------------------------------------------------

@Composable
fun AppIconItem(
    appInfo: AppInfo,
    onLaunch: () -> Unit, // Callback when the app is clicked
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clickable {
                onLaunch() // Trigger the parent callback before launching
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

@Composable
fun LauncherScreen(appContext: Context) {
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var currentBackgroundResId by remember { mutableStateOf(DEFAULT_BACKGROUND_RES_ID) }

    // Load app list once
    LaunchedEffect(Unit) {
        (appContext as? MainActivity)?.let { activity ->
            appList = activity.loadInstalledApps()
        }
    }

    // Effect to reset the background when the screen becomes active again
    if (currentBackgroundResId != DEFAULT_BACKGROUND_RES_ID) {
        LaunchedEffect(Unit) {
            delay(500)
            currentBackgroundResId = DEFAULT_BACKGROUND_RES_ID
        }
    }

    // Use Box to stack the background image and the content layers
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Default background color
    ) {
        // Display the special background image if the state is set
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
            // Make content transparent so the Box background shows
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 1. Status/Search Bar Placeholder
                Text(
                    text = "CatPeople Launcher 🐈 - Apps: ${appList.size}",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color(0x33FFFFFF))
                )

                // 2. Main App Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(appList) { appInfo ->
                        AppIconItem(
                            appInfo = appInfo,
                            onLaunch = {
                                if (appInfo.packageName in TARGET_APP_PACKAGES) {
                                    currentBackgroundResId = SPECIAL_BACKGROUND_RES_ID
                                } else {
                                    currentBackgroundResId = DEFAULT_BACKGROUND_RES_ID
                                }
                            }
                        )
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
                    // Placeholder: Use the first 4 apps in the list for the dock
                    appList.take(4).forEach { appInfo ->
                        AppIconItem(
                            appInfo = appInfo,
                            // Apply the same logic for dock apps
                            onLaunch = {
                                if (appInfo.packageName in TARGET_APP_PACKAGES) {
                                    currentBackgroundResId = SPECIAL_BACKGROUND_RES_ID
                                } else {
                                    currentBackgroundResId = DEFAULT_BACKGROUND_RES_ID
                                }
                            },
                            modifier = Modifier.width(64.dp)
                        )
                    }
                }
            }
        }
    }
}