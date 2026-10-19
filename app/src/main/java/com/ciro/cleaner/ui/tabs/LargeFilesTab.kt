package com.ciro.cleaner.ui.tabs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.ciro.cleaner.model.FileUtils
import com.ciro.cleaner.model.LargeFileItem
import com.ciro.cleaner.model.LargeFileType
import com.ciro.cleaner.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LargeFilesTab(
    isShizukuActive: Boolean
) {
    val context = LocalContext.current
    var largeFiles by remember { mutableStateOf<List<LargeFileItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var sizeThresholdMB by remember { mutableStateOf(100L) }
    var selectedCategory by remember { mutableStateOf(LargeFileType.ALL) }
    var showHiddenOnly by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun scanFiles() {
        scope.launch {
            isScanning = true
            largeFiles = withContext(Dispatchers.IO) {
                val results = mutableListOf<LargeFileItem>()
                val minBytes = sizeThresholdMB * 1024 * 1024

                // 1. Jika Shizuku Aktif: Gunakan ADB `find` untuk menembus /sdcard/Android/obb dan folder tersembunyi
                if (isShizukuActive) {
                    try {
                        val filePaths = ShizukuHelper.findLargeFilesAdb(sizeThresholdMB)
                        for (path in filePaths) {
                            val f = File(path)
                            if (f.exists() && f.isFile) {
                                val len = f.length()
                                if (len >= minBytes) {
                                    val isHidden = f.name.startsWith(".") || path.contains("/.")
                                    val ext = f.extension
                                    results.add(
                                        LargeFileItem(
                                            file = f,
                                            name = f.name,
                                            sizeBytes = len,
                                            isHidden = isHidden,
                                            extension = ext,
                                            fileType = categorizeFileType(ext, path)
                                        )
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 2. Fallback / Tambahan: Scan recursive standard Java File API
                if (results.isEmpty()) {
                    val root = File("/sdcard")
                    if (root.exists()) {
                        scanForLargeFiles(root, results, minBytes)
                    }
                }

                // Deduplikasi berdasarkan path absolut & urutkan terbesar
                results.distinctBy { it.file.absolutePath }.sortedByDescending { it.sizeBytes }
            }
            isScanning = false
        }
    }

    LaunchedEffect(sizeThresholdMB, isShizukuActive) {
        scanFiles()
    }

    val filteredList = remember(largeFiles, selectedCategory, showHiddenOnly) {
        largeFiles.filter { item ->
            val matchCategory = (selectedCategory == LargeFileType.ALL) || (item.fileType == selectedCategory)
            val matchHidden = !showHiddenOnly || item.isHidden
            matchCategory && matchHidden
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Control Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Detektor Berkas Raksasa",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isShizukuActive) "⚡ Shizuku: Mampu menembus Android/obb & folder tersembunyi" else "Lacak film, zip, obb & arsip besar",
                            color = if (isShizukuActive) Color(0xFF81C784) else Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = { scanFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Pindai Ulang", tint = Color(0xFF64B5F6))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Threshold Chips
                Text("Ambang Batas Ukuran:", color = Color.LightGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(50L to "> 50 MB", 100L to "> 100 MB", 500L to "> 500 MB", 1024L to "> 1 GB").forEach { (mb, label) ->
                        FilterChip(
                            selected = sizeThresholdMB == mb,
                            onClick = { sizeThresholdMB = mb },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00695C),
                                selectedLabelColor = Color(0xFF80CBC4)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips (OBB, ZIP, Video, dll)
                Text("Kategori Ekstensi Berkas:", color = Color.LightGray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LargeFileType.values()) { type ->
                        FilterChip(
                            selected = selectedCategory == type,
                            onClick = { selectedCategory = type },
                            label = { Text(type.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF1565C0),
                                selectedLabelColor = Color(0xFF90CAF9)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Hanya Berkas Tersembunyi (Dot Files)", color = Color.White, fontSize = 12.sp)
                    Switch(
                        checked = showHiddenOnly,
                        onCheckedChange = { showHiddenOnly = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isScanning) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF00897B))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isShizukuActive) "Mencari berkas raksasa via Shizuku ADB..." else "Memindai berkas besar...",
                        color = Color.Gray
                    )
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Tidak ditemukan berkas untuk kategori ini di atas ${sizeThresholdMB} MB",
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.file.absolutePath }) { item ->
                    LargeFileRow(
                        item = item,
                        onOpenFile = {
                            try {
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, getMimeType(item.extension))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(android.net.Uri.fromFile(item.file), "*/*")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        },
                        onDeleteFile = {
                            scope.launch(Dispatchers.IO) {
                                val ok = FileUtils.deleteFileOrDirectory(item.file)
                                if (!ok && isShizukuActive) {
                                    ShizukuHelper.executeCommand("rm -rf '${item.file.absolutePath}'")
                                }
                                scanFiles()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LargeFileRow(
    item: LargeFileItem,
    onOpenFile: () -> Unit,
    onDeleteFile: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (item.fileType) {
                    LargeFileType.VIDEO -> Icons.Default.Movie
                    LargeFileType.ARCHIVE -> Icons.Default.Archive
                    LargeFileType.OBB_GAME -> Icons.Default.SportsEsports
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = when (item.fileType) {
                    LargeFileType.VIDEO -> Color(0xFF4DB6AC)
                    LargeFileType.ARCHIVE -> Color(0xFFFFB74D)
                    LargeFileType.OBB_GAME -> Color(0xFFAB47BC)
                    else -> Color(0xFF90CAF9)
                },
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isHidden) {
                        Text(
                            text = "[TERSEMBUNYI] ",
                            color = Color(0xFFFF5252),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                }
                Text(
                    text = item.file.parent ?: "",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = FileUtils.formatFileSize(item.sizeBytes),
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "• ${item.fileType.displayName}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onOpenFile) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Buka", tint = Color(0xFF64B5F6))
            }

            IconButton(onClick = onDeleteFile) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF5350))
            }
        }
    }
}

private fun categorizeFileType(extension: String, path: String): LargeFileType {
    val ext = extension.lowercase()
    val p = path.lowercase()
    return when {
        ext == "obb" || ext == "xapk" || p.contains("/obb/") -> LargeFileType.OBB_GAME
        ext in listOf("zip", "rar", "7z", "iso", "tar", "gz", "bz2") -> LargeFileType.ARCHIVE
        ext in listOf("mp4", "mkv", "avi", "webm", "mov", "flv", "3gp") -> LargeFileType.VIDEO
        else -> LargeFileType.DOCUMENT_OTHER
    }
}

private fun scanForLargeFiles(dir: File, results: MutableList<LargeFileItem>, minBytes: Long, depth: Int = 0) {
    if (depth > 8 || !dir.canRead()) return
    val files = dir.listFiles() ?: return

    for (f in files) {
        if (f.isDirectory) {
            scanForLargeFiles(f, results, minBytes, depth + 1)
        } else {
            val length = f.length()
            if (length >= minBytes) {
                val isHidden = f.name.startsWith(".") || (f.parentFile?.name?.startsWith(".") == true)
                val ext = f.extension
                results.add(
                    LargeFileItem(
                        file = f,
                        name = f.name,
                        sizeBytes = length,
                        isHidden = isHidden,
                        extension = ext,
                        fileType = categorizeFileType(ext, f.absolutePath)
                    )
                )
            }
        }
    }
}

private fun getMimeType(extension: String): String {
    return when (extension.lowercase()) {
        "mp4", "mkv", "avi", "webm" -> "video/*"
        "mp3", "wav", "m4a", "flac" -> "audio/*"
        "jpg", "jpeg", "png", "webp" -> "image/*"
        "pdf" -> "application/pdf"
        "apk" -> "application/vnd.android.package-archive"
        "zip" -> "application/zip"
        "rar" -> "application/x-rar-compressed"
        "7z" -> "application/x-7z-compressed"
        else -> "*/*"
    }
}
