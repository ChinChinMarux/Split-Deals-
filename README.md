# Split the Bills

Aplikasi Android offline untuk membagi tagihan makan bersama secara adil, dalam Rupiah, dengan hasil sampai sen.

## Fitur

- Tambah orang dan item belanja.
- Tap chip orang untuk menentukan siapa yang menikmati tiap item.
- Dua mode split:
  - **Per item**: tiap orang bayar item yang dia ambil.
  - **Rata**: total dibagi rata.
- Pajak: tanpa pajak, persen, atau nominal.
- Biaya lain (service charge, ongkir, dll.) dibagi rata.
- Hasil per orang dengan rincian item, pajak, dan biaya lain.
- Cek otomatis: jumlah bagian selalu sama dengan total.
- Simpan bill di HP saja (tanpa internet, cloud, atau AI).
- Buka kembali dan edit bill tersimpan.

## Mata uang

- Semua uang disimpan sebagai sen (`Long`), tanpa `Double`.
- Input string diparse dengan `BigDecimal`.
- Format tampilan: `Rp12.50`.

## Teknologi

- Kotlin
- Jetpack Compose Material 3
- Navigation Compose
- SharedPreferences + `org.json` untuk penyimpanan lokal
- JUnit untuk unit test

## Struktur

```text
app/src/main/java/com/example/splitthedeals/
  domain/     Money.kt, Model.kt, SplitCalculator.kt
  data/       BillStore.kt, BillSession.kt
  ui/         theme/, common/, navigation/, bill/, result/, saved/
  MainActivity.kt
app/src/test/java/com/example/splitthedeals/domain/SplitCalculatorTest.kt
```

## Build

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-19"
.\gradlew.bat assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Test

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-19"
.\gradlew.bat testDebugUnitTest
```

Saat ini: 7 test, 0 failure.

## Lint

```powershell
$env:JAVA_HOME="C:\Program Files\Java\jdk-19"
.\gradlew.bat lintDebug
```

Saat ini: 0 error.

## Desain

- Warna terang: canvas `#F7F6F2`, surface putih, tinta `#1B1D1B`, aksen eucalyptus `#0B6E62`.
- Tombol minimal 56dp, radius 12dp, angka `tabular-nums`.
- Tanpa gradient, glow, emoji, grid, atau statistik palsu.
- Layout menyesuaikan inset navigasi HP dan keyboard.

## Status

- MVP offline selesai.
- Belum ada: edit nama duplikat di UI realtime, test penyimpanan, lokalisasi `Locale("id","ID")`, signing release.