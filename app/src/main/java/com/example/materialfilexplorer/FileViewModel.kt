package com.example.materialfilexplorer

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.Stack
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class FileViewModel : ViewModel() {

    val files: LiveData<List<File>> = MutableLiveData()
    val currentDirectory: LiveData<File> = MutableLiveData()
    val directoryStack = Stack<File>()
    private val _currentPath: MutableLiveData<String> = MutableLiveData("/")
    val currentPath: LiveData<String> = _currentPath

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
            if (directoryStack.size > 1) {
                val parentDirectory = directoryStack.pop().parentFile
                directoryStack.clear()
                if (parentDirectory != null) {
                    directoryStack.push(parentDirectory)
                    _currentPath.value = directoryStack.joinToString(separator = "/") { it.name }
                    loadDirectory(parentDirectory)
                } else {
                    _currentPath.value = "/"
                }
            }
        }
    }


    fun searchFiles(query: String) {
        val currentFiles = files.value
        val filteredFiles = currentFiles?.filter { it.name.contains(query, ignoreCase = true) }
        (files as MutableLiveData).value = filteredFiles
    }

    fun loadFilesWithExtension(directory: File, extension: String) {
        val filteredFiles = directory.listFiles { _, name -> name.endsWith(extension, ignoreCase = true) }?.toList()
        (files as MutableLiveData).value = filteredFiles
        (currentDirectory as MutableLiveData).value = directory
    }

}