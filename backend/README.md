# SplitDeals API

Express + libSQL (SQLite lokal atau Turso). Skala kecil, penggunaan pribadi.

## Jalankan

```bash
npm install
npm start
```

Default: `http://localhost:3000`, DB = `backend/local.db`.

Untuk Turso, isi `.env` dari `.env.example`:

```properties
TURSO_URL=libsql://splitdeals-xxx.turso.io
TURSO_TOKEN=token_dari_turso
```

`GET /health` harus mengembalikan `"database":"turso"`.

## Tabel

Tabel `bills` menyimpan kolom ringkasan (`title`, `status`, `mode`, pajak, diskon, total) plus `payload` JSON untuk detail (anggota, item, shares, payments). Foto profil **tidak** lewat API — tetap lokal di HP.

Tidak ada tabel `settings`, `profiles`, atau object storage.

## Endpoint

| Method | Path | Fungsi |
|---|---|---|
| GET | `/health` | Cek server + DB |
| GET | `/bills` | Semua bill |
| GET | `/bills/:id` | Satu bill |
| POST | `/bills` | Buat/simpan bill |
| PUT | `/bills/:id` | Update bill |
| PATCH | `/bills/:id/status` | Ubah status |
| PATCH | `/bills/:id/members/:memberId/paid` | Tandai lunas/batal |
| DELETE | `/bills/:id` | Hapus bill |

## Postman

```text
GET http://localhost:3000/health
GET http://localhost:3000/bills
POST http://localhost:3000/bills
Body → raw → JSON → payload BillInput
```

Emulator: `http://10.0.2.2:3000/`.
HP fisik: `http://IP_LAPTOP:3000/` (set `baseUrl` di `local.properties`).
