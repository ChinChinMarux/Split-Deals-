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

function normalizeMode(mode) {
  if (mode === "EVEN" || mode === "EQUAL") return "EVEN";
  return "BY_ITEM";
}

function normalizeKind(kind) {
  if (kind === "PERCENT") return "PERCENT";
  if (kind === "FIXED" || kind === "NOMINAL") return "FIXED";
  return "NONE";
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
          created_at INTEGER NOT NULL DEFAULT 0,
          updated_at INTEGER NOT NULL DEFAULT 0
        )
      `);

      await db.execute(`
        CREATE TABLE IF NOT EXISTS bill_members (
          id TEXT PRIMARY KEY,
          bill_id TEXT NOT NULL,
          name TEXT NOT NULL DEFAULT '',
          sort_order INTEGER NOT NULL DEFAULT 0,
          FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
        )
      `);

      await db.execute(`
        CREATE TABLE IF NOT EXISTS bill_items (
          id TEXT PRIMARY KEY,
          bill_id TEXT NOT NULL,
          name TEXT NOT NULL DEFAULT '',
          price_cents INTEGER NOT NULL DEFAULT 0,
          sort_order INTEGER NOT NULL DEFAULT 0,
          FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
        )
      `);

      await db.execute(`
        CREATE TABLE IF NOT EXISTS bill_shares (
          item_id TEXT NOT NULL,
          member_id TEXT NOT NULL,
          PRIMARY KEY (item_id, member_id),
          FOREIGN KEY (item_id) REFERENCES bill_items(id) ON DELETE CASCADE,
          FOREIGN KEY (member_id) REFERENCES bill_members(id) ON DELETE CASCADE
        )
      `);

      await db.execute(`
        CREATE TABLE IF NOT EXISTS payments (
          person_id TEXT PRIMARY KEY,
          paid INTEGER NOT NULL DEFAULT 0,
          FOREIGN KEY (person_id) REFERENCES bill_members(id) ON DELETE CASCADE
        )
      `);

      const billInfo = await db.execute("PRAGMA table_info(bills)");
      const billColumns = new Set(billInfo.rows.map((row) => row.name));
      const billMigrations = {
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
      for (const [name, definition] of Object.entries(billMigrations)) {
        if (!billColumns.has(name)) {
          await db.execute(`ALTER TABLE bills ADD COLUMN ${name} ${definition}`);
        }
      }

      const memberInfo = await db.execute("PRAGMA table_info(bill_members)");
      if (!memberInfo.rows.some((row) => row.name === "sort_order")) {
        await db.execute("ALTER TABLE bill_members ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");
      }
      const itemInfo = await db.execute("PRAGMA table_info(bill_items)");
      if (!itemInfo.rows.some((row) => row.name === "sort_order")) {
        await db.execute("ALTER TABLE bill_items ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0");
      }

      if (billColumns.has("payload")) {
        const legacy = await db.execute(
          "SELECT * FROM bills WHERE payload IS NOT NULL AND payload <> '' AND payload <> '{}'",
        );
        for (const row of legacy.rows) {
          try {
            const parsed = JSON.parse(row.payload);
            if (parsed && Array.isArray(parsed.people) && Array.isArray(parsed.items)) {
              await writeBill(db, parsed);
            }
          } catch (e) {
            state.__splitdealsLegacyError = e.message;
          }
        }
        try {
          await db.execute("ALTER TABLE bills DROP COLUMN payload");
        } catch (e) {
          state.__splitdealsDropPayloadError = e.message;
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

async function writeBill(db, bill) {
  const statements = [
    {
      sql: `INSERT INTO bills (
        id, title, status, mode,
        tax_kind, tax_percent_bps, tax_cents, other_costs_cents,
        discount_kind, discount_percent_bps, discount_cents,
        grand_total_cents, created_at, updated_at
      ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
        updated_at = excluded.updated_at`,
      args: [
        bill.id,
        bill.title || "",
        bill.status === "SELESAI" ? "SELESAI" : "BERJALAN",
        normalizeMode(bill.mode),
        normalizeKind(bill.taxKind),
        Number(bill.taxPercentBps || 0),
        Number(bill.taxCents || 0),
        Number(bill.otherCostsCents || 0),
        normalizeKind(bill.discountKind),
        Number(bill.discountPercentBps || 0),
        Number(bill.discountCents || 0),
        Number(bill.grandTotalCents || 0),
        Number(bill.createdAt || Date.now()),
        Date.now(),
      ],
    },
    { sql: "DELETE FROM bill_shares WHERE item_id IN (SELECT id FROM bill_items WHERE bill_id = ?)", args: [bill.id] },
    { sql: "DELETE FROM payments WHERE person_id IN (SELECT id FROM bill_members WHERE bill_id = ?)", args: [bill.id] },
    { sql: "DELETE FROM bill_items WHERE bill_id = ?", args: [bill.id] },
    { sql: "DELETE FROM bill_members WHERE bill_id = ?", args: [bill.id] },
  ];

  (bill.people || []).forEach((person, index) => {
    statements.push({
      sql: "INSERT INTO bill_members (id, bill_id, name, sort_order) VALUES (?, ?, ?, ?)",
      args: [person.id, bill.id, person.name || "", index],
    });
  });

  (bill.items || []).forEach((item, index) => {
    statements.push({
      sql: "INSERT INTO bill_items (id, bill_id, name, price_cents, sort_order) VALUES (?, ?, ?, ?, ?)",
      args: [item.id, bill.id, item.name || "", Number(item.priceCents || 0), index],
    });
    (item.sharerIds || []).forEach((memberId) => {
      statements.push({
        sql: "INSERT OR IGNORE INTO bill_shares (item_id, member_id) VALUES (?, ?)",
        args: [item.id, memberId],
      });
    });
  });

  const payments = Array.isArray(bill.payments)
    ? bill.payments
    : (bill.people || []).map((person) => ({ personId: person.id, paid: false }));
  payments.forEach((payment) => {
    statements.push({
      sql: "INSERT INTO payments (person_id, paid) VALUES (?, ?) ON CONFLICT(person_id) DO UPDATE SET paid = excluded.paid",
      args: [payment.personId, payment.paid ? 1 : 0],
    });
  });

  await db.batch(statements);
}

async function assembleBill(db, row) {
  const memberRs = await db.execute({
    sql: "SELECT id, name FROM bill_members WHERE bill_id = ? ORDER BY sort_order",
    args: [row.id],
  });
  const people = memberRs.rows.map((r) => ({ id: String(r.id), name: String(r.name || "") }));

  const itemRs = await db.execute({
    sql: "SELECT id, name, price_cents FROM bill_items WHERE bill_id = ? ORDER BY sort_order",
    args: [row.id],
  });

  const shareRs = await db.execute({
    sql: `SELECT s.item_id, s.member_id
          FROM bill_shares s
          JOIN bill_items i ON i.id = s.item_id
          WHERE i.bill_id = ?`,
    args: [row.id],
  });
  const sharesByItem = new Map();
  shareRs.rows.forEach((r) => {
    const itemId = String(r.item_id);
    if (!sharesByItem.has(itemId)) sharesByItem.set(itemId, []);
    sharesByItem.get(itemId).push(String(r.member_id));
  });

  const items = itemRs.rows.map((r) => ({
    id: String(r.id),
    name: String(r.name || ""),
    priceCents: Number(r.price_cents || 0),
    sharerIds: sharesByItem.get(String(r.id)) || [],
  }));

  const paymentRs = await db.execute({
    sql: `SELECT p.person_id, p.paid
          FROM payments p
          JOIN bill_members m ON m.id = p.person_id
          WHERE m.bill_id = ?`,
    args: [row.id],
  });
  const payments = paymentRs.rows.map((r) => ({
    personId: String(r.person_id),
    paid: Number(r.paid || 0) !== 0,
  }));

  return {
    id: String(row.id),
    title: String(row.title || ""),
    people,
    items,
    mode: normalizeMode(row.mode),
    taxKind: normalizeKind(row.tax_kind),
    taxPercentBps: Number(row.tax_percent_bps || 0),
    taxCents: Number(row.tax_cents || 0),
    otherCostsCents: Number(row.other_costs_cents || 0),
    discountKind: normalizeKind(row.discount_kind),
    discountPercentBps: Number(row.discount_percent_bps || 0),
    discountCents: Number(row.discount_cents || 0),
    status: String(row.status) === "SELESAI" ? "SELESAI" : "BERJALAN",
    payments,
    createdAt: Number(row.created_at || 0),
  };
}

async function loadBill(db, id) {
  const rs = await db.execute({ sql: "SELECT * FROM bills WHERE id = ?", args: [id] });
  if (rs.rows.length === 0) return null;
  return assembleBill(db, rs.rows[0]);
}

function parseBill(req, res) {
  const bill = req.body;
  if (!bill || !bill.id || !Array.isArray(bill.people) || !Array.isArray(bill.items)) {
    res.status(400).json({ error: "Bill must contain id, people[], items[]" });
    return null;
  }
  return bill;
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
    const rs = await db.execute("SELECT * FROM bills ORDER BY created_at DESC");
    const bills = [];
    for (const row of rs.rows) bills.push(await assembleBill(db, row));
    res.json(bills);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

app.get("/bills/:id", async (req, res) => {
  try {
    const db = await readyDb();
    const bill = await loadBill(db, req.params.id);
    if (!bill) {
      res.status(404).json({ error: "Bill not found" });
      return;
    }
    res.json(bill);
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

async function upsert(bill, res) {
  try {
    const db = await readyDb();
    await writeBill(db, bill);
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
    const bill = await loadBill(db, req.params.id);
    if (!bill) {
      res.status(404).json({ error: "Bill not found" });
      return;
    }
    await upsert({ ...bill, status }, res);
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
    const bill = await loadBill(db, req.params.id);
    if (!bill) {
      res.status(404).json({ error: "Bill not found" });
      return;
    }
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
    await db.batch([
      { sql: "DELETE FROM bill_shares WHERE item_id IN (SELECT id FROM bill_items WHERE bill_id = ?)", args: [req.params.id] },
      { sql: "DELETE FROM payments WHERE person_id IN (SELECT id FROM bill_members WHERE bill_id = ?)", args: [req.params.id] },
      { sql: "DELETE FROM bill_items WHERE bill_id = ?", args: [req.params.id] },
      { sql: "DELETE FROM bill_members WHERE bill_id = ?", args: [req.params.id] },
      { sql: "DELETE FROM bills WHERE id = ?", args: [req.params.id] },
    ]);
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
