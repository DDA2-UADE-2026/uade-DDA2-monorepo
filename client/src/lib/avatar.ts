interface AvatarOptions {
  size?: number;
  fontSize?: number;
  textColor?: string;
  seed?: string;
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
const AVATAR_STYLE_VERSION = 3

let cachedStoredAvatarValue: string | null | undefined
let cachedStoredAvatar: StoredAvatar | null = null

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

function hashText(text: string): number {
  let hash = 0x811c9dc5;
  const normalized = text.trim().toLowerCase();

  for (let i = 0; i < normalized.length; i++) {
    hash ^= normalized.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }

  // Avalanche the bits so similar names do not produce neighboring colors.
  hash += hash << 13;
  hash ^= hash >>> 7;
  hash += hash << 3;
  hash ^= hash >>> 17;
  hash += hash << 5;
  return hash >>> 0;
}

function createSeededRandom(seed: number): () => number {
  let state = seed;
  return () => {
    state += 0x6d2b79f5;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function textToColorDefs(text: string): [ColorDef, ColorDef, ColorDef] {
  const random = createSeededRandom(hashText(text));
  const baseHue = Math.floor(random() * 360);
  const accentHue = (baseHue + 70 + Math.floor(random() * 71)) % 360;
  const contrastHue = (baseHue + 200 + Math.floor(random() * 81)) % 360;

  return [
    {
      h: baseHue,
      s: 62 + Math.floor(random() * 24),
      l: 36 + Math.floor(random() * 13),
    },
    {
      h: accentHue,
      s: 68 + Math.floor(random() * 23),
      l: 46 + Math.floor(random() * 13),
    },
    {
      h: contrastHue,
      s: 64 + Math.floor(random() * 27),
      l: 43 + Math.floor(random() * 14),
    },
  ];
}

function generateAvatar(text: string, options: AvatarOptions = {}): string {
  const { size = 128, fontSize, textColor = "#ffffff", seed = text } = options;

  const canvas = document.createElement("canvas");
  canvas.width = size;
  canvas.height = size;
  const ctx = canvas.getContext("2d");
  if (!ctx) throw new Error("Could not get 2D context");

  const colors = textToColorDefs(seed);
  const layoutRandom = createSeededRandom(hashText(`${seed}\0layout`));

  // Fondo base
  ctx.fillStyle = hsla(colors[0], 1);
  ctx.fillRect(0, 0, size, size);

  // Blobs con radialGradient
  const blobs: Array<{ x: number; y: number; r: number; color: ColorDef }> = [
    {
      x: size * (0.15 + layoutRandom() * 0.7),
      y: size * (0.15 + layoutRandom() * 0.7),
      r: size * (0.5 + layoutRandom() * 0.2),
      color: colors[1],
    },
    {
      x: size * (0.15 + layoutRandom() * 0.7),
      y: size * (0.15 + layoutRandom() * 0.7),
      r: size * (0.45 + layoutRandom() * 0.2),
      color: colors[2],
    },
    {
      x: size * (0.15 + layoutRandom() * 0.7),
      y: size * (0.15 + layoutRandom() * 0.7),
      r: size * (0.35 + layoutRandom() * 0.2),
      color: colors[0],
    },
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
  ctx.fillText(initials, (size / 2), (size / 2)+4);

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
  if (value === cachedStoredAvatarValue) return cachedStoredAvatar

  cachedStoredAvatarValue = value
  if (!value) {
    cachedStoredAvatar = null
    return cachedStoredAvatar
  }

  try {
    const stored = JSON.parse(value) as StoredAvatar
    cachedStoredAvatar = stored.version === AVATAR_STYLE_VERSION && stored.identity && stored.src
      ? stored
      : null
  } catch {
    cachedStoredAvatar = null
  }

  return cachedStoredAvatar
}

export function saveCurrentUserAvatar(user: AvatarUser): void {
  const identity = getAvatarUserIdentity(user)
  const stored: StoredAvatar = {
    version: AVATAR_STYLE_VERSION,
    identity,
    src: getAvatar(getAvatarUserLabel(user), { seed: identity }),
  }
  localStorage.setItem(CURRENT_USER_AVATAR_KEY, JSON.stringify(stored))
  window.dispatchEvent(new Event(CURRENT_USER_AVATAR_EVENT))
}

export function clearCurrentUserAvatar(): void {
  localStorage.removeItem(CURRENT_USER_AVATAR_KEY)
  window.dispatchEvent(new Event(CURRENT_USER_AVATAR_EVENT))
}

export function getAvatarForUser(user: AvatarUser, stored = readCurrentUserAvatar()): string {
  const identity = getAvatarUserIdentity(user)
  return stored?.identity === identity
    ? stored.src
    : getAvatar(getAvatarUserLabel(user), { seed: identity })
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
