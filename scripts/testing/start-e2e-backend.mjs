import { spawn } from 'node:child_process'
import { readdirSync, mkdirSync, createWriteStream } from 'node:fs'
import { fileURLToPath } from 'node:url'
import path from 'node:path'

const root = fileURLToPath(new URL('../../', import.meta.url))
const libs = path.join(root, 'server/build/libs')
const jar = readdirSync(libs).find(name => name.endsWith('.jar') && !name.endsWith('-plain.jar'))
if (!jar) throw new Error('Build the backend first: server/gradlew bootJar')
const logs = path.join(root, 'client/e2e-server-logs')
mkdirSync(logs, { recursive: true })
const log = createWriteStream(path.join(logs, 'backend.log'))
const child = spawn(process.env.E2E_JAVA || 'java', ['-jar', path.join(libs, jar)], {
  cwd: path.join(root, 'server'),
  stdio: ['ignore', 'pipe', 'pipe'],
  env: {
    ...process.env,
    SERVER_PORT: '18080',
    DB_URL: 'jdbc:postgresql://127.0.0.1:55440/uade_e2e',
    DB_USERNAME: 'e2e', DB_PASSWORD: 'e2e-disposable-only',
    JPA_DDL_AUTO: 'create-drop', CORE_ENABLED: 'false',
    BOOTSTRAP_ADMIN_ENABLED: 'true', BOOTSTRAP_ADMIN_PASSWORD: 'E2e-Admin-Disposable-2026!',
    JWT_SECRET: 'e2e-only-disposable-jwt-secret-123456789012345678901234567890',
    AUTH_COOKIE_SECURE: 'false', AUTH_COOKIE_SAME_SITE: 'Lax',
    CORS_ALLOWED_ORIGIN_1: 'http://localhost:15173',
    CORS_ALLOWED_ORIGIN_2: 'http://127.0.0.1:15173',
    ENROLLMENT_PERIOD_EXPIRATION_CRON: '-', BENEFIT_GRANT_EXPIRATION_ENABLED: 'false',
  },
})
child.stdout.pipe(log, { end: false })
child.stderr.pipe(log, { end: false })
child.stdout.pipe(process.stdout)
child.stderr.pipe(process.stderr)
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => child.kill(signal))
child.on('error', error => { console.error(error); process.exit(1) })
child.on('exit', code => { log.end(); process.exit(code ?? 1) })
