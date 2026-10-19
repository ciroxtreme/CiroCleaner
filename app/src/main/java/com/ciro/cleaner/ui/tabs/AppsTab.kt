package com.ciro.cleaner.ui.tabs

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.ciro.cleaner.model.AppStorageItem
import com.ciro.cleaner.model.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AppsTab() {
    val context = LocalContext.current
    var appsList by remember { mutableStateOf<List<AppStorageItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(true) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var currentScannedApp by remember { mutableStateOf("") }
    var totalAppsCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    fun loadAppsStorage() {
        scope.launch {
            isLoading = true
            scanProgress = 0f
            currentScannedApp = "Mempersiapkan daftar aplikasi..."
            appsList = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                val userApps = installed.filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || it.packageName.contains("transsion") }
                totalAppsCount = userApps.size
                val results = mutableListOf<AppStorageItem>()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                        val uuid = StorageManager.UUID_DEFAULT

                        for ((index, app) in userApps.withIndex()) {
                            val appLabel = pm.getApplicationLabel(app).toString()
                            withContext(Dispatchers.Main) {
                                currentScannedApp = appLabel
                                scanProgress = if (userApps.isNotEmpty()) (index + 1).toFloat() / userApps.size else 0f
                            }
                            try {
                                val stats = storageStatsManager.queryStatsForPackage(uuid, app.packageName, Process.myUserHandle())
                                val icon = try { pm.getApplicationIcon(app) } catch (_: Exception) { null }
                                results.add(
                                    AppStorageItem(
                                        packageName = app.packageName,
                                        appName = appLabel,
                                        appIcon = icon,
                                        totalSizeBytes = stats.appBytes + stats.dataBytes,
                                        appSizeBytes = stats.appBytes,
                                        dataSizeBytes = stats.dataBytes - stats.cacheBytes,
                                        cacheSizeBytes = stats.cacheBytes
                                    )
                                )
                            } catch (_: SecurityException) {
                                hasPermission = false
                                break
                            } catch (_: Exception) {}
                        }
                    } catch (_: Exception) {}
                }
                results.sortedByDescending { it.totalSizeBytes }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadAppsStorage()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
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
                            text = "Analisis Penyimpanan Aplikasi",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (totalAppsCount > 0) "Total: $totalAppsCount aplikasi terlacak" else "Lacak aplikasi yang datanya membengkak",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = { loadAppsStorage() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Pindai Ulang", tint = Color(0xFF64B5F6))
                    }
                }

                if (!hasPermission) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                    ) {
                        Text("Buka Izin Akses Penggunaan (Wajib)")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
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
                            text = currentScannedApp,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Text(
                            text = "Menghitung ukuran data & cache aplikasi...",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(appsList) { item ->
                    AppStorageRow(
                        item = item,
                        onOpenAppInfo = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${item.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppStorageRow(
    item: AppStorageItem,
    onOpenAppInfo: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.appIcon != null) {
                    Image(
                        bitmap = item.appIcon.toBitmap(96, 96).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp)
                    )
                } else {
                    Icon(Icons.Default.Android, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(40.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.appName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                    Text(
                        text = item.packageName,
                        color = Color.Gray,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                }

                Text(
                    text = FileUtils.formatFileSize(item.totalSizeBytes),
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFF2C2C2C))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "APK: ${FileUtils.formatFileSize(item.appSizeBytes)} | Data: ${FileUtils.formatFileSize(item.dataSizeBytes)}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Cache: ${FileUtils.formatFileSize(item.cacheSizeBytes)}",
                        color = Color(0xFF81D4FA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onOpenAppInfo,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Kelola / Hapus Cache", fontSize = 11.sp)
                }
            }
        }
    }
}
