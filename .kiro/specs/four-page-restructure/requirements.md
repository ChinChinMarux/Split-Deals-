# Requirements Document

## Introduction

Dokumen ini menentukan kebutuhan untuk merestrukturisasi aplikasi "Split the Bills!" dari satu halaman utama menjadi empat tab navigasi bawah (Home, Split, Riwayat, Profil) ditambah sub-layar detail. Perubahan mencakup penambahan Bottom Navigation Bar, halaman Home dengan ringkasan dan daftar bill berjalan, perbaikan halaman Split (item dapat dilipat, kolom Diskon, opsi "Semuanya", dan pembungkus "Biaya Lainnya"), pemindahan "Tersimpan" ke halaman Riwayat dengan aksi Selesaikan, Edit, dan Catat pembayaran, serta halaman Profil dengan foto dan identitas yang dapat disunting. Aplikasi tetap offline-first: data tersimpan lokal (BillStore) tidak boleh hilang, dan integrasi Web Service (REST API) ditambahkan sebagai sumber utama dengan fallback lokal.

## Glossary

- **SplitApp**: Aplikasi Android "Split the Bills!".
- **BottomBar**: Navigation Bar bawah dengan empat tab: Home, Split, Riwayat, Profil.
- **HomeScreen**: Tab Home — ringkasan, daftar bill berjalan, dan ringkasan status pembayaran.
- **SplitScreen**: Tab Split — pembuatan dan penyuntingan bill (pengganti BillScreen saat ini).
- **RiwayatScreen**: Tab Riwayat — daftar bill tersimpan.
- **ProfileScreen**: Tab Profil — data pengguna dan About aplikasi.
- **BillDetailScreen**: Sub-layar rincian bill dari Home atau Riwayat.
- **EditProfileScreen**: Sub-layar sunting profil.
- **SplitCalculator**: Logika pembagian murni Kotlin, tanpa dependensi Android.
- **BillStore**: Penyimpanan lokal (SharedPreferences + org.json).
- **BillRepository**: Repository yang membungkus sumber API dan fallback lokal.
- **Money**: Nilai uang sebagai Long sen; tidak boleh memakai Float atau Double.
- **Diskon**: Potongan harga, disimpan sebagai Long sen, dihitung setelah Pajak dan Biaya_lain.
- **Pajak**: Pajak dengan jenis TaxKind NONE, PERCENT, atau FIXED.
- **Biaya_lain**: Biaya tambahan (service charge, ongkir) yang dibagi rata.
- **Biaya_Lainnya**: Pembungkus UI yang berisi Pajak dan Biaya_lain.
- **Partisipan**: Orang dalam bill (PersonInput).
- **Sharer**: Partisipan yang menikmati suatu item.
- **Status_pembayaran**: Status lunas atau belum per partisipan untuk suatu bill.
- **BillStatus**: Status bill, bernilai BERJALAN atau SELESAI.
- **Semuanya**: Opsi memilih seluruh partisipan sekaligus sebagai sharer item.

## Requirements

### Requirement 1: Bottom Navigation Bar dan Struktur Halaman

**User Story:** Sebagai pengguna, saya ingin berpindah cepat antar fungsi utama lewat bilah navigasi bawah, sehingga tidak perlu menempuh banyak langkah.

#### Acceptance Criteria

1. THE SplitApp SHALL menampilkan BottomBar dengan empat tab: Home, Split, Riwayat, Profil.
2. WHEN pengguna menekan tab, THE SplitApp SHALL membuka halaman root tab tersebut dengan `launchSingleTop`, `saveState`, dan `restoreState` aktif.
3. THE SplitApp SHALL tidak menampilkan sub-layar BillDetailScreen dan EditProfileScreen sebagai tab di BottomBar.
4. WHEN aplikasi dibuka tanpa riwayat navigasi tersimpan, THE SplitApp SHALL menampilkan tab Home sebagai tujuan awal.
5. THE SplitApp SHALL menyediakan ikon garis tunggal Material Symbols untuk tiap tab dan tidak boleh memakai emoji sebagai ikon.
6. THE SplitApp SHALL menyediakan minimal enam halaman: HomeScreen, SplitScreen, RiwayatScreen, ProfileScreen, BillDetailScreen, dan EditProfileScreen.
7. WHEN pengguna menekan item pada daftar di HomeScreen atau RiwayatScreen, THE SplitApp SHALL membuka BillDetailScreen.

### Requirement 2: Halaman Home

**User Story:** Sebagai pengguna, saya ingin melihat ringkasan aktivitas bill dan status pembayaran, sehingga tahu apa yang perlu ditindak lanjuti.

#### Acceptance Criteria

1. THE HomeScreen SHALL menampilkan ringkasan yang berisi: jumlah bill BERJALAN, jumlah bill SELESAI, dan total partisipan belum membayar yang dihitung dengan menjumlahkan partisipan belum membayar dari setiap bill BERJALAN.
2. THE HomeScreen SHALL menampilkan daftar bill BERJALAN sebagai list; tiap item menampilkan judul, tanggal, total, dan jumlah partisipan yang sudah dan belum membayar.
3. WHEN pengguna menekan item bill BERJALAN, THE SplitApp SHALL membuka BillDetailScreen untuk bill tersebut.
4. THE HomeScreen SHALL menampilkan pintasan untuk membuka SplitScreen dan membuat bill baru.
5. WHILE data sedang dimuat, THE HomeScreen SHALL menampilkan state Loading.
6. IF sumber data gagal dimuat, THEN THE HomeScreen SHALL menampilkan state Error dengan tombol Coba lagi.
7. IF tidak ada bill tersimpan, THEN THE HomeScreen SHALL menampilkan state Empty.
8. WHEN pengguna menekan tombol Coba lagi, THE HomeScreen SHALL memuat ulang data.
9. WHEN pengguna menekan pintasan SplitScreen, THE SplitApp SHALL membuka SplitScreen.
10. WHEN pengguna menekan pintasan membuat bill baru, THE SplitApp SHALL membuka layar pembuatan bill baru.

### Requirement 3: Halaman Split — Item Kolaps

**User Story:** Sebagai pengguna, saya ingin item dapat dilipat, sehingga layar tidak penuh ketika terdapat banyak item.

#### Acceptance Criteria

1. THE SplitScreen SHALL menampilkan tiap item dalam kartu yang dapat dilipat.
2. WHILE kartu item dalam keadaan dilipat, THE SplitScreen SHALL menampilkan hanya nama item dan harga total item tersebut.
3. WHEN pengguna menekan kartu item yang dilipat, THE SplitScreen SHALL mengembangkan kartu untuk menampilkan field nama, harga, pilihan sharer, dan tombol hapus.
4. WHILE suatu item belum memiliki nama, THE SplitScreen SHALL mempertahankan kartu item tersebut dalam keadaan terbuka.
5. WHEN pengguna menekan kartu item yang terbuka dan telah memiliki nama, THE SplitScreen SHALL melipat kartu tersebut sehingga hanya menampilkan nama item dan harga total item tersebut.

### Requirement 4: Halaman Split — Opsi "Semuanya"

**User Story:** Sebagai pengguna, saya ingin memilih seluruh partisipan sekaligus untuk suatu item, sehingga tidak perlu menekan partisipan satu per satu.

#### Acceptance Criteria

1. WHILE mode Split adalah BY_ITEM, THE SplitScreen SHALL menampilkan opsi "Semuanya" pada pilihan sharer tiap item.
2. WHEN pengguna menekan "Semuanya" yang belum terpilih pada suatu item, THE SplitScreen SHALL menandai seluruh partisipan saat ini sebagai sharer item tersebut.
3. WHILE jumlah partisipan lebih dari 0 DAN seluruh partisipan sudah menjadi sharer item, THE SplitScreen SHALL menampilkan opsi "Semuanya" dalam keadaan terpilih.
4. WHEN pengguna menekan "Semuanya" yang sudah terpilih, THE SplitScreen SHALL menghapus seluruh sharer item tersebut.
5. WHEN kumpulan sharer item atau jumlah partisipan berubah, THE SplitScreen SHALL memperbarui keadaan terpilih "Semuanya" sesuai sharer item saat ini.

### Requirement 5: Halaman Split — Urutan dan Pembungkus "Biaya Lainnya"

**User Story:** Sebagai pengguna, saya ingin urutan input yang jelas yaitu Item lalu Diskon lalu Biaya Lainnya, sehingga susunan perhitungan mudah dipahami.

#### Acceptance Criteria

1. THE SplitScreen SHALL menampilkan section dalam urutan vertikal dari atas ke bawah: Items, kemudian Diskon, kemudian pembungkus "Biaya Lainnya".
2. THE SplitScreen SHALL menampilkan input Pajak dan input Biaya Lain di dalam satu pembungkus yang berlabel "Biaya Lainnya".
3. THE SplitScreen SHALL menampilkan kolom Diskon langsung di atas pembungkus "Biaya Lainnya".
4. THE SplitScreen SHALL menampilkan label pembungkus sebagai "Biaya Lainnya" dan bukan sebagai "Pajak".

### Requirement 6: Diskon — Input

**User Story:** Sebagai pengguna, saya ingin memasukkan diskon dengan jenis tanpa, persen, atau nominal, sehingga total tagihan berkurang sesuai potongan.

#### Acceptance Criteria

1. THE SplitScreen SHALL menyediakan pilihan jenis diskon: Tanpa, Persen, dan Nominal, dengan jenis Tanpa aktif secara default saat layar dimuat.
2. WHEN jenis diskon adalah Persen, THE SplitScreen SHALL menampilkan field persentase diskon yang menerima nilai antara 0 dan 100.
3. WHEN jenis diskon adalah Nominal, THE SplitScreen SHALL menampilkan field nominal diskon dalam format Money yang menerima nilai antara 0 dan total tagihan sebelum diskon.
4. THE SplitScreen SHALL menyimpan nilai diskon sebagai Money (Long sen) dan tidak boleh memakai Float atau Double.
5. WHEN jenis diskon adalah Tanpa, THE SplitScreen SHALL menyembunyikan field persentase dan field nominal diskon serta menetapkan nilai diskon menjadi 0.

### Requirement 7: Diskon — Perhitungan

**User Story:** Sebagai pengguna, saya ingin diskon dihitung dengan tepat dan dibagi adil, sehingga total bagian tiap orang konsisten dengan grand total.

#### Acceptance Criteria

1. THE SplitCalculator SHALL menghitung grand total sebagai totalItems ditambah Pajak ditambah Biaya_lain dikurangi Diskon.
2. WHEN jenis diskon adalah Persen, THE SplitCalculator SHALL menghitung Diskon sebagai persentase dari totalItems ditambah Pajak ditambah Biaya_lain menggunakan BigDecimal, dengan nilai persentase berada dalam rentang 0 hingga 100 inklusif.
3. WHEN jenis diskon adalah Nominal, THE SplitCalculator SHALL menggunakan nominal diskon yang dimasukkan.
4. WHEN jenis diskon adalah Tanpa, THE SplitCalculator SHALL mengatur Diskon menjadi nol.
5. THE SplitCalculator SHALL mengalokasikan Diskon tiap partisipan proporsional terhadap subtotal item partisipan tersebut pada mode BY_ITEM dan rata pada mode EVEN, dengan membulatkan bagian tiap partisipan ke bilangan bulat ke bawah lalu mendistribusikan sisa Diskon ke partisipan dengan pecahan terbesar (metode largest-remainder).
6. THE SplitCalculator SHALL menjamin bahwa jumlah Diskon seluruh partisipan sama dengan Diskon total.
7. THE SplitCalculator SHALL menjamin bahwa jumlah amount seluruh partisipan sama dengan grand total untuk semua kombinasi mode, Pajak, Biaya_lain, dan Diskon.
8. IF Diskon melebihi totalItems ditambah Pajak ditambah Biaya_lain, THEN THE SplitCalculator SHALL menolak bill dengan pesan error yang menunjukkan bahwa diskon melebihi subtotal bill, dan mempertahankan data bill yang dimasukkan tanpa perubahan.
9. THE SplitCalculator SHALL menampilkan baris Diskon per partisipan dan mengurangi amount tiap partisipan sesuai bagian diskonnya.
10. IF jenis diskon adalah Persen dan nilai persentase berada di luar rentang 0 hingga 100, atau jenis diskon adalah Nominal dan nominal diskon bernilai negatif, THEN THE SplitCalculator SHALL menolak bill dengan pesan error yang menunjukkan bahwa nilai diskon tidak valid, dan mempertahankan data bill yang dimasukkan tanpa perubahan.

### Requirement 8: Halaman Riwayat — Daftar Bill

**User Story:** Sebagai pengguna, saya ingin melihat seluruh bill tersimpan, sehingga dapat melanjutkan atau menyelesaikan bill.

#### Acceptance Criteria

1. THE RiwayatScreen SHALL menampilkan daftar bill tersimpan yang diurutkan dari tanggal dibuat terbaru ke terlama; tiap item menampilkan judul, tanggal dibuat, total, dan BillStatus.
2. WHEN pengguna menekan item bill, THE SplitApp SHALL membuka BillDetailScreen untuk bill tersebut.
3. THE SplitApp SHALL menampilkan fungsi "Tersimpan" pada tab Riwayat dan tidak lagi menampilkannya pada halaman utama.
4. WHILE data sedang dimuat, THE RiwayatScreen SHALL menampilkan state Loading.
5. IF sumber data gagal dimuat, THEN THE RiwayatScreen SHALL menampilkan state Error dengan tombol Coba lagi.
6. IF tidak ada bill tersimpan, THEN THE RiwayatScreen SHALL menampilkan state Empty.
7. WHEN pengguna menekan tombol Coba lagi pada state Error, THE RiwayatScreen SHALL memuat ulang daftar bill tersimpan.

### Requirement 9: BillDetailScreen — Aksi Selesaikan, Edit, Catat

**User Story:** Sebagai pengguna, saya ingin menyelesaikan, menyunting, dan mencatat pembayaran bill, sehingga status bill selalu mutakhir.

#### Acceptance Criteria

1. THE BillDetailScreen SHALL menampilkan rincian bill meliputi item per partisipan, Pajak, Biaya_lain, Diskon, grand total, dan Status_pembayaran tiap partisipan.
2. WHEN pengguna menekan aksi "Selesaikan", THE SplitApp SHALL menandai BillStatus bill menjadi SELESAI.
3. WHEN pengguna menekan aksi "Edit", THE SplitApp SHALL membuka SplitScreen dalam mode sunting untuk bill tersebut.
4. THE BillDetailScreen SHALL menampilkan checklist Status_pembayaran tiap partisipan beserta nominal yang harus dibayar partisipan tersebut.
5. WHEN pengguna menandai partisipan sudah membayar, THE SplitApp SHALL memperbarui Status_pembayaran partisipan tersebut menjadi sudah.
6. THE BillDetailScreen SHALL menampilkan progres pembayaran berupa jumlah partisipan sudah membayar dari total partisipan.
7. WHILE seluruh partisipan ditandai sudah membayar, THE SplitApp SHALL menampilkan aksi "Selesaikan" sebagai aksi yang direkomendasikan.
8. THE BillDetailScreen SHALL menjaga konsistensi sehingga jumlah partisipan ditandai bayar tidak melebihi total partisipan dan Status_pembayaran hanya bernilai sudah atau belum.
9. WHEN pengguna menekan aksi "Edit" pada bill yang BillStatus-nya SELESAI, THE SplitApp SHALL mengubah BillStatus bill menjadi belum SELESAI.

### Requirement 10: Halaman Profil dan Edit Profil

**User Story:** Sebagai pengguna, saya ingin mengisi dan menyunting profil saya, sehingga identitas saya tersimpan di aplikasi.

#### Acceptance Criteria

1. THE ProfileScreen SHALL menampilkan foto profil, Nama, email, nomor telepon, dan deskripsi.
2. THE ProfileScreen SHALL menyediakan opsi "Edit Profil" yang membuka EditProfileScreen.
3. WHEN pengguna memilih foto di EditProfileScreen, THE SplitApp SHALL mengambil gambar dari sistem file perangkat melalui file picker gambar.
4. WHEN pengguna menyimpan perubahan profil, THE SplitApp SHALL menyimpan foto, Nama, email, nomor telepon, dan deskripsi.
5. THE ProfileScreen SHALL menampilkan bagian About aplikasi yang memuat nama aplikasi.
6. WHILE profil sedang dimuat, THE ProfileScreen SHALL menampilkan state Loading.
7. IF profil gagal dimuat, THEN THE ProfileScreen SHALL menampilkan state Error dengan tombol Coba lagi.
8. WHEN EditProfileScreen dibuka, THE SplitApp SHALL menampilkan nilai profil saat ini untuk foto, Nama, email, nomor telepon, dan deskripsi agar dapat disunting.
9. WHEN penyimpanan profil berhasil, THE SplitApp SHALL menampilkan konfirmasi bahwa profil berhasil disimpan dan kembali ke ProfileScreen.
10. IF penyimpanan profil gagal, THEN THE SplitApp SHALL menampilkan pesan error yang menunjukkan penyimpanan gagal, menyediakan opsi untuk mencoba kembali, dan mempertahankan perubahan yang belum disimpan.

### Requirement 11: Data dan Penyimpanan Offline-first

**User Story:** Sebagai pengguna, saya ingin data tetap tersimpan walau tanpa internet, sehingga bill lama tidak hilang.

#### Acceptance Criteria

1. THE BillRepository SHALL mendelegasikan setiap operasi baca dan tulis bill ke API Retrofit sebagai sumber utama dan ke BillStore sebagai sumber cadangan lokal.
2. WHEN panggilan API Retrofit mengembalikan respons berhasil tanpa kesalahan jaringan, THE BillRepository SHALL membaca dan menulis data bill melalui API, lalu menyalin hasil operasi tersebut ke BillStore sehingga data di BillStore identik dengan data terbaru dari API.
3. IF panggilan API Retrofit gagal karena kesalahan jaringan, batas waktu terlampaui, atau respons tidak berhasil, THEN THE BillRepository SHALL melayani permintaan baca bill dari BillStore tanpa menghapus atau menimpa data bill tersimpan yang ada.
4. WHILE kontrak API belum final, THE BillRepository SHALL memakai data dummy sementara yang ditandai TODO untuk setiap panggilan API jaringan.
5. THE BillStore SHALL menyimpan setiap bill yang ditulis dan memuatnya kembali pada permintaan baca berikutnya tanpa menghapus atau mengubah bill yang disimpan sebelumnya.
6. THE BillRepository SHALL menyimpan Status_pembayaran partisipan dan BillStatus secara lokal.
7. THE BillRepository SHALL menyimpan data Profil termasuk foto secara lokal di BillStore.
8. THE BillStore SHALL memenuhi sifat round-trip sehingga bill yang dimuat kembali setelah disimpan memiliki nilai yang sama untuk setiap field dengan bill asli, termasuk Status_pembayaran partisipan dan BillStatus.
9. WHEN panggilan API Retrofit mengembalikan respons berhasil tanpa kesalahan jaringan, THE BillRepository SHALL menyalin data Profil termasuk foto dari BillStore ke API sehingga data API identik dengan data di BillStore.

### Requirement 12: Anti-slop dan UX

**User Story:** Sebagai pengguna, saya ingin tampilan bersih dan konsisten, sehingga informasi mudah dibaca tanpa gangguan visual.

#### Acceptance Criteria

1. THE SplitApp SHALL membatasi visual pada batas dengan ketebalan maksimal 1dp dan ikon garis tunggal tanpa gradien, glow, atau emoji.
2. THE SplitApp SHALL memakai angka tabular-nums untuk seluruh nominal finansial.
3. WHEN pengguna menekan elemen interaktif, THE SplitApp SHALL menerapkan efek tekan taktil scale(0,97) dengan transisi ease-out berdurasi kurang dari 150 mdetik.
4. THE SplitApp SHALL menjaga target sentuh interaktif minimal 48dp.
5. WHEN HomeScreen ditampilkan, THE SplitApp SHALL menampilkan angka ringkasan yang nilainya sama dengan hasil perhitungan dari data bill tersimpan dan nilai Status_pembayaran aktual.
6. THE SplitApp SHALL menampilkan string pengguna dari berkas strings.xml dengan bahasa default Indonesia.

## Correctness Properties

Properti berikut wajib diuji, terutama melalui property-based test pada SplitCalculator dan BillStore:

1. **Jumlah bagian sama dengan grand total**: Untuk semua kombinasi mode (EVEN dan BY_ITEM), Pajak (NONE, PERCENT, FIXED), Biaya_lain, dan Diskon (Tanpa, Persen, Nominal), jumlah amount seluruh partisipan sama persis dengan grand total (totalItems + Pajak + Biaya_lain − Diskon).
2. **Jumlah diskon per partisipan sama dengan diskon total**: Alokasi diskon memakai largest-remainder sehingga tidak ada sen hilang.
3. **Diskon mengurangi total**: Grand total setelah diskon tidak lebih besar dari total sebelum diskon; bila diskon melebihi total sebelum diskon, bill ditolak.
4. **Konsistensi status pembayaran**: Jumlah partisipan ditandai bayar tidak melebihi total partisipan; Status_pembayaran hanya bernilai sudah atau belum; BillStatus SELESAI konsisten dengan keadaan pembayaran bill.
5. **Round-trip penyimpanan**: Menyalin bill ke JSON lalu memuat kembali menghasilkan bill yang setara, termasuk Status_pembayaran dan BillStatus.
6. **Tanpa tipe pecahan**: Seluruh nilai uang adalah Long sen atau BigDecimal; tidak ada Float atau Double di jalur uang.
