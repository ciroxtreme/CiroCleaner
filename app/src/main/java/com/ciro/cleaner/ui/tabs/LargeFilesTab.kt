package com.ciro.cleaner.ui.tabs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun LargeFilesTab() {
    val context = LocalContext.current
    var largeFiles by remember { mutableStateOf<List<LargeFileItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var sizeThresholdMB by remember { mutableStateOf(100L) }
    var showHiddenOnly by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun scanFiles() {
        scope.launch {
            isScanning = true
            largeFiles = withContext(Dispatchers.IO) {
                val results = mutableListOf<LargeFileItem>()
                val root = File("/sdcard")
                if (root.exists()) {
                    scanForLargeFiles(root, results, sizeThresholdMB * 1024 * 1024)
                }
                results.sortedByDescending { it.sizeBytes }
            }
            isScanning = false
        }
    }

    LaunchedEffect(sizeThresholdMB) {
        scanFiles()
    }

    val filteredList = remember(largeFiles, showHiddenOnly) {
        if (showHiddenOnly) largeFiles.filter { it.isHidden } else largeFiles
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Control Header
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
                            text = "Lacak film, video, obb & arsip besar yang tersembunyi",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = { scanFiles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Pindai Ulang", tint = Color(0xFF64B5F6))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Threshold Chips
                Text("Ambang Batas Ukuran:", color = Color.LightGray, fontSize = 12.sp)
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tampilkan Berkas Tersembunyi Saja (Dot Files)", color = Color.White, fontSize = 12.sp)
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
                    Text("Mencari berkas raksasa di penyimpanan...", color = Color.Gray)
                }
            }
        } else if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ditemukan berkas di atas ${sizeThresholdMB} MB", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList) { item ->
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
                                // Fallback generic
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
                                FileUtils.deleteFileOrDirectory(item.file)
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
                imageVector = when (item.extension.lowercase()) {
                    "mp4", "mkv", "avi", "webm", "mov" -> Icons.Default.Movie
                    "zip", "rar", "7z", "iso", "tar" -> Icons.Default.Archive
                    "apk" -> Icons.Default.Android
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                tint = if (item.isHidden) Color(0xFFFF5252) else Color(0xFF4DB6AC),
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
                Text(
                    text = FileUtils.formatFileSize(item.sizeBytes),
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
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
                results.add(LargeFileItem(f, f.name, length, isHidden, ext))
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
        else -> "*/*"
    }
}
