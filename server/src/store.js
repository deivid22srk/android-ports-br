import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { parseFeed } from "./rss.js";

const HERE = path.dirname(fileURLToPath(import.meta.url));
const ROOT = path.resolve(HERE, "..");
const SEED_FILE = path.join(ROOT, "data", "seed.json");
const CACHE_FILE = path.join(ROOT, "data", "cache.json");
const SOURCES_FILE = path.join(ROOT, "sources.json");

export const CATEGORIES = [
  { id: "ports", name: "Ports" },
  { id: "roms", name: "ROMs" },
  { id: "apps", name: "Apps" },
  { id: "dicas", name: "Dicas" },
  { id: "lancamentos", name: "Lançamentos" },
];

const state = {
  items: [],
  seedItems: [],
  updatedAt: null,
  mode: "seed",
  refreshing: false,
  lastError: null,
  sources: 0,
  startedAt: Date.now(),
};

function readJson(file, fallback = null) {
  try {
    return JSON.parse(fs.readFileSync(file, "utf8"));
  } catch {
    return fallback;
  }
}

function sortNews(items) {
  return [...items].sort((a, b) => Date.parse(b.publishedAt) - Date.parse(a.publishedAt));
}

function dedupe(items) {
  const seen = new Set();
  return items.filter((item) => {
    if (!item?.id || !item?.title) return false;
    const key = item.sourceUrl || item.id;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function persistCache() {
  const payload = {
    updatedAt: state.updatedAt,
    mode: state.mode,
    items: state.items,
  };
  try {
    fs.mkdirSync(path.dirname(CACHE_FILE), { recursive: true });
    const temp = `${CACHE_FILE}.tmp`;
    fs.writeFileSync(temp, `${JSON.stringify(payload)}\n`);
    fs.renameSync(temp, CACHE_FILE);
  } catch (error) {
    console.warn("[cache] não foi possível gravar:", error.message);
  }
}

export function initStore() {
  const seed = readJson(SEED_FILE, { items: [] });
  state.seedItems = sortNews(Array.isArray(seed.items) ? seed.items : []);
  const cache = readJson(CACHE_FILE);
  if (cache && Array.isArray(cache.items) && cache.items.length) {
    state.items = sortNews(dedupe(cache.items));
    state.updatedAt = cache.updatedAt ?? null;
    state.mode = cache.mode === "live" ? "live" : "cache";
  } else {
    state.items = state.seedItems;
    state.mode = "seed";
  }
  const config = readJson(SOURCES_FILE, { sources: [] });
  state.sources = Array.isArray(config.sources) ? config.sources.length : 0;
  console.log(`[store] ${state.items.length} notícias em modo ${state.mode}`);
}

export function getState() {
  return state;
}

export function allNews() {
  return state.items;
}

export function getNewsById(id) {
  return state.items.find((item) => item.id === id) ?? null;
}

export function pageNews(category, cursor, limit) {
  const categoryId = category && category !== "all" ? category : null;
  const filtered = categoryId ? state.items.filter((item) => item.category === categoryId) : state.items;
  let offset = 0;
  if (cursor) {
    const parsed = Number.parseInt(Buffer.from(cursor, "base64url").toString("utf8"), 10);
    if (Number.isFinite(parsed) && parsed >= 0) offset = parsed;
  }
  const items = filtered.slice(offset, offset + limit);
  const next = offset + limit < filtered.length ? Buffer.from(String(offset + limit)).toString("base64url") : null;
  return { items, nextCursor: next, total: filtered.length };
}

export function categoriesWithCounts() {
  return CATEGORIES.map((category) => ({
    ...category,
    count: state.items.filter((item) => item.category === category.id).length,
  }));
}

export function searchNews(query) {
  const q = query.trim().toLowerCase();
  if (!q) return [];
  return state.items
    .filter((item) => `${item.title} ${item.summary} ${item.content}`.toLowerCase().includes(q))
    .slice(0, 40);
}

export async function refreshSources({ force = false } = {}) {
  if (state.refreshing && !force) return { skipped: true };
  state.refreshing = true;
  const config = readJson(SOURCES_FILE, { sources: [] });
  const sources = Array.isArray(config.sources) ? config.sources : [];
  state.sources = sources.length;

  const results = await Promise.allSettled(
    sources.map(async (source) => {
      const response = await fetch(source.feed, {
        headers: {
          "user-agent": "AndroidPortsBR/1.0 (+https://github.com/deivid22srk/android-ports-br)",
          accept: "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.5",
        },
        signal: AbortSignal.timeout(9000),
        redirect: "follow",
      });
      if (!response.ok) throw new Error(`${source.name}: HTTP ${response.status}`);
      const xml = await response.text();
      const items = parseFeed(xml, source).slice(0, 20);
      if (!items.length) throw new Error(`${source.name}: feed sem itens`);
      return items;
    }),
  );

  const fetched = [];
  const errors = [];
  results.forEach((result, index) => {
    if (result.status === "fulfilled") fetched.push(...result.value);
    else errors.push(`${sources[index]?.name ?? "fonte"}: ${result.reason?.message ?? "erro"}`);
  });

  if (fetched.length) {
    // Feeds primeiro; o seed completa o catálogo e garante o modo offline.
    const merged = dedupe([...fetched, ...state.seedItems]);
    state.items = sortNews(merged);
    state.mode = "live";
    state.updatedAt = new Date().toISOString();
    state.lastError = errors.length ? errors.join(" | ") : null;
    persistCache();
    console.log(`[refresh] ${fetched.length} itens de feeds, ${state.items.length} no total`);
  } else {
    state.lastError = errors.join(" | ") || "Nenhuma fonte respondeu";
    console.warn("[refresh] usando fallback:", state.lastError);
  }

  state.refreshing = false;
  return { fetched: fetched.length, errors };
}

export function startRefreshLoop(minutes = 15) {
  const interval = Math.max(1, minutes) * 60_000;
  const timer = setInterval(() => {
    refreshSources().catch((error) => console.warn("[refresh]", error.message));
  }, interval);
  timer.unref?.();
  setTimeout(() => {
    refreshSources().catch((error) => console.warn("[refresh]", error.message));
  }, 1500).unref?.();
  return timer;
}
