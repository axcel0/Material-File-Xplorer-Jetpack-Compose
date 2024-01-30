package com.example.materialfilexplorer

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Stack

class FileViewModel : ViewModel() {

    val files: LiveData<List<File>> = MutableLiveData()
    val currentDirectory: LiveData<File> = MutableLiveData()
    val directoryStack = Stack<File>()
    private val _currentPath: MutableLiveData<String> = MutableLiveData("/")
    val currentPath: LiveData<String> = _currentPath
    // This set will hold all selected files
    private val _selectedFiles = MutableLiveData<Set<File>>(emptySet())
    val selectedFiles: LiveData<Set<File>> = _selectedFiles

    fun loadDirectory(directory: File? = File("/storage/emulated/0")) {
        if (directory != null) {
            val path = directory.absolutePath
            directoryStack.push(directory)
            _currentPath.value = directoryStack.joinToString(separator = "/") { it.name }
            if (path.contains("/Android/data") || path.contains("/Android/obb")) {
                return
            }
            (files as MutableLiveData).value = directory.listFiles()?.toList()
            (currentDirectory as MutableLiveData).value = directory
        } else {
            if (directoryStack.isNotEmpty()) {
                val parentDirectory = directoryStack.pop()
                if (parentDirectory != null) {
                    _currentPath.value = directoryStack.joinToString(separator = "/") { it.name }
                    loadDirectory(parentDirectory)
                } else {
                    _currentPath.value = "/"
                }
            }
        }
    }

    fun createItem(context: Context, name: String, isDirectory: Boolean) {
        val currentDirectory = currentDirectory.value ?: return
        val newFile = File(currentDirectory, name)
        val result = if (isDirectory) newFile.mkdirs() else newFile.createNewFile()

        if (result) {
            // Refresh the files list
            loadDirectory(currentDirectory)
        } else {
            Toast.makeText(context, "Failed to create item", Toast.LENGTH_SHORT).show()
        }
    }


    fun loadFilesWithExtension(directory: File, extension: String) {
        val filteredFiles = directory.listFiles { _, name -> name.endsWith(extension, ignoreCase = true) }?.toList()
        (files as MutableLiveData).value = filteredFiles
        (currentDirectory as MutableLiveData).value = directory
    }

    fun searchFiles(query: String) {
        val currentFiles = files.value ?: return
        val filteredFiles = currentFiles.filterTo(mutableListOf()) { it.name.contains(query, ignoreCase = true) }
        (files as MutableLiveData).value = filteredFiles
    }

    fun copyFiles(selectedFiles: Set<File>, destinationDirectory: File) {
        selectedFiles.forEach { file ->
            val destinationFile = File(destinationDirectory, file.name)
            try {
                Files.copy(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        // Refresh the files list
        loadDirectory(destinationDirectory)
    }

    fun deleteFiles(selectedFiles: Set<File>) {
        selectedFiles.forEach { file ->
            val result = file.delete()
            if (!result) {
                println("Failed to delete ${file.name}")
            }
        }
        // Refresh the files list
        val currentDirectory = currentDirectory.value
        if (currentDirectory != null) {
            loadDirectory(currentDirectory)
        }
    }

    fun pasteFiles(source: Set<File>, destination: File) {
        source.forEach { file ->
            val destinationFile = File(destination, file.name)
            try {
                Files.copy(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        // Refresh the files list
        loadDirectory(destination)
    }

    fun moveFiles(selectedFiles: Set<File>, destinationDirectory: File) {
        selectedFiles.forEach { file ->
            val destinationFile = File(destinationDirectory, file.name)
            try {
                Files.move(file.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        // Refresh the files list
        loadDirectory(destinationDirectory)
    }

    // Function to check if a file is selected
    fun isSelected(file: File): Boolean {
        return _selectedFiles.value?.contains(file) ?: false
    }

    fun renameFiles(source: File, newName: String) {
        try {
            val destinationFile = File(source.parent, newName)
            source.renameTo(destinationFile)
        } catch (e: Exception) {
            // Handle the exception here
            println("Error renaming file: ${source.name}")
            println("Exception: $e")
        }
    }


    // Function to select or deselect a file
    fun selectFile(file: File, isSelected: Boolean) {
        val newSet = _selectedFiles.value?.toMutableSet() ?: mutableSetOf()
        if (isSelected) {
            newSet.add(file)
        } else {
            newSet.remove(file)
        }
        _selectedFiles.value = newSet
    }


    fun countSelectedFiles(): Int {
        return _selectedFiles.value?.size ?: 0
    }

    fun getSelectedFiles(): Set<File> {
        return _selectedFiles.value ?: emptySet()
    }



}