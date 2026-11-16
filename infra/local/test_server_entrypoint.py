"""Checks the local non-root truststore lifecycle without Docker or real secrets."""
import json
import os
from pathlib import Path
import stat
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
ENTRYPOINT = Path(os.environ.get("ENTRYPOINT_UNDER_TEST", ROOT / "server/local-entrypoint.sh"))


class LocalEntrypointTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.java_home = self.directory / "jdk"
        self.cacerts = self.java_home / "lib/security/cacerts"
        self.cacerts.parent.mkdir(parents=True)
        self.cacerts.write_bytes(b"original-public-truststore")
        self.cacerts.chmod(0o444)
        self.log = self.directory / "commands.jsonl"
        tools = self.directory / "bin"
        tools.mkdir()
        stub = f"#!{sys.executable}\n" + '''import json
import os
from pathlib import Path
import sys

tool = Path(sys.argv[0]).name
args = sys.argv[1:]
with open(os.environ["TEST_COMMAND_LOG"], "a") as log:
    log.write(json.dumps({"tool": tool, "args": args}) + "\\n")
if tool == "keytool":
    if "-delete" in args:
        sys.exit(1)  # A fresh JDK truststore has no local CA alias yet.
    if os.environ.get("TEST_IMPORT_FAIL") == "1":
        sys.exit(72)
    if "-keystore" in args:
        store = Path(args[args.index("-keystore") + 1])
        store.write_bytes(store.read_bytes() + b"-local-ca")
'''
        for name in ("keytool", "java"):
            executable = tools / name
            executable.write_text(stub)
            executable.chmod(0o755)
        self.environment = dict(os.environ, JAVA_HOME=str(self.java_home), TMPDIR=str(self.directory),
                                TEST_COMMAND_LOG=str(self.log), PATH=f"{tools}{os.pathsep}{os.environ['PATH']}")
        self.environment.pop("TEST_IMPORT_FAIL", None)

    def run_entrypoint(self, **overrides):
        return subprocess.run(["sh", str(ENTRYPOINT)], env=dict(self.environment, **overrides),
                              capture_output=True, text=True)

    def events(self):
        return [json.loads(line) for line in self.log.read_text().splitlines()]

    def test_imports_into_private_copy_and_preserves_jdk_truststore(self):
        result = self.run_entrypoint()
        self.assertEqual(result.returncode, 0, result.stderr)
        events = self.events()
        imports = [event for event in events if event["tool"] == "keytool" and "-importcert" in event["args"]]
        self.assertEqual(len(imports), 1)
        args = imports[0]["args"]
        self.assertNotIn("-cacerts", args)
        store = Path(args[args.index("-keystore") + 1])
        self.assertTrue(store.is_relative_to(self.directory))
        self.assertEqual(stat.S_IMODE(store.stat().st_mode), 0o600)
        self.assertEqual(stat.S_IMODE(store.parent.stat().st_mode), 0o700)
        self.assertEqual(store.read_bytes(), b"original-public-truststore-local-ca")
        self.assertEqual(self.cacerts.read_bytes(), b"original-public-truststore")
        self.assertEqual(args[args.index("-file") + 1], "/run/secrets/ca.crt")
        java = [event for event in events if event["tool"] == "java"]
        self.assertEqual(len(java), 1)
        self.assertIn(f"-Djavax.net.ssl.trustStore={store}", java[0]["args"])
        self.assertIn("-Djavax.net.ssl.trustStorePassword=changeit", java[0]["args"])
        self.assertEqual(java[0]["args"][-2:], ["-jar", "/app/app.jar"])
        self.assertEqual(result.stdout, "")

    def test_restart_uses_fresh_copy_without_mutating_original(self):
        for _ in range(2):
            result = self.run_entrypoint()
            self.assertEqual(result.returncode, 0, result.stderr)
        starts = [event["args"] for event in self.events() if event["tool"] == "java"]
        stores = [next(arg for arg in args if arg.startswith("-Djavax.net.ssl.trustStore=")) for args in starts]
        self.assertEqual(len(set(stores)), 2)
        self.assertEqual(self.cacerts.read_bytes(), b"original-public-truststore")

    def test_import_failure_stops_before_starting_application(self):
        result = self.run_entrypoint(TEST_IMPORT_FAIL="1")
        self.assertEqual(result.returncode, 72)
        self.assertFalse(any(event["tool"] == "java" for event in self.events()))
        self.assertEqual(self.cacerts.read_bytes(), b"original-public-truststore")
        self.assertEqual(result.stdout, "")


if __name__ == "__main__":
    unittest.main()
