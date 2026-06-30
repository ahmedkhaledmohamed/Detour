import type { VercelRequest, VercelResponse } from "@vercel/node";

interface Corridor {
  slug: string;
  origin: { name: string; lat: number; lng: number };
  destination: { name: string; lat: number; lng: number };
  category: string;
}

const CORRIDORS: Corridor[] = [
  { slug: "toronto-montreal-coffee", origin: { name: "Toronto", lat: 43.6532, lng: -79.3832 }, destination: { name: "Montreal", lat: 45.5017, lng: -73.5673 }, category: "coffee" },
  { slug: "toronto-ottawa-coffee", origin: { name: "Toronto", lat: 43.6532, lng: -79.3832 }, destination: { name: "Ottawa", lat: 45.4215, lng: -75.6972 }, category: "coffee" },
  { slug: "toronto-niagara-food", origin: { name: "Toronto", lat: 43.6532, lng: -79.3832 }, destination: { name: "Niagara Falls", lat: 43.0896, lng: -79.0849 }, category: "restaurant" },
  { slug: "toronto-muskoka-gas", origin: { name: "Toronto", lat: 43.6532, lng: -79.3832 }, destination: { name: "Muskoka", lat: 45.0, lng: -79.3 }, category: "gas station" },
  { slug: "la-sf-coffee", origin: { name: "Los Angeles", lat: 34.0522, lng: -118.2437 }, destination: { name: "San Francisco", lat: 37.7749, lng: -122.4194 }, category: "coffee" },
  { slug: "nyc-boston-food", origin: { name: "New York City", lat: 40.7128, lng: -74.006 }, destination: { name: "Boston", lat: 42.3601, lng: -71.0589 }, category: "restaurant" },
  { slug: "nyc-dc-coffee", origin: { name: "New York City", lat: 40.7128, lng: -74.006 }, destination: { name: "Washington DC", lat: 38.9072, lng: -77.0369 }, category: "coffee" },
  { slug: "chicago-detroit-food", origin: { name: "Chicago", lat: 41.8781, lng: -87.6298 }, destination: { name: "Detroit", lat: 42.3314, lng: -83.0458 }, category: "restaurant" },
  { slug: "sf-la-gas", origin: { name: "San Francisco", lat: 37.7749, lng: -122.4194 }, destination: { name: "Los Angeles", lat: 34.0522, lng: -118.2437 }, category: "gas station" },
  { slug: "miami-orlando-coffee", origin: { name: "Miami", lat: 25.7617, lng: -80.1918 }, destination: { name: "Orlando", lat: 28.5383, lng: -81.3792 }, category: "coffee" },
];

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== "GET") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  const slug = req.query.slug as string;

  if (!slug) {
    const index = CORRIDORS.map((c) => ({
      slug: c.slug,
      title: `Best ${c.category} stops ${c.origin.name} to ${c.destination.name}`,
      url: `/api/corridor?slug=${c.slug}`,
    }));
    res.setHeader("Content-Type", "application/json");
    return res.status(200).json({ corridors: index });
  }

  const corridor = CORRIDORS.find((c) => c.slug === slug);
  if (!corridor) {
    return res.status(404).json({ error: "Corridor not found" });
  }

  const title = `Best ${corridor.category} stops ${corridor.origin.name} to ${corridor.destination.name}`;
  const description = `Find the best ${corridor.category} stops along the ${corridor.origin.name} to ${corridor.destination.name} route, ranked by how little time they add to your trip. Powered by OnRoute.`;

  const mapUrl = `https://maps.googleapis.com/maps/api/staticmap?size=1200x630&scale=2&maptype=roadmap`
    + `&markers=color:green|label:A|${corridor.origin.lat},${corridor.origin.lng}`
    + `&markers=color:red|label:B|${corridor.destination.lat},${corridor.destination.lng}`
    + `&path=color:0x008DA6ff|weight:4|${corridor.origin.lat},${corridor.origin.lng}|${corridor.destination.lat},${corridor.destination.lng}`
    + `&key=${process.env.GOOGLE_MAPS_API_KEY}`;

  const shareUrl = `/api/share?oLat=${corridor.origin.lat}&oLng=${corridor.origin.lng}&dLat=${corridor.destination.lat}&dLng=${corridor.destination.lng}&oName=${encodeURIComponent(corridor.origin.name)}&dName=${encodeURIComponent(corridor.destination.name)}&query=${encodeURIComponent(corridor.category)}`;

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${esc(title)} | OnRoute</title>
  <meta name="description" content="${esc(description)}">
  <meta property="og:title" content="${esc(title)}">
  <meta property="og:description" content="${esc(description)}">
  <meta property="og:image" content="${mapUrl}">
  <meta property="og:type" content="article">
  <meta name="twitter:card" content="summary_large_image">
  <link rel="canonical" href="https://backend-navy-iota.vercel.app/api/corridor?slug=${corridor.slug}">
  <style>
    body { font-family: -apple-system, sans-serif; margin: 0; background: #FAFBFC; color: #1A1A2E; }
    .container { max-width: 700px; margin: 0 auto; padding: 24px; }
    h1 { font-size: 28px; line-height: 1.3; }
    .subtitle { color: #6B7280; font-size: 16px; margin-bottom: 24px; }
    .map { width: 100%; border-radius: 16px; margin-bottom: 24px; }
    .cta { display: inline-block; padding: 14px 32px; background: linear-gradient(135deg, #008DA6, #005A80); color: white; border-radius: 12px; text-decoration: none; font-size: 16px; font-weight: 600; margin-top: 16px; }
    .badge { display: inline-flex; padding: 4px 12px; background: #E0F7FA; color: #008DA6; border-radius: 12px; font-size: 13px; font-weight: 600; margin-bottom: 16px; }
    .how { margin-top: 32px; padding: 24px; background: white; border-radius: 16px; border: 1px solid #E5E7EB; }
    .how h2 { font-size: 20px; margin-bottom: 16px; }
    .how ol { padding-left: 20px; }
    .how li { margin-bottom: 8px; color: #444; }
    footer { margin-top: 40px; text-align: center; font-size: 13px; color: #888; }
    footer a { color: #008DA6; }
  </style>
</head>
<body>
  <div class="container">
    <div class="badge">OnRoute</div>
    <h1>${esc(title)}</h1>
    <p class="subtitle">${esc(description)}</p>
    <img class="map" src="${mapUrl}" alt="Route from ${esc(corridor.origin.name)} to ${esc(corridor.destination.name)}">
    <p>OnRoute ranks every stop by <strong>detour time</strong> — how many minutes it adds to your trip. No more guessing which exit to take or backtracking for a missed turn.</p>
    <a class="cta" href="https://onroute-landing.vercel.app">Try OnRoute Free</a>
    <div class="how">
      <h2>How it works</h2>
      <ol>
        <li>Enter your route: <strong>${esc(corridor.origin.name)}</strong> to <strong>${esc(corridor.destination.name)}</strong></li>
        <li>Pick a category: ${esc(corridor.category)}</li>
        <li>See every option ranked by detour time — "+2 min" beats "+15 min"</li>
        <li>Tap to preview the detour route, then open in Google Maps or Waze</li>
      </ol>
    </div>
    <footer>
      <p><a href="https://onroute-landing.vercel.app">OnRoute</a> · <a href="https://onroute-landing.vercel.app/privacy.html">Privacy</a> · <a href="https://onroute-landing.vercel.app/terms.html">Terms</a></p>
      <p>&copy; 2026 OnRoute. Built in Toronto.</p>
    </footer>
  </div>
</body>
</html>`;

  res.setHeader("Content-Type", "text/html");
  res.setHeader("Cache-Control", "public, s-maxage=86400, stale-while-revalidate=604800");
  return res.status(200).send(html);
}

function esc(s: string): string {
  return s.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
