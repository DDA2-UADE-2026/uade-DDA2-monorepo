"""Smoke test against the running Compose stack. No third-party Python packages."""
import http.cookiejar
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import HTTPCookieProcessor, Request, build_opener

opener = build_opener(HTTPCookieProcessor(http.cookiejar.CookieJar()))


def api(path, body=None):
    headers = {"Origin": "http://localhost:5173"}
    if body is not None:
        csrf = api("/auth/session/csrf")
        headers.update({csrf["headerName"]: csrf["token"], "Content-Type": "application/json"})
    request = Request("http://server:8080" + path,
                      data=json.dumps(body).encode() if body is not None else None, headers=headers)
    with opener.open(request, timeout=15) as result:
        assert result.headers.get("Access-Control-Allow-Origin") == "http://localhost:5173", "CORS local incorrecto"
        return json.load(result)


with opener.open("http://client/", timeout=10) as page:
    assert page.status == 200 and b'<div id="root">' in page.read(), "Frontend no disponible"
assert api("/actuator/health")["status"] == "UP"
password = Path("/run/secrets/BOOTSTRAP_ADMIN_PASSWORD").read_text().strip()
login = api("/auth/session/login", {"username": "admin", "password": password})
assert login["requiresRoleSelection"], "Falta selección de rol"
selected = api("/auth/session/select-role", {"role": "ADMIN"})
assert selected["user"]["activeRole"] == "ADMIN"
assert api("/auth/me")["user"]["activeRole"] == "ADMIN"
status = api("/api/admin/integrations/core/status")
assert status["http"]["state"] == "UP", "Core HTTPS no disponible"
assert status["amqp"]["state"] == "UP", "RabbitMQ TLS no disponible"
dashboard = api("/api/admin/integrations/core/dashboard?windowHours=24")
assert dashboard["core"]["module"] == "desarrollo-social"
assert dashboard["core"]["windowHours"] == 24
assert api("/api/admin/integrations/core/publications") == []
assert api("/api/admin/integrations/core/subscriptions") == []
assert api("/api/admin/integrations/core/events")["items"] == []
assert api("/api/admin/integrations/core/metrics")["pending"] == 0
try:
    api("/api/admin/integrations/core/events/00000000-0000-0000-0000-000000000000")
    raise AssertionError("El mock debe informar evento inexistente")
except HTTPError as error:
    assert error.code == 404
print("G1 smoke OK: frontend, health, login con cookies/CSRF, rol ADMIN, Core HTTPS, RabbitMQ TLS y dashboard.")
