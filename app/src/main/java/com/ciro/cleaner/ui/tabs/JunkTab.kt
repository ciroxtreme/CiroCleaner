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
    val scope = rememberCoroutineScope()

    val totalSelectedBytes = junkItems.filter { it.isSelected }.sumOf { it.sizeBytes }

    fun scanJunk() {
        scope.launch {
            isScanning = true
            junkItems = withContext(Dispatchers.IO) {
                val results = mutableListOf<JunkItem>()
                val rootStorage = File("/sdcard")
                if (rootStorage.exists()) {
                    scanDirectoryForJunk(rootStorage, results)
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF64B5F6))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Memindai berkas sampah...", color = Color.Gray)
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

private fun scanDirectoryForJunk(dir: File, results: MutableList<JunkItem>, depth: Int = 0) {
    if (depth > 6 || !dir.canRead()) return
    val files = dir.listFiles() ?: return

    if (files.isEmpty() && dir.name != "0" && dir.name != "sdcard") {
        results.add(
            JunkItem(
                file = dir,
                name = dir.name,
                sizeBytes = 0,
                category = JunkCategory.EMPTY_FOLDER
            )
        )
        return
    }

    for (f in files) {
        if (f.isDirectory) {
            if (f.name.equals(".thumbnails", ignoreCase = true)) {
                val size = FileUtils.getDirectorySize(f)
                if (size > 0) {
                    results.add(JunkItem(f, f.name, size, JunkCategory.THUMBNAILS))
                }
            } else if (!f.name.startsWith(".")) {
                scanDirectoryForJunk(f, results, depth + 1)
            }
        } else {
            val name = f.name.lowercase()
            val size = f.length()
            if (size <= 0) continue

            when {
                name.endsWith(".apk") -> {
                    results.add(JunkItem(f, f.name, size, JunkCategory.APK))
                }
                name.endsWith(".tmp") || name.endsWith(".log") || name.endsWith(".bak") || name.endsWith(".cache") -> {
                    results.add(JunkItem(f, f.name, size, JunkCategory.TEMP_LOG))
                }
            }
        }
    }
}
