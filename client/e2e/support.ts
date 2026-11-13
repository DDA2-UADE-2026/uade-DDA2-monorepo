import { request, expect, type APIRequestContext, type Page } from '@playwright/test'
import { randomUUID } from 'node:crypto'

export const API = 'http://localhost:18080'
export const ADMIN_PASSWORD = 'E2e-Admin-Disposable-2026!'
export const USER_PASSWORD = 'E2e-Citizen-Disposable-2026!'
export const unique = () => randomUUID().slice(0, 8)
export type Entity = { id: string; name: string; status?: string; applicationNumber?: number }
export type User = { id: number; username: string; name: string }

export class Api {
  readonly context: APIRequestContext
  constructor(context: APIRequestContext) { this.context = context }
  async call<T = Entity>(method: string, path: string, data?: unknown): Promise<T> {
    const headers: Record<string, string> = { Origin: 'http://localhost:15173' }
    if (method !== 'GET') {
      const csrf = await this.context.get(`${API}/auth/session/csrf`)
      expect(csrf.ok(), 'local CSRF endpoint').toBeTruthy()
      const token = await csrf.json()
      headers[token.headerName] = token.token
    }
    const response = await this.context.fetch(`${API}${path}`, { method, data, headers })
    expect(response.ok(), `${method} ${path}: ${response.status()} ${await response.text()}`).toBeTruthy()
    return response.status() === 204 ? undefined as T : await response.json() as T
  }
}

export async function adminApi() {
  const api = new Api(await request.newContext())
  await api.call('POST', '/auth/session/login', { username: 'admin', password: ADMIN_PASSWORD })
  await api.call('POST', '/auth/session/select-role', { role: 'ADMIN' })
  return api
}

export async function citizen(admin: Api): Promise<User> {
  const tag = unique()
  return admin.call<User>('POST', '/users', {
    username: `e2e.citizen.${tag}`, password: USER_PASSWORD,
    name: `Ciudadano E2E ${tag}`, email: `citizen.${tag}@example.test`, roles: ['CIUDADANO'],
  })
}

export function date(offset: number) {
  const d = new Date()
  d.setDate(d.getDate() + offset)
  return [d.getFullYear(), String(d.getMonth() + 1).padStart(2, '0'), String(d.getDate()).padStart(2, '0')].join('-')
}

export async function program(admin: Api, document = false) {
  const tag = unique()
  const p = await admin.call('POST', '/api/admin/programs', { name: `Programa E2E ${tag}`, objective: 'Programa sintético de integración' })
  const e = await admin.call('POST', `/api/admin/program-editions/program/${p.id}`, {
    name: `Edición E2E ${tag}`, startDate: date(-7), endDate: date(60), maxCapacity: 20,
  })
  if (document) await admin.call('POST', `/api/admin/program-editions/${e.id}/document-requirements`, {
    code: `DOC_${tag}`, name: 'Constancia E2E', required: true,
  })
  await admin.call('PATCH', `/api/admin/program-editions/${e.id}/activate`)
  const period = await admin.call('POST', `/api/admin/programs/${p.id}/editions/${e.id}/enrollment-periods`, {
    openDate: date(-1), closeDate: date(30), notes: 'Período E2E descartable',
  })
  await admin.call('POST', `/api/admin/programs/${p.id}/editions/${e.id}/enrollment-periods/${period.id}/open`)
  return { ...p, periodId: period.id }
}

export async function login(page: Page, username: string, admin = false) {
  await page.goto('/login')
  await page.getByLabel('Usuario', { exact: true }).fill(username)
  await page.getByLabel('Contraseña', { exact: true }).fill(admin ? ADMIN_PASSWORD : USER_PASSWORD)
  await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
  if (admin) {
    await expect(page).toHaveURL(/seleccionar-rol/)
    await page.getByRole('button', { name: /Administración/ }).click()
    await expect(page).toHaveURL(/\/gestion\/?$/)
  } else await expect(page).toHaveURL(/\/portal\/?$/)
}

export async function appointmentData(admin: Api) {
  const tag = unique()
  const roles = await admin.call<{ name: string }[]>('GET', '/roles')
  if (!roles.some(r => r.name === 'PROFESIONAL_CENTRO')) await admin.call('POST', '/roles', { name: 'PROFESIONAL_CENTRO', permissions: [] })
  const professional = await admin.call<User>('POST', '/users', {
    username: `e2e.professional.${tag}`, password: USER_PASSWORD, name: `Profesional E2E ${tag}`,
    email: `professional.${tag}@example.test`, roles: ['PROFESIONAL_CENTRO'],
  })
  const center = await admin.call('POST', '/api/admin/municipal-centers', { name: `Centro E2E ${tag}`, address: 'Calle de prueba 123' })
  const service = await admin.call('POST', '/api/admin/municipal-services', { name: `Servicio E2E ${tag}`, description: 'Atención sintética', durationMinutes: 30 })
  const cs = await admin.call('POST', `/api/admin/municipal-centers/${center.id}/services`, { serviceId: service.id })
  const assignment = await admin.call('POST', `/api/admin/center-services/${cs.id}/professionals`, { professionalId: professional.id })
  const days = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']
  await admin.call('POST', `/api/admin/municipal-centers/${center.id}/opening-hours/batch`, { days, startTime: '09:00', endTime: '12:00' })
  await admin.call('POST', `/api/admin/professional-assignments/${assignment.id}/availability/batch`, { days, startTime: '09:00', endTime: '12:00' })
  return { center, service, date: date(1) }
}
