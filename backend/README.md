# SplitDeals API

Express + libSQL/Turso. Skala kecil, penggunaan pribadi. Deploy di Vercel.

## Tabel

```text
bills          ringkasan tagihan (status, pajak, diskon, total)
bill_members   anggota + urutan
bill_items     item + harga + urutan
bill_shares    relasi item ↔ anggota
payments       status lunas per anggota
```

Tidak ada kolom `payload`. Tidak ada tabel `settings` atau `profiles`. Foto profil tetap lokal di HP.

## Endpoint

| Method | Path | Fungsi |
|---|---|---|
| GET | `/health` | Cek server + DB |
| GET | `/bills` | Semua bill lengkap |
| GET | `/bills/:id` | Satu bill lengkap |
| POST | `/bills` | Buat/simpan bill |
| PUT | `/bills/:id` | Update bill |
| PATCH | `/bills/:id/status` | Ubah status |
| PATCH | `/bills/:id/members/:memberId/paid` | Tandai lunas/batal |
| DELETE | `/bills/:id` | Hapus bill |

Response `BillInput` dirakit dari kelima tabel, jadi format Android tetap sama.

## Lokal

```bash
npm install
npm start
```

Default `http://localhost:3000`, DB `backend/local.db`. Untuk Turso, isi `.env` dari `.env.example`.

## Deploy Vercel

1. Commit & push `backend/`.
2. Vercel → import repo, **Root Directory** = `backend`.
3. Env:
   ```text
   TURSO_URL=libsql://...
   TURSO_TOKEN=...
   ```
4. Deploy.
5. Cek `https://<project>.vercel.app/health`.
6. Set `baseUrl` di `local.properties` ke URL Vercel.
7. Build ulang APK release.

## Dummy data

Jangan insert langsung ke tabel `bills` saja. Bill tanpa `bill_members` dan `bill_items` akan muncul di list tetapi error saat dibuka di detail.

Cara aman:

```http
POST https://<project>.vercel.app/bills
Content-Type: application/json
```

Body = `BillInput` lengkap dengan `people[]`, `items[]`, `payments[]`.
