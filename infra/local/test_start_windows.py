"""Windows launcher contracts; runtime cases explicitly require PowerShell.

These tests do not claim Windows coverage when run on another operating system.
Only generation/ACL cases require Windows; existing-dotenv cases can run in pwsh.
"""
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
PS1 = ROOT / 'start.ps1'
CMD = ROOT / 'start.cmd'
POWERSHELL = shutil.which('pwsh') or shutil.which('powershell.exe')
NAMES = ('DB_PASSWORD', 'RABBITMQ_PASSWORD', 'JWT_SECRET',
         'CORE_MACHINE_SECRET', 'BOOTSTRAP_ADMIN_PASSWORD',
         'COMPOSE_ENV_FILES', 'COMPOSE_DISABLE_ENV_FILE')
FAKE = '''import json, os, pathlib, sys
args = sys.argv[1:]
with open('calls.jsonl', 'a') as log:
    log.write(json.dumps({'args': args, 'cwd': os.getcwd(), 'env': {
        k: os.environ[k] for k in %r if k in os.environ}}) + '\\n')
if args == ['info']:
    sys.exit(int(os.environ.get('FAIL_INFO', '0')))
if args == ['compose', 'version']:
    sys.exit(int(os.environ.get('FAIL_VERSION', '0')))
if args[:2] == ['compose', 'up']:
    sys.exit(int(os.environ.get('FAIL_UP', '0')))
sys.exit(0)
''' % (NAMES,)


class StaticWindowsContracts(unittest.TestCase):
    def setUp(self):
        self.ps = PS1.read_text(encoding='ascii')
        self.cmd = CMD.read_text(encoding='ascii')

    def test_cmd_process_policy_and_exit_code(self):
        self.assertIn('powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"', self.cmd)
        self.assertIn('set "code=%errorlevel%"', self.cmd)
        self.assertIn('endlocal & exit /b %code%', self.cmd)
        self.assertNotIn('Set-ExecutionPolicy', self.ps + self.cmd)
        self.assertTrue(CMD.read_bytes().endswith(b'\r\n'))

    def test_native_randomness_and_secret_input(self):
        self.assertIn('#Requires -Version 5.1', self.ps)
        self.assertEqual(self.ps.count('-AsSecureString'), 2)
        self.assertIn('RandomNumberGenerator]::Create()', self.ps)
        self.assertIn('New-Object byte[] 32', self.ps)
        self.assertIn('$password.Length -lt 12', self.ps)
        self.assertIn('$password -cne $confirmation', self.ps)
        self.assertIn('ZeroFreeBSTR', self.ps)
        self.assertNotRegex(self.ps, r'(?im)^\s*&.*\$(?:password|escaped|content)')

    def test_atomic_acl_encoding_and_cleanup_contract(self):
        self.assertIn('$acl.SetAccessRuleProtection($true, $false)', self.ps)
        self.assertIn('$acl.SetOwner($sid)', self.ps)
        self.assertIn('[IO.FileMode]::CreateNew', self.ps)
        self.assertIn('[IO.FileSystemAclExtensions]::Create', self.ps)
        self.assertIn('[Text.UTF8Encoding]::new($false)', self.ps)
        self.assertIn('[IO.File]::Move($tempPath, $configPath)', self.ps)
        self.assertNotIn('Move-Item', self.ps)
        self.assertLess(self.ps.index('$stream = New-PrivateConfigStream $candidate'),
                        self.ps.index('$stream.Write('))
        self.assertIn('[IO.File]::Delete($tempPath)', self.ps)
        self.assertIn('[System.IO.FileAttributes]::ReparsePoint', self.ps)
        self.assertIn(".Replace('\\', '\\\\').Replace('\"', '\\\"').Replace('$', '$$')", self.ps)

    def test_compose_flags_and_environment_restoration(self):
        for name in NAMES:
            self.assertIn("'" + name + "'", self.ps)
        self.assertIn("SetEnvironmentVariable($name, $null, 'Process')", self.ps)
        self.assertIn("SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')", self.ps)
        self.assertIn('compose up --build --wait --wait-timeout 300', self.ps)
        self.assertIn('http://localhost:5173', self.ps)
        self.assertIn('$exitCode = $LASTEXITCODE', self.ps)
        self.assertIn('exit $exitCode', self.ps)


@unittest.skipUnless(POWERSHELL, 'pwsh/powershell.exe unavailable; no PowerShell runtime exercised')
class PowerShellRuntime(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        shutil.copy2(PS1, self.root / 'start.ps1')
        fake = self.root / 'fake.py'
        fake.write_text(FAKE)
        bindir = self.root / 'bin'
        bindir.mkdir()
        if os.name == 'nt':
            (bindir / 'docker.cmd').write_text('@echo off\n"' + sys.executable + '" "' + str(fake) + '" %*\nexit /b %errorlevel%\n')
        else:
            docker = bindir / 'docker'
            docker.write_text('#!' + sys.executable + '\n' + FAKE)
            docker.chmod(0o755)
        self.env = dict(os.environ, PATH=str(bindir) + os.pathsep + os.environ['PATH'])
        self.env.update({name: 'inherited-override' for name in NAMES})

    def run_launcher(self, **extra):
        return subprocess.run([POWERSHELL, '-NoProfile', '-File', str(self.root / 'start.ps1')],
                              cwd=self.root.parent, env=dict(self.env, **extra),
                              capture_output=True, timeout=30)

    def calls(self):
        path = self.root / 'calls.jsonl'
        return [json.loads(x) for x in path.read_text().splitlines()] if path.exists() else []

    def test_existing_config_preserved_root_environment_and_exit(self):
        config = self.root / '.env'
        content = b'# existing\r\nDB_PASSWORD=unchanged\r\n'
        config.write_bytes(content)
        result = self.run_launcher(FAIL_UP='23')
        self.assertEqual(result.returncode, 23, result.stderr)
        self.assertEqual(config.read_bytes(), content)
        last = self.calls()[-1]
        self.assertEqual(last['args'], ['compose', 'up', '--build', '--wait', '--wait-timeout', '300'])
        self.assertEqual(Path(last['cwd']), self.root)
        self.assertEqual(last['env'], {})
        self.assertEqual(list(self.root.glob('.start.*')), [])

    def test_process_overrides_and_location_are_restored(self):
        (self.root / '.env').write_text('# existing\n')
        # Replace only the final process exit in this disposable fixture so the
        # harness can inspect the same process after the production finally.
        script = self.root / 'start.ps1'
        script.write_text(script.read_text().replace('exit $exitCode', 'return $exitCode'))
        wrapper = self.root / 'restore.ps1'
        wrapper.write_text("$before = (Get-Location).Path\n"
                           "$result = & (Join-Path $PSScriptRoot 'start.ps1')\n"
                           "if ($result -ne 0) { exit 10 }\n"
                           "if ((Get-Location).Path -ne $before) { exit 11 }\n"
                           "foreach ($n in @(" + ','.join("'" + n + "'" for n in NAMES) + ")) {\n"
                           "if ([Environment]::GetEnvironmentVariable($n, 'Process') -ne 'inherited-override') { exit 12 }\n}\n")
        result = subprocess.run([POWERSHELL, '-NoProfile', '-File', str(wrapper)],
                                cwd=self.root.parent, env=self.env, capture_output=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.calls()[-1]['env'], {})

    def test_preflight_failure_does_not_prompt(self):
        for flag in ('FAIL_INFO', 'FAIL_VERSION'):
            with self.subTest(flag=flag):
                result = self.run_launcher(**{flag: '1'})
                self.assertNotEqual(result.returncode, 0)
                self.assertNotIn(b'Confirmar', result.stdout)
                self.assertFalse((self.root / '.env').exists())

    def test_directory_and_symlink_rejected(self):
        config = self.root / '.env'
        config.mkdir()
        self.assertNotEqual(self.run_launcher().returncode, 0)
        config.rmdir()
        try:
            config.symlink_to(self.root / 'missing')
        except OSError as exc:
            self.skipTest('Symlink creation unavailable: ' + str(exc))
        self.assertNotEqual(self.run_launcher().returncode, 0)
        self.assertTrue(config.is_symlink())

    @unittest.skipUnless(os.name == 'nt' and shutil.which('powershell.exe'),
                         'CMD bridge requires real Windows and powershell.exe')
    def test_cmd_bridge_exit_code_from_cmd_and_powershell(self):
        shutil.copy2(CMD, self.root / 'start.cmd')
        (self.root / '.env').write_text('# existing\n')
        environment = dict(self.env, FAIL_UP='23')
        for command in ([os.environ.get('COMSPEC', 'cmd.exe'), '/d', '/c', str(self.root / 'start.cmd')],
                        [POWERSHELL, '-NoProfile', '-Command',
                         "& '" + str(self.root / 'start.cmd').replace("'", "''") + "'; exit $LASTEXITCODE"]):
            result = subprocess.run(command, cwd=self.root.parent, env=environment,
                                    capture_output=True, timeout=30)
            self.assertEqual(result.returncode, 23, result.stderr)

    def secure_input_fixture(self, passwords):
        (self.root / 'passwords.json').write_text(json.dumps(passwords), encoding='utf-8')
        wrapper = self.root / 'input.ps1'
        wrapper.write_text("$global:LauncherTestReads = 0\n"
                           "function Read-Host { param($Prompt, [switch]$AsSecureString)\n"
                           "$values = @(Get-Content -Raw -Encoding UTF8 (Join-Path $PSScriptRoot 'passwords.json') | ConvertFrom-Json)\n"
                           "$p = $values[$global:LauncherTestReads]; $global:LauncherTestReads++\n"
                           "ConvertTo-SecureString $p -AsPlainText -Force\n}\n"
                           "& (Join-Path $PSScriptRoot 'start.ps1')\nexit $LASTEXITCODE\n")
        return subprocess.run([POWERSHELL, '-NoProfile', '-File', str(wrapper)],
                              cwd=self.root.parent, env=self.env, capture_output=True, timeout=30)

    @unittest.skipUnless(os.name == 'nt', 'Secure input fixture requires Windows runtime')
    def test_short_and_mismatched_password_rejected_without_artifacts(self):
        for values in (['short', 'short'], ['long-password123!', 'another-password123!']):
            result = self.secure_input_fixture(values)
            self.assertNotEqual(result.returncode, 0)
            for value in values:
                self.assertNotIn(value.encode(), result.stdout + result.stderr)
            self.assertFalse((self.root / '.env').exists())
            self.assertEqual(list(self.root.glob('.start.*')), [])

    @unittest.skipUnless(os.name == 'nt', 'Atomic Windows publication requires Windows runtime')
    def test_concurrent_config_is_not_overwritten_and_temp_is_cleaned(self):
        script = self.root / 'start.ps1'
        source = script.read_text()
        source = source.replace('[IO.File]::Move($tempPath, $configPath)',
                                "[IO.File]::WriteAllText($configPath, 'concurrent configuration')\n"
                                '            [IO.File]::Move($tempPath, $configPath)')
        script.write_text(source)
        result = self.secure_input_fixture(['StrongPassword123!', 'StrongPassword123!'])
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual((self.root / '.env').read_text(), 'concurrent configuration')
        self.assertEqual(list(self.root.glob('.start.*')), [])
        self.assertFalse(any(call['args'][:2] == ['compose', 'up'] for call in self.calls()))

    @unittest.skipUnless(os.name == 'nt', 'Windows ACL generation requires a real Windows runtime')
    def test_generation_hidden_escaping_and_acl(self):
        # Fixture-only Read-Host adapter; production still uses secure interactive input.
        password = ' a$HOME\\#"`! contrasena123 '
        fixture = self.root / 'password.txt'
        fixture.write_text(password, encoding='utf-8')
        wrapper = self.root / 'test.ps1'
        wrapper.write_text("function Read-Host { param($Prompt, [switch]$AsSecureString) "
                           "$p = [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'password.txt')); "
                           "ConvertTo-SecureString $p -AsPlainText -Force }\n"
                           "& (Join-Path $PSScriptRoot 'start.ps1')\nexit $LASTEXITCODE\n")
        result = subprocess.run([POWERSHELL, '-NoProfile', '-File', str(wrapper)],
                                cwd=self.root.parent, env=self.env, capture_output=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertNotIn(password.encode(), result.stdout + result.stderr)
        data = (self.root / '.env').read_bytes()
        self.assertFalse(data.startswith(b'\xef\xbb\xbf'))
        lines = data.decode().splitlines()
        secrets = [line.split('=', 1)[1] for line in lines[:4]]
        self.assertEqual(len(set(secrets)), 4)
        for value in secrets:
            self.assertRegex(value, r'^[0-9a-f]{64}$')
        encoded = password.replace('\\', '\\\\').replace('"', '\\"').replace('$', '$$')
        self.assertEqual(lines[-1], 'BOOTSTRAP_ADMIN_PASSWORD="' + encoded + '"')
        self.assertEqual(list(self.root.glob('.start.*')), [])
        # Inspect the actual published DACL, not just the source contract.
        check = """$a = Get-Acl -LiteralPath (Join-Path $args[0] '.env');
$s = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value;
if (-not $a.AreAccessRulesProtected) { exit 1 };
foreach ($r in $a.Access) {
 if ($r.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -ne $s) { exit 2 }
}; if ($a.Access.Count -ne 1) { exit 3 }
"""
        acl = self.root / 'acl.ps1'
        acl.write_text(check)
        result = subprocess.run([POWERSHELL, '-NoProfile', '-File', str(acl), str(self.root)],
                                capture_output=True, timeout=30)
        self.assertEqual(result.returncode, 0, result.stderr)


if __name__ == '__main__':
    unittest.main()
