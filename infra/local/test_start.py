"""Launcher contract tests; no Docker daemon or third-party modules required."""
import errno
import fcntl
import json
import os
from pathlib import Path
import pty
import select
import shutil
import stat
import subprocess
import tempfile
import termios
import time
import unittest

SOURCE = Path(__file__).resolve().parents[2] / 'start.sh'
NAMES = ('DB_PASSWORD', 'RABBITMQ_PASSWORD', 'JWT_SECRET',
         'CORE_MACHINE_SECRET', 'BOOTSTRAP_ADMIN_PASSWORD')
FAKE = r'''#!/usr/bin/env python3
import json, os, pathlib, subprocess, sys
args = sys.argv[1:]
with open('calls.jsonl', 'a') as log:
    log.write(json.dumps({'args': args, 'cwd': os.getcwd(), 'env': {
        key: os.environ[key] for key in ('DB_PASSWORD', 'RABBITMQ_PASSWORD',
        'JWT_SECRET', 'CORE_MACHINE_SECRET', 'BOOTSTRAP_ADMIN_PASSWORD',
        'admin', 'confirmation', 'COMPOSE_ENV_FILES', 'COMPOSE_DISABLE_ENV_FILE') if key in os.environ}}) + '\n')
if args == ['info']:
    sys.exit(int(os.environ.get('FAIL_INFO', '0')))
if args[:2] == ['compose', 'version']:
    sys.exit(int(os.environ.get('FAIL_VERSION', '0')))
if args[0] == 'run':
    if os.environ.get('FAIL_RUN'):
        print('partial output')
        sys.exit(1)
    if os.environ.get('RACE'):
        pathlib.Path('.env').write_text('concurrent configuration\n')
    if os.environ.get('RACE_SYMLINK'):
        pathlib.Path('.env').symlink_to('target', target_is_directory=True)
    sys.exit(subprocess.run([sys.executable, '-c', args[-1]]).returncode)
if args[:2] == ['compose', 'up']:
    sys.exit(int(os.environ.get('FAIL_UP', '0')))
sys.exit(0)
'''


class LauncherTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        shutil.copy2(SOURCE, self.root / 'start.sh')
        self.bin = self.root / 'bin'
        self.bin.mkdir()
        docker = self.bin / 'docker'
        docker.write_text(FAKE)
        docker.chmod(0o755)
        self.env = os.environ.copy()
        self.env['PATH'] = str(self.bin) + os.pathsep + self.env['PATH']
        self.env.update({name: 'inherited-override' for name in NAMES})

    def calls(self):
        path = self.root / 'calls.jsonl'
        return [json.loads(line) for line in path.read_text().splitlines()] if path.exists() else []

    def run_plain(self, **extra):
        return subprocess.run(['sh', str(self.root / 'start.sh')], cwd='/',
                              env=dict(self.env, **extra), capture_output=True)

    def run_tty(self, password='StrongPassword123!', confirmation=None, **extra):
        try:
            master, slave = pty.openpty()
        except OSError:
            # Sandboxed runners may deny PTY allocation. Keep functional coverage
            # using a fixture-only terminal adapter; a separate test reports the
            # unavailable real-terminal coverage explicitly as skipped.
            script = self.root / 'start.sh'
            original_script = script.read_text()
            terminal = self.root / 'terminal-input'
            terminal.write_text(password + '\n' + (password if confirmation is None else confirmation) + '\n')
            stty = self.bin / 'stty'
            stty.write_text('#!/bin/sh\n[ "$1" != "-g" ] || printf "test-state\\n"\n')
            stty.chmod(0o755)
            script.write_text(original_script.replace('exec 3<>/dev/tty', 'exec 3<"$ROOT/terminal-input"').replace('>&3', '>&2'))
            try:
                result = self.run_plain(**extra)
                return result.returncode, (result.stdout + result.stderr).decode(errors='replace')
            finally:
                script.write_text(original_script)
                stty.unlink()
                terminal.unlink()
        original = termios.tcgetattr(slave)

        def controlling_terminal():
            os.setsid()
            fcntl.ioctl(0, termios.TIOCSCTTY, 0)

        process = subprocess.Popen(['sh', str(self.root / 'start.sh')], cwd='/',
                                   env=dict(self.env, **extra), stdin=slave,
                                   stdout=slave, stderr=slave,
                                   preexec_fn=controlling_terminal)
        output = b''
        sent = 0
        deadline = time.monotonic() + 10
        try:
            while time.monotonic() < deadline:
                ready, _, _ = select.select([master], [], [], 0.1)
                if ready:
                    try:
                        output += os.read(master, 65536)
                    except OSError as exc:
                        if exc.errno != errno.EIO:
                            raise
                        break
                if sent == 0 and b'Contrase' in output:
                    os.write(master, password.encode() + b'\n')
                    sent = 1
                if sent == 1 and b'Confirmar' in output:
                    os.write(master, (password if confirmation is None else confirmation).encode() + b'\n')
                    sent = 2
                if process.poll() is not None:
                    # Read any remaining buffered diagnostics without waiting for EOF
                    # (slave remains open to inspect restored terminal flags).
                    while select.select([master], [], [], 0)[0]:
                        output += os.read(master, 65536)
                    break
            else:
                process.kill()
                self.fail('launcher timed out')
            code = process.wait(timeout=2)
            self.assertEqual(termios.tcgetattr(slave), original)
            return code, output.decode(errors='replace')
        finally:
            if process.poll() is None:
                process.kill()
                process.wait()
            os.close(master)
            os.close(slave)

    def assert_clean(self):
        self.assertEqual(list(self.root.glob('.start.*')), [])

    def test_existing_config_is_unchanged_and_runs_from_root(self):
        path = self.root / '.env'
        path.write_bytes(b'# existing\nDB_PASSWORD=unchanged\n')
        path.chmod(0o640)
        before = path.stat()
        result = self.run_plain()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(path.read_bytes(), b'# existing\nDB_PASSWORD=unchanged\n')
        self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o640)
        self.assertEqual(path.stat().st_ino, before.st_ino)
        call = self.calls()[-1]
        self.assertEqual(call['args'], ['compose', 'up', '--build', '--wait', '--wait-timeout', '300'])
        self.assertEqual(call['cwd'], str(self.root))
        self.assertEqual(call['env'], {})
        self.assert_clean()

    def test_generated_config_permissions_randomness_and_hidden_specials(self):
        self.env.update(admin='inherited', confirmation='inherited',
                        COMPOSE_ENV_FILES='untrusted.env', COMPOSE_DISABLE_ENV_FILE='1')
        password = " a'$HOME\\#\"`! z123456 "
        code, output = self.run_tty(password)
        self.assertEqual(code, 0, output)
        self.assertNotIn(password, output)
        path = self.root / '.env'
        self.assertEqual(stat.S_IMODE(path.stat().st_mode), 0o600)
        lines = path.read_text().splitlines()
        values = [line.split('=', 1)[1] for line in lines[:4]]
        self.assertEqual(len(set(values)), 4)
        for value in values:
            self.assertRegex(value, r'^[0-9a-f]{64}$')
        encoded = password.replace('\\', '\\\\').replace('"', '\\"').replace('$', '$$')
        self.assertEqual(lines[-1], 'BOOTSTRAP_ADMIN_PASSWORD="' + encoded + '"')
        for call in self.calls():
            self.assertNotIn(password, json.dumps(call))
            if call['args'][0] == 'run' or call['args'][:2] == ['compose', 'up']:
                self.assertEqual(call['env'], {})
        self.assert_clean()

    def test_short_or_mismatched_password_does_not_create_config(self):
        for password, confirmation in [('short', 'short'), ('long-enough-password', 'different')]:
            with self.subTest(password=password):
                code, output = self.run_tty(password, confirmation)
                self.assertNotEqual(code, 0)
                self.assertNotIn(password, output)
                self.assertFalse((self.root / '.env').exists())
                self.assert_clean()

    def test_daemon_and_compose_errors_precede_prompt(self):
        for flag in ('FAIL_INFO', 'FAIL_VERSION'):
            result = self.run_plain(**{flag: '1'})
            self.assertNotEqual(result.returncode, 0)
            self.assertNotIn(b'Contrase', result.stderr)
            self.assertFalse((self.root / '.env').exists())

    def test_missing_docker_precedes_prompt(self):
        (self.bin / 'docker').unlink()
        self.env['PATH'] = str(self.bin)
        result = subprocess.run(['/bin/sh', './start.sh'], cwd=self.root,
                                env=self.env, capture_output=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn(b'Contrase', result.stderr)

    def test_symlinks_including_dangling_are_rejected(self):
        for target in ('missing', 'target'):
            (self.root / 'target').write_text('untouched')
            link = self.root / '.env'
            link.symlink_to(target)
            result = self.run_plain()
            self.assertNotEqual(result.returncode, 0)
            self.assertTrue(link.is_symlink())
            self.assertEqual((self.root / 'target').read_text(), 'untouched')
            link.unlink()

    def test_generation_failure_cleans_partial_file(self):
        code, output = self.run_tty(FAIL_RUN='1')
        self.assertNotEqual(code, 0)
        self.assertNotIn('partial output', output)
        self.assertFalse((self.root / '.env').exists())
        self.assert_clean()

    def test_concurrent_config_is_never_overwritten(self):
        code, _ = self.run_tty(RACE='1')
        self.assertNotEqual(code, 0)
        self.assertEqual((self.root / '.env').read_text(), 'concurrent configuration\n')
        self.assert_clean()

    def test_concurrent_symlink_to_directory_is_not_followed(self):
        (self.root / 'target').mkdir()
        code, _ = self.run_tty(RACE_SYMLINK='1')
        self.assertNotEqual(code, 0)
        self.assertTrue((self.root / '.env').is_symlink())
        self.assertEqual(list((self.root / 'target').iterdir()), [])
        self.assert_clean()

    def test_real_terminal_echo_restoration(self):
        try:
            master, slave = pty.openpty()
        except OSError as exc:
            self.skipTest('PTY unavailable in this runner: ' + str(exc))
        else:
            os.close(master)
            os.close(slave)
        code, output = self.run_tty('TerminalHidden123!')
        self.assertEqual(code, 0, output)
        self.assertNotIn('TerminalHidden123!', output)

    def test_real_compose_dotenv_roundtrip_without_daemon(self):
        docker = shutil.which('docker', path=os.environ.get('PATH', ''))
        if not docker:
            self.skipTest('Docker CLI unavailable (no daemon required)')
        version = subprocess.run([docker, 'compose', 'version'], capture_output=True)
        if version.returncode:
            self.skipTest('Compose CLI unavailable')
        compose = self.root / 'compose.yml'
        compose.write_text('services:\n  test:\n    image: alpine\n    environment:\n      VALUE: ${BOOTSTRAP_ADMIN_PASSWORD}\n')
        passwords = [r'abc\def123456', r'abc\\def123456', "abc'def123456",
                     'abc123456789' + chr(92), 'abc' + chr(92) + "'def123456",
                     ' a$HOME#\"`!z123456 ', 'unicode-contraseña-123']
        environment = {key: value for key, value in os.environ.items()
                       if key not in NAMES and key not in ('COMPOSE_ENV_FILES', 'COMPOSE_DISABLE_ENV_FILE')}
        for password in passwords:
            with self.subTest(case=passwords.index(password)):
                config = self.root / '.env'
                if config.exists():
                    config.unlink()
                code, output = self.run_tty(password)
                self.assertEqual(code, 0, output)
                result = subprocess.run([docker, 'compose', '--project-directory', str(self.root),
                                         'config', '--environment'], env=environment, capture_output=True)
                self.assertEqual(result.returncode, 0, 'Compose rejected generated dotenv')
                values = dict(line.split('=', 1) for line in result.stdout.decode().splitlines() if '=' in line)
                # Do not include actual values in failure messages.
                self.assertTrue(values.get('BOOTSTRAP_ADMIN_PASSWORD') == password,
                                'Compose altered the administrator password')

    def test_compose_failure_propagates_but_keeps_config(self):
        code, _ = self.run_tty(FAIL_UP='23')
        self.assertEqual(code, 23)
        self.assertTrue((self.root / '.env').is_file())
        self.assert_clean()


if __name__ == '__main__':
    unittest.main()
