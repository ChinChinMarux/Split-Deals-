# PLAN — Split the Deals!

Panduan eksekusi pengembangan. Turun dari `PRD.md` + `AGENTS.md`. Baca keduanya sebelum mulai.

> **Status:** Keputusan §0 resolved. Tanggal mulai: 2026-11-19.

---

## 0. Keputusan (RESOLVED — jawaban user 2026-11-19)

### 0.1 Bottom nav 5 tab — OPSI B (dipilih user)
Root = **Home | Split | Scan | Riwayat | Profil**. Teman jadi sub-layar (non-root), diakses dari Beranda atau Profil.

**Risiko tercatat:** melanggar `AGENTS.md` §5 eksplisit (Teman root). Ketentuan "minimal 2 halaman utama berisi list" tetap dipenuhi: Riwayat (root list) + Teman (sub-layar list, item klik → `/friend/{id}`). Teman non-root tapi tetap list utama dengan detail. Patuh Bagian 1.

### 0.2 Endpoint status pembayaran — `/sessions/{id}/status`
Pakai rute `PUT /sessions/{id}/status` (PRD §7). API = sumber kebenaran; cek rute aktual saat Web Service siap.

### 0.3 Port Web Service — 8080
Base URL: `http://10.0.2.2:8080/api/v1/`. Simpan di `BuildConfig` via `local.properties`, bisa override. Jangan hardcode banyak file.

### 0.4 Data dummy sementara — OK
Boleh pakai data dummy bertanda `// TODO: ganti dengan API` sampai Web Service jadi. Tidak boleh permanen.

---

## 1. Lingkup & Tujuan

Bangun aplikasi Android split-bill. Foto struk → OCR → alokasi peserta → hitung proporsional (PPN/service/diskon) → simpan via REST API → bagi link/QRIS.

Patuh 4 ketentuan tugas (`AGENTS.md` §1):
1. Pakai Web Service (REST API).
2. Bottom Navigation Bar.
3. Minimal 5 halaman.
4. Minimal 2 halaman utama list + detail.
5. 1 halaman Profile/About.

---

## 2. Tech Stack (kunci di `AGENTS.md` §2)

Kotlin · Jetpack Compose + Material 3 · Navigation Compose · Retrofit + OkHttp + Gson · MVVM (ViewModel + StateFlow + Repository) · CameraX · ML Kit Text Recognition (on-device) · Coil · minSdk 26 · Gradle Kotlin DSL.

**Jangan** tambah library tanpa alasan jelas. **Jangan** ganti Compose → XML. **Jangan** pakai DI framework.

---

## 3. Struktur Paket (`AGENTS.md` §3)

```text
app/src/main/java/com/example/splitthedeals/
├─ data/
│  ├─ remote/        # ApiService, DTO, RetrofitClient
│  └─ repository/    # SessionRepository, FriendRepository, ProfileRepository
├─ ocr/              # ReceiptScanner (ML Kit), ReceiptParser (regex)
├─ domain/           # SplitCalculator (pure Kotlin, no Android deps)
├─ ui/
│  ├─ navigation/    # Screen (sealed), AppNavHost, BottomBar
│  ├─ home/          # HomeScreen, HomeViewModel
│  ├─ split/         # SplitScreen, SplitViewModel (manual split, tanpa kamera)
│  ├─ scan/          # ScanScreen, ScanViewModel (kamera + OCR)
│  ├─ history/       # HistoryScreen (list), SessionDetailScreen (detail)
│  ├─ friends/       # FriendsScreen (list), FriendDetailScreen (detail)
│  ├─ profile/       # ProfileScreen (profil + about)
│  └─ theme/
└─ MainActivity.kt
```

---

## 4. Rute & Navigasi (Opsi B)

| Rute | Halaman | Root? | List? |
|---|---|---|---|
| `home` | Home | ✅ | — |
| `split` | Split manual | ✅ | — |
| `scan` | Scan (kamera + OCR) | ✅ | — |
| `history` | Riwayat | ✅ | ✅ item → detail |
| `session/{id}` | Detail Sesi | — | detail |
| `friends` | Teman | sub-layar | ✅ item → detail |
| `friend/{id}` | Detail Teman | — | detail |
| `profile` | Profil / About | ✅ | — |

Aturan (`AGENTS.md` §5): detail tidak muncul di bottom bar. Pakai `launchSingleTop` + `saveState` + `restoreState` saat pindah tab.

Total halaman = 7 ≥ 5 ✅. List utama = 2 (Riwayat, Teman) ✅. Detail masing-masing = 2 ✅. Profile/About = 1 ✅.

---

## 5. Tahapan Eksekusi

Tiap tahap = satu PR / commit kecil. Build + unit test wajib lulus sebelum lanjut (`AGENTS.md` §4).

### Tahap 0 — Skeleton & Build Config
- Init Gradle Kotlin DSL project, `com.example.splitthedeals`, minSdk 26, compileSdk 36.
- Tambah dependency: Compose BOM, Material 3, navigation-compose, retrofit, okhttp, gson, coil, camerax, mlkit-text-recognition, lifecycle-viewmodel-compose.
- `local.properties` untuk base URL + port + sdk.dir (jangan commit). Baca di `BuildConfig` via `buildConfigField`.
- `AndroidManifest.xml`: izin `CAMERA`, `usesCleartextTraffic` true (debug manifest only).
- `MainActivity.kt` + `AppNavHost` + `BottomBar` kosong (5 tab, klik pindah, isi placeholder).
- **Definisi Selesai:** `./gradlew assembleDebug` lulus, 5 tab bisa diklik, `launchSingleTop`/`saveState`/`restoreState` aktif.

### Tahap 1 — Theme & Design Token (anti-slop)
- `ui/theme/`: palet deep dark + high-contrast light, hairline border 1px (`rgba(255,255,255,0.08)` / `#e2e8f0`). **Dilarang** radial gradient blob.
- Font Plus Jakarta Sans / Inter (downloadable font atau bundle). Hierarki: Heading 16–20px, Subtitle 12–13px, Caption 11px.
- Token angka finansial: `tabular-nums` (Compose: `TextStyle(fontFeatureSettings = "tnum")` atau `FontVariation`). Tujuan: angka tidak bergeser saat update.
- Komponen dasar bebas-state: `TactileCard`, `PrimaryButton`, `TabularAmountText`, `SectionHeader`. Tactile: `scale(0.97)` on press, ease-out < 150ms.
- Ikon: Material Symbols / Phosphor garis tunggal 1.5/2px. **Dilarang** emoji sebagai ikon.
- **Definisi Selesai:** komponen reusable jadi, preview Compose tampil sesuai anti-slop.

### Tahap 2 — Domain: SplitCalculator (pure Kotlin)
- Kelas murni, **nol** dependensi Android.
- Tipe uang: `Long` (rupiah bulat) atau `BigDecimal`. **Dilarang** `Float`/`Double` (`AGENTS.md` §8).
- Algoritma:
  1. `rasio_i = S_i / S_total` (pakai `BigDecimal`, scale presisi).
  2. `pajak_i = rasio_i × total_pajak`, bulat `RoundingMode.HALF_UP`.
  3. `service_i`, `diskon_i` sama.
  4. `beban_i = subtotal_i + pajak_i + service_i − diskon_i`.
  5. **Largest remainder**: hitung sisa = `grand_total − Σ beban_i`, alokasikan ke peserta rasio terbesar satu-satu sampai sisa 0.
- Input: `Receipt(subtotal, pajak, service, diskon, grandTotal)`, `List<ItemAssignment(item, peserta, qty)>`. Output: `Map<Peserta, Long>` + selisih verifikasi = 0.
- Item sharing: satu item ke banyak peserta (subtotal item dibagi sesuai bobot).
- Mode bagi rata: semua item ke semua peserta.
- **Unit test wajib** (Tahap 2 sebelum lanjut): item bersama, diskon, pajak, pembulatan, tanpa service, harga dengan titik dan koma, sisa pembulatan = 0, grand total cocok.
- **Definisi Selesai:** test lulus, `./gradlew testDebugUnitTest` hijau, Σ beban == grand total di semua kasus.

### Tahap 3 — Data Layer: Retrofit + DTO + Repository
- `data/remote/RetrofitClient.kt`: singleton OkHttpClient + Gson + `BuildConfig.BASE_URL`. Logging interceptor debug only (jangan log data pribadi di release).
- `data/remote/ApiService.kt`: interface, sesuaikan dengan kontrak `AGENTS.md` §6 + PRD §7. Endpoint: sessions CRUD, friends CRUD, profile GET/PUT, status PUT `/sessions/{id}/status`.
- `data/remote/dto/`: `SessionDto`, `SessionItemDto`, `ParticipantDto`, `FriendDto`, `ProfileDto`, `ApiResponse<T>` (`{status, data}`).
- Mapper DTO ↔ domain model (`domain/model/`).
- `data/repository/`: `SessionRepository`, `FriendRepository`, `ProfileRepository`. Antarmuka + impl. Panggilan jaringan di `suspend fun`, `runCatching`, hasil `Result<T>` atau `UiState`.
- **Definisi Selesai:** kompilasi lolos, repository bisa di-fake untuk test ViewModel.

### Tahap 4 — Home
- `HomeViewModel`: expose `StateFlow<UiState<HomeSummary>>`.
- Summary: net financial position (total utang + piutang dari API), sesi aktif (live), teman sering transaksi, 4 quick action (Scan, Split Manual, Tagih Pending, Teman).
- UI: ringkasan di atas, quick action grid, list sesi aktif, list teman top. Tabular nums di semua angka.
- State: loading, error (tombol coba lagi), empty.
- **Definisi Selesai:** Home tampil dari API (atau dummy bertanda TODO), 3 state lengkap.

### Tahap 5 — Scan + OCR
- `ocr/ReceiptScanner.kt`: bungkus ML Kit `TextRecognition`. Jalankan di perangkat. Foto tidak dikirim ke server (`AGENTS.md` §9).
- `ocr/ReceiptParser.kt`: **fungsi murni**, input teks OCR → `ParsedReceipt`. Regex:
  - Format angka RI: `\d{1,3}(?:\.\d{3})*` (45.000).
  - Qty: `(\d+)x` atau `x(\d+)`.
  - Keyword: `SUBTOTAL|SUB TOTAL`, `SERVICE`, `PB1|PPN|TAX`, `DISC`, `TOTAL`, `PEMBULATAN`.
  - Baris keyakinan rendah / total tidak cocok → ditandai `confidence: Low`.
- `ScanViewModel`: alur izin → kamera → OCR → parsed → editable preview.
- `ScanScreen`:
  1. Minta izin `CAMERA` saat pertama buka, penjelasan singkat. Tangani tolak (pesan + tombol buka setting).
  2. CameraX viewfinder + bounding box target.
  3. Setelah foto → tabel inspeksi item, semua baris bisa disunting (FR-02).
  4. Alokasi peserta: item ke satu orang, item sharing, atau bagi rata (FR-03).
  5. Tombol primer: "Simpan & Buat Tagihan" → panggil `SplitCalculator` → `POST /sessions`.
- UX 1-to-3: satu aksi primer per layar.
- **Unit test ReceiptParser** (bareng Tahap 2 atau di Tahap 5): contoh teks struk asli, kasus titik+koma, tanpa service, keyakinan rendah.
- **Definisi Selesai:** scan → OCR → edit → alokasi → simpan API. Parser test lulus. Izin ditangani.

### Tahap 6 — History (List) + Session Detail
- `HistoryViewModel`: `StateFlow<UiState<List<SessionItem>>>`. Filter status (Semua/Belum Lunas/Lunas).
- `HistoryScreen`: list padat, angka tabular, item klik → `session/{id}`.
- `SessionDetailViewModel`: ambil `GET /sessions/{id}`. Rincian item per orang, status centang lunas per orang, log pembukuan. Tombol ubah status → `PUT /sessions/{id}/status` (lihat §0.2).
- State loading/error/empty di keduanya.
- **Definisi Selesai:** list dari API, item buka detail, filter jalan, status lunas bisa diubah.

### Tahap 7 — Friends (List) + Friend Detail
- `FriendViewModel`: `StateFlow<UiState<List<Friend>>>`. Tampilkan delta piutang.
- `FriendsScreen`: list, item klik → `friend/{id}`.
- `FriendDetailViewModel`: riwayat patungan bersama teman, saldo akhir.
- Tambah/edit/hapus teman (GET/POST/PUT/DELETE `/friends/{id}`).
- State lengkap.
- **Definisi Selesai:** list+detail dari API, CRUD teman jalan.

### Tahap 8 — Profile / About
- `ProfileViewModel`: `GET /profile`.
- `ProfileScreen`: data identitas host, rekening BCA + QRIS statis (tombol salin sekali klik, FR-05), pengaturan pembulatan kasir, info teknis Modul 1 (About).
- Jangan simpan nomor kartu. Info rekening/QRIS hanya tampil atas aksi user (`AGENTS.md` §11).
- **Definisi Selesai:** profil + about tampil, QRIS bisa disalin.

### Tahap 9 — Integrasi & Polish
- Hubungkan semua layar lewat navigasi.
- Cek semua state (loading/error/empty) di tiap layar.
- Lint: `./gradlew lintDebug` bersih.
- Verifikasi touch target ≥ 48dp (NFR-03).
- Cek `usesCleartextTraffic` hanya di debug manifest, bukan release.
- Cek tidak ada secret ter-commit (`local.properties`, keystore, token).
- **Definisi Selesai:** `assembleDebug` + `testDebugUnitTest` + `lintDebug` lulus. DoD `AGENTS.md` §13 centang semua.

---

## 6. Domain Model (inti)

```kotlin
// domain/model/ — nol dependensi Android
data class Receipt(
    val items: List<ReceiptItem>,
    val subtotal: Long,
    val tax: Long = 0,          // PB1/PPN
    val service: Long = 0,
    val discount: Long = 0,
    val rounding: Long = 0,
    val grandTotal: Long,
)

data class ReceiptItem(
    val name: String,
    val qty: Int,
    val price: Long,            // harga satuan
    val confidence: Confidence = Confidence.High,
)

enum class Confidence { High, Low }

data class Assignment(
    val item: ReceiptItem,
    val shares: List<Share>,    // satu share = satu orang; item sharing > 1 share
)

data class Share(
    val friendId: String,
    val weight: BigDecimal = BigDecimal.ONE,  // untuk bagi proporsional dalam item
)

// domain/SplitCalculator.kt
class SplitCalculator {
    fun split(receipt: Receipt, assignments: List<Assignment>): SplitResult
}

data class SplitResult(
    val perPerson: Map<String, Long>,   // friendId -> beban final
    val breakdown: Map<String, PersonBreakdown>,
    val checksum: Long,                 // Σ perPerson harus == receipt.grandTotal
)

data class PersonBreakdown(
    val subtotal: Long,
    val tax: Long,
    val service: Long,
    val discount: Long,
    val total: Long,
)
```

---

## 7. Kontrak API (ringkas, sumber `AGENTS.md` §6 + PRD §7)

| Metode | Endpoint | Fungsi |
|---|---|---|
| GET | `/sessions` | Daftar sesi |
| GET | `/sessions/{id}` | Detail sesi |
| POST | `/sessions` | Buat sesi dari hasil scan |
| PUT | `/sessions/{id}/status` | Ubah status pembayaran (dipilih §0.2) |
| DELETE | `/sessions/{id}` | Hapus sesi |
| GET/POST | `/friends` | Daftar / tambah teman |
| GET/PUT/DELETE | `/friends/{id}` | Detail / ubah / hapus teman |
| GET/PUT | `/profile` | Profil pengguna |

Respons standar: `{ "status": "success", "data": {...} }`. API = sumber kebenaran; sesuaikan klien bila beda.

---

## 8. Anti-Slop Checklist (PRD §4)

- [ ] Nol radial gradient blob. Hairline border 1px saja.
- [ ] Nol emoji sebagai ikon. SVG garis tunggal 1.5/2px.
- [ ] Nol avatar/foto dekoratif raksasa. Layout padat informasi.
- [ ] Angka finansial: `tabular-nums`.
- [ ] Font Plus Jakarta Sans / Inter, hierarki proporsional.
- [ ] Satu aksi primer per layar, maks 3 sekunder.
- [ ] Tactile press: `scale(0.97)`, ease-out < 150ms.
- [ ] Radius harmonis: `R_inner = max(0, R_outer − padding)`.
- [ ] Touch target ≥ 48dp.

---

## 9. Keamanan & Privasi (`AGENTS.md` §11)

- Jangan commit `local.properties`, keystore, kata sandi, token.
- Jangan simpan nomor kartu. Rekening/QRIS tampil atas aksi user saja.
- `usesCleartextTraffic` hanya dev. Release wajib HTTPS.
- Jangan log data pribadi di rilis.

---

## 10. Pengujian (`AGENTS.md` §10)

Unit test wajib:
- `SplitCalculator`: item bersama, diskon, pajak, pembulatan, tanpa service, harga titik+koma, sisa = 0.
- `ReceiptParser`: contoh teks struk asli, keyakinan rendah, total tidak cocok.
- ViewModel: fake repository, cek `UiState` Loading/Success/Error.

**Dilarang** ubah test agar lulus tanpa perbaiki sebab (`AGENTS.md` §10).

---

## 11. Definition of Done (`AGENTS.md` §13)

- [ ] Build debug tanpa error.
- [ ] Unit test lulus.
- [ ] Bottom nav jalan di semua tab.
- [ ] Riwayat + Teman tampilkan list dari API, item buka detail.
- [ ] Ada loading, error, empty state.
- [ ] Halaman Profil/About ada.
- [ ] Nol secret ter-commit.

---

## 12. Urutan Prioritas Cepat

1. Tahap 0 → 2 (skeleton + domain + test). Bisa jalan tanpa API.
2. Tahap 3 (data layer) bisa difake bila Web Service belum siap.
3. Tahap 4–8 linear, tiap tahap build+test hijau.
4. Tahap 9 polish + DoD.

---

## 13. Catatan Risiko

| Risiko | Mitigasi |
|---|---|
| Teman non-root melanggar AGENTS §5 | Tetap list utama + detail; diakses jelas dari Home/Profil |
| Web Service belum siap | Data dummy bertanda `TODO`, ganti saat API jadi |
| Akurasi OCR rendah | Parser tandai baris keyakinan rendah, user wakib edit sebelum simpan |
| Pembulatan tidak cocok | Largest remainder + checksum assert Σ == grand total |
| Konflik AGENTS vs PRD | Keputusan user §0; API = sumber kebenaran |
| Performa OCR > 800ms (NFR-01) | ML Kit on-device, batasi ≤ 20 baris, profiling |
