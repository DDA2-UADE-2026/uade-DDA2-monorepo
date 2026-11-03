"""Local read-only stand-in for the Core routes used by the integration dashboard.

Contract: server/docs/roadmaps/00-contrato-core-verificado.md and CoreDashboardDtos.kt.
It does not simulate Core's event worker, retries, DLQ or other municipal modules.
"""
import hashlib
import hmac
import json
import ssl
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

MODULE = "desarrollo-social"


def response(method, target, headers, body, secret):
    url = urlsplit(target)
    token = hashlib.sha256(secret.encode()).hexdigest()
    if method == "GET" and url.path == "/health/ready":
        return 200, {"status": "up", "mock": True}
    if method == "POST" and url.path == "/api/v1/auth/module-token":
        if not isinstance(body, dict) or body.get("module") != MODULE or not isinstance(body.get("secret"), str) or not hmac.compare_digest(body["secret"], secret):
            return 401, {"code": "INVALID_CREDENTIALS", "message": "Credenciales inválidas", "details": None, "traceId": "local-mock"}
        return 200, {"accessToken": token, "tokenType": "bearer", "expiresIn": 3600,
                     "module": MODULE, "displayName": "Desarrollo Social (mock local)",
                     "isAdmin": False, "kind": "module", "actor": None}
    if not hmac.compare_digest(headers.get("Authorization", ""), f"Bearer {token}"):
        return 401, {"code": "UNAUTHORIZED", "message": "Token requerido", "details": None, "traceId": "local-mock"}
    if method == "GET" and url.path == "/api/v1/dashboard":
        try:
            hours = int(parse_qs(url.query).get("windowHours", ["24"])[0])
            if not 1 <= hours <= 720:
                raise ValueError
        except ValueError:
            return 400, {"code": "INVALID_WINDOW", "message": "Ventana inválida", "details": None, "traceId": "local-mock"}
        return 200, {"module": MODULE, "windowHours": hours, "generatedAt": datetime.now(timezone.utc).isoformat(),
                     "published": {"total": 0, "inWindow": 0, "topTypes": []},
                     "received": {"byStatus": {}, "total": 0, "delivered": 0, "pendingRetry": 0, "dead": 0},
                     "deadLetters": {"open": 0, "byReason": {}},
                     "subscriptions": {"total": 0, "active": 0, "eventTypes": []},
                     "publications": {"declared": []}, "volumeByHour": [], "processing": {"avgMs": 0.0, "maxMs": 0.0}}
    if method == "GET" and url.path in ("/api/v1/publications", "/api/v1/subscriptions"):
        return 200, []
    if method == "GET" and url.path == "/api/v1/events":
        query = parse_qs(url.query)
        try:
            page, size = int(query.get("page", ["1"])[0]), int(query.get("size", ["25"])[0])
            if page < 1 or not 1 <= size <= 200:
                raise ValueError
        except ValueError:
            return 400, {"code": "INVALID_PAGE", "message": "Página inválida", "details": None, "traceId": "local-mock"}
        return 200, {"items": [], "total": 0, "page": page, "size": size, "pages": 0}
    return 404, {"code": "NOT_FOUND", "message": "Recurso no encontrado en el mock local", "details": None, "traceId": "local-mock"}


class Handler(BaseHTTPRequestHandler):
    def handle_request(self):
        try:
            length = int(self.headers.get("Content-Length", "0"))
            if not 0 <= length <= 8192:
                raise ValueError
            body = json.loads(self.rfile.read(length)) if length else None
            status, payload = response(self.command, self.path, self.headers, body, self.server.secret)
        except (ValueError, json.JSONDecodeError):
            status, payload = 400, {"code": "INVALID_JSON", "message": "JSON inválido", "details": None, "traceId": "local-mock"}
        data = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    do_GET = handle_request
    do_POST = handle_request

    def log_message(self, *_args):
        pass  # Never log authentication bodies or headers.


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", 8443), Handler)
    server.secret = Path("/run/secrets/machine-secret").read_text().strip()
    context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain("/run/secrets/tls.crt", "/run/secrets/tls.key")
    server.socket = context.wrap_socket(server.socket, server_side=True)
    server.serve_forever()
