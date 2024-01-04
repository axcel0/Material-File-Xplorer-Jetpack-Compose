import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File

@Composable
fun FileListScreen(dir: File) {
    val viewModel: FileViewModel = viewModel()
    viewModel.loadFiles(dir)

    LazyColumn {
        items(viewModel.files.value) { file ->
            Text(text = file.name)
        }
    }
}