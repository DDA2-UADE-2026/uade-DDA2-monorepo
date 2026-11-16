#!/bin/sh
set -eu
umask 077
# Only local Compose uses this CA. Keep the JDK truststore read-only for the non-root user.
truststore_dir="$(mktemp -d "${TMPDIR:-/tmp}/dda2-truststore.XXXXXX")"
truststore="$truststore_dir/cacerts"
cp "$JAVA_HOME/lib/security/cacerts" "$truststore"
chmod 600 "$truststore"
keytool -delete -alias dda2-local -keystore "$truststore" -storepass changeit >/dev/null 2>&1 || true
keytool -importcert -noprompt -alias dda2-local -keystore "$truststore" -storepass changeit \
    -file /run/secrets/ca.crt >/dev/null 2>&1
exec java -Djavax.net.ssl.trustStore="$truststore" -Djavax.net.ssl.trustStorePassword=changeit \
    -jar /app/app.jar
