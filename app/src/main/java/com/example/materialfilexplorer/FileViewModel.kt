import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileViewModel : ViewModel() {
    val files = mutableStateOf(listOf<File>())

    fun loadFiles(dir: File) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                dir.listFiles()?.toList() ?: listOf()
            }
            files.value = result
        }
    }
}