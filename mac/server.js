import { createServer } from "node:http";
import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { networkInterfaces } from "node:os";
import { randomBytes, timingSafeEqual } from "node:crypto";
import { spawn } from "node:child_process";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const __dirname = dirname(fileURLToPath(import.meta.url));
const envFile = readEnvFile(join(__dirname, "bridge.env"));

const port = Number(config("PBI_PORT", "PORT", "8765"));
const host = config("PBI_HOST", "HOST", "0.0.0.0");
const maxBodyBytes = Number(config("PBI_MAX_BODY_BYTES", "MAX_BODY_BYTES", "65536"));
const tokenPath = join(__dirname, ".token");
const relayCursorPath = join(__dirname, ".relay-cursor");

const token = getToken();
let lastText = "";
let relayState = "disabled";

function config(primary, fallback, defaultValue = "") {
  return process.env[primary] || envFile[primary] || process.env[fallback] || envFile[fallback] || defaultValue;
}

function readEnvFile(path) {
  if (!existsSync(path)) return {};
  const result = {};
  for (const rawLine of readFileSync(path, "utf8").split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const index = line.indexOf("=");
    if (index === -1) continue;
    const key = line.slice(0, index).trim();
    let value = line.slice(index + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    result[key] = value;
  }
  return result;
}

function getToken() {
  const configured = config("PBI_TOKEN", "PARSEC_BUBBLE_TOKEN");
  if (configured) return configured;
  if (existsSync(tokenPath)) return readFileSync(tokenPath, "utf8").trim();

  const generated = randomBytes(18).toString("base64url");
  writeFileSync(tokenPath, `${generated}\n`, { mode: 0o600 });
  return generated;
}

function relayUrl() {
  const value = config("PBI_RELAY_URL", "RELAY_URL");
  return value.replace(/\/+$/, "");
}

function readCursor() {
  if (!existsSync(relayCursorPath)) return null;
  const value = Number(readFileSync(relayCursorPath, "utf8").trim());
  return Number.isFinite(value) && value >= 0 ? value : null;
}

function writeCursor(value) {
  writeFileSync(relayCursorPath, `${value}\n`, { mode: 0o600 });
}

function localIps() {
  return Object.values(networkInterfaces())
    .flat()
    .filter((item) => item && item.family === "IPv4" && !item.internal)
    .map((item) => item.address);
}

function sendJson(res, status, body) {
  const data = JSON.stringify(body);
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "access-control-allow-origin": "*",
    "access-control-allow-headers": "content-type,x-parsec-bubble-token",
    "access-control-allow-methods": "GET,POST,OPTIONS",
    "content-length": Buffer.byteLength(data)
  });
  res.end(data);
}

async function relayJson(path, options = {}) {
  const base = relayUrl();
  if (!base) throw new Error("relay_disabled");

  const response = await fetch(`${base}${path}`, {
    ...options,
    headers: {
      "x-parsec-bubble-token": token,
      ...(options.headers || {})
    },
    signal: AbortSignal.timeout(options.timeout || 35000)
  });
  const body = await response.json();
  if (!response.ok || !body.ok) {
    throw new Error(body.error || `relay_${response.status}`);
  }
  return body;
}

async function initialRelayCursor() {
  const health = await relayJson("/api/health", { timeout: 5000 });
  const latest = Number(health.id || 0);
  const stored = readCursor();

  if (process.env.PBI_REPLAY_ON_START === "1" || process.env.RELAY_REPLAY_ON_START === "1") {
    return 0;
  }

  if (stored !== null && stored <= latest) return stored;
  writeCursor(latest);
  return latest;
}

async function startRelayClient() {
  if (!relayUrl()) return;

  let cursor = 0;
  try {
    cursor = await initialRelayCursor();
  } catch (error) {
    relayState = `initializing: ${error.message}`;
  }

  console.log(`Relay: ${relayUrl()}`);
  for (;;) {
    try {
      relayState = "connected";
      const body = await relayJson(`/api/poll?after=${encodeURIComponent(cursor)}`);
      if (body.empty) {
        cursor = Number(body.id || cursor);
        writeCursor(cursor);
        continue;
      }

      const id = Number(body.id);
      const text = String(body.text || "");
      if (id > cursor && text.length > 0) {
        await pasteText(text);
        lastText = text;
        cursor = id;
        writeCursor(cursor);
        console.log(`Relay pasted ${text.length} chars.`);
      }
    } catch (error) {
      relayState = `retrying: ${error.message}`;
      console.error(`Relay error: ${error.message}`);
      await new Promise((resolve) => setTimeout(resolve, 2000));
    }
  }
}

function run(command, args, input = "") {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, { stdio: ["pipe", "pipe", "pipe"] });
    let stdout = "";
    let stderr = "";

    child.stdout.on("data", (chunk) => {
      stdout += chunk;
    });
    child.stderr.on("data", (chunk) => {
      stderr += chunk;
    });
    child.on("error", reject);
    child.on("close", (code) => {
      if (code === 0) resolve({ stdout, stderr });
      else reject(new Error(stderr || `${command} exited with ${code}`));
    });

    child.stdin.end(input);
  });
}

async function pasteText(text) {
  await run("pbcopy", [], text);
  await run("osascript", [
    "-e",
    'tell application "System Events" to keystroke "v" using command down'
  ]);
}

async function readBody(req) {
  const chunks = [];
  let total = 0;
  for await (const chunk of req) {
    total += chunk.length;
    if (total > maxBodyBytes) throw new Error("body_too_large");
    chunks.push(chunk);
  }
  return Buffer.concat(chunks).toString("utf8");
}

function constantTimeEqual(a, b) {
  const left = Buffer.from(a || "");
  const right = Buffer.from(b || "");
  if (left.length !== right.length) return false;
  return timingSafeEqual(left, right);
}

function authorized(req) {
  const tokenFromHeader = req.headers["x-parsec-bubble-token"];
  return constantTimeEqual(tokenFromHeader, token);
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);

  try {
    if (req.method === "OPTIONS") {
      sendJson(res, 200, { ok: true });
      return;
    }

    if (url.pathname === "/" || url.pathname === "/api/health") {
      sendJson(res, 200, {
        ok: true,
        name: "Parsec Bubble Input Mac Bridge",
        phone: localIps().map((ip) => `http://${ip}:${port}`),
        relay: relayUrl() || null,
        relayState
      });
      return;
    }

    if (url.pathname === "/api/paste" && req.method === "POST") {
      if (!authorized(req)) {
        sendJson(res, 403, { ok: false, error: "unauthorized" });
        return;
      }

      const body = JSON.parse(await readBody(req) || "{}");
      const text = String(body.text ?? "");
      if (text.length === 0) {
        sendJson(res, 400, { ok: false, error: "empty_text" });
        return;
      }

      lastText = text;
      await pasteText(text);
      sendJson(res, 200, { ok: true, pasted: text.length });
      return;
    }

    if (url.pathname === "/api/last" && req.method === "POST") {
      if (!authorized(req)) {
        sendJson(res, 403, { ok: false, error: "unauthorized" });
        return;
      }

      if (!lastText) {
        sendJson(res, 400, { ok: false, error: "no_last_text" });
        return;
      }

      await pasteText(lastText);
      sendJson(res, 200, { ok: true, pasted: lastText.length });
      return;
    }

    sendJson(res, 404, { ok: false, error: "not_found" });
  } catch (error) {
    sendJson(res, 500, {
      ok: false,
      error: error instanceof Error ? error.message : String(error)
    });
  }
});

server.listen(port, host, () => {
  console.log("Parsec Bubble Input Mac Bridge is running.");
  console.log(`Local: http://localhost:${port}`);
  for (const ip of localIps()) console.log(`Phone/App: http://${ip}:${port}`);
  if (relayUrl()) console.log(`Relay: ${relayUrl()}`);
  else console.log("Relay: disabled");
  startRelayClient().catch((error) => {
    console.error(`Relay stopped: ${error.message}`);
  });
});
