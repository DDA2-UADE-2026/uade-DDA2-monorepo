interface AvatarOptions {
  size?: number;
  fontSize?: number;
  textColor?: string;
}

export interface AvatarUser {
  id?: string | number
  username?: string
  email?: string
  name?: string
}

interface StoredAvatar {
  version: number
  identity: string
  src: string
}

const CURRENT_USER_AVATAR_KEY = "current-user-avatar"
const CURRENT_USER_AVATAR_EVENT = "current-user-avatar-change"
const AVATAR_STYLE_VERSION = 2

interface ColorDef {
  h: number;
  s: number;
  l: number;
}

function hsla(c: ColorDef, alpha: number): string {
  return `hsla(${c.h}, ${c.s}%, ${c.l}%, ${alpha})`;
}

function getInitials(text: string): string {
  const words = text.trim().split(/\s+/);
  if (words.length === 1) return words[0].slice(0, 2).toUpperCase();
  return (words[0][0] + words[words.length - 1][0]).toUpperCase();
}

function textToColorDefs(text: string): [ColorDef, ColorDef, ColorDef] {
  let hash = 0;
  for (let i = 0; i < text.length; i++) {
    hash = text.charCodeAt(i) + ((hash << 5) - hash);
    hash |= 0;
  }
  const hue1 = Math.abs(hash % 360);
  return [
    { h: hue1, s: 60, l: 45 },
    { h: (hue1 + 60) % 360, s: 70, l: 55 },
    { h: (hue1 + 200) % 360, s: 65, l: 50 },
  ];
}

function generateAvatar(text: string, options: AvatarOptions = {}): string {
  const { size = 128, fontSize, textColor = "#ffffff" } = options;

  const canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Could not get 2D context");

  const colors = textToColorDefs(text);

  // Fondo base
  ctx.fillStyle = hsla(colors[0], 1);
  ctx.fillRect(0, 0, size, size);

  // Blobs con radialGradient
  const blobs: Array<{ x: number; y: number; r: number; color: ColorDef }> = [
    { x: size * 0.2, y: size * 0.3, r: size * 0.6, color: colors[1] },
    { x: size * 0.8, y: size * 0.7, r: size * 0.5, color: colors[2] },
    { x: size * 0.5, y: size * 0.9, r: size * 0.4, color: colors[0] },
  ];

  for (const blob of blobs) {
    const gradient = ctx.createRadialGradient(
      blob.x, blob.y, 0,
      blob.x, blob.y, blob.r
    );
    gradient.addColorStop(0, hsla(blob.color, 0.8));
    gradient.addColorStop(1, hsla(blob.color, 0));
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, size, size);
  }

  // Iniciales
  const initials = getInitials(text);
  const resolvedFontSize = fontSize ?? size * 0.41;
  ctx.fillStyle = textColor;
  ctx.font = `700 ${resolvedFontSize}px system-ui, sans-serif`;
  ctx.textAlign = "center";
  ctx.textBaseline = "middle";
  ctx.fillText(initials, size / 2, size / 2);

  return canvas.toDataURL("image/png");
}

// Caché en memoria por sesión
const avatarCache = new Map<string, string>();

export function getAvatar(text: string, options: AvatarOptions = {}): string {
  const key = `${AVATAR_STYLE_VERSION}__${text}__${JSON.stringify(options)}`;
  if (!avatarCache.has(key)) {
    avatarCache.set(key, generateAvatar(text, options));
  }
  return avatarCache.get(key)!;
}

export function getAvatarUserIdentity(user: AvatarUser): string {
  return user.username || user.email || user.name || String(user.id ?? "user")
}

export function getAvatarUserLabel(user: AvatarUser): string {
  return user.name || user.username || user.email || "Usuario"
}

export function readCurrentUserAvatar(): StoredAvatar | null {
  const value = localStorage.getItem(CURRENT_USER_AVATAR_KEY)
  if (!value) return null
  try {
    const stored = JSON.parse(value) as StoredAvatar
    return stored.version === AVATAR_STYLE_VERSION && stored.identity && stored.src
      ? stored
      : null
  } catch {
    return null
  }
}

export function saveCurrentUserAvatar(user: AvatarUser): void {
  const stored: StoredAvatar = {
    version: AVATAR_STYLE_VERSION,
    identity: getAvatarUserIdentity(user),
    src: getAvatar(getAvatarUserLabel(user)),
  }
  localStorage.setItem(CURRENT_USER_AVATAR_KEY, JSON.stringify(stored))
  window.dispatchEvent(new Event(CURRENT_USER_AVATAR_EVENT))
}

export function clearCurrentUserAvatar(): void {
  localStorage.removeItem(CURRENT_USER_AVATAR_KEY)
  window.dispatchEvent(new Event(CURRENT_USER_AVATAR_EVENT))
}

export function getAvatarForUser(user: AvatarUser, stored = readCurrentUserAvatar()): string {
  return stored?.identity === getAvatarUserIdentity(user)
    ? stored.src
    : getAvatar(getAvatarUserLabel(user))
}

export function subscribeCurrentUserAvatar(callback: () => void): () => void {
  const onStorage = (event: StorageEvent) => {
    if (event.key === CURRENT_USER_AVATAR_KEY) callback()
  }
  window.addEventListener(CURRENT_USER_AVATAR_EVENT, callback)
  window.addEventListener("storage", onStorage)
  return () => {
    window.removeEventListener(CURRENT_USER_AVATAR_EVENT, callback)
    window.removeEventListener("storage", onStorage)
  }
}

export { generateAvatar, type AvatarOptions };
