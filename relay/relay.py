#!/usr/bin/env python3
import hmac
import json
import os
import time
from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


TOKEN = os.environ.get("PBI_TOKEN") or os.environ.get("PARSEC_BUBBLE_TOKEN") or ""
PORT = int(os.environ.get("PBI_PORT") or os.environ.get("PORT", "8765"))
MAX_BODY = int(os.environ.get("PBI_MAX_BODY_BYTES") or os.environ.get("MAX_BODY_BYTES", "65536"))
POLL_SECONDS = int(os.environ.get("PBI_POLL_SECONDS") or os.environ.get("POLL_SECONDS", "25"))

condition = None
messages = deque(maxlen=100)
last_id = 0
last_text = ""


def token_ok(value):
    return bool(TOKEN) and hmac.compare_digest(value or "", TOKEN)


def enqueue(text):
    global last_id, last_text
    with condition:
        last_id += 1
        item = {"id": last_id, "text": text, "time": int(time.time())}
        messages.append(item)
        last_text = text
        condition.notify_all()
        return item


def next_after(after):
    for item in messages:
        if item["id"] > after:
            return item
    return None


class Handler(BaseHTTPRequestHandler):
    server_version = "ParsecBubbleRelay/1.0"

    def log_message(self, fmt, *args):
        print("%s - %s" % (self.address_string(), fmt % args), flush=True)

    def send_json(self, status, body):
        data = json.dumps(body, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        self.send_response(status)
        self.send_header("content-type", "application/json; charset=utf-8")
        self.send_header("cache-control", "no-store")
        self.send_header("access-control-allow-origin", "*")
        self.send_header("access-control-allow-headers", "content-type,x-parsec-bubble-token")
        self.send_header("access-control-allow-methods", "GET,POST,OPTIONS")
        self.send_header("content-length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def parsed(self):
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)
        return parsed.path, {key: values[-1] for key, values in query.items()}

    def authorized(self, query):
        return token_ok(self.headers.get("x-parsec-bubble-token"))

    def read_json(self):
        size = int(self.headers.get("content-length") or "0")
        if size <= 0:
            return {}
        if size > MAX_BODY:
            raise ValueError("body_too_large")
        raw = self.rfile.read(size).decode("utf-8")
        return json.loads(raw or "{}")

    def do_OPTIONS(self):
        self.send_json(200, {"ok": True})

    def do_GET(self):
        path, query = self.parsed()

        if path in ("/", "/api/health"):
            with condition:
                current_id = last_id
                waiting = len(messages)
            self.send_json(200, {"ok": True, "name": "Parsec Bubble Relay", "id": current_id, "queued": waiting})
            return

        if path == "/api/poll":
            if not self.authorized(query):
                self.send_json(403, {"ok": False, "error": "unauthorized"})
                return

            try:
                after = int(query.get("after") or "0")
            except ValueError:
                after = 0

            deadline = time.time() + POLL_SECONDS
            with condition:
                item = next_after(after)
                while item is None and time.time() < deadline:
                    condition.wait(timeout=max(0.1, deadline - time.time()))
                    item = next_after(after)

                if item is None:
                    self.send_json(200, {"ok": True, "empty": True, "id": last_id})
                else:
                    self.send_json(200, {"ok": True, **item})
            return

        self.send_json(404, {"ok": False, "error": "not_found"})

    def do_POST(self):
        path, query = self.parsed()

        if path not in ("/api/paste", "/api/last"):
            self.send_json(404, {"ok": False, "error": "not_found"})
            return

        if not self.authorized(query):
            self.send_json(403, {"ok": False, "error": "unauthorized"})
            return

        try:
            if path == "/api/last":
                if not last_text:
                    self.send_json(400, {"ok": False, "error": "no_last_text"})
                    return
                item = enqueue(last_text)
            else:
                body = self.read_json()
                text = str(body.get("text") or "")
                if not text:
                    self.send_json(400, {"ok": False, "error": "empty_text"})
                    return
                item = enqueue(text)

            self.send_json(200, {"ok": True, "id": item["id"], "queued": len(messages)})
        except json.JSONDecodeError:
            self.send_json(400, {"ok": False, "error": "bad_json"})
        except ValueError as error:
            self.send_json(400, {"ok": False, "error": str(error)})
        except Exception as error:
            self.send_json(500, {"ok": False, "error": str(error)})


def main():
    global condition
    if not TOKEN:
        raise SystemExit("PBI_TOKEN or PARSEC_BUBBLE_TOKEN is required")
    import threading

    condition = threading.Condition()
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"Parsec Bubble Relay listening on 0.0.0.0:{PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
