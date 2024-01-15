package com.example.materialfilexplorer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.Stack

class FileViewModel : ViewModel() {
    val files: LiveData<List<File>> = MutableLiveData()
    val currentDirectory: LiveData<File> = MutableLiveData()
    val directoryStack = Stack<File>()

    fun loadFiles(directory: File) {
        directoryStack.push(directory)
        (files as MutableLiveData).value = directory.listFiles()?.toList()
        (currentDirectory as MutableLiveData).value = directory
    }

    fun loadPreviousDirectory() {
        if (directoryStack.size > 1) {
            directoryStack.pop()
            loadFiles(directoryStack.peek())
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