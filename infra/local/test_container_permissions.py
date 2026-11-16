"""Exercise local init safely: remap mounts to temporary dirs and record chown.

chmod, OpenSSL and the actual init script run normally; chown is recorded because
host tests must never assign real files to the container UIDs. Container runtime
ownership still needs a Docker smoke test.
"""
import os
from pathlib import Path
import re
import secrets
import shutil
import stat
import subprocess
import tempfile
import unittest

LOCAL = Path(__file__).resolve().parent
CONSUMERS = {
    "server": ("10001:10001", (
        "JWT_SECRET", "BOOTSTRAP_ADMIN_PASSWORD", "DB_PASSWORD",
        "CORE_AMQP_PASSWORD", "CORE_MACHINE_SECRET", "ca.crt",
    )),
    "core": ("10002:10002", ("machine-secret", "tls.key", "tls.crt", "ca.crt")),
}
MOUNT_PATH = re.compile(r"(?<![\w/])(?:/(?:server|core|ca|db|rabbit|tmp)(?=/|[\s\";)]|$)|/definitions\.json)")


class CoreMockImageTests(unittest.TestCase):
    def test_mock_has_explicit_unprivileged_identity(self):
        dockerfile = (LOCAL / "core-mock/Dockerfile").read_text()
        self.assertRegex(dockerfile, r"(?m)^USER 10002:10002$")
        self.assertRegex(dockerfile, r"addgroup\s+-S\s+-g\s+10002\s+core")
        self.assertRegex(dockerfile, r"adduser\s+-S\s+-D\s+-H\s+-u\s+10002\s+-G\s+core\s+core")
        self.assertIn("ENV PYTHONDONTWRITEBYTECODE=1", dockerfile)
        self.assertIn("EXPOSE 8443", dockerfile)
        self.assertNotIn("--chown", dockerfile, "El proceso no debe ser dueño de su código")


@unittest.skipUnless(shutil.which("openssl"), "OpenSSL is required for init tests")
class InitConsumerPermissionsTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="dda2-init-permissions-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        for name in ("server", "core", "ca", "db", "rabbit", "tmp", "bin"):
            (self.root / name).mkdir(mode=0o755)
        # Existing unrelated content must never be included in broad chmod/chown.
        self.unrelated = self.root / "server/unrelated"
        self.unrelated.write_text("leave unchanged")
        self.unrelated.chmod(0o644)
        (self.root / "ca").chmod(0o700)
        (self.root / "definitions.json").write_bytes((LOCAL / "definitions.json").read_bytes())
        self.log = self.root / "chown.log"
        chown = self.root / "bin/chown"
        chown.write_text('#!/bin/sh\nprintf "%s\\n" "$@" >> "$CHOWN_LOG"\nprintf "%s\\n" "--end--" >> "$CHOWN_LOG"\n')
        chown.chmod(0o755)
        source = (LOCAL / "init/init.sh").read_text()
        self.script = self.root / "init.sh"
        self.script.write_text(MOUNT_PATH.sub(lambda match: str(self.root / match[0][1:]), source))
        self.values = {key: secrets.token_hex(32) for key in (
            "DB_PASSWORD", "RABBITMQ_PASSWORD", "JWT_SECRET",
            "BOOTSTRAP_ADMIN_PASSWORD", "CORE_MACHINE_SECRET",
        )}
        self.env = dict(os.environ, **self.values,
                        PATH=str(self.root / "bin") + os.pathsep + os.environ.get("PATH", ""),
                        CHOWN_LOG=str(self.log))

    def run_init(self, **overrides):
        result = subprocess.run(["sh", str(self.script)], cwd=self.root,
                                env=dict(self.env, **overrides), capture_output=True,
                                text=True, timeout=30)
        for value in self.values.values():
            self.assertNotIn(value, result.stdout + result.stderr, "Se expuso una credencial")
        return result

    def private_snapshot(self):
        files = [self.root / directory / name
                 for directory, (_, names) in CONSUMERS.items() for name in names]
        files.extend(path for path in (self.root / "ca").iterdir() if path.is_file())
        return {path: (path.read_bytes(), stat.S_IMODE(path.stat().st_mode)) for path in files}

    def assert_hardened(self):
        for directory, (owner, names) in CONSUMERS.items():
            target = self.root / directory
            self.assertEqual(0o700, stat.S_IMODE(target.stat().st_mode))
            for name in names:
                self.assertEqual(0o600, stat.S_IMODE((target / name).stat().st_mode))
            expected = [owner, str(target), *(str(target / name) for name in names)]
            calls = [call.splitlines() for call in self.log.read_text().split("--end--\n") if call]
            self.assertIn(expected, calls)
        self.assertEqual(0o644, stat.S_IMODE(self.unrelated.stat().st_mode))
        self.assertEqual("leave unchanged", self.unrelated.read_text())
        self.assertEqual(0o700, stat.S_IMODE((self.root / "ca").stat().st_mode))
        calls = [call.splitlines() for call in self.log.read_text().split("--end--\n") if call]
        expected_calls = [[owner, str(self.root / directory),
                           *(str(self.root / directory / name) for name in names)]
                          for directory, (owner, names) in CONSUMERS.items()]
        self.assertEqual(expected_calls, calls[-2:], "Solo se cambia propiedad de rutas explícitas")
        self.assertTrue(all(call in expected_calls for call in calls))

    def test_new_volumes_receive_private_permissions_and_targeted_ownership(self):
        result = self.run_init()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assert_hardened()

    def test_existing_files_are_hardened_without_rotating_credentials_or_ca(self):
        self.assertEqual(0, self.run_init().returncode)
        before = self.private_snapshot()
        for directory, (_, names) in CONSUMERS.items():
            (self.root / directory).chmod(0o755)
            for name in names:
                (self.root / directory / name).chmod(0o644)
        result = self.run_init()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assert_hardened()
        self.assertEqual(before, self.private_snapshot())
        self.assertEqual(0, self.run_init().returncode, "El reinicio debe seguir siendo válido")
        self.assertEqual(before, self.private_snapshot())

    def test_changed_credential_is_rejected_without_rotating_private_files(self):
        self.assertEqual(0, self.run_init().returncode)
        before = self.private_snapshot()
        result = self.run_init(JWT_SECRET=secrets.token_hex(32))
        self.assertNotEqual(0, result.returncode)
        self.assertIn("JWT_SECRET no coincide", result.stderr)
        self.assertEqual(before, self.private_snapshot())


if __name__ == "__main__":
    unittest.main()
