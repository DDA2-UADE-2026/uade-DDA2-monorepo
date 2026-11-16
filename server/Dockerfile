FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts gradle.lockfile ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:17-jdk-jammy

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home \
        --home-dir /app --shell /usr/sbin/nologin app

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY local-entrypoint.sh /app/local-entrypoint.sh

EXPOSE 8080

USER 10001:10001

ENTRYPOINT ["java", "-jar", "app.jar"]
