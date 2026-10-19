# 🧹 Ciro Cleaner

Aplikasi pembersih berkas, analisis penyimpanan, dan pemantau performa Android yang jujur, ringan, 100% offline (tanpa akses internet), bebas iklan, dan mendukung hak istimewa ADB via **Shizuku**.

---

## 🌟 Fitur Utama

1. **🧹 Tab Sampah (Junk Cleaner):**
   - Memindai berkas APK mentahan lawas, cache galeri (`.thumbnails`), berkas `.tmp`, `.log`, dan folder kosong.
   - Dilengkapi sistem centang (*checkbox*) manual dan fitur sortir ukuran (Terbesar ke Terkecil).
   - Tombol sapu bersih cache internal aplikasi via perintah ADB `pm trim-caches` (Mode Shizuku).

2. **📁 Tab File Besar (Hidden Large Files Finder):**
   - Mendeteksi video, film, arsip `.zip`/`.rar`, dan data berkas besar yang tersembunyi (*dot files / nomedia*).
   - Filter ambang batas ukuran: `> 50 MB`, `> 100 MB`, `> 500 MB`, `> 1 GB`.
   - Tombol **Pratinjau / Putar Video** langsung sebelum memutuskan untuk menghapus.

3. **📦 Tab Aplikasi (Storage Analyzer):**
   - Membedah penggunaan memori aplikasi dari yang paling bengkak (APK vs Data Pengguna vs Cache).
   - Pintasan langsung untuk membersihkan data aplikasi.

4. **⚡ Tab Performa (RAM, CPU & Suhu Asli):**
   - Indikator penggunaan RAM % dan estimasi beban kerja CPU %.
   - Sensor termal suhu baterai & CPU asli dari kernel.
   - Tombol **[ Stop ]** per-aplikasi untuk melakukan Force Stop seketika via Shizuku (`am force-stop`).

---

## 🔒 Privasi & Keamanan
- **100% Offline:** Tanpa izin `android.permission.INTERNET`.
- **0 Byte Kuota:** Tidak ada pelacak, analitik, atau iklan.
