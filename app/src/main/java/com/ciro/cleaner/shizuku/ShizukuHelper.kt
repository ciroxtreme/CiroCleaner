package com.ciro.cleaner.shizuku

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

object ShizukuHelper {

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
    }

    fun hasShizukuPermission(): Boolean {
        return try {
            if (isShizukuAvailable()) {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun requestShizukuPermission(requestCode: Int = 1001) {
        try {
            if (isShizukuAvailable() && !hasShizukuPermission()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (_: Exception) {}
    }

    suspend fun executeCommand(command: String): String = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) return@withContext "Shizuku not granted"
        try {
            // Shizuku.newProcess via reflection to maintain binary compatibility across Shizuku versions
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString().trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun forceStopApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        val result = executeCommand("am force-stop $packageName")
        !result.startsWith("Error")
    }

    suspend fun clearInternalCaches(): Boolean = withContext(Dispatchers.IO) {
        val result = executeCommand("pm trim-caches 1000G")
        !result.startsWith("Error")
    }

    suspend fun findLargeFilesAdb(
        minMegabytes: Long,
        onFileFound: ((String) -> Unit)? = null
    ): List<String> = withContext(Dispatchers.IO) {
        if (!hasShizukuPermission()) return@withContext emptyList()
        try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val command = "find /sdcard -type f -size +${minMegabytes}M 2>/dev/null"
            val process = method.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val list = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val path = line?.trim() ?: continue
                if (path.isNotBlank() && !path.startsWith("Error")) {
                    list.add(path)
                    onFileFound?.invoke(path)
                }
            }
            process.waitFor()
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
