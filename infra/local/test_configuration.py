"""Validates the actual Compose configuration without a running Docker daemon."""
import os
from pathlib import Path
import secrets
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
VARIABLES = ("DB_PASSWORD", "RABBITMQ_PASSWORD", "JWT_SECRET", "BOOTSTRAP_ADMIN_PASSWORD", "CORE_MACHINE_SECRET")


class ComposeConfigurationTests(unittest.TestCase):
    def config(self, values, create_file=True, shell_values=None):
        with tempfile.TemporaryDirectory() as directory:
            if create_file:
                Path(directory, ".env").write_text("".join(f"{key}='{value}'\n" for key, value in values.items()))
            environment = {key: value for key, value in os.environ.items() if key not in VARIABLES}
            environment.pop("COMPOSE_ENV_FILES", None)
            environment.pop("COMPOSE_DISABLE_ENV_FILE", None)
            environment.update(shell_values or {})
            return subprocess.run(["docker", "compose", "-f", str(ROOT / "docker-compose.yml"),
                                   "--project-directory", directory, "config", "--quiet"],
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


if __name__ == "__main__":
    unittest.main()
