require("dotenv").config();

const express = require("express");
const cors = require("cors");
const { createClient } = require("@libsql/client");

const app = express();
app.use(cors());
app.use(express.json({ limit: "2mb" }));

const state = globalThis;

function getDb() {
  if (!state.__splitdealsDb) {
    const url = process.env.TURSO_URL || "file:local.db";
    const config = process.env.TURSO_TOKEN
      ? { url, authToken: process.env.TURSO_TOKEN }
      : { url };
    state.__splitdealsDb = createClient(config);
  }
  return state.__splitdealsDb;
}

async function ensureInit() {
  if (!state.__splitdealsInit) {
    state.__splitdealsInit = (async () => {
      const db = getDb();
      await db.execute(`
        CREATE TABLE IF NOT EXISTS bills (
          id TEXT PRIMARY KEY,
          title TEXT NOT NULL DEFAULT '',
          status TEXT NOT NULL DEFAULT 'BERJALAN',
          mode TEXT NOT NULL DEFAULT 'BY_ITEM',
          tax_kind TEXT NOT NULL DEFAULT 'NONE',
          tax_percent_bps INTEGER NOT NULL DEFAULT 0,
          tax_cents INTEGER NOT NULL DEFAULT 0,
          other_costs_cents INTEGER NOT NULL DEFAULT 0,
          discount_kind TEXT NOT NULL DEFAULT 'NONE',
          discount_percent_bps INTEGER NOT NULL DEFAULT 0,
          discount_cents INTEGER NOT NULL DEFAULT 0,
          grand_total_cents INTEGER NOT NULL DEFAULT 0,
          payload TEXT NOT NULL,
          created_at INTEGER NOT NULL DEFAULT 0,
          updated_at INTEGER NOT NULL DEFAULT 0
        )
      `);

      const info = await db.execute("PRAGMA table_info(bills)");
      const columns = new Set(info.rows.map((row) => row.name));
      const migrations = {
        title: "TEXT NOT NULL DEFAULT ''",
        status: "TEXT NOT NULL DEFAULT 'BERJALAN'",
        mode: "TEXT NOT NULL DEFAULT 'BY_ITEM'",
        tax_kind: "TEXT NOT NULL DEFAULT 'NONE'",
        tax_percent_bps: "INTEGER NOT NULL DEFAULT 0",
        tax_cents: "INTEGER NOT NULL DEFAULT 0",
        other_costs_cents: "INTEGER NOT NULL DEFAULT 0",
        discount_kind: "TEXT NOT NULL DEFAULT 'NONE'",
        discount_percent_bps: "INTEGER NOT NULL DEFAULT 0",
        discount_cents: "INTEGER NOT NULL DEFAULT 0",
        grand_total_cents: "INTEGER NOT NULL DEFAULT 0",
        created_at: "INTEGER NOT NULL DEFAULT 0",
        updated_at: "INTEGER NOT NULL DEFAULT 0",
      };
      for (const [name, definition] of Object.entries(migrations)) {
        if (!columns.has(name)) {
          await db.execute(`ALTER TABLE bills ADD COLUMN ${name} ${definition}`);
        }
      }
    })().catch((error) => {
      state.__splitdealsInit = null;
      throw error;
    });
  }
  return state.__splitdealsInit;
}

async function readyDb() {
  await ensureInit();
  return getDb();
}

function parseBill(req, res) {
  const bill = req.body;
  if (!bill || !bill.id || !Array.isArray(bill.people) || !Array.isArray(bill.items)) {
    res.status(400).json({ error: "Bill must contain id, people[], items[]" });
    return null;
  }
  return bill;
}

function rowToBill(row) {
  return JSON.parse(row.payload);
}

function databaseName() {
  const url = process.env.TURSO_URL || "file:local.db";
  return url.startsWith("libsql") ? "turso" : "sqlite";
}

app.get("/health", async (req, res) => {
  try {
    await readyDb();
    res.json({ status: "ok", database: databaseName() });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get("/bills", async (req, res) => {
  try {
    const db = await readyDb();
    const rs = await db.execute("SELECT payload FROM bills ORDER BY created_at DESC");
    res.json(rs.rows.map(rowToBill));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get("/bills/:id", async (req, res) => {
  try {
    const db = await readyDb();
    const rs = await db.execute({
      sql: "SELECT payload FROM bills WHERE id = ?",
      args: [req.params.id],
    });
    if (rs.rows.length === 0) {
      res.status(404).json({ error: "Bill not found" });
      return;
    }
    res.json(rowToBill(rs.rows[0]));
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

async function upsert(bill, res) {
  try {
    const db = await readyDb();
    await db.execute({
      sql: `INSERT INTO bills (
        id, title, status, mode,
        tax_kind, tax_percent_bps, tax_cents, other_costs_cents,
        discount_kind, discount_percent_bps, discount_cents,
        grand_total_cents, payload, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
      ON CONFLICT(id) DO UPDATE SET
        title = excluded.title,
        status = excluded.status,
        mode = excluded.mode,
        tax_kind = excluded.tax_kind,
        tax_percent_bps = excluded.tax_percent_bps,
        tax_cents = excluded.tax_cents,
        other_costs_cents = excluded.other_costs_cents,
        discount_kind = excluded.discount_kind,
        discount_percent_bps = excluded.discount_percent_bps,
        discount_cents = excluded.discount_cents,
        grand_total_cents = excluded.grand_total_cents,
        payload = excluded.payload,
        updated_at = excluded.updated_at`,
      args: [
        bill.id,
        bill.title || "",
        bill.status || "BERJALAN",
        bill.mode || "BY_ITEM",
        bill.taxKind || "NONE",
        bill.taxPercentBps || 0,
        bill.taxCents || 0,
        bill.otherCostsCents || 0,
        bill.discountKind || "NONE",
        bill.discountPercentBps || 0,
        bill.discountCents || 0,
        bill.grandTotalCents || 0,
        JSON.stringify(bill),
        bill.createdAt || Date.now(),
        Date.now(),
      ],
    });
    res.json(bill);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
}

app.post("/bills", async (req, res) => {
  const bill = parseBill(req, res);
  if (!bill) return;
  await upsert(bill, res);
});

app.put("/bills/:id", async (req, res) => {
  const bill = parseBill(req, res);
  if (!bill) return;
  await upsert({ ...bill, id: req.params.id }, res);
});

app.patch("/bills/:id/status", async (req, res) => {
  const status = req.body && req.body.status;
  if (status !== "BERJALAN" && status !== "SELESAI") {
    res.status(400).json({ error: "status must be BERJALAN or SELESAI" });
    return;
  }
  try {
    const db = await readyDb();
    const rs = await db.execute({
      sql: "SELECT payload FROM bills WHERE id = ?",
      args: [req.params.id],
    });
    if (rs.rows.length === 0) {
      res.status(404).json({ error: "Bill not found" });
      return;
    }
    await upsert({ ...rowToBill(rs.rows[0]), status }, res);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.patch("/bills/:id/members/:memberId/paid", async (req, res) => {
  const paid = req.body && req.body.paid;
  if (typeof paid !== "boolean") {
    res.status(400).json({ error: "paid must be boolean" });
    return;
  }
  try {
    const db = await readyDb();
    const rs = await db.execute({
      sql: "SELECT payload FROM bills WHERE id = ?",
      args: [req.params.id],
    });
    if (rs.rows.length === 0) {
      res.status(404).json({ error: "Bill not found" });
      return;
    }
    const bill = rowToBill(rs.rows[0]);
    const payments = Array.isArray(bill.payments) ? [...bill.payments] : [];
    const index = payments.findIndex((p) => p.personId === req.params.memberId);
    if (index >= 0) payments[index] = { ...payments[index], paid };
    else payments.push({ personId: req.params.memberId, paid });
    await upsert({ ...bill, payments }, res);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.delete("/bills/:id", async (req, res) => {
  try {
    const db = await readyDb();
    await db.execute({ sql: "DELETE FROM bills WHERE id = ?", args: [req.params.id] });
    res.status(204).end();
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

if (require.main === module) {
  const port = process.env.PORT || 3000;
  app.listen(port, "0.0.0.0", () => {
    console.log(`SplitDeals API listening on http://0.0.0.0:${port}`);
    console.log(`Database: ${process.env.TURSO_URL || "file:local.db"}`);
  });
}

module.exports = app;
