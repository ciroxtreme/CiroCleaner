package com.ciro.cleaner.model

import java.io.File

enum class JunkCategory(val displayName: String) {
    APK("APK Mentahan Lawas"),
    THUMBNAILS("Cache Galeri (.thumbnails)"),
    TEMP_LOG("Berkas Sampah (.tmp, .log)"),
    EMPTY_FOLDER("Folder Kosong")
}

data class JunkItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val category: JunkCategory,
    val isSelected: Boolean = true
)

data class LargeFileItem(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val isHidden: Boolean,
    val extension: String
)

data class AppStorageItem(
    val packageName: String,
    val appName: String,
    val appIcon: android.graphics.drawable.Drawable?,
    val totalSizeBytes: Long,
    val appSizeBytes: Long,
    val dataSizeBytes: Long,
    val cacheSizeBytes: Long
)

data class RunningProcessItem(
    val packageName: String,
    val appName: String,
    val appIcon: android.graphics.drawable.Drawable?,
    val ramUsedBytes: Long,
    val cpuPercent: Double,
    val pid: Int
)

data class SystemPerformance(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val usedRamPercent: Int,
    val cpuUsagePercent: Int,
    val batteryTempCelsius: Float,
    val cpuTempCelsius: Float
)
