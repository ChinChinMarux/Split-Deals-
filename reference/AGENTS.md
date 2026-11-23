# AGENTS.md

Panduan untuk AI coding agent (dan kontributor) yang bekerja di repositori **Split the Deals!**. Baca seluruh file ini sebelum mengubah kode.

## 1. Ringkasan Proyek

**Split the Deals!** adalah aplikasi Android untuk membagi tagihan (split bill). Pengguna memotret struk, OCR membaca item dan harga, lalu pengguna menetapkan siapa memesan apa. Aplikasi menghitung bagian tiap orang termasuk PPN, service charge, dan diskon. Data disimpan lewat Web Service (REST API) buatan sendiri.

Ini adalah proyek tugas kuliah. Ketentuan wajib (jangan dilanggar saat mengubah kode):

1. Memakai API (Web Service).
2. Memiliki Bottom Navigation Bar.
3. Minimal 5 halaman.
4. Minimal 2 halaman utama berisi list, setiap item dapat diklik menuju halaman detail.
5. Memiliki 1 halaman Profile/About.

## 2. Tech Stack

| Bagian | Teknologi |
|---|---|
| Bahasa | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigasi | Navigation Compose (`NavigationBar` + `NavHost`) |
| Jaringan | Retrofit, OkHttp, Gson |
| Arsitektur | MVVM: ViewModel + StateFlow + Repository |
| Kamera | CameraX |
| OCR | ML Kit Text Recognition (on-device) |
| Gambar | Coil |
| Min SDK | 26 |
| Build | Gradle (Kotlin DSL) |

Jangan menambah library baru tanpa alasan jelas. Jangan mengganti Compose dengan XML, dan jangan memakai dependency injection framework kecuali diminta.

## 3. Struktur Proyek

```text
app/src/main/java/com/example/splitthedeals/
├─ data/
│  ├─ remote/        # ApiService, DTO, RetrofitClient
│  └─ repository/    # SessionRepository, FriendRepository, ProfileRepository
├─ ocr/              # ReceiptScanner (ML Kit), ReceiptParser (regex)
├─ domain/           # SplitCalculator (logika pembagian, tanpa dependensi Android)
├─ ui/
│  ├─ navigation/    # Screen, AppNavHost, bottom bar
│  ├─ home/          # HomeScreen, HomeViewModel
│  ├─ scan/          # ScanScreen, ScanViewModel
│  ├─ history/       # HistoryScreen (list), SessionDetailScreen (detail)
│  ├─ friends/       # FriendsScreen (list), FriendDetailScreen (detail)
│  ├─ profile/       # ProfileScreen (profil + about)
│  └─ theme/
└─ MainActivity.kt
```

Ubah struktur hanya jika perlu, dan perbarui bagian ini jika berubah.

## 4. Perintah Penting

```bash
./gradlew assembleDebug            # build APK debug
./gradlew testDebugUnitTest        # unit test
./gradlew lintDebug                # lint
./gradlew installDebug             # pasang ke emulator/perangkat
```

Sebelum menyatakan pekerjaan selesai, pastikan `assembleDebug` dan `testDebugUnitTest` lulus.

## 5. Navigasi dan Halaman

Bottom Navigation (5 tab): **Beranda | Scan | Riwayat | Teman | Profil**.

| Rute | Halaman | Catatan |
|---|---|---|
| `home` | Beranda | Ringkasan dan sesi terbaru |
| `scan` | Scan | Kamera, OCR, edit hasil, simpan ke API |
| `history` | Riwayat | **List** sesi, item dapat diklik |
| `session/{id}` | Detail Sesi | Halaman detail dari Riwayat |
| `friends` | Teman | **List** teman, item dapat diklik |
| `friend/{id}` | Detail Teman | Halaman detail dari Teman |
| `profile` | Profil / About | Data pengguna dan info aplikasi |

Aturan:

- Halaman detail tidak muncul di bottom bar.
- Gunakan `launchSingleTop`, `saveState`, dan `restoreState` saat berpindah tab.
- Jangan menghapus atau menggabungkan halaman sehingga ketentuan tugas (Bagian 1) tidak terpenuhi.

## 6. Kontrak API (Web Service Modul 1)

- Base URL emulator: `http://10.0.2.2:<port>/api/v1/`. Simpan di satu tempat (`RetrofitClient` atau `BuildConfig`), jangan di-hardcode di banyak file.
- Format: JSON. Bentuk respons standar:

```json
{ "status": "success", "data": { } }
```

| Metode | Endpoint | Fungsi |
|---|---|---|
| GET | `/sessions` | Daftar sesi |
| GET | `/sessions/{id}` | Detail sesi (item, peserta) |
| POST | `/sessions` | Buat sesi dari hasil scan |
| PUT | `/sessions/{id}` | Ubah sesi / status lunas |
| DELETE | `/sessions/{id}` | Hapus sesi |
| GET/POST | `/friends` | Daftar / tambah teman |
| GET/PUT/DELETE | `/friends/{id}` | Detail / ubah / hapus teman |
| GET/PUT | `/profile` | Profil pengguna |

Jika endpoint di API sebenarnya berbeda, **API adalah sumber kebenaran**. Sesuaikan kode klien, jangan mengarang endpoint. Tanyakan jika kontrak tidak jelas.

## 7. Konvensi Kode

- Kotlin idiomatik, ikuti Kotlin Coding Conventions. Nama paket `lowercase`, kelas `PascalCase`, fungsi dan variabel `camelCase`.
- Satu Composable per tanggung jawab. Pisahkan `Screen` (terhubung ke ViewModel) dan komponen UI yang bebas state.
- ViewModel mengekspos `StateFlow<UiState<T>>` dengan status `Loading`, `Success`, `Error`.
- Panggilan jaringan hanya lewat Repository di `viewModelScope` atau coroutine. Jangan memanggil Retrofit langsung dari Composable.
- Selalu tangani state loading, error (dengan tombol coba lagi), dan data kosong.
- Hindari `!!`. Pakai null-safety dan nilai default.
- String yang tampil ke pengguna ditaruh di `strings.xml`, bahasa default Indonesia.
- Format mata uang: `Rp 45.000` (pemisah ribuan titik).

## 8. Aturan Domain: Perhitungan Split

- Uang memakai `Long` (rupiah bulat) atau `BigDecimal`. **Jangan memakai `Float` atau `Double`.**
- PPN, service charge, dan diskon dibagi **proporsional** terhadap subtotal tiap orang.
- Sisa pembulatan dialokasikan (metode largest remainder) sehingga **jumlah total semua peserta persis sama dengan total struk**.
- Logika ini hidup di `SplitCalculator` (kelas Kotlin murni) dan wajib punya unit test.

## 9. OCR dan Kamera

- Izin `CAMERA` diminta saat pertama membuka halaman Scan, dengan penjelasan singkat. Tangani kasus izin ditolak.
- OCR dijalankan di perangkat (ML Kit). Foto struk tidak dikirim ke server kecuali fitur unggah diaktifkan secara eksplisit.
- `ReceiptParser` harus mengenali format angka Indonesia (`45.000`), qty (`2x`), dan kata kunci `SUBTOTAL`, `SERVICE`, `PB1`, `PPN`, `TAX`, `DISC`, `TOTAL`, `PEMBULATAN`.
- Hasil OCR **selalu** dapat diedit pengguna sebelum disimpan. Beri penanda pada baris dengan keyakinan rendah atau total yang tidak cocok.
- Parser ditulis sebagai fungsi murni agar mudah diuji dengan contoh teks struk.

## 10. Pengujian

- Unit test: `SplitCalculator`, `ReceiptParser`, ViewModel (dengan fake repository).
- Minimal uji kasus: item bersama, diskon, pajak, pembulatan, struk tanpa service charge, harga dengan titik dan koma.
- Jangan mengubah test agar lulus tanpa memperbaiki penyebabnya.

## 11. Keamanan dan Privasi

- Jangan commit `local.properties`, keystore, kata sandi, atau token.
- Jangan menyimpan nomor kartu. Info rekening atau QRIS hanya ditampilkan atas aksi pengguna.
- `usesCleartextTraffic` hanya untuk pengembangan lokal. Pakai HTTPS untuk API yang di-hosting.
- Jangan mencatat (log) data pribadi pengguna di rilis.

## 12. Cara Agent Bekerja

**Lakukan:**

- Baca kode yang ada dan ikuti pola yang sudah dipakai sebelum menulis fitur baru.
- Kerjakan perubahan kecil dan terarah, satu fitur per langkah.
- Jalankan build dan test setelah mengubah kode.
- Jelaskan singkat apa yang diubah dan file mana saja.

**Jangan:**

- Menghapus atau melemahkan ketentuan tugas (Bagian 1).
- Mengubah kontrak API tanpa konfirmasi.
- Menambah dependency besar atau memigrasi arsitektur tanpa diminta.
- Mengarang data tiruan permanen: data harus berasal dari API (data dummy hanya sementara untuk pengembangan dan harus ditandai `TODO`).
- Mengubah file di luar cakupan permintaan.

**Tanyakan lebih dulu jika:** kontrak API tidak jelas, ada beberapa pilihan desain yang berbeda jauh, atau perubahan memengaruhi banyak halaman.

## 13. Definition of Done

- [ ] Build debug berhasil tanpa error
- [ ] Unit test lulus
- [ ] Bottom Navigation berfungsi di semua tab
- [ ] Halaman Riwayat dan Teman menampilkan list dari API, item membuka halaman detail
- [ ] Ada loading, error, dan empty state
- [ ] Halaman Profil / About tersedia
- [ ] Tidak ada secret yang ter-commit
