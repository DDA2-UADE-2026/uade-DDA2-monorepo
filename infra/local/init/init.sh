#!/bin/sh
set -eu
umask 077

: "${DB_PASSWORD:?Falta DB_PASSWORD en .env.}"
: "${RABBITMQ_PASSWORD:?Falta RABBITMQ_PASSWORD en .env.}"
: "${JWT_SECRET:?Falta JWT_SECRET en .env.}"
: "${BOOTSTRAP_ADMIN_PASSWORD:?Falta BOOTSTRAP_ADMIN_PASSWORD en .env.}"
: "${CORE_MACHINE_SECRET:?Falta CORE_MACHINE_SECRET en .env.}"

if [ "${#JWT_SECRET}" -lt 32 ]; then
    echo 'JWT_SECRET debe tener al menos 32 caracteres ASCII.' >&2
    exit 1
fi
if [ "${#BOOTSTRAP_ADMIN_PASSWORD}" -lt 12 ]; then
    echo 'BOOTSTRAP_ADMIN_PASSWORD debe tener al menos 12 caracteres.' >&2
    exit 1
fi

secret() {
    name="$1"
    value="$2"
    destination="$3"
    if [ -s "$destination" ] && [ "$(cat "$destination")" != "$value" ]; then
        echo "$name no coincide con el entorno inicializado. Conserva el valor original de .env o usa un proyecto Docker nuevo; no se modificaron las credenciales existentes." >&2
        exit 1
    fi
    printf '%s' "$value" > "$destination"
}

secret DB_PASSWORD "$DB_PASSWORD" /db/password
secret RABBITMQ_PASSWORD "$RABBITMQ_PASSWORD" /rabbit/password
secret JWT_SECRET "$JWT_SECRET" /server/JWT_SECRET
secret BOOTSTRAP_ADMIN_PASSWORD "$BOOTSTRAP_ADMIN_PASSWORD" /server/BOOTSTRAP_ADMIN_PASSWORD
secret CORE_MACHINE_SECRET "$CORE_MACHINE_SECRET" /core/machine-secret
cp /db/password /server/DB_PASSWORD
cp /rabbit/password /server/CORE_AMQP_PASSWORD
cp /core/machine-secret /server/CORE_MACHINE_SECRET

if [ ! -s /ca/ca.crt ]; then
    openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
        -keyout /ca/ca.key -out /ca/ca.crt -subj '/CN=DDA2 local development CA' \
        -addext 'basicConstraints=critical,CA:TRUE' \
        -addext 'keyUsage=critical,keyCertSign,cRLSign' 2>/dev/null
fi

certificate() {
    directory="$1"
    hostname="$2"
    if [ ! -s "$directory/tls.crt" ]; then
        openssl req -new -newkey rsa:2048 -nodes -keyout "$directory/tls.key" \
            -out /tmp/service.csr -subj "/CN=$hostname" 2>/dev/null
        printf 'subjectAltName=DNS:%s,DNS:localhost,IP:127.0.0.1\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid,issuer\n' "$hostname" > /tmp/extensions
        openssl x509 -req -in /tmp/service.csr -CA /ca/ca.crt -CAkey /ca/ca.key \
            -CAcreateserial -out "$directory/tls.crt" -days 365 \
            -extfile /tmp/extensions 2>/dev/null
    fi
    cp /ca/ca.crt "$directory/ca.crt"
}

certificate /rabbit rabbitmq
certificate /core core-mock
cp /ca/ca.crt /server/ca.crt
# RabbitMQ drops root privileges; its private volume is granted only to this service.
chmod 644 /rabbit/tls.key /rabbit/tls.crt /rabbit/ca.crt
openssl rand 4 > /tmp/salt
(cat /tmp/salt; cat /rabbit/password) | openssl dgst -sha256 -binary > /tmp/digest
password_hash=$(cat /tmp/salt /tmp/digest | openssl base64 -A)
sed "s|GENERATED_PASSWORD_HASH|$password_hash|" /definitions.json > /rabbit/definitions.json
chmod 644 /rabbit/definitions.json
echo 'Preparación local completada. Secretos conservados en volúmenes privados de Docker.'
