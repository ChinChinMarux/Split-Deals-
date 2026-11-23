# Split%Deals

Aplikasi Android offline untuk membagi tagihan makan bersama secara adil, dalam Rupiah, dengan hasil akurat.

## Fitur

- Tambah orang dan item belanja.
- Tap chip orang untuk menentukan siapa yang harus membayar tiap item.
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

- Semua uang disimpan sebagai karakter (`Long`), tanpa `Double`.
- Input string diparse dengan `BigDecimal`.
- Format tampilan: `Rp12500`.

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

## Desain

- Warna terang: canvas `#F7F6F2`, surface putih, tinta `#1B1D1B`, aksen eucalyptus `#0B6E62`.
- Tombol minimal 56dp, radius 12dp, angka `tabular-nums`.
- Layout menyesuaikan inset navigasi HP dan keyboard.