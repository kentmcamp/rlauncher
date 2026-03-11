package ca.kent.raunchlaunch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable? = null
)

data class Directory(val name: String, val packageNames: List<String>)

const val ALL_APPS_DIR = "ALL_APPS"
const val PINNED_APPS_DIR = "PINNED_APPS"

object AppProvider {
    fun getInstalledApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.map {
            AppInfo(
                label = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = it.loadIcon(pm)
            )
        }.sortedBy { it.label.uppercase() }
    }

    fun launchApp(context: Context, packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        }
    }
}

object PersistenceManager {
    fun saveDirectories(context: Context, directories: List<Directory>) {
        val prefs = context.getSharedPreferences("raunch_prefs", Context.MODE_PRIVATE)
        val serialized = directories.joinToString("|") { "${it.name}:${it.packageNames.joinToString(",")}" }
        prefs.edit().putString("directories", serialized).apply()
    }

    fun loadDirectories(context: Context): List<Directory> {
        val prefs = context.getSharedPreferences("raunch_prefs", Context.MODE_PRIVATE)
        val serialized = prefs.getString("directories", null)
        
        val loaded = if (serialized == null) {
            listOf(
                Directory("FAVORITES", emptyList()),
                Directory("WORK", emptyList()),
                Directory("SYSTEM", emptyList()),
                Directory(PINNED_APPS_DIR, emptyList())
            )
        } else {
            serialized.split("|").filter { it.isNotEmpty() }.map {
                val parts = it.split(":")
                val name = parts[0]
                val packages = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].split(",") else emptyList()
                Directory(name, packages)
            }
        }

        val finalDirs = loaded.toMutableList()
        
        // Ensure ALL_APPS is always present and at the top
        val allAppsIndex = finalDirs.indexOfFirst { it.name == ALL_APPS_DIR }
        if (allAppsIndex != -1) {
            val allApps = finalDirs.removeAt(allAppsIndex)
            finalDirs.add(0, allApps.copy(packageNames = emptyList()))
        } else {
            finalDirs.add(0, Directory(ALL_APPS_DIR, emptyList()))
        }

        // Ensure PINNED_APPS is always present
        if (finalDirs.none { it.name == PINNED_APPS_DIR }) {
            finalDirs.add(Directory(PINNED_APPS_DIR, emptyList()))
        }

        return finalDirs
    }
}
