"""Validates the actual Compose configuration without a running Docker daemon."""
import ast
import json
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import unittest
from urllib.parse import urlparse

ROOT = Path(__file__).resolve().parents[2]
VARIABLES = ("DB_PASSWORD", "RABBITMQ_PASSWORD", "JWT_SECRET", "BOOTSTRAP_ADMIN_PASSWORD", "CORE_MACHINE_SECRET")


class ComposeConfigurationTests(unittest.TestCase):
    def config(self, values, create_file=True, shell_values=None, as_json=False):
        with tempfile.TemporaryDirectory() as directory:
            if create_file:
                Path(directory, ".env").write_text("".join(f"{key}='{value}'\n" for key, value in values.items()))
            environment = {key: value for key, value in os.environ.items() if key not in VARIABLES}
            environment.pop("COMPOSE_ENV_FILES", None)
            environment.pop("COMPOSE_DISABLE_ENV_FILE", None)
            environment.update(shell_values or {})
            options = ["--format", "json"] if as_json else ["--quiet"]
            return subprocess.run(["docker", "compose", "-f", str(ROOT / "docker-compose.yml"),
                                   "--project-directory", directory, "config", *options],
                                  env=environment, capture_output=True, text=True)

    def test_each_empty_or_missing_variable_fails_without_disclosing_values(self):
        values = {key: secrets.token_hex(32) for key in VARIABLES}
        for missing in VARIABLES:
            for empty in (True, False):
                with self.subTest(variable=missing, empty=empty):
                    configured = dict(values)
                    if empty:
                        configured[missing] = ""
                    else:
                        del configured[missing]
                    result = self.config(configured)
                    self.assertNotEqual(0, result.returncode)
                    self.assertIn(f"Falta {missing} en .env", result.stderr)
                    self.assertTrue(all(value not in result.stdout + result.stderr for value in values.values()),
                                    "La validación expuso una credencial")

    def test_missing_file_fails_even_with_variables_exported(self):
        result = self.config({}, create_file=False, shell_values={key: secrets.token_hex(32) for key in VARIABLES})
        self.assertNotEqual(0, result.returncode)
        self.assertIn(".env", result.stderr)

    def test_complete_configuration_accepts_literal_dollar_and_hash(self):
        result = self.config({key: secrets.token_hex(32) + "$#" for key in VARIABLES})
        self.assertEqual(0, result.returncode, "La configuración completa debe ser válida")
        self.assertEqual("", result.stdout)
        self.assertEqual("", result.stderr)

    def test_non_root_frontend_port_and_read_only_secrets(self):
        result = self.config({key: secrets.token_hex(32) for key in VARIABLES}, as_json=True)
        self.assertEqual(0, result.returncode, "La configuración debe resolverse sin daemon")
        services = json.loads(result.stdout)["services"]
        client = services["client"]
        self.assertEqual(len(client["ports"]), 1)
        port = client["ports"][0]
        self.assertEqual(port["target"], 8080)
        self.assertEqual(str(port["published"]), "5173")
        self.assertEqual(port["host_ip"], "127.0.0.1")
        self.assertIn("http://127.0.0.1:8080/", client["healthcheck"]["test"][1])
        # Inspect the verifier without importing it: importing would contact the stack.
        verifier = ast.parse((ROOT / "infra/local/verify.py").read_text())
        urls = [urlparse(node.args[0].value) for node in ast.walk(verifier)
                if isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute)
                and node.func.attr == "open" and node.args
                and isinstance(node.args[0], ast.Constant) and isinstance(node.args[0].value, str)]
        frontend_urls = [url for url in urls if url.hostname == "client"]
        self.assertEqual(len(frontend_urls), 1, "El smoke debe verificar el frontend")
        self.assertEqual(frontend_urls[0].port, port["target"], "El smoke debe usar el puerto interno de Nginx")
        for service in ("server", "core-mock"):
            mounts = [mount for mount in services[service]["volumes"] if mount["target"] == "/run/secrets"]
            self.assertEqual(len(mounts), 1)
            self.assertTrue(mounts[0]["read_only"])
        self.assertNotIn("ports", services["local-init"])
        self.assertTrue(all(mount["type"] == "volume" for mount in services["local-init"]["volumes"]))


if __name__ == "__main__":
    unittest.main()
