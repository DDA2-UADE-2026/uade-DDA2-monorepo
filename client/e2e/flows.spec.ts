import { test, expect } from '@playwright/test'
import { adminApi, appointmentData, citizen, login, program, unique, API, type Api, type User } from './support'

let admin: Api
let user: User
test.beforeEach(async () => {
  admin = await adminApi()
  user = await citizen(admin)
})
test.afterEach(async ({ page }, info) => {
  if (info.status === 'passed') await info.attach('resultado-visible', { body: await page.screenshot({ fullPage: true }), contentType: 'image/png' })
  await admin?.context.dispose()
})

test('E2E-01 — login ciudadano y acceso restringido por rol', async ({ page }) => {
  await login(page, user.username)
  await page.goto('/portal/programas')
  await expect(page.getByRole('heading', { name: 'Programas disponibles' })).toBeVisible()
  const denied = await page.request.get(`${API}/api/admin/programs`)
  expect(denied.status()).toBe(403)
  await page.goto('/gestion/programas')
  await expect(page).toHaveURL(/\/portal\/?$/)
  await expect(page.getByRole('button', { name: 'Nuevo programa' })).toHaveCount(0)
})

test('E2E-02 — crear programa y consultar persistencia', async ({ page }) => {
  await login(page, 'admin', true)
  await page.goto('/gestion/programas')
  await page.getByRole('button', { name: 'Nuevo programa' }).click()
  const name = `Creado desde navegador E2E ${unique()}`
  await page.getByRole('dialog').getByLabel('Nombre', { exact: true }).fill(name)
  await page.getByRole('dialog').getByLabel('Objetivo').fill('Objetivo verificado por navegador E2E')
  await page.getByRole('button', { name: 'Crear programa', exact: true }).click()
  await expect(page).toHaveURL(/\/gestion\/programas\/[0-9a-f-]{36}$/)
  await page.reload()
  await expect(page.getByText(name, { exact: true }).first()).toBeVisible()
  await expect(page.getByText('Objetivo verificado por navegador E2E', { exact: true })).toBeVisible()
})

test('E2E-03 — presentar solicitud y consultar el listado propio', async ({ page }) => {
  const p = await program(admin)
  await login(page, user.username)
  await page.goto(`/portal/programas/${p.id}`)
  await page.getByRole('button', { name: 'Solicitar este programa' }).click()
  await page.getByRole('button', { name: 'Presentar solicitud', exact: true }).click()
  await expect(page).toHaveURL(/\/portal\/solicitudes\/[0-9a-f-]{36}$/)
  await expect(page.getByRole('heading', { name: /Solicitud N.º/ })).toBeVisible()
  await page.reload()
  await expect(page.getByText(p.name, { exact: true })).toBeVisible()
  await expect(page.getByText('Presentada', { exact: true })).toBeVisible()
  await page.goto('/portal/solicitudes')
  await expect(page.getByText(p.name, { exact: true }).first()).toBeVisible()
})

test('E2E-04 — adjuntar documento, revisar y ver estado ciudadano', async ({ page, browser }) => {
  const p = await program(admin, true)
  await login(page, user.username)
  await page.goto(`/portal/programas/${p.id}`)
  await page.getByRole('button', { name: 'Solicitar este programa' }).click()
  await page.getByRole('button', { name: 'Presentar solicitud', exact: true }).click()
  await expect(page).toHaveURL(/\/portal\/solicitudes\/[0-9a-f-]{36}$/)
  const applicationId = page.url().split('/').pop()!
  await page.getByRole('button', { name: 'Adjuntar documento', exact: true }).click()
  await page.getByLabel('Archivo del documento').setInputFiles({
    name: 'constancia-e2e.png', mimeType: 'image/png',
    buffer: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+a4qkAAAAASUVORK5CYII=', 'base64'),
  })
  await page.getByRole('button', { name: 'Enviar documento', exact: true }).click()
  await expect(page.getByText('Pendiente de revisión', { exact: true })).toBeVisible()
  const reviewContext = await browser.newContext({ baseURL: 'http://localhost:15173', timezoneId: 'America/Argentina/Buenos_Aires' })
  try {
    const reviewer = await reviewContext.newPage()
    await login(reviewer, 'admin', true)
    await reviewer.goto(`/gestion/solicitudes/${applicationId}`)
    await reviewer.getByRole('button', { name: 'Revisar', exact: true }).click()
    await reviewer.getByRole('button', { name: 'Validar', exact: true }).click()
    await reviewer.getByRole('button', { name: 'Confirmar validación' }).click()
    await expect(reviewer.getByText('Válido', { exact: true })).toBeVisible()
    await page.reload()
    await expect(page.getByText('Válido', { exact: true })).toBeVisible()
    await expect(page.getByText('constancia-e2e.png', { exact: true })).toBeVisible()
  } finally { await reviewContext.close() }
})

test('E2E-05 — reservar disponibilidad y consultar turno persistido', async ({ page }) => {
  const data = await appointmentData(admin)
  await login(page, user.username)
  await page.goto('/portal/turnos/nuevo')
  await page.getByRole('region', { name: 'Servicio', exact: true }).getByRole('button', { name: 'Elegir', exact: true }).first().click()
  await page.getByRole('region', { name: 'Centro', exact: true }).getByRole('button', { name: 'Elegir', exact: true }).first().click()
  await page.getByRole('button', { name: 'Seleccioná la fecha del turno' }).click()
  const [year, month, day] = data.date.split('-').map(Number)
  await page.locator(`button[data-day="${day}/${month}/${year}"]`).first().click()
  await page.getByRole('region', { name: 'Horario', exact: true }).getByRole('button', { name: 'Elegir', exact: true }).first().click()
  await page.getByRole('button', { name: 'Confirmar turno', exact: true }).click()
  await expect(page).toHaveURL(/\/portal\/turnos\/[0-9a-f-]{36}$/)
  await page.reload()
  await expect(page.getByRole('heading', { name: 'Turno confirmado', exact: true })).toBeVisible()
  await expect(page.getByText(data.service.name, { exact: true }).first()).toBeVisible()
  await page.goto('/portal/turnos')
  await expect(page.getByText(data.service.name, { exact: true }).first()).toBeVisible()
})
