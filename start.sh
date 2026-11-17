#!/bin/sh
# Docker is the only application runtime required on the host.
set -eu
set +x
umask 077
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
cd "$ROOT"
fail() { printf '%s\n' "$1" >&2; exit 1; }
command -v docker >/dev/null 2>&1 || fail 'Docker no está instalado.'
docker info >/dev/null 2>&1 || fail 'Docker no está disponible; iniciá el daemon.'
docker compose version >/dev/null 2>&1 || fail 'Se requiere Docker Compose v2.'
# Compose interpolation must use .env, not inherited credential overrides.
unset DB_PASSWORD RABBITMQ_PASSWORD JWT_SECRET CORE_MACHINE_SECRET BOOTSTRAP_ADMIN_PASSWORD
unset COMPOSE_ENV_FILES COMPOSE_DISABLE_ENV_FILE
[ ! -L .env ] || fail 'Se rechaza .env: es un enlace simbólico.'
tmp=
tty_state=
cleanup() {
    if [ -n "$tty_state" ]; then stty "$tty_state" <&3; fi
    if [ -n "$tmp" ]; then
        # Validate the private mktemp path before removing anything.
        case "$tmp" in "$ROOT"/.start.????????) ;; *) return 1 ;; esac
        rm -f -- "$tmp/.env"
        rmdir -- "$tmp"
    fi
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
trap 'exit 129' HUP
if [ -e .env ]; then
    [ -f .env ] || fail '.env debe ser un archivo regular.'
else
    # Remove inherited export attributes before storing interactive secrets.
    unset admin confirmation
    exec 3<>/dev/tty || fail 'Se requiere una terminal para configurar el administrador.'
    tty_state=$(stty -g <&3) || fail 'No se pudo leer la terminal.'
    stty -echo <&3
    printf 'Contraseña admin (mínimo 12 caracteres): ' >&3
    IFS= read -r admin <&3 || fail 'No se pudo leer la contraseña.'
    printf '\nConfirmar contraseña: ' >&3
    IFS= read -r confirmation <&3 || fail 'No se pudo leer la confirmación.'
    printf '\n' >&3
    stty "$tty_state" <&3
    tty_state=
    [ "$admin" = "$confirmation" ] || fail 'Las contraseñas no coinciden.'
    [ "${#admin}" -ge 12 ] || fail 'La contraseña requiere al menos 12 caracteres.'
    tmp=$(mktemp -d "$ROOT/.start.XXXXXXXX") || fail 'No se pudo crear el archivo temporal.'
    # The password travels only on stdin; never in argv or exported variables.
    if ! printf '%s' "$admin" | docker run --rm -i python:3.13-alpine python -c '
import secrets, sys
password = sys.stdin.read()
if len(password) < 12 or any(c in password for c in "\r\n\0"):
    sys.exit(1)
for name in ("DB_PASSWORD", "RABBITMQ_PASSWORD", "JWT_SECRET", "CORE_MACHINE_SECRET"):
    print(name + "=" + secrets.token_hex(32))
# Double quoting handles trailing backslashes and backslash/quote sequences.
# Compose uses $$ for a literal dollar, independently of host interpolation.
password = password.replace("\\", "\\\\").replace(chr(34), "\\" + chr(34)).replace("$", "$$")
print("BOOTSTRAP_ADMIN_PASSWORD=" + chr(34) + password + chr(34))
' > "$tmp/.env"; then
        fail 'No se pudo generar .env.'
    fi
    unset admin confirmation
    chmod 600 "$tmp/.env"
    # Hard-link publication is atomic and must not replace any existing path.
    # BSD ln uses -h; GNU ln uses -T to avoid following destination directories.
    case $(uname -s) in
        Darwin) ln -h "$tmp/.env" "$ROOT/.env" ;;
        *) ln -T "$tmp/.env" "$ROOT/.env" ;;
    esac || fail 'No se pudo publicar .env sin sobrescribir.'
fi
cleanup
tmp=
docker compose up --build --wait --wait-timeout 300
printf 'La aplicación ya está corriendo en http://localhost:5173\n'
