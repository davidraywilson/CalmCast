package com.calmcast.podcast.data.download

import android.content.Context
import android.os.Environment
import android.util.Log
import com.calmcast.podcast.data.DownloadLocation
import com.calmcast.podcast.data.SettingsManager
import java.io.File

class StorageUnavailableException(message: String) : Exception(message)

object StorageManager {
    private const val TAG = "StorageManager"

    /**
     * Get the base directory for downloads based on the current preference.
     * Throws an exception if external storage is requested but unavailable.
     *
     * @param context Android context
     * @param settingsManager Settings manager with download location preference
     * @return Base directory for downloads
     * @throws StorageUnavailableException if external storage is requested but not available
     */
    fun getDownloadBaseDir(context: Context, settingsManager: SettingsManager): File {
        val location = settingsManager.getDownloadLocationSync()
        return when (location) {
            DownloadLocation.EXTERNAL -> getExternalStorageDir(context) ?: throw StorageUnavailableException("External storage is not available")
            DownloadLocation.INTERNAL -> getInternalStorageDir(context)
        }
    }

    /**
     * Get the internal app-scoped storage directory.
     *
     * @param context Android context
     * @return Internal files directory (never null)
     */
    private fun getInternalStorageDir(context: Context): File {
        return context.filesDir.also {
            it.mkdirs()
            Log.d(TAG, "Using internal storage: ${it.absolutePath}")
        }
    }

    /**
     * Get the external app-scoped storage directory.
     * On API 29+, this uses app-scoped external storage (no WRITE_EXTERNAL_STORAGE needed).
     * Returns null if external storage is not available.
     *
     * @param context Android context
     * @return External files directory, or null if unavailable
     */
    private fun getExternalStorageDir(context: Context): File? {
        return try {
            // Iterate over all app-scoped external dirs and return the first that is
            // removable, mounted, and writable. This represents real expandable storage
            // (e.g., SD card or USB OTG) rather than emulated primary storage.
            val candidates = context.getExternalFilesDirs(null)
            candidates?.firstOrNull { dir ->
                if (dir == null) return@firstOrNull false
                val state = Environment.getExternalStorageState(dir)
                val isRemovable = Environment.isExternalStorageRemovable(dir)
                val ok = isRemovable && state == Environment.MEDIA_MOUNTED && dir.canWrite()
                if (ok) {
                    dir.mkdirs()
                    Log.d(TAG, "Using removable external storage: ${dir.absolutePath}")
                }
                ok
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to access removable external storage: ${e.message}", e)
            null
        }
    }

    /**
     * Check if external storage is available and writable.
     *
     * @param context Android context
     * @return True if external storage is available
     */
    fun isExternalStorageAvailable(context: Context): Boolean {
        return try {
            getExternalStorageDir(context) != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get free space for a given storage location.
     *
     * @param context Android context
     * @param location Download location preference
     * @return Free space in bytes
     */
    fun getFreeSpace(context: Context, location: DownloadLocation): Long {
        val dir = when (location) {
            DownloadLocation.EXTERNAL -> context.getExternalFilesDir(null) ?: context.filesDir
            DownloadLocation.INTERNAL -> context.filesDir
        }
        return try {
            val statFs = android.os.StatFs(dir.absolutePath)
            statFs.availableBlocksLong * statFs.blockSizeLong
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get free space: ${e.message}")
            0L
        }
    }
}
