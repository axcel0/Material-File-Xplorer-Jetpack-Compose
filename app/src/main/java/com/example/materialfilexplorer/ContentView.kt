package com.example.materialfilexplorer

import android.content.Context
import androidx.compose.material.ListItem
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class ContentView(private val fileViewModel: FileViewModel) {
    @Composable
    fun Content(scaffoldPadding: PaddingValues) {
        val files by fileViewModel.files.observeAsState(emptyList())
        val context = LocalContext.current
        var isGridView by remember { mutableStateOf(false) }
        var sortType by remember { mutableStateOf("name") }
        var sortOrder by remember { mutableStateOf("ascending") }
        val sortedFiles = files.sortBy(sortType, sortOrder)
        var expanded by remember { mutableStateOf(false) }
        val currentDirectory by fileViewModel.currentDirectory.observeAsState()
        val selectedFiles = fileViewModel.selectedFiles.value ?: emptySet()


        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "")
                Row {
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Filled.ViewList else Icons.Filled.ViewModule,
                            contentDescription = if (isGridView) "Switch to List View" else "Switch to Grid View"
                        )
                    }
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.Sort,
                            contentDescription = "Sort"
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val list = listOf("Sort by name", "Sort by date", "Sort by size", "Sort by type", "Sort by directory")
                        list.forEach { item ->
                            DropdownMenuItem(onClick = {
                                expanded = false
                                sortType = when (item) {
                                    "Sort by name" -> "name"
                                    "Sort by date" -> "date"
                                    "Sort by size" -> "size"
                                    "Sort by type" -> "type"
                                    "Sort by directory" -> "isDirectory"
                                    else -> "name"
                                }
                                sortOrder = if (sortOrder == "ascending") "descending" else "ascending"
                            }) {
                                Text(text = item)
                            }
                        }
                    }
                    var moreVertExpanded by remember { mutableStateOf(false) }

                    IconButton(onClick = { moreVertExpanded = !moreVertExpanded }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More"
                        )
                    }

                    DropdownMenu(
                        expanded = moreVertExpanded,
                        onDismissRequest = { moreVertExpanded = false }
                    ) {
                        val list = listOf("Create", "Copy", "Paste", "Delete", "Move", "Select All", "Cancel")
                        list.forEach { item ->
                            DropdownMenuItem(onClick = {
                                moreVertExpanded = false
                                when (item) {
                                    "Create" -> {
                                        MaterialAlertDialogBuilder(context)
                                            .setTitle("Create")
                                            .setPositiveButton("Create") { _, _ ->
                                                fileViewModel.createDirectory(context, "New Folder")
                                            }
                                            .setNegativeButton("Cancel") { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                            .show()
                                    }
                                    "Copy"
                                    -> {
                                        fileViewModel.copyFiles(selectedFiles, currentDirectory!!)
                                    }
                                    "Paste"
                                    -> {
                                        fileViewModel.pasteFiles(selectedFiles, currentDirectory!!)
                                    }
                                    "Delete"
                                    -> {
                                        MaterialAlertDialogBuilder(context)
                                            .setTitle("Delete")
                                            .setMessage("Are you sure you want to delete the selected files?")
                                            .setPositiveButton("Delete") { _, _ ->
                                                fileViewModel.deleteFiles(selectedFiles)
                                            }
                                            .setNegativeButton("Cancel") { dialog, _ ->
                                                dialog.dismiss()
                                            }
                                            .show()
                                    }
                                    "Move"
                                    -> {
                                        fileViewModel.moveFiles(selectedFiles, currentDirectory!!)
                                    }
                                    "Select All"
                                    -> {
                                        fileViewModel.selectAllFiles(sortedFiles)
                                    }
                                    "Cancel"
                                    -> {
                                        fileViewModel.clearSelectedFiles()
                                    }
                                }
                            }) {
                                Text(text = item)
                            }
                        }
                    }
                }
            }

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = scaffoldPadding,
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(sortedFiles) { file ->
                        FileItem(file = file, context = context, fileViewModel = fileViewModel, isGridView = true)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = scaffoldPadding,
                    modifier = Modifier.padding(16.dp)
                ) {
                    items(sortedFiles) { file ->
                        FileItem(file = file, context = context, fileViewModel = fileViewModel, isGridView = false)
                    }
                }
            }
        }
    }


    private fun List<File>.sortBy(sortType: String, sortOrder: String): List<File> {
        return when (sortType) {
            "name" -> if (sortOrder == "ascending") sortedBy { it.name } else sortedByDescending { it.name }
            "date" -> if (sortOrder == "ascending") sortedBy { it.lastModified() } else sortedByDescending { it.lastModified() }
            "size" -> if (sortOrder == "ascending") sortedBy { it.length() } else sortedByDescending { it.length() }
            "type" -> if (sortOrder == "ascending") sortedBy { it.extension } else sortedByDescending { it.extension }
            "isDirectory" -> if (sortOrder == "ascending") sortedWith(compareBy<File> { it.isDirectory }.thenBy { it.name }) else sortedWith(compareBy<File> { it.isDirectory }.thenByDescending { it.name }).reversed()
            else -> this
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    fun FileItem(file: File, context: Context, fileViewModel: FileViewModel, isGridView: Boolean) {
        var isSelected by remember { mutableStateOf(false) }

        ListItem(
            text = {
                if (isGridView) {
                    Text(file.name, fontSize = 20.sp)
                } else {
                    Text(file.name)
                }
            },
            icon = {
                if (file.isDirectory) {
                    if (isGridView) {
                        Icon(imageVector = Icons.Filled.Folder, contentDescription = "Folder", modifier = Modifier.size(48.dp), tint = Color(0xFFFFA400))
                    } else {
                        Icon(imageVector = Icons.Filled.Folder, contentDescription = "Folder", tint = Color(0xFFFFA400))
                    }
                }
            },
            modifier = Modifier.clickable {
                if (file.isDirectory) {
                    fileViewModel.loadDirectory(file)
                } else {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.fromFile(file)
                    context.startActivity(intent)
                }
            },
            trailing = {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = {
                        isSelected = it
                        fileViewModel.selectFile(file, isSelected)
                    }
                )
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