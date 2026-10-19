package com.ciro.cleaner.model

import java.io.File
import java.text.DecimalFormat

object FileUtils {

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = sizeBytes / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }

    fun getDirectorySize(dir: File): Long {
        var size: Long = 0
        val files = dir.listFiles() ?: return 0
        for (f in files) {
            size += if (f.isDirectory) {
                getDirectorySize(f)
            } else {
                f.length()
            }
        }
        return size
    }

    fun deleteFileOrDirectory(file: File): Boolean {
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    deleteFileOrDirectory(child)
                }
            }
        }
        return file.delete()
    }
}
