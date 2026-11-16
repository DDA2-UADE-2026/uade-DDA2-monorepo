FROM alpine:3.22

# Intentionally root: this local one-shot job prepares/migrates volume ownership
# for distinct service UIDs before their non-root runtimes start.
# Generates the local CA and service certificates without requiring host tools.
RUN apk add --no-cache openssl
COPY init/init.sh /init.sh
COPY definitions.json /definitions.json
ENTRYPOINT ["sh", "/init.sh"]
