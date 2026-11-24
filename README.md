# Split%Deals

Aplikasi Android split bill untuk penggunaan pribadi skala kecil. Berjalan offline-first dan tersinkron dengan Web Service sendiri (Express + libSQL/Turso) yang di-deploy di Vercel.

## Fitur

- Tambah orang dan item belanja.
- Pilih pembayaran tiap item (Semuanya / per orang).
- Mode split **Per item** dan **Rata**.
- Pajak: tanpa / persen / nominal.
- Biaya lain dan diskon.
- Hasil per orang, rincian:
  ```text
  Anggota 1: {total}
  - item . Harga
  ```
- Catat pembayaran per anggota.

## Teknologi

| Lapisan | Teknologi |
|---|---|
| Android | Kotlin, Jetpack Compose Material 3 |
| Navigasi | Navigation Compose |
| State | ViewModel + StateFlow + collectAsState |
| UI states | UiState (Loading / Error / Success) |
| Lokal | SharedPreferences + org.json |
| Network | Retrofit + OkHttp + Gson |
| Backend | Node.js + Express |
| Database | libSQL / Turso (SQLite cloud) |
| Deploy | Vercel Serverless Function |
| Uang | Long + BigDecimal |
| Test | JUnit |
| Build | Gradle 9.5, AGP 9.3.3, JDK 19 |

## Arsitektur sinkron

```text
APK (offline-first)
  ↕ SharedPreferences + StateFlow
SyncManager (Retrofit)
  ↕ HTTPS
API Express (Vercel)
  ↕ libSQL client
Turso
```

- `HomeViewModel` dan `RiwayatViewModel` memanggil `GET /bills` saat dibuka.
- `BillScreen` memanggil `POST /bills` saat simpan/edit.
- `BillDetailViewModel` memanggil PATCH status/paid.
- `RiwayatViewModel` memanggil DELETE saat hapus.
- Jika API gagal, app tetap memakai data lokal.

## Web Service

Source: `backend/`

### Endpoint

| Method | Path | Fungsi |
|---|---|---|
| GET | `/bills` | Semua bill lengkap |
| GET | `/bills/:id` | Satu bill lengkap |
| POST | `/bills` | Buat / simpan bill |
| PUT | `/bills/:id` | Update bill |
| PATCH | `/bills/:id/status` | Ubah status BERJALAN / SELESAI |
| PATCH | `/bills/:id/members/:memberId/paid` | Tandai lunas / batal |
| DELETE | `/bills/:id` | Hapus bill |

Contoh:

```http
GET https://{vercel-app URL}/health
```

```json
{"status":"ok","database":"turso"}
```

Body `POST /bills` adalah `BillInput` lengkap:

```json
{
  "id": "bill-001",
  "title": "Makan Malam",
  "people": [{ "id": "p1", "name": "A" }],
  "items": [{ "id": "i1", "name": "Nasi", "priceCents": 10000, "sharerIds": ["p1"] }],
  "mode": "BY_ITEM",
  "taxKind": "NONE",
  "taxPercentBps": 0,
  "taxCents": 0,
  "otherCostsCents": 0,
  "discountKind": "NONE",
  "discountPercentBps": 0,
  "discountCents": 0,
  "status": "BERJALAN",
  "payments": [{ "personId": "p1", "paid": false }],
  "createdAt": 1
}
```

### Tabel database

```text
bills          ringkasan tagihan
bill_members   anggota + urutan
bill_items     item + harga + urutan
bill_shares    relasi item ↔ anggota
payments       status lunas per anggota
```

## Konfigurasi

### Android

`local.properties` (gitignored):

```properties
baseUrl=https://{vercel-app URL}/health
```

Ganti `baseUrl` → link deploy vercel.

### Backend

`backend/.env` (gitignored):

```properties
TURSO_URL=libsql://...
TURSO_TOKEN=...
```

`backend/vercel.json` me-rewrite semua path ke `/api/index.js`.

## Deploy backend

1. Commit & push `backend/` ke GitHub.
2. Vercel → import repo.
3. Root Directory: `backend`.
4. Build Command: `npm install`.
5. Start Command: `npm start`.
6. Environment Variables: `TURSO_URL`, `TURSO_TOKEN`.
7. Deploy.
8. Cek `/health`.
9. Jika `baseUrl` berubah, rebuild APK.

`Built for personal purposes.`