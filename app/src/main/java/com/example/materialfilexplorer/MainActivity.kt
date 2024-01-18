@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)

package com.example.materialfilexplorer

import android.content.pm.PackageManager
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

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
                fileViewModel.loadDirectory(currentDirectory.parentFile!!)
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
            if (!fileViewModel.directoryStack.empty()) {
                fileViewModel.loadDirectory(null)

            } else {
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
                    // App is running on TV, show a confirmation dialog before exiting
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle("Exit")
                        .setMessage("Are you sure you want to exit?")
                        .setPositiveButton("Yes") { _, _ ->
                            finish()
                        }
                        .setNegativeButton("No") { _, _ -> }
                        .show()
                } else {
                    finish()
                }
            }
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            // App is running on TV, load the directory without asking for permissions
            fileViewModel.loadDirectory(Environment.getExternalStorageDirectory())
        } else {
            // App is not running on TV, request permissions as usual
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.all { it.value }) {
                    fileViewModel.loadDirectory(Environment.getExternalStorageDirectory())
                } else {
                    Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show()
                }
            }

            requestPermissionLauncher.launch(arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
        fileViewModel.currentPath.observe(this) {
            title = it
        }
        fileViewModel.currentDirectory.observe(this) {
            if (it != null) {
                title = it.name
            }
        }
        fileViewModel.selectedFiles.observe(this) {
            title = if (it.isNotEmpty()) {
                "${it.size} selected"
            } else {
                fileViewModel.currentPath.value
            }
        }

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
    fun CurrentPathSection(fileViewModel: FileViewModel) {
        val currentPath by fileViewModel.currentPath.observeAsState("/")
        Text(
            text = currentPath,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }

    @Composable
    fun MainContent(isDarkTheme: Boolean, onDarkModeChange: (Boolean) -> Unit = {}, fileViewModel: FileViewModel) {
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val contentView by lazy { ContentView(fileViewModel) }

        ModalNavigationDrawer(
            drawerContent = { DrawerSheet(drawerState) },
            drawerState = drawerState,
        ) {
            Scaffold (
                topBar = { TopBar(isDarkTheme, onDarkModeChange, drawerState) },
                content = { innerPadding ->
                    Column {
                        CurrentPathSection(fileViewModel)
                        contentView.Content(innerPadding)
                    }
                },
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
            title = { Text("Material File Xplorer") },
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
                IconButton(onClick = {
                    fileViewModel.searchFiles("query")
                }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search"
                    )
                }
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
                    fileViewModel.loadDirectory(Environment.getExternalStorageDirectory())
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


