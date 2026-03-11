package ca.kent.raunchlaunch

import android.content.Context

data class Directory(val name: String, val apps: List<String>)

const val ALL_APPS_DIR = "ALL_APPS"

val mockAvailableApps = listOf(
    "TERMINAL", "FILES", "BROWSER", "MEDIA", "SYSTEM",
    "CALCULATOR", "CAMERA", "CLOCK", "CONTACTS", "EMAIL",
    "MAPS", "NOTES", "PHONE", "SLACK", "JIRA", "OUTLOOK",
    "TEAMS", "DOCS", "SPOTIFY", "YOUTUBE", "NETFLIX",
    "PHOTOS", "VLC", "SETTINGS", "LOGCAT", "SHELL", "STORAGE", "CPU"
)

object PersistenceManager {
    fun saveDirectories(context: Context, directories: List<Directory>) {
        val prefs = context.getSharedPreferences("raunch_prefs", Context.MODE_PRIVATE)
        val serialized = directories.joinToString("|") { "${it.name}:${it.apps.joinToString(",")}" }
        prefs.edit().putString("directories", serialized).apply()
    }

    fun loadDirectories(context: Context): List<Directory> {
        val prefs = context.getSharedPreferences("raunch_prefs", Context.MODE_PRIVATE)
        val serialized = prefs.getString("directories", null)
        
        val loaded = if (serialized == null) {
            listOf(
                Directory("FAVORITES", listOf("TERMINAL", "FILES", "BROWSER", "MEDIA", "SYSTEM")),
                Directory("WORK", listOf("SLACK", "JIRA", "OUTLOOK")),
                Directory("SYSTEM", listOf("SETTINGS", "LOGCAT", "SHELL"))
            )
        } else {
            serialized.split("|").filter { it.isNotEmpty() }.map {
                val parts = it.split(":")
                val name = parts[0]
                val apps = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].split(",") else emptyList()
                Directory(name, apps)
            }
        }

        val finalDirs = loaded.toMutableList()
        val allAppsIndex = finalDirs.indexOfFirst { it.name == ALL_APPS_DIR }
        if (allAppsIndex != 0) {
            if (allAppsIndex != -1) {
                val existing = finalDirs.removeAt(allAppsIndex)
                finalDirs.add(0, existing.copy(apps = emptyList()))
            } else {
                finalDirs.add(0, Directory(ALL_APPS_DIR, emptyList()))
            }
        }
        return finalDirs
    }
}
