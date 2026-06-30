import type { VercelRequest, VercelResponse } from "@vercel/node";

const DATABASE_URL = process.env.DATABASE_URL;

async function query(sql: string, params: unknown[] = []): Promise<{ rows: Record<string, unknown>[] }> {
  const url = DATABASE_URL!.replace("postgresql://", "https://").replace(/\/[^?]+/, "/sql");
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Neon-Connection-String": DATABASE_URL! },
    body: JSON.stringify({ query: sql, params }),
  });
  if (!response.ok) throw new Error(`DB error: ${response.status}`);
  return response.json() as Promise<{ rows: Record<string, unknown>[] }>;
}

let tableReady = false;

async function ensureTable() {
  await query(`
    CREATE TABLE IF NOT EXISTS visits (
      id SERIAL PRIMARY KEY,
      anonymous_id TEXT NOT NULL,
      place_id TEXT NOT NULL,
      place_name TEXT,
      lat DOUBLE PRECISION,
      lng DOUBLE PRECISION,
      corridor_key TEXT,
      visited_at TIMESTAMPTZ DEFAULT NOW(),
      UNIQUE(anonymous_id, place_id)
    )
  `);
  await query(`
    CREATE INDEX IF NOT EXISTS idx_visits_user_corridor
    ON visits(anonymous_id, corridor_key)
  `);
  tableReady = true;
}

function makeCorridorKey(oLat?: number, oLng?: number, dLat?: number, dLng?: number): string | null {
  if (!oLat || !oLng || !dLat || !dLng) return null;
  return `${oLat.toFixed(2)},${oLng.toFixed(2)}|${dLat.toFixed(2)},${dLng.toFixed(2)}`;
}

export default async function handler(req: VercelRequest, res: VercelResponse) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, X-Anonymous-Id");

  if (req.method === "OPTIONS") return res.status(200).end();

  if (!DATABASE_URL) {
    return res.status(500).json({ error: "Database not configured" });
  }

  if (!tableReady) await ensureTable();

  const anonymousId = req.headers["x-anonymous-id"] as string;
  if (!anonymousId) {
    return res.status(400).json({ error: "X-Anonymous-Id header required" });
  }

  if (req.method === "POST") {
    const { placeId, placeName, lat, lng, originLat, originLng, destLat, destLng } = req.body || {};
    if (!placeId) return res.status(400).json({ error: "placeId required" });

    const corridorKey = makeCorridorKey(originLat, originLng, destLat, destLng);

    await query(
      `INSERT INTO visits (anonymous_id, place_id, place_name, lat, lng, corridor_key)
       VALUES ($1, $2, $3, $4, $5, $6)
       ON CONFLICT (anonymous_id, place_id) DO UPDATE SET visited_at = NOW()`,
      [anonymousId, placeId, placeName || null, lat || null, lng || null, corridorKey]
    );

    return res.status(200).json({ ok: true });
  }

  if (req.method === "GET") {
    const placeIds = req.query.placeIds as string;
    if (!placeIds) return res.status(400).json({ error: "placeIds query param required" });

    const ids = placeIds.split(",");
    const placeholders = ids.map((_, i) => `$${i + 2}`).join(",");
    const result = await query(
      `SELECT place_id, visited_at FROM visits WHERE anonymous_id = $1 AND place_id IN (${placeholders})`,
      [anonymousId, ...ids]
    );

    const visitMap: Record<string, string> = {};
    for (const row of result.rows) {
      visitMap[row.place_id as string] = row.visited_at as string;
    }
    return res.status(200).json({ visits: visitMap });
  }

  return res.status(405).json({ error: "Method not allowed" });
}
