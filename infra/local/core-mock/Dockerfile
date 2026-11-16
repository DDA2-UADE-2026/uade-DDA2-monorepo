FROM python:3.13-alpine
RUN addgroup -S -g 10002 core \
    && adduser -S -D -H -u 10002 -G core core
ENV PYTHONDONTWRITEBYTECODE=1
WORKDIR /app
COPY --chmod=0444 server.py test_server.py ./
USER 10002:10002
EXPOSE 8443
CMD ["python", "server.py"]
