package com.example.materialfilexplorer

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.materialfilexplorer.ui.theme.MaterialFileXplorerTheme
import kotlinx.coroutines.launch



class MainActivity : ComponentActivity() {
    companion object {
        const val THEME_PREFS = "theme_prefs"
        const val IS_DARK_THEME = "is_dark_theme"
    }
    private val sharedPreferences by lazy {
        getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPref = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        var isDarkTheme by mutableStateOf(sharedPref.getBoolean(IS_DARK_THEME, true))

        setContent {

            MaterialFileXplorerTheme(isInDarkTheme = isDarkTheme) {
                LaunchedEffect(isDarkTheme) {
                    with(sharedPref.edit()) {
                        putBoolean("is_dark_theme", isDarkTheme)
                        apply()
                    }
                }
                MainScreen(isDarkTheme) {
                    isDarkTheme = it
                }
            }
        }
    }
}
@Composable
fun customShape(): Shape {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    LocalDensity.current.density

    val widthPx = with(LocalDensity.current) { screenWidth.toPx() }
    val heightPx = with(LocalDensity.current) { screenHeight.toPx() }

    val width = widthPx * 0.25f // 25% of screen width

    return object : Shape {
        override fun createOutline(
            size: Size,
            layoutDirection: LayoutDirection,
            density: Density
        ): Outline {
            return Outline.Rectangle(Rect(0f, 0f, width, heightPx))
        }
    }
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    Surface(color = MaterialTheme.colors.background) {
        Scaffold(
            scaffoldState = scaffoldState,
            backgroundColor = MaterialTheme.colors.background,
            drawerShape = customShape(),
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (scaffoldState.drawerState.isOpen) {
                                    scaffoldState.drawerState.close()
                                } else {
                                    scaffoldState.drawerState.open()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open drawer")
                        }
                    },
                    actions = {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Filled.NightsStay else Icons.Filled.WbSunny,
                            contentDescription = "Theme switch"
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onThemeChange,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                        if (isDarkTheme) {
                            Toast.makeText(
                                LocalContext.current,
                                "Dark theme is enabled",
                                Toast.LENGTH_SHORT
                            ).show()
                            //print log isDarkTheme
                            Log.d("isDarkTheme", isDarkTheme.toString())
                        } else {
                            Toast.makeText(
                                LocalContext.current,
                                "Light theme is enabled",
                                Toast.LENGTH_SHORT
                            ).show()
                            //print log isDarkTheme
                            Log.d("isDarkTheme", isDarkTheme.toString())
                        }
                    }
                )
            },
            drawerContent = {

                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .width(200.dp) // Set the width
                        .height(400.dp) // Set the height
                ) {
                    Column {
                        //add close button to drawer
                        IconButton(onClick = {
                            coroutineScope.launch {
                                if (scaffoldState.drawerState.isOpen) {
                                    scaffoldState.drawerState.close()
                                } else {
                                    scaffoldState.drawerState.open()
                                }
                            }
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close drawer")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        LazyColumn {
                            items(listOf("All Files", "Photos", "Videos")) { item ->
                                ListItem(
                                    text = { Text(text = item, style = MaterialTheme.typography.h6) },
                                    icon = {
                                        when (item) {
                                            "All Files" -> Icon(Icons.Filled.Folder, contentDescription = "This tab shows all files in the device")
                                            "Photos" -> Icon(Icons.Filled.Photo, contentDescription = "This tab shows all photos in the device")
                                            "Videos" -> Icon(Icons.Filled.VideoLibrary, contentDescription = "This tab shows all videos in the device")
                                        }
                                    },
                                    modifier = Modifier.clickable {
                                        when (item) {
                                            "All Files" -> {
                                                Log.d("ListItemClick", "All Files clicked")
                                            }
                                            "Photos" -> {
                                                Log.d("ListItemClick", "Photos clicked")
                                            }
                                            "Videos" -> {
                                                Log.d("ListItemClick", "Videos clicked")
                                            }
                                        }
                                    }
                                )
                                Divider()
                            }
                        }
                    }
                }
            },
            content = { paddingValues ->
                //set Box color to surface color
                Box(
                    modifier = Modifier
                        .padding(paddingValues)
                        .background(MaterialTheme.colors.surface) // Change this line
                ){
                    Column(
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        Text(
                            text = "Hello, World!",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.h6
                        )
                    }
                }
            }
        )
    }
}