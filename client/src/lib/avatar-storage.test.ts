import { afterEach, expect, it, vi } from 'vitest'
import { clearCurrentUserAvatar, getAvatarForUser, getAvatarUserIdentity, getAvatarUserLabel, readCurrentUserAvatar, saveCurrentUserAvatar, subscribeCurrentUserAvatar } from './avatar'

afterEach(() => { localStorage.clear(); vi.restoreAllMocks() })

it.each([
  { user: { id: 0, username: 'ana', email: 'ana@example.test', name: 'Ana' }, identity: 'id:0', label: 'Ana' },
  { user: { username: 'ana', email: 'ana@example.test' }, identity: 'ana', label: 'ana' },
  { user: { email: 'ana@example.test' }, identity: 'ana@example.test', label: 'ana@example.test' },
  { user: { name: 'Ana' }, identity: 'Ana', label: 'Ana' },
  { user: {}, identity: 'user', label: 'Usuario' },
])('identifica consistentemente a una persona con datos parciales: %j', ({ user, identity, label }) => {
  expect(getAvatarUserIdentity(user)).toBe(identity)
  expect(getAvatarUserLabel(user)).toBe(label)
})

it.each(['invalid-json', '{"version":2,"identity":"id:2","src":"old"}', '{"version":3,"identity":"id:2"}', ''])('ignora valores inválidos o de otra versión: %s', value => {
  localStorage.setItem('current-user-avatar', value)
  expect(readCurrentUserAvatar()).toBeNull()
})

it('reutiliza el avatar solo para la misma identidad y detecta cambios en el almacenamiento', () => {
  const stored = { version: 3, identity: 'id:2', src: 'data:image/png;base64,test' }
  localStorage.setItem('current-user-avatar', JSON.stringify(stored))
  const first = readCurrentUserAvatar()
  expect(first).toEqual(stored)
  expect(readCurrentUserAvatar()).toBe(first)
  expect(getAvatarForUser({ id: 2, name: 'Ana' })).toBe(stored.src)
  expect(getAvatarForUser({ id: 3, name: 'Otra persona' })).not.toBe(stored.src)
  localStorage.removeItem('current-user-avatar')
  expect(readCurrentUserAvatar()).toBeNull()
})

it('notifica guardado, borrado y cambios de otra pestaña y retira los listeners al desuscribir', () => {
  const context = { fillRect: vi.fn(), fillText: vi.fn(), createRadialGradient: vi.fn(() => ({ addColorStop: vi.fn() })) }
  vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(context as unknown as CanvasRenderingContext2D)
  vi.spyOn(HTMLCanvasElement.prototype, 'toDataURL').mockReturnValue('data:image/png;base64,generated')
  const callback = vi.fn()
  const unsubscribe = subscribeCurrentUserAvatar(callback)
  try {
    saveCurrentUserAvatar({ id: 202, name: 'Avatar Storage Probe' })
    expect(callback).toHaveBeenCalledTimes(1)
    expect(readCurrentUserAvatar()).toMatchObject({ version: 3, identity: 'id:202', src: 'data:image/png;base64,generated' })
    expect(context.fillText).toHaveBeenCalledWith('AP', 64, 68)
    window.dispatchEvent(new StorageEvent('storage', { key: 'theme' }))
    expect(callback).toHaveBeenCalledTimes(1)
    window.dispatchEvent(new StorageEvent('storage', { key: 'current-user-avatar' }))
    expect(callback).toHaveBeenCalledTimes(2)
    clearCurrentUserAvatar()
    expect(callback).toHaveBeenCalledTimes(3)
    expect(readCurrentUserAvatar()).toBeNull()
    unsubscribe()
    clearCurrentUserAvatar()
    expect(callback).toHaveBeenCalledTimes(3)
  } finally { unsubscribe() }
})
