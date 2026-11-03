#!/bin/sh
set -eu
# Only the local Compose command uses this CA; production keeps the normal entrypoint.
keytool -delete -alias dda2-local -cacerts -storepass changeit >/dev/null 2>&1 || true
keytool -importcert -noprompt -alias dda2-local -cacerts -storepass changeit \
    -file /run/secrets/ca.crt >/dev/null 2>&1
exec java -jar /app/app.jar
