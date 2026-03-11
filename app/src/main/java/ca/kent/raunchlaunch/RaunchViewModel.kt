package ca.kent.raunchlaunch

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel

class RaunchViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext

    // --- State ---
    var directories by mutableStateOf(PersistenceManager.loadDirectories(context))
        private set
    
    var installedApps by mutableStateOf(AppProvider.getInstalledApps(context))
        private set

    var currentDirIndex by mutableIntStateOf(0)
    var scrollIndex by mutableIntStateOf(0)
    
    var isMenuVisible by mutableStateOf(false)
    var isEditMode by mutableStateOf(false)
    
    var selectedSlotRight by mutableIntStateOf(-1)
    var selectedSlotLeft by mutableIntStateOf(-1)

    // Pinned Apps scrolling state
    var pinnedScrollIndex by mutableIntStateOf(0)

    val currentDir: Directory?
        get() = directories.getOrNull(currentDirIndex)

    val pinnedApps: List<AppInfo>
        get() {
            val pinnedDir = directories.find { it.name == PINNED_APPS_DIR } ?: return emptyList()
            return pinnedDir.packageNames.mapNotNull { pkg ->
                installedApps.find { it.packageName == pkg }
            }
        }

    val currentItems: List<AppInfo>
        get() {
            val dir = currentDir ?: return emptyList()
            // No longer filtering out pinned apps. Apps can appear in multiple directories.
            return if (dir.name == ALL_APPS_DIR) {
                installedApps
            } else {
                dir.packageNames.mapNotNull { pkg ->
                    installedApps.find { it.packageName == pkg }
                }
            }
        }

    fun updateDirectories(newDirs: List<Directory>) {
        directories = newDirs
        PersistenceManager.saveDirectories(context, newDirs)
    }

    fun moveDirectory(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in directories.indices || toIndex !in directories.indices) return
        val list = directories.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        updateDirectories(list)
        if (currentDirIndex == fromIndex) currentDirIndex = toIndex
    }

    fun moveAppInDirectory(dirIndex: Int, fromIndex: Int, toIndex: Int) {
        val dir = directories.getOrNull(dirIndex) ?: return
        val packages = dir.packageNames.toMutableList()
        if (fromIndex !in packages.indices || toIndex !in packages.indices) return
        val pkg = packages.removeAt(fromIndex)
        packages.add(toIndex, pkg)
        
        val newList = directories.toMutableList()
        newList[dirIndex] = dir.copy(packageNames = packages)
        updateDirectories(newList)
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

    fun scrollPinnedUp() {
        if (pinnedScrollIndex > 0) pinnedScrollIndex--
    }

    fun scrollPinnedDown() {
        val maxVisible = 5
        if (pinnedScrollIndex < pinnedApps.size - maxVisible) pinnedScrollIndex++
    }

    fun launchSelectedApp() {
        val maxVisible = 5
        val displayItems = currentItems.subList(
            scrollIndex,
            (scrollIndex + maxVisible).coerceAtMost(currentItems.size)
        )
        val appIndex = selectedSlotRight - 1
        if (appIndex in displayItems.indices) {
            val app = displayItems[appIndex]
            AppProvider.launchApp(context, app.packageName)
        }
    }

    fun launchApp(packageName: String) {
        AppProvider.launchApp(context, packageName)
    }

    fun refreshApps() {
        installedApps = AppProvider.getInstalledApps(context)
    }
}
