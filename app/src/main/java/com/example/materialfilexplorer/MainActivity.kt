package com.example.materialfilexplorer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.materialfilexplorer.ui.theme.MaterialFileXplorerTheme
import kotlinx.coroutines.launch

private const val THEME_PREFS = "theme_prefs"
private const val IS_DARK_THEME = "is_dark_theme"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        var isDarkTheme by mutableStateOf(sharedPref.getBoolean(IS_DARK_THEME, true))

        setContent {
            MaterialFileXplorerTheme(isInDarkTheme = isDarkTheme) {
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                LaunchedEffect(isDarkTheme) {
                    with(sharedPref.edit()) {
                        putBoolean("is_dark_theme", isDarkTheme)
                        apply()
                    }
                }

                ModalNavigationDrawer(

                    drawerContent = {
                        Column(

                        ) {
                            Box(modifier =
                            Modifier) {
                                //make close button to close drawer
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            drawerState.close()
                                        }
                                    },
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Close drawer"
                                    )
                                }
                            }
                            Divider()
                            val items = listOf("All Files", "Photos", "Videos")
                            LazyColumn {
                                //make this LaxyColumn size 25% of the screen width
                                item {
                                    Spacer(modifier = Modifier.width(0.25f * 360.dp))
                                }
                                items(items) { item ->
                                    Text(
                                        text = item,
                                        modifier = Modifier
                                            //half of the screen width
                                            .width(0.5f * 360.dp)
                                            .padding(16.dp)
                                            .clickable { /* Handle item click here */ }
//                                            .background(MaterialTheme.colors.surface)
                                    )
                                }
                            }
                        }
                    },
                    drawerState = drawerState,
                    gesturesEnabled = true,
                    content = {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("Material File Xplorer") },
                                    navigationIcon = {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    if (drawerState.currentValue == DrawerValue.Closed) {
                                                        drawerState.open()
                                                    } else {
                                                        drawerState.close()
                                                    }
                                                }
                                            },
                                        ) {
                                            Icon(
                                                Icons.Filled.Menu,
                                                contentDescription = "Open drawer"
                                            )
                                        }
                                    },
                                    actions = {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Filled.NightsStay else Icons.Filled.WbSunny,
                                            contentDescription = "Theme switch"
                                        )
                                        Switch(
                                            checked = isDarkTheme,

                                            onCheckedChange = { isDarkTheme = it }
                                        )
                                    }
                                )
                            },content = { contentPadding ->
                                Box(modifier = Modifier.padding(contentPadding)) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text(text = "Hello World!")
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }
    }
}