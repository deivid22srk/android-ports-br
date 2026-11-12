import { createHash } from "node:crypto";
import { gzipSync } from "node:zlib";
import Fastify from "fastify";
import {
  categoriesWithCounts,
  getNewsById,
  getState,
  initStore,
  pageNews,
  refreshSources,
  searchNews,
  startRefreshLoop,
} from "./store.js";

const PORT = Number(process.env.PORT ?? 3000);
const HOST = process.env.HOST ?? "0.0.0.0";

function sendJson(request, reply, payload, { maxAge = 60, noStore = false } = {}) {
  const body = Buffer.from(JSON.stringify(payload));
  const etag = `W/"${createHash("sha1").update(body).digest("base64url")}"`;
  const state = getState();

  reply.header("ETag", etag);
  reply.header("Cache-Control", noStore ? "no-store" : `public, max-age=${maxAge}, stale-while-revalidate=300`);
  reply.header("Access-Control-Allow-Origin", "*");
  reply.header("X-Ports-Mode", state.mode);
  if (request.headers["if-none-match"] === etag) {
    return reply.code(304).send();
  }
  if ((request.headers["accept-encoding"] ?? "").includes("gzip")) {
    reply.header("Content-Encoding", "gzip");
    reply.header("Vary", "Accept-Encoding");
    return reply.type("application/json; charset=utf-8").send(gzipSync(body, { level: 6 }));
  }
  return reply.type("application/json; charset=utf-8").send(body);
}

export function buildServer() {
  const app = Fastify({
    logger: false,
    trustProxy: true,
  });

  app.addHook("onSend", async (request, reply) => {
    if (!reply.getHeader("Access-Control-Allow-Origin")) {
      reply.header("Access-Control-Allow-Origin", "*");
    }
  });

  const health = (request, reply) => {
    const state = getState();
    return sendJson(
      request,
      reply,
      {
        status: "ok",
        time: new Date().toISOString(),
        uptimeSeconds: Math.round((Date.now() - state.startedAt) / 1000),
        newsCount: state.items.length,
        mode: state.mode,
        sources: state.sources,
        lastRefresh: state.updatedAt,
        lastError: state.lastError,
      },
      { noStore: true },
    );
  };

  app.get("/health", health);
  app.get("/api/v1/health", health);

  app.get("/api/v1/news", (request, reply) => {
    const { category, cursor } = request.query;
    const limit = Math.min(40, Math.max(1, Number.parseInt(request.query.limit ?? "12", 10) || 12));
    const state = getState();
    const page = pageNews(category, cursor, limit);
    return sendJson(request, reply, {
      ...page,
      updatedAt: state.updatedAt,
      mode: state.mode,
      offline: state.mode !== "live",
    });
  });

  app.get("/api/v1/news/:id", (request, reply) => {
    const item = getNewsById(request.params.id);
    if (!item) {
      return reply.code(404).send({ error: "not_found", message: "Notícia não encontrada" });
    }
    return sendJson(request, reply, item, { maxAge: 300 });
  });

  app.get("/api/v1/categories", (request, reply) => {
    return sendJson(request, reply, { categories: categoriesWithCounts() }, { maxAge: 300 });
  });

  app.get("/api/v1/search", (request, reply) => {
    const query = String(request.query.q ?? "").slice(0, 120);
    return sendJson(request, reply, { query, items: searchNews(query) }, { maxAge: 30 });
  });

  app.get("/", (request, reply) => {
    return sendJson(request, reply, {
      name: "Android Ports BR API",
      version: "1.0.0",
      endpoints: [
        "GET /api/v1/health",
        "GET /api/v1/news?category=&cursor=&limit=",
        "GET /api/v1/news/:id",
        "GET /api/v1/categories",
        "GET /api/v1/search?q=",
      ],
    });
  });

  app.setNotFoundHandler((request, reply) => {
    return reply.code(404).send({ error: "not_found", message: "Rota não encontrada" });
  });

  return app;
}

async function start() {
  initStore();
  const app = buildServer();
  try {
    await app.listen({ port: PORT, host: HOST });
    console.log(`[android-ports-br] http://localhost:${PORT}/api/v1/health`);
    startRefreshLoop(15);
  } catch (error) {
    console.error("[android-ports-br] falha ao iniciar:", error);
    process.exit(1);
  }

  const shutdown = async () => {
    await app.close();
    process.exit(0);
  };
  process.on("SIGINT", shutdown);
  process.on("SIGTERM", shutdown);
}

if (process.argv[1] && import.meta.url === new URL(`file://${process.argv[1]}`).href) {
  start();
}

export { refreshSources };
