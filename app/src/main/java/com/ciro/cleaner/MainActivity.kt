package com.ciro.cleaner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ciro.cleaner.shizuku.ShizukuHelper
import com.ciro.cleaner.ui.tabs.AppsTab
import com.ciro.cleaner.ui.tabs.JunkTab
import com.ciro.cleaner.ui.tabs.LargeFilesTab
import com.ciro.cleaner.ui.tabs.PerformanceTab
import rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var isShizukuActive by mutableStateOf(false)

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        isShizukuActive = (grantResult == PackageManager.PERMISSION_GRANTED)
        if (isShizukuActive) {
            Toast.makeText(this, "Mode Super Shizuku Aktif! 🚀", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek izin Shizuku
        checkShizukuStatus()
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) {}

        // Minta Izin Kelola Penyimpanan Luas (All Files Access) jika belum
        requestStoragePermissions()

        setContent {
            MainAppScreen(
                isShizukuActive = isShizukuActive,
                onRequestShizuku = {
                    if (ShizukuHelper.isShizukuAvailable()) {
                        ShizukuHelper.requestShizukuPermission()
                    } else {
                        Toast.makeText(this, "Shizuku belum berjalan di ponsel Anda.", Toast.LENGTH_LONG).show()
                    }
                },
                onClearInternalCaches = {
                    CoroutineScope(Dispatchers.Main).launch {
                        val ok = ShizukuHelper.clearInternalCaches()
                        if (ok) {
                            Toast.makeText(this@MainActivity, "Semua cache internal aplikasi berhasil disapu!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MainActivity, "Gagal menyapu cache internal.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    private fun checkShizukuStatus() {
        isShizukuActive = ShizukuHelper.hasShizukuPermission()
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    isShizukuActive: Boolean,
    onRequestShizuku: () -> Unit,
    onClearInternalCaches: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(
                            text = "CIRO CLEANER",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64B5F6),
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF2E7D32),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "v1.0.3",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    FilterChip(
                        selected = isShizukuActive,
                        onClick = onRequestShizuku,
                        label = {
                            Text(
                                text = if (isShizukuActive) "⚡ Super (Shizuku)" else "📱 Mode Standar",
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1B5E20),
                            selectedLabelColor = Color(0xFFA5D6A7),
                            containerColor = Color(0xFF263238),
                            labelColor = Color.LightGray
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF181818))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF181818)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text("Sampah", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF64B5F6),
                        selectedTextColor = Color(0xFF64B5F6),
                        indicatorColor = Color(0xFF263238)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = null) },
                    label = { Text("File Besar", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00897B),
                        selectedTextColor = Color(0xFF00897B),
                        indicatorColor = Color(0xFF004D40)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                    label = { Text("Aplikasi", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFFFB74D),
                        selectedTextColor = Color(0xFFFFB74D),
                        indicatorColor = Color(0xFF4E342E)
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Speed, contentDescription = null) },
                    label = { Text("Performa", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFE53935),
                        selectedTextColor = Color(0xFFE53935),
                        indicatorColor = Color(0xFF3E2723)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> JunkTab(isShizukuActive = isShizukuActive, onClearInternalCacheShizuku = onClearInternalCaches)
                1 -> LargeFilesTab(isShizukuActive = isShizukuActive)
                2 -> AppsTab()
                3 -> PerformanceTab(isShizukuActive = isShizukuActive)
            }
        }
    }
}
