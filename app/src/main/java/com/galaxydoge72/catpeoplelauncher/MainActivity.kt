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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxydoge72.catpeoplelauncher.ui.theme.CatPeopleLauncherTheme
import com.google.accompanist.drawablepainter.rememberDrawablePainter

// ----------------------------------------------------------------------
// DATA MODEL
// ----------------------------------------------------------------------

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
                // Pass the Context to the Composable to access the Package Manager
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

        // Query for all activities that can handle the LAUNCHER intent
        val resolvedInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)

        return resolvedInfoList
            .map { resolveInfo ->
                val appName = resolveInfo.loadLabel(packageManager).toString()
                val packageName = resolveInfo.activityInfo.packageName
                val icon = resolveInfo.loadIcon(packageManager)

                // Create the intent used to launch the app
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                    ?: Intent().apply {
                        setClassName(packageName, resolveInfo.activityInfo.name)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }

                AppInfo(
                    appName = appName,
                    packageName = packageName,
                    icon = icon,
                    launchIntent = launchIntent
                )
            }
            .filter { true } // Only include apps we can actually launch
            .sortedBy { it.appName.lowercase() } // Sort alphabetically
    }
}

// ----------------------------------------------------------------------
// COMPOSE UI COMPONENTS
// ----------------------------------------------------------------------

@Composable
fun AppIconItem(appInfo: AppInfo, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .clickable {
                // Launches the external application
                context.startActivity(appInfo.launchIntent)
            }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Uses accompanist to draw the Android Drawable (app icon)
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
    // State to hold the list of installed applications
    var appList by remember { mutableStateOf<List<AppInfo>>(emptyList()) }

    // Load apps when the composable first enters the composition
    LaunchedEffect(Unit) {
        // Safely call the loading function
        (appContext as? MainActivity)?.let { activity ->
            appList = activity.loadInstalledApps()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black // Set a dark background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Status/Search Bar Placeholder
            Text(
                text = "CatPeople Launcher 🐈 - Total Apps: ${appList.size}",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0x33FFFFFF))
            )

            // 2. Main App Grid (Takes up the majority of the screen space)
            LazyVerticalGrid(
                columns = GridCells.Fixed(5), // 5 icons per row
                contentPadding = PaddingValues(8.dp),
                // Use weight to make the grid fill the space between the top bar and the dock
                modifier = Modifier.weight(1f)
            ) {
                items(appList) { appInfo ->
                    AppIconItem(appInfo = appInfo)
                }
            }

            // 3. Dock (Persistent Bottom Row)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(Color(0x55000000)), // Darker, semi-transparent dock
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Placeholder: Use the first 4 apps in the list for the dock
                appList.take(4).forEach { appInfo ->
                    AppIconItem(
                        appInfo = appInfo,
                        modifier = Modifier.width(64.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------
// PREVIEW (Optional, for development)
// ----------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun LauncherPreview() {
    CatPeopleLauncherTheme {
        Text("Preview is difficult for launchers, but this works!")
    }
}