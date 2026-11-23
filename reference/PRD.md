# Product Requirements Document (PRD) & Engineering Brief
**Project:** Split the Deals! — Android Split Bill Application  
**Version:** 1.0.0 (Release Candidate)  
**Standard:** Module 1 Android Mobile Engineering & Anti-Slop UI/UX Architecture  
**Status:** Approved for Implementation  

---

## 1. Executive Summary & Product Vision

### 1.1 Problem Statement
Membagi tagihan kelompok saat makan di restoran atau belanja bersama sering memicu gesekan sosial, kesalahan kalkulasi pembagian pajak/service charge secara manual, dan lambatnya pelunasan piutang. Kebanyakan solusi utilitas saat ini memiliki kelemahan:
1. Menuntut input manual yang repetitif untuk setiap baris struk belanja.
2. Membebankan biaya pajak (*PB1*) dan diskon secara tidak proporsional (sering kali dibagi rata, bukan proporsional terhadap nilai belanja masing-masing orang).
3. UI dipenuhi oleh "slop" AI (gradien kabur, visual map/globe tak berguna, emoji acak, dan layout jumping saat angka berubah).

### 1.2 Solution & Vision
**Split the Deals!** adalah aplikasi utilitas finansial Android modern berbasis Jetpack Compose yang mengotomatisasi pemindaian struk menggunakan *on-device ML Kit OCR*, mengalokasikan item dan kalkulasi proporsional secara transparan, serta memfasilitasi pelunasan melalui integrasi Web Service REST API dan tautan pembayaran/QRIS statis.

### 1.3 Academic & Architectural Compliance (Modul 1)
Sebagai syarat pemenuhan tugas proyek modul Android:
- **Client Framework**: Kotlin murni dengan Jetpack Compose & Material 3.
- **Backend Communication**: REST API Web Service Modul 1 via Retrofit + OkHttp + Gson.
- **Navigasi Global**: 5 Bottom Navigation Tab bar (**Home**, **Split!**, **Scan**, **Riwayat**, **Profil**).
- **Relasi List-Detail**: Minimal 2 layar utama bertipe list yang masing-masing memiliki layar detail spesifik.
- **Profil & Dedikasi**: 1 layar Profil Akun + Dokumen About Pengembang.

---

## 2. Target Persona & User Journey

### 2.1 Primary Persona
- **Nama**: Sarah Anindya (21 tahun)
- **Peran**: Mahasiswi / Bendahara Kelompok Sosial & Kuliner
- **Pain Points**: Lelah menghitung kalkulator kalkulasi struk panjang berulang kali, sungkan menagih teman satu per satu, sering nombok karena selisih pembulatan ratusan rupiah.
- **Goals**: Foto struk, pilih siapa pesan apa dalam < 30 detik, bagi link/QRIS tagihan otomatis via WhatsApp dengan nominal rupiah yang 100% adil dan presisi.

### 2.2 Core User Journey
```text
[Makan Bersama] ──> [Buka Tab Scan / Split!] ──> [Auto OCR ML Kit]
                                                         │
[Bagikan Link / QRIS] <── [Review & Simpan API] <── [Tandai Peserta & Diskon]
```

---

## 3. Product Architecture & Navigation Schema

Aplikasi mengadopsi navigasi berbasis *Single-Activity Architecture* dengan `NavHost` dan `NavigationBar`.

```text
app/
 ├── ui/
 │    ├── navigation/
 │    │    ├── Screen (Sealed Class: Home, Split, Scan, History, Profile, SessionDetail, FriendDetail)
 │    │    └── AppNavHost (SingleTop, SaveState, RestoreState)
 │    ├── home/          # HomeScreen (Dashboard Ringkasan & Aksi Cepat)
 │    ├── scan/          # CameraX OCR Viewfinder & OCR Table Inspector
 │    ├── split/         # Manual Split Bill Screen
 │    ├── history/       # HistoryListScreen & SessionDetailScreen (Detail per sesi)
 │    ├── friends/       # FriendListScreen & FriendDetailScreen (Saldo per teman)
 │    └── profile/       # User Profile, QRIS, & Modul 1 About Specs
 ├── domain/             # SplitCalculator (Pure Kotlin domain, Zero Android deps)
 ├── ocr/                # ML Kit Text Recognition & ReceiptParser regex engine
 └── data/               # Retrofit Remote Client, Repositories, DTOs
```

### 3.1 Mapping 5 Tab Utama & 2 Alur Detail
| Rute | Tipe Layar | Deskripsi & Komponen Kunci |
|---|---|---|
| `/home` | Root Tab 1 | Ringkasan *Net Financial Position* (Total Utang & Piutang), 4 Quick Actions, Live Sesi Aktif, dan Teman Sering Transaksi. |
| `/split` | Root Tab 2 | Kalkulator Split Manual tanpa kamera fisik: toggle pajak PB1 (11%), Service (5%), diskon voucher nominal, dan alokasi orang. |
| `/scan` | Root Tab 3 (Hero) | Floating central button. CameraX viewfinder, bounding box target, tabel inspeksi item OCR ML Kit dengan opsi sunting baris. |
| `/history` | Root Tab 4 (List 1) | List sesi split sebelumnya dengan filter status (*Semua*, *Belum Lunas*, *Lunas*). Item dapat diklik menuju `/session/{id}`. |
| `/session/{id}` | Sub-Layar Detail | Rincian lengkap struk: item tiap orang, status centang lunas per orang, dan log pembukuan. |
| `/friends` | Sub-Layar (List 2) | List relasi teman dengan delta piutang. Item dapat diklik menuju `/friend/{id}` untuk rincian riwayat patungan bersama. |
| `/profile` | Root Tab 5 | Data identitas Sarah Anindya, rekening BCA/QRIS statis, pengaturan pembulatan kasir, serta info teknis Modul 1. |

---

## 4. Anti-Slop UI/UX Engineering Guidelines

Mengacu pada repositori standar **`wwewtech/anti-slop-design`**, UI aplikasi dilarang keras mengadopsi klise AI dan wajib memenuhi 7-Axis Quality Gate:

1. **Anti-Slop Purity**:
   - **Zero Radial Gradient Blobs**: Tidak ada gradien ungu/indigo kabur. Seluruh permukaan menggunakan palet *Deep Dark / High-Contrast Clean Light* dengan batas garis rambut (1px hairline border: `rgba(255,255,255,0.08)` atau `#e2e8f0`).
   - **Zero Emoji as Icons**: Tombol dan navigasi 100% menggunakan SVG garis tunggal (*single-stroke vector icon*) 1.5px/2px (Material Symbols / Phosphor).
   - **Zero Space-Wasters**: Menghapus avatar atau foto dekoratif berukuran raksasa yang membuang ruang pandang. Layout padat informasi yang fungsional.
2. **Typographic Discipline & Tabular Monospace**:
   - Seluruh angka finansial menggunakan `font-variant-numeric: tabular-nums` agar tidak bergeser saat angka diperbarui secara dinamis.
   - Font: Plus Jakarta Sans / Inter / Hanken Grotesk dengan hierarki proporsional (Heading 16–20px, Subtitle 12–13px, Caption 11px).
3. **UX 1-to-3 Axiom**:
   - Satu aksi primer utama per layar (contoh: *"Simpan & Buat Tagihan"* atau *"Tagih Semua Tagihan Pending"*), didukung maksimal 3 aksi sekunder.
4. **Physical Tactile Micro-Interactions**:
   - Respon tekan interaktif fisik: `:active { transform: scale(0.97); }` dengan transisi snappy *ease-out* `< 150ms`.
   - Formula radius harmonis: $R_{\text{inner}} = \max(0, R_{\text{outer}} - \text{padding})$.

---

## 5. Domain Logic: Mathematical Split Algorithm

Kalkulator pembagian tagihan tidak boleh menggunakan tipe data pecahan non-deterministik (`Float` atau `Double`). Wajib menggunakan **`Long`** (senilai satuan rupiah bulat) atau **`BigDecimal`**.

### 5.1 Proporsionalitas Pajak, Service, dan Diskon
Untuk peserta $i$ dengan subtotal konsumsi $S_i$ dari total subtotal struk $S_{\text{total}}$:

$$\text{Rasio}_i = \frac{S_i}{S_{\text{total}}}$$

$$\text{Pajak}_i = \text{round}(\text{Rasio}_i \times \text{Total Pajak})$$

$$\text{Diskon}_i = \text{round}(\text{Rasio}_i \times \text{Total Diskon})$$

### 5.2 Largest Remainder Method (Zero Floating Penny Error)
Sisa pembulatan rupiah akibat kalkulasi diskrit dialokasikan ke pemilik pembagian terbesar sehingga:

$$\sum_{i=1}^{N} \text{Beban Final}_i \equiv \text{Grand Total Struk}$$

---

## 6. Functional & Non-Functional Specifications

### 6.1 Functional Requirements (FR)
- **FR-01 (OCR Scanning)**: On-device ML Kit membaca teks struk, regex mengekstrak nama menu, kuantitas `(\d+)x`, harga dengan format titik `(\d{1,3}(?:\.\d{3})*)`, serta baris Subtotal, Tax/PB1, dan Total.
- **FR-02 (Manual Correction)**: Semua baris teks dan angka hasil deteksi OCR dapat disunting langsung di preview sheet sebelum dikirim ke API.
- **FR-03 (Split Allocation)**: Pengguna dapat menetapkan menu tertentu ke satu orang, membagi satu menu ke beberapa orang (*item sharing*), atau menerapkan metode *Bagi Rata*.
- **FR-04 (REST API Integration)**: Sinkronisasi CRUD sesi dan teman ke endpoint Web Service Modul 1.
- **FR-05 (Payment Sharing)**: Menampilkan QRIS statis dan nomor rekening bank terverifikasi milik Host untuk disalin dengan sekali klik.

### 6.2 Non-Functional Requirements (NFR)
- **NFR-01 (Performance)**: Durasi ekstraksi OCR ML Kit pada struk berisi ≤ 20 baris diselesaikan dalam waktu kurang dari 800ms.
- **NFR-02 (Privacy & Offline Capability)**: Foto kamera diproses lokal di perangkat (*on-device sandbox*), tidak dikirimkan ke cloud pihak ketiga.
- **NFR-03 (Touch Targets)**: Seluruh target sentuhan interaktif memenuhi standar aksesibilitas WCAG (minimal $48 \times 48\,\text{dp}$).
- **NFR-04 (Architecture Purity)**: State management menggunakan `StateFlow<UiState<T>>` dengan penanganan eksplisit untuk state: `Loading`, `Success`, dan `Error` (dengan aksi retry).

---

## 7. Web Service REST API Contract (Modul 1)

**Base URL (Localhost Emulator):** `http://10.0.2.2:8080/api/v1/`

```json
{
  "status": "success",
  "data": {}
}
```

### Endpoint Matrix
- `GET /sessions`: Mengambil riwayat seluruh sesi split bill.
- `GET /sessions/{id}`: Mengambil detail breakdown pesanan per orang pada sesi spesifik.
- `POST /sessions`: Membuat sesi patungan baru dari hasil scan/input manual.
- `PUT /sessions/{id}/status`: Memperbarui status pembayaran peserta (Lunas / Pending).
- `GET /friends`: Mengambil daftar relasi teman dan ringkasan saldo utang-piutang.
- `GET /profile`: Mengambil metadata profil pengguna host dan konfigurasi rekening pembayaran.

---

## 8. Definition of Done (DoD) Checklist
- [x] Dokumen PRD dan spesifikasi teknis diselaraskan dengan Modul 1 & `wwewtech/anti-slop-design`.
- [x] Struktur 5 Bottom Navigation Tab bar terdefinisi secara presisi.
- [x] Algoritma pembagian tagihan matematis dirumuskan tanpa *floating-point loss*.
- [x] Skema relasi List-to-Detail pada Riwayat dan Teman memenuhi standar modul.
- [x] Komponen UI bebas dari klise AI (*anti-slop purity*, angka tabular, micro-interaction taktil).
- [x] Kontrak API dan arsitektur MVVM Clean Architecture terdokumentasi lengkap.
