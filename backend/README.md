# SplitDeals API

Express + libSQL/Turso. Skala kecil, penggunaan pribadi. Bisa jalan lokal atau serverless di Vercel.

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

Tidak ada upload foto, tidak ada tabel `settings`/`profiles`. Foto profil tetap lokal di HP.

## Jalankan lokal

```bash
npm install
npm start
```

Default `http://localhost:3000`. DB = `backend/local.db`.

Untuk Turso, isi `.env` dari `.env.example`, lalu `npm start`. `GET /health` harus `"database":"turso"`.

## Deploy ke Vercel

1. Commit & push `backend/` ke GitHub.
2. Vercel → **Add New... → Project** → import repo.
3. Set **Root Directory** = `backend`.
4. Framework Preset: Other.
5. Build Command: `npm install`.
6. Output Directory: kosong.
7. Environment Variables:
   ```text
   TURSO_URL=libsql://...
   TURSO_TOKEN=token_dari_turso
   ```
8. Deploy.
9. Cek `https://<project>.vercel.app/health`.
10. Di `local.properties`:
    ```properties
    baseUrl=https://<project>.vercel.app/
    ```
11. Build ulang APK release.

`vercel.json` me-rewrite semua path ke `/api/index.js`.

## Postman

```text
GET http://localhost:3000/health
GET https://<project>.vercel.app/health
POST https://<project>.vercel.app/bills
```

Emulator: `http://10.0.2.2:3000/`.
HP fisik + Vercel: pakai URL HTTPS langsung.
