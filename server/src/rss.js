import { createHash } from "node:crypto";

const CATEGORY_IDS = new Set(["ports", "roms", "apps", "dicas", "lancamentos"]);

const ENTITIES = {
  amp: "&",
  lt: "<",
  gt: ">",
  quot: '"',
  apos: "'",
  nbsp: " ",
};

export function decodeEntities(text = "") {
  return text
    .replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1")
    .replace(/&([a-z]+);/gi, (_, name) => ENTITIES[name.toLowerCase()] ?? `&${name};`)
    .replace(/&#(\d+);/g, (_, code) => String.fromCodePoint(Number(code)))
    .replace(/&#x([0-9a-f]+);/gi, (_, code) => String.fromCodePoint(parseInt(code, 16)));
}

export function plainText(html = "") {
  return decodeEntities(html)
    .replace(/<script[\s\S]*?<\/script>/gi, " ")
    .replace(/<style[\s\S]*?<\/style>/gi, " ")
    .replace(/<[^>]+>/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function pick(block, names) {
  for (const name of names) {
    const re = new RegExp(`<${name}(?:\\s[^>]*)?>([\\s\\S]*?)<\\/${name}>`, "i");
    const match = block.match(re);
    if (match) return match[1];
  }
  return "";
}

function pickAttr(block, tag, attr) {
  const re = new RegExp(`<${tag}[^>]*\\s${attr}=["']([^"']+)["'][^>]*>`, "i");
  return block.match(re)?.[1] ?? "";
}

function extractBlocks(xml) {
  const items = [...xml.matchAll(/<item(?:\s[^>]*)?>([\s\S]*?)<\/item>/gi)].map((m) => m[1]);
  if (items.length) return items;
  return [...xml.matchAll(/<entry(?:\s[^>]*)?>([\s\S]*?)<\/entry>/gi)].map((m) => m[1]);
}

function guessCategory(source, text) {
  const value = text.toLowerCase();
  if (/\bport(s)?\b|portabilidade|gsi/.test(value)) return "ports";
  if (/\brom(s)?\b|lineage|magisk|root|bootloader|recovery|custom rom/.test(value)) return "roms";
  if (/\bapp(s)?\b|aplicativo|play store|launcher|widget/.test(value)) return "apps";
  if (/dica|tutorial|como |guia|how to|review|análise|analise/.test(value)) return "dicas";
  return CATEGORY_IDS.has(source.defaultCategory) ? source.defaultCategory : "lancamentos";
}

function normalizeDate(value) {
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : new Date().toISOString();
}

function truncate(text, max) {
  if (text.length <= max) return text;
  return `${text.slice(0, max).replace(/\s+\S*$/, "")}…`;
}

function imageFrom(block, description) {
  const media =
    pickAttr(block, "media:content", "url") ||
    pickAttr(block, "media:thumbnail", "url") ||
    pickAttr(block, "enclosure", "url");
  if (media && /^https?:\/\//i.test(media)) return media;
  const img = description.match(/<img[^>]+src=["']([^"']+)["']/i)?.[1];
  return img && /^https?:\/\//i.test(img) ? decodeEntities(img) : null;
}

export function parseFeed(xml, source, now = Date.now()) {
  const blocks = extractBlocks(xml);
  return blocks
    .map((block) => {
      const title = plainText(pick(block, ["title"]));
      if (!title) return null;

      const rawLink = plainText(pick(block, ["link"])) || pickAttr(block, "link", "href");
      const link = rawLink.startsWith("http") ? rawLink : "";
      if (!link) return null;

      const rawDescription = pick(block, ["content:encoded", "description", "summary", "content"]);
      const body = plainText(rawDescription);
      const summary = truncate(body || title, 190);
      const category = guessCategory(source, `${title} ${body}`);
      const dateValue = plainText(pick(block, ["pubDate", "published", "updated", "dc:date"]));
      const hash = createHash("sha1").update(link).digest("hex").slice(0, 16);
      const likes = 80 + (parseInt(hash.slice(0, 6), 16) % 1900);

      return {
        id: `rss-${hash}`,
        title: truncate(title, 130),
        summary,
        content: `${truncate(body || title, 700)}\n\nLeia a matéria completa em ${source.name}.`,
        imageUrl: imageFrom(block, rawDescription),
        category,
        sourceName: source.name,
        sourceUrl: link,
        publishedAt: normalizeDate(dateValue || new Date(now).toISOString()),
        likes,
      };
    })
    .filter(Boolean);
}
