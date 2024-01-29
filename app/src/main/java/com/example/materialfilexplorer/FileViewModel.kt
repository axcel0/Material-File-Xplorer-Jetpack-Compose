package com.example.materialfilexplorer

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
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

    fun loadDirectory(directory: File?) {
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
    fun createDirectory(context: Context, directoryName: String) {
        val currentDirectory = currentDirectory.value
        val newDirectory = File(currentDirectory, directoryName)
        if (newDirectory.exists()) {
            Toast.makeText(context, "Directory already exists", Toast.LENGTH_SHORT).show()
        } else {
            newDirectory.mkdir()
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

    fun copyFiles(source: Set<File>, destination: File) {
        source.forEach {
            val destinationFile = File(destination, it.name)
            it.copyTo(destinationFile, overwrite = true)
        }
    }
    fun pasteFiles(source: Set<File>, destination: File) {
        source.forEach {
            val destinationFile = File(destination, it.name)
            it.copyTo(destinationFile, overwrite = true)
            it.delete() // delete the source file after copying
        }
    }

    fun moveFiles(source: Set<File>, destination: File) {
        source.forEach {
            val destinationFile = File(destination, it.name)
            it.renameTo(destinationFile)
        }
    }

    // Function to check if a file is selected
    fun isSelected(file: File): Boolean {
        return _selectedFiles.value?.contains(file) ?: false
    }

    fun renameFiles(source: File, newName: String) {
        val destination = File(source.parentFile, newName)
        source.renameTo(destination)
    }

    fun deleteFiles(source: Set<File>) {
        source.forEach {
            it.deleteRecursively()
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

    // Function to select all files
    fun selectAllFiles() {
        _selectedFiles.value = files.value?.toSet()
    }

    fun countSelectedFiles(): Int {
        return _selectedFiles.value?.size ?: 0
    }

    fun clearSelectedFiles() {
        _selectedFiles.value = emptySet()
    }

    fun getSelectedFiles(): Set<File> {
        return _selectedFiles.value ?: emptySet()
    }



}