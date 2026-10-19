package com.ciro.cleaner.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ciro.cleaner.model.FileUtils
import com.ciro.cleaner.model.JunkCategory
import com.ciro.cleaner.model.JunkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun JunkTab(
    isShizukuActive: Boolean,
    onClearInternalCacheShizuku: () -> Unit
) {
    var junkItems by remember { mutableStateOf<List<JunkItem>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var sortByLargest by remember { mutableStateOf(true) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var currentScannedFolder by remember { mutableStateOf("") }
    var junkFoundCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val totalSelectedBytes = junkItems.filter { it.isSelected }.sumOf { it.sizeBytes }

    fun scanJunk() {
        scope.launch {
            isScanning = true
            scanProgress = 0f
            currentScannedFolder = "Memulai pemindaian berkas sampah..."
            junkFoundCount = 0
            junkItems = withContext(Dispatchers.IO) {
                val results = mutableListOf<JunkItem>()
                val rootStorage = File("/sdcard")
                if (rootStorage.exists()) {
                    val subDirs = rootStorage.listFiles()?.filter { it.canRead() } ?: emptyList()
                    val total = subDirs.size.coerceAtLeast(1)
                    for ((idx, sub) in subDirs.withIndex()) {
                        withContext(Dispatchers.Main) {
                            currentScannedFolder = sub.name
                            scanProgress = (idx.toFloat() / total)
                        }
                        if (sub.isDirectory) {
                            scanDirectoryForJunk(sub, results) { item ->
                                scope.launch(Dispatchers.Main) {
                                    junkFoundCount = results.size
                                    currentScannedFolder = item.name
                                }
                            }
                        } else {
                            checkAndAddJunkFile(sub, results)
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    scanProgress = 1f
                    currentScannedFolder = "Selesai"
                }
                if (sortByLargest) results.sortedByDescending { it.sizeBytes } else results
            }
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        scanJunk()
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        bottomBar = {
            if (junkItems.isNotEmpty()) {
                Surface(
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Terpilih: ${FileUtils.formatFileSize(totalSelectedBytes)}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${junkItems.count { it.isSelected }} dari ${junkItems.size} berkas",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    val selected = junkItems.filter { it.isSelected }
                                    selected.forEach { FileUtils.deleteFileOrDirectory(it.file) }
                                    scanJunk()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            enabled = totalSelectedBytes > 0
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Bersihkan")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Header Card
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
                                text = "Pembersih Sampah Berkas",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "APK lawas, thumbnail galeri, berkas .tmp & folder kosong",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = { scanJunk() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Pindai Ulang", tint = Color(0xFF64B5F6))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = sortByLargest,
                            onClick = {
                                sortByLargest = !sortByLargest
                                junkItems = if (sortByLargest) {
                                    junkItems.sortedByDescending { it.sizeBytes }
                                } else {
                                    junkItems.sortedBy { it.name }
                                }
                            },
                            label = { Text(if (sortByLargest) "Urutan: Terbesar ⬇" else "Urutan: Nama A-Z") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF263238),
                                selectedLabelColor = Color(0xFF81D4FA)
                            )
                        )

                        TextButton(onClick = {
                            val allSelected = junkItems.all { it.isSelected }
                            junkItems = junkItems.map { it.copy(isSelected = !allSelected) }
                        }) {
                            Text(if (junkItems.all { it.isSelected }) "Lepas Semua" else "Pilih Semua")
                        }
                    }

                    if (isShizukuActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onClearInternalCacheShizuku,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sapu Bersih Semua Cache Internal (Mode Shizuku)")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isScanning) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(24.dp).fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${(scanProgress * 100).toInt()}%",
                                color = Color(0xFF64B5F6),
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = Color(0xFF64B5F6),
                                trackColor = Color(0xFF2C2C3E)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = currentScannedFolder,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = if (junkFoundCount > 0) "Ditemukan $junkFoundCount berkas sampah..." else "Memeriksa berkas sampah...",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            } else if (junkItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Penyimpanan Bersih!", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Tidak ditemukan sampah berkas.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(junkItems) { item ->
                        JunkRow(
                            item = item,
                            onToggleSelect = {
                                junkItems = junkItems.map {
                                    if (it.file.absolutePath == item.file.absolutePath) it.copy(isSelected = !it.isSelected) else it
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JunkRow(
    item: JunkItem,
    onToggleSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isSelected,
                onCheckedChange = { onToggleSelect() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFFE53935),
                    checkmarkColor = Color.White
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = item.category.displayName,
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp
                )
                Text(
                    text = item.file.parent ?: "",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = FileUtils.formatFileSize(item.sizeBytes),
                color = Color(0xFFFFB74D),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

private fun scanDirectoryForJunk(
    dir: File,
    results: MutableList<JunkItem>,
    depth: Int = 0,
    onJunkFound: ((JunkItem) -> Unit)? = null
) {
    if (depth > 6 || !dir.canRead()) return
    val files = dir.listFiles() ?: return

    if (files.isEmpty() && dir.name != "0" && dir.name != "sdcard") {
        val emptyItem = JunkItem(
            file = dir,
            name = dir.name,
            sizeBytes = 0,
            category = JunkCategory.EMPTY_FOLDER
        )
        results.add(emptyItem)
        onJunkFound?.invoke(emptyItem)
        return
    }

    for (f in files) {
        if (f.isDirectory) {
            if (f.name.equals(".thumbnails", ignoreCase = true)) {
                val size = FileUtils.getDirectorySize(f)
                if (size > 0) {
                    val item = JunkItem(f, f.name, size, JunkCategory.THUMBNAILS)
                    results.add(item)
                    onJunkFound?.invoke(item)
                }
            } else if (!f.name.startsWith(".")) {
                scanDirectoryForJunk(f, results, depth + 1, onJunkFound)
            }
        } else {
            checkAndAddJunkFile(f, results, onJunkFound)
        }
    }
}

private fun checkAndAddJunkFile(
    file: File,
    results: MutableList<JunkItem>,
    onJunkFound: ((JunkItem) -> Unit)? = null
) {
    val name = file.name.lowercase()
    val size = file.length()
    if (size <= 0) return

    val item = when {
        name.endsWith(".apk") -> JunkItem(file, file.name, size, JunkCategory.APK)
        name.endsWith(".tmp") || name.endsWith(".log") || name.endsWith(".bak") || name.endsWith(".cache") -> {
            JunkItem(file, file.name, size, JunkCategory.TEMP_LOG)
        }
        else -> null
    }

    if (item != null) {
        results.add(item)
        onJunkFound?.invoke(item)
    }
}

