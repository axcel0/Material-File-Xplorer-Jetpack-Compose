@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.materialfilexplorer

import androidx.compose.material.ListItem
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFERENCE_THEME = "preference_theme"
        private const val DARK_MODE_PREF = "dark_mode"
    }

    private val sharedPreferences by lazy {
        getSharedPreferences(PREFERENCE_THEME, MODE_PRIVATE)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            val currentDirectory = fileViewModel.currentDirectory.value
            if (currentDirectory?.parentFile != null) {
                fileViewModel.loadFiles(currentDirectory.parentFile!!)
            } else {
                finish()
            }
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }
    private val fileViewModel: FileViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            val currentDirectory = fileViewModel.currentDirectory.value
            if (currentDirectory?.parentFile != null) {
                fileViewModel.loadFiles(currentDirectory.parentFile!!)
            } else {
                finish()
            }
        }
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                fileViewModel.loadFiles(Environment.getExternalStorageDirectory())
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
            }
        }

        requestPermissionLauncher.launch(arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ))
        setContent {
            var isDarkTheme by remember { mutableStateOf(sharedPreferences.getBoolean(DARK_MODE_PREF, false)) }
            val onDarkModeChange : (Boolean) -> Unit = {
                isDarkTheme = it
                sharedPreferences.edit().putBoolean(DARK_MODE_PREF, it).apply()
            }
            MaterialFileXplorerTheme(isInDarkTheme = isDarkTheme) {
                MainContent(isDarkTheme, onDarkModeChange, fileViewModel)
            }
        }
    }

    @Composable
    fun MainContent(isDarkTheme: Boolean, onDarkModeChange: (Boolean) -> Unit = {}, fileViewModel: FileViewModel) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        ModalNavigationDrawer(
            drawerContent = { DrawerSheet(drawerState) },
            drawerState = drawerState,
        ) {
            Scaffold (
                topBar = { TopBar(isDarkTheme, onDarkModeChange, drawerState) },
                content = { innerPadding -> Content(innerPadding) },
            )
        }
    }

    @Composable
    fun TopBar(
        isDarkTheme: Boolean,
        onDarkModeChange: (Boolean) -> Unit = {},
        drawerState: DrawerState
    ) {

        val scope = rememberCoroutineScope()
        TopAppBar(
            title = {
                Column {
                    Text("Material File Xplorer")
                    Divider()
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    scope.launch {
                        if (drawerState.isClosed) {
                            drawerState.open()
                        } else {
                            drawerState.close()
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open Drawer"
                    )
                }
            },
            actions = {
                Icon (
                    imageVector = if (isDarkTheme) Icons.Filled.NightsStay else Icons.Filled.WbSunny,
                    contentDescription = "Toggle Light/Dark Mode"
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onDarkModeChange
                )
            }
        )
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun Content(scaffoldPadding: PaddingValues) {
        val files by fileViewModel.files.observeAsState(emptyList())
        val context = LocalContext.current

        LazyColumn(
            contentPadding = scaffoldPadding,
            modifier = Modifier.padding(16.dp)
        ) {
            items(files) { file ->
                ListItem(
                    text = { Text(file.name) },
                    modifier = Modifier.clickable {
                        if (file.isDirectory) {
                            fileViewModel.loadFiles(file)
                        } else {
                            // Open the file using an Intent
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.fromFile(file)
                            context.startActivity(intent)
                        }
                    },
                    trailing = {
                        if (!file.isDirectory) {
                            IconButton(onClick = {
                                val destination = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.name)
                                Files.copy(file.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = "Download"
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun DrawerSheet(drawerState: DrawerState) {
        val scope = rememberCoroutineScope()
        ModalDrawerSheet {
            NavigationDrawerItem (
                icon = { Icon(Icons.Filled.Close, contentDescription = "Close Drawer") },
                label = { Text("Close Drawer") },
                selected = false,
                onClick = {
                    scope.launch {
                        drawerState.close()
                    }
                },
            )

            Divider()

            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Folder, contentDescription = "All Files") },
                label = { Text("All Files") },
                selected = false,
                onClick = {
                    fileViewModel.loadFiles(Environment.getExternalStorageDirectory())
                    scope.launch {
                        drawerState.close()
                    }
                },
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.VideoLibrary, contentDescription = "Videos") },
                label = { Text("Videos") },
                selected = false,
                onClick = {
                    fileViewModel.loadFilesWithExtension(Environment.getExternalStorageDirectory(), ".mp4")
                    scope.launch {
                        drawerState.close()
                    }
                },
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Photo, contentDescription = "Photos") },
                label = { Text("Photos") },
                selected = false,
                onClick = {
                    fileViewModel.loadFilesWithExtension(Environment.getExternalStorageDirectory(), ".jpg")
                    scope.launch {
                        drawerState.close()
                    }
                },
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Download, contentDescription = "Downloads") },
                label = { Text("Downloads") },
                selected = false,
                onClick = {
                    fileViewModel.loadFilesWithExtension(Environment.getExternalStorageDirectory(), ".apk")
                    scope.launch {
                        drawerState.close()
                    }
                },
            )
        }
    }

    @Composable
    fun MaterialFileXplorerTheme(
        isInDarkTheme: Boolean,
        content: @Composable () -> Unit,
    ) {
        val colors = if (isInDarkTheme) darkColorScheme() else lightColorScheme()

        val backgroundColor by animateColorAsState(targetValue = colors.background, label = "")
        val primaryColor by animateColorAsState(targetValue = colors.primary, label = "")
        val secondaryColor by animateColorAsState(targetValue = colors.secondary, label = "")

        val animatedColors = colors.copy(background = backgroundColor, primary = primaryColor, secondary = secondaryColor)

        MaterialTheme(
            colorScheme = animatedColors,
            shapes = Shapes(),
            content = content
        )
    }

}


