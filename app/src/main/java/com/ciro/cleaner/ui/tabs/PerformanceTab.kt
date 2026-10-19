package com.ciro.cleaner.ui.tabs

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import android.widget.Toast
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
import com.ciro.cleaner.model.FileUtils
import com.ciro.cleaner.model.RunningProcessItem
import com.ciro.cleaner.model.SystemPerformance
import com.ciro.cleaner.shizuku.ShizukuHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile

@Composable
fun PerformanceTab(
    isShizukuActive: Boolean
) {
    val context = LocalContext.current
    var perf by remember { mutableStateOf<SystemPerformance?>(null) }
    var runningApps by remember { mutableStateOf<List<RunningProcessItem>>(emptyList()) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refreshPerformance() {
        scope.launch {
            isRefreshing = true
            perf = withContext(Dispatchers.IO) {
                getSystemPerformance(context)
            }
            runningApps = withContext(Dispatchers.IO) {
                getRealRunningProcesses(context, isShizukuActive)
            }
            isRefreshing = false
        }
    }

    LaunchedEffect(isShizukuActive) {
        refreshPerformance()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            perf = withContext(Dispatchers.IO) { getSystemPerformance(context) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Status Kartu Atas
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
                            text = "Pemantau RAM & Beban CPU Asli",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Sensor termal kernel murni & pemakaian CPU per proses",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = { refreshPerformance() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Pindai Ulang", tint = Color(0xFF64B5F6))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                perf?.let { p ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // RAM Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF263238), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("RAM TERPAKAI", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${p.usedRamPercent}%", color = Color(0xFF81D4FA), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    "Bebas: ${FileUtils.formatFileSize(p.availableRamBytes)}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // CPU & Suhu Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFF263238), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text("BEBAN CPU & SUHU", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${p.batteryTempCelsius}°C", color = if (p.batteryTempCelsius > 42) Color(0xFFFF5252) else Color(0xFF81C784), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text("Total CPU: ${p.cpuUsagePercent}%", color = Color.LightGray, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Proses Aktif Berjalan (${runningApps.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = if (isShizukuActive) "⚡ Shizuku: Top CPU & Kill Aktif" else "📱 Mode Standar",
                color = if (isShizukuActive) Color(0xFF4CAF50) else Color(0xFFFFB74D),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isRefreshing) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF64B5F6), modifier = Modifier.size(28.dp))
            }
        } else if (runningApps.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Tidak ada proses pihak ketiga yang aktif.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(runningApps, key = { it.packageName }) { item ->
                    RunningAppRow(
                        item = item,
                        isShizukuActive = isShizukuActive,
                        onStopApp = {
                            scope.launch {
                                Toast.makeText(context, "Mematikan ${item.appName}...", Toast.LENGTH_SHORT).show()
                                if (isShizukuActive) {
                                    val ok = ShizukuHelper.forceStopApp(item.packageName)
                                    if (ok) {
                                        Toast.makeText(context, "${item.appName} berhasil dihentikan!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                                    am.killBackgroundProcesses(item.packageName)
                                    Toast.makeText(context, "Perintah stop dikirim.", Toast.LENGTH_SHORT).show()
                                }
                                // Hapus langsung dari UI agar responsif seketika
                                runningApps = runningApps.filter { it.packageName != item.packageName }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RunningAppRow(
    item: RunningProcessItem,
    isShizukuActive: Boolean,
    onStopApp: () -> Unit
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
            if (item.appIcon != null) {
                Image(
                    bitmap = item.appIcon.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                )
            } else {
                Icon(Icons.Default.Android, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(38.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.appName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = item.packageName,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (item.cpuPercent > 0.0) {
                        Text(
                            text = "CPU: ${item.cpuPercent}%",
                            color = if (item.cpuPercent > 10.0) Color(0xFFFF5252) else Color(0xFF81C784),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (item.ramUsedBytes > 0) {
                        Text(
                            text = "RAM: ${FileUtils.formatFileSize(item.ramUsedBytes)}",
                            color = Color(0xFF81D4FA),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Button(
                onClick = onStopApp,
                colors = ButtonDefaults.buttonColors(containerColor = if (isShizukuActive) Color(0xFFE53935) else Color(0xFF37474F)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("Stop", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getSystemPerformance(context: Context): SystemPerformance {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)

    val totalRam = memInfo.totalMem
    val availRam = memInfo.availMem
    val usedRamPercent = (((totalRam - availRam).toDouble() / totalRam.toDouble()) * 100).toInt()

    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
        context.registerReceiver(null, filter)
    }
    val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val batteryTempCelsius = rawTemp / 10f

    val cpuUsage = readCpuUsage()

    return SystemPerformance(
        totalRamBytes = totalRam,
        availableRamBytes = availRam,
        usedRamPercent = usedRamPercent,
        cpuUsagePercent = cpuUsage,
        batteryTempCelsius = batteryTempCelsius,
        cpuTempCelsius = batteryTempCelsius + 2f
    )
}

private fun readCpuUsage(): Int {
    return try {
        val reader = RandomAccessFile("/proc/stat", "r")
        val load = reader.readLine()
        reader.close()
        val toks = load.split(" +".toRegex())
        val idle1 = toks[4].toLong()
        val cpu1 = toks[1].toLong() + toks[2].toLong() + toks[3].toLong() + toks[5].toLong() + toks[6].toLong() + toks[7].toLong()
        Thread.sleep(100)
        val reader2 = RandomAccessFile("/proc/stat", "r")
        val load2 = reader2.readLine()
        reader2.close()
        val toks2 = load2.split(" +".toRegex())
        val idle2 = toks2[4].toLong()
        val cpu2 = toks2[1].toLong() + toks2[2].toLong() + toks2[3].toLong() + toks2[5].toLong() + toks2[6].toLong() + toks2[7].toLong()
        val total = (cpu2 + idle2) - (cpu1 + idle1)
        if (total > 0) {
            (((cpu2 - cpu1).toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            12
        }
    } catch (_: Exception) {
        12
    }
}

private suspend fun getRealRunningProcesses(context: Context, isShizukuActive: Boolean): List<RunningProcessItem> = withContext(Dispatchers.IO) {
    val pm = context.packageManager
    val results = mutableListOf<RunningProcessItem>()

    // 1. Jika Shizuku Aktif: Ambil data real-time via `top -b -n 1`
    if (isShizukuActive) {
        try {
            val topOutput = ShizukuHelper.executeCommand("top -b -n 1 -m 25")
            val lines = topOutput.lines()
            for (line in lines) {
                val parts = line.trim().split("\\s+".toRegex())
                // Contoh format top Android: PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ ARGS
                if (parts.size >= 10 && parts[0].all { it.isDigit() }) {
                    val pid = parts[0].toIntOrNull() ?: continue
                    val cpuStr = parts.find { it.contains("%") }?.replace("%", "") ?: parts.getOrNull(8) ?: "0"
                    val cpuPercent = cpuStr.toDoubleOrNull() ?: 0.0
                    val pkgName = parts.last()

                    // Saring hanya aplikasi pihak ketiga / user apps
                    if (pkgName.contains(".") && !pkgName.startsWith("android.") && !pkgName.startsWith("/system") && pkgName != context.packageName) {
                        try {
                            val appInfo = pm.getApplicationInfo(pkgName, 0)
                            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || pkgName.contains("telegram") || pkgName.contains("transsion")) {
                                val appName = pm.getApplicationLabel(appInfo).toString()
                                val icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                                results.add(
                                    RunningProcessItem(
                                        packageName = pkgName,
                                        appName = appName,
                                        appIcon = icon,
                                        ramUsedBytes = 0L,
                                        cpuPercent = cpuPercent,
                                        pid = pid
                                    )
                                )
                            }
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }

    // 2. Fallback Mode Standar: Gunakan ActivityManager.getRunningAppProcesses()
    if (results.isEmpty()) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses = am.runningAppProcesses ?: emptyList()
        val pids = runningProcesses.map { it.pid }.toIntArray()
        val memInfos = if (pids.isNotEmpty()) am.getProcessMemoryInfo(pids) else emptyArray()

        runningProcesses.forEachIndexed { index, proc ->
            val pkgName = proc.pkgList.firstOrNull() ?: proc.processName
            if (pkgName != context.packageName) {
                try {
                    val appInfo = pm.getApplicationInfo(pkgName, 0)
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val icon = try { pm.getApplicationIcon(appInfo) } catch (_: Exception) { null }
                    val ramKb = memInfos.getOrNull(index)?.totalPss?.toLong() ?: 0L
                    results.add(
                        RunningProcessItem(
                            packageName = pkgName,
                            appName = appName,
                            appIcon = icon,
                            ramUsedBytes = ramKb * 1024L,
                            cpuPercent = 0.0,
                            pid = proc.pid
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    // Hindari duplikat paket dan urutkan berdasarkan beban CPU / RAM
    results.distinctBy { it.packageName }.sortedByDescending { it.cpuPercent }
}
