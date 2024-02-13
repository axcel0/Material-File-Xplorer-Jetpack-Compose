@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.example.materialfilexplorer

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        return if (keyCode == KeyEvent.KEYCODE_BACK) {
//            val currentDirectory = fileViewModel.currentDirectory.value
//            if (currentDirectory?.parentFile != null) {
//                fileViewModel.loadDirectory(currentDirectory.parentFile!!)
//            } else {
//                finish()
//            }
//            true
//        } else {
//            super.onKeyDown(keyCode, event)
//        }
//    }

    private val fileViewModel: FileViewModel by viewModels {
        FileViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this) {
            val currentDirectory = fileViewModel.currentDirectory.value
            if (currentDirectory?.parentFile != null) {
                fileViewModel.loadInternalStorage(currentDirectory.parentFile!!)
            } else {
                //add dialog to confirm exit
                MaterialAlertDialogBuilder(this@MainActivity)
                    .setTitle("Exit")
                    .setMessage("Are you sure you want to exit?")
                    .setPositiveButton("Yes") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("No") { _, _ ->
                        // Respond to negative button press
                    }
                    .show()
            }
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)) {
            // App is running on TV, load the directory without asking for permissions
            fileViewModel.loadInternalStorage(Environment.getExternalStorageDirectory())
        } else {
            // App is not running on TV, request permissions as usual
            val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                if (permissions.all { it.value }) {
                    fileViewModel.loadInternalStorage(Environment.getExternalStorageDirectory())
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
            drawerContent = { DrawerSheet(drawerState)},
            drawerState = drawerState,
        ) {
            Scaffold (
                topBar = { TopBar(isDarkTheme, onDarkModeChange, drawerState, fileViewModel) },
                content = { innerPadding ->
                    Column {
                        CurrentPathSection(fileViewModel)
                        contentView.Content(innerPadding)
                    }
                },
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBar(
        isDarkTheme: Boolean,
        onDarkModeChange: (Boolean) -> Unit = {},
        drawerState: DrawerState,
        fileViewModel: FileViewModel
    ) {
        val scope = rememberCoroutineScope()
        var isSearchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val focusManager = LocalFocusManager.current

        TopAppBar(
            title = {
                if (isSearchActive) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { newValue ->
                            searchQuery = newValue
                            if (searchQuery.isNotEmpty()) {
                                fileViewModel.searchFiles(searchQuery)
                            } else {
                                fileViewModel.currentDirectory.value?.let {
                                    fileViewModel.loadInternalStorage(it)
                                }
                            }
                        },
                        placeholder = { Text("Search") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            isSearchActive = false
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                isSearchActive = true
                            }
                        }
                    )
                } else {
                    Text("Material File Xplorer", style = MaterialTheme.typography.bodyMedium)
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
                IconButton(onClick = {
                    isSearchActive = !isSearchActive
                    if (isSearchActive) {
                        fileViewModel.searchFiles(searchQuery)
                    }
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
                    fileViewModel.currentDirectory.value?.let {
                        fileViewModel.loadInternalStorage(it)
                    }
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
                    fileViewModel.currentDirectory.value?.let {
                        it.listFiles()
                            ?.let { it1 -> fileViewModel.filterFilesByExtension(it1.toList(), listOf("mp4", "avi", "mov")) }
                    }
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
                    fileViewModel.currentDirectory.value?.let {
                        it.listFiles()
                            ?.let { it1 -> fileViewModel.filterFilesByExtension(it1.toList(), listOf("jpg", "png", "img")) }
                    }
                    scope.launch {
                        drawerState.close()
                    }
                },
            )

            NavigationDrawerItem(
                icon = { Icon(Icons.Filled.Usb, contentDescription = "External Storage") },
                label = { Text("External Storage") },
                selected = false,
                onClick = {

                    scope.launch {
                        drawerState.close()
                    }
                },
            )
        }
    }

    @Composable
    fun MaterialFileXplorerTheme(
        isInDarkTheme: Boolean = isSystemInDarkTheme(),
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


