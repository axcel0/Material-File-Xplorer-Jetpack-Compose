package com.example.materialfilexplorer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import com.example.materialfilexplorer.ui.theme.MaterialFileXplorerTheme
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

private const val THEME_PREFS = "theme_prefs"
private const val IS_DARK_THEME = "is_dark_theme"

@Composable
fun CustomDrawer(
    drawerContent: @Composable (CoroutineScope, DrawerState) -> Unit,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val drawerWidth = LocalConfiguration.current.screenWidthDp.dp * 0.25f
    val drawerOffset by animateDpAsState(
        targetValue = if (drawerState.currentValue == DrawerValue.Open) drawerWidth else 0.dp,
        animationSpec = tween(300), label = ""
    )

    Box(
        Modifier.fillMaxSize()
    ) {
        content()

        Box(
            Modifier
                .offset(x = drawerOffset)
                .width(drawerWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colors.surface)
        ) {
            drawerContent(scope, drawerState)
        }

        if (drawerState.currentValue == DrawerValue.Open) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    private var allFiles by mutableStateOf(listOf<File>())

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalMaterialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
        var isDarkTheme by mutableStateOf(sharedPref.getBoolean(IS_DARK_THEME, true))

        setContent {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            LaunchedEffect(isDarkTheme) {
                with(sharedPref.edit()) {
                    putBoolean("is_dark_theme", isDarkTheme)
                    apply()
                }
            }

            MaterialFileXplorerTheme(isInDarkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Row {
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
                                    Text("Material File Xplorer")
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
                        CustomDrawer(
                            drawerContent = { scope, drawerState ->
                                Column {
                                    Row(
                                        Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    if (drawerState.currentValue == DrawerValue.Closed) {
                                                        drawerState.open()
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Drawer opened",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    } else {
                                                        drawerState.close()
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Drawer closed",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            },
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Close drawer")
                                        }
                                    }
                                    Divider()
                                    LazyColumn {
                                        items(listOf("All Files", "Photos", "Videos")) { item ->
                                            ListItem(
                                                text = { Text(text = item) },
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
                                                            // show all files in the device
                                                            Toast.makeText(this@MainActivity, "All Files", Toast.LENGTH_SHORT).show()
                                                        }
                                                        "Photos" -> {
                                                            // show all photos in the device
                                                            Toast.makeText(this@MainActivity, "Photos", Toast.LENGTH_SHORT).show()
                                                        }
                                                        "Videos" -> {
                                                            // show all videos in the device
                                                            Toast.makeText(this@MainActivity, "Videos", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            },
                        ) {
                            FileList(files = allFiles)
                        }
                    }

                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
        }
    }

    private fun showAllFiles() {
        val internalFiles = getFiles(Environment.getDataDirectory())
        val externalFiles = getFiles(Environment.getExternalStorageDirectory())
        allFiles = internalFiles + externalFiles
    }

    private fun getFiles(dir: File): List<File> {
        val files = mutableListOf<File>()
        if (dir.exists()) {
            val fileList = dir.listFiles()
            if (fileList != null) {
                for (file in fileList) {
                    if (file.isDirectory) {
                        files.addAll(getFiles(file))
                    } else {
                        files.add(file)
                    }
                }
            }
        }
        return files
    }

    @Composable
    fun FileList(files: List<File>) {
        LazyColumn {
            items(files) { file ->
                Text(text = file.name)
            }
        }
    }
}