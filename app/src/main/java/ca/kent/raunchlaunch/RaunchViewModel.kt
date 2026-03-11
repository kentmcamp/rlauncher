package ca.kent.raunchlaunch

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel

class RaunchViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    // --- State ---
    var directories by mutableStateOf(PersistenceManager.loadDirectories(context))
        private set
    
    var currentDirIndex by mutableIntStateOf(0)
    var scrollIndex by mutableIntStateOf(0)
    
    var isMenuVisible by mutableStateOf(false)
    var isEditMode by mutableStateOf(false)
    
    var selectedSlotRight by mutableIntStateOf(-1)
    var selectedSlotLeft by mutableIntStateOf(-1)

    val currentDir: Directory?
        get() = directories.getOrNull(currentDirIndex)

    val currentItems: List<String>
        get() {
            val dir = currentDir ?: return emptyList()
            return if (dir.name == ALL_APPS_DIR) mockAvailableApps else dir.apps
        }

    fun updateDirectories(newDirs: List<Directory>) {
        directories = newDirs
        PersistenceManager.saveDirectories(context, newDirs)
    }

    fun switchDirectory(index: Int) {
        if (index in directories.indices && index != currentDirIndex) {
            currentDirIndex = index
            scrollIndex = 0
        }
    }

    fun scrollUp() {
        if (scrollIndex > 0) scrollIndex--
    }

    fun scrollDown() {
        val maxVisible = 5
        if (scrollIndex < currentItems.size - maxVisible) scrollIndex++
    }
}
