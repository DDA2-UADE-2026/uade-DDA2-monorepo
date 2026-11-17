plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.9"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("plugin.lombok") version "2.2.21"
}

group = "com.uade.dda2"
version = "0.0.1-SNAPSHOT"

// Override oficial del BOM de Boot, aplicado a todos los classpaths administrados.
extra["rabbit-amqp-client.version"] = "5.37.0"
// Actualiza la familia OpenTelemetry usada por el tooling SwiftExport del plugin Kotlin (fuera del runtime).
extra["opentelemetry.version"] = "1.67.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencyLocking {
    lockAllConfigurations()
}

// Sólo el classpath de los helpers PGP de Kotlin: BC no se agrega al runtime de la aplicación.
// dependency-management restaura las versiones explícitas de las defaults del plugin;
// esta regla local las alinea sin agregar dependencias ni cambiar la gestión global.
configurations.named("kotlinBouncyCastleConfiguration") {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.bouncycastle" && requested.name in setOf(
                "bcpg-jdk18on", "bcpkix-jdk18on", "bcprov-jdk18on", "bcutil-jdk18on"
            )) {
            useVersion("1.87")
            because("Corrige vulnerabilidades del tooling Kotlin manteniendo los cuatro módulos BC alineados.")
        }
    }
}

dependencies {
    // ─────────────────────────────────────────────
    // Spring Boot - Web / API REST
    // ─────────────────────────────────────────────

    // Spring MVC: controllers REST, HTTP, JSON, Tomcat, etc.
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    // Core: RabbitMQ con confirms y ack manual; versión administrada por Spring Boot.
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // AsyncAPI desde tipos/anotaciones AMQP y visor; 2.5.x mantiene la línea Boot 4.0.x.
    implementation("io.github.springwolf:springwolf-amqp:2.5.0")
    runtimeOnly("io.github.springwolf:springwolf-ui:2.5.0")

    // Validaciones con Jakarta Validation:
    // @NotNull, @NotBlank, @Size, @Positive, etc.
    implementation("org.springframework.boot:spring-boot-starter-validation")


    // ─────────────────────────────────────────────
    // Spring Boot - Seguridad
    // ─────────────────────────────────────────────

    // Spring Security:
    // autenticación, autorización, filtros, @PreAuthorize, etc.
    implementation("org.springframework.boot:spring-boot-starter-security")


    // ─────────────────────────────────────────────
    // JWT
    // ─────────────────────────────────────────────

    // API pública de JJWT.
    // Proporciona Jwts, Claims, JwtParser, Keys, etc.
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")

    // Implementación interna de JJWT.
    // Solo es necesaria en runtime.
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")

    // Serialización/deserialización JSON utilizada internamente por JJWT.
    // Usamos Gson para evitar mezclar Jackson 2 con Jackson 3.
    runtimeOnly("io.jsonwebtoken:jjwt-gson:0.13.0")


    // ─────────────────────────────────────────────
    // Persistencia / Base de datos
    // ─────────────────────────────────────────────

    // Spring Data JPA + Hibernate.
    // Repositories, @Entity, EntityManager, transacciones, etc.
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // Driver JDBC de PostgreSQL.
    // Solo es necesario cuando la aplicación está ejecutándose.
    runtimeOnly("org.postgresql:postgresql")


    // ─────────────────────────────────────────────
    // JSON
    // ─────────────────────────────────────────────

    // Soporte de Jackson 3 para Kotlin.
    // Permite serializar/deserializar data classes correctamente.
    implementation("tools.jackson.module:jackson-module-kotlin")


    // ─────────────────────────────────────────────
    // Kotlin
    // ─────────────────────────────────────────────

    // Reflection de Kotlin.
    // Spring la utiliza para inspeccionar clases, constructores,
    // parámetros, anotaciones, nullability, etc.
    implementation("org.jetbrains.kotlin:kotlin-reflect")


    // ─────────────────────────────────────────────
    // OpenAPI / Swagger
    // ─────────────────────────────────────────────

    // Generación automática de documentación OpenAPI
    // y Swagger UI para los endpoints REST.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")


    // ─────────────────────────────────────────────
    // Monitoring / Actuator
    // ─────────────────────────────────────────────

    // Endpoints operativos de Spring Boot:
    // /actuator/health, metrics, info, etc.
    implementation("org.springframework.boot:spring-boot-starter-actuator")


    // ─────────────────────────────────────────────
    // Desarrollo
    // ─────────────────────────────────────────────

    // Reinicio automático y herramientas útiles durante desarrollo.
    // No se incluye en el artefacto de producción.
    developmentOnly("org.springframework.boot:spring-boot-devtools")


    // ─────────────────────────────────────────────
    // Lombok
    // ─────────────────────────────────────────────

    // Permite utilizar anotaciones de Lombok durante compilación.
    compileOnly("org.projectlombok:lombok")

    // Procesador de anotaciones de Lombok.
    annotationProcessor("org.projectlombok:lombok")


    // ─────────────────────────────────────────────
    // Testing - Spring Boot
    // ─────────────────────────────────────────────

    // Herramientas de testing para Actuator.
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")

    // Herramientas de testing para JPA y repositories.
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")

    // Spring Security Test:
    // usuarios mock, autenticación mock, CSRF, etc.
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")

    // Soporte de validaciones durante tests.
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")

    // Testing de controllers Spring MVC / MockMvc.
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")


    // ─────────────────────────────────────────────
    // Testing - Kotlin / JUnit
    // ─────────────────────────────────────────────

    // Integración de kotlin.test con JUnit 5.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Launcher de JUnit Platform requerido para ejecutar tests.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")


    // ─────────────────────────────────────────────
    // Testing - Lombok
    // ─────────────────────────────────────────────

    // Lombok disponible durante la compilación de tests.
    testCompileOnly("org.projectlombok:lombok")

    // Procesador de anotaciones Lombok para código de tests.
    testAnnotationProcessor("org.projectlombok:lombok")


    // ─────────────────────────────────────────────
    // Testing - Generación de documentación OpenAPI
    // ─────────────────────────────────────────────

    // Base de datos en memoria usada solo para levantar el contexto de
    // Spring bajo el perfil "docs" (ver OpenApiDocsGenerationTest), para
    // no depender de una Postgres real al generar el spec en CI.
    developmentOnly("com.h2database:h2")
    testRuntimeOnly("com.h2database:h2")
    testImplementation(kotlin("test"))

    // Real disposable PostgreSQL and RabbitMQ services for integration tests.
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.5"))
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-rabbitmq")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Test content stays UTF-8 even when the Windows launcher needs a native-encoded classpath file.
    defaultCharacterEncoding = "UTF-8"
    // Bound retained Hibernate contexts across the full flow suite instead of exhausting the default test heap.
    maxHeapSize = "1g"
    systemProperty("spring.test.context.cache.maxSize", "8")
}

tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("core-smoke", "testcontainers") }
}

val testM1Integration by tasks.registering(Test::class) {
    group = "verification"
    description = "Verifica M1 con HTTP loopback, H2 y adaptadores locales; genera OpenAPI/AsyncAPI sin contactar a M1 ni Core."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { excludeTags("core-smoke", "testcontainers") }
    filter {
        // Lista explícita: no incorporar pruebas remotas nuevas mediante un comodín.
        listOf(
            "feature.integration.M1AuthClientTest",
            "feature.integration.M1AuthConfigurationTest",
            "feature.auth.M1CitizenAccountServiceTest",
            "feature.auth.M1SessionFlowTest",
            "feature.auth.M1DisabledSessionFlowTest",
            "feature.integration.M1DocumentClientTest",
            "feature.integration.M1DocumentsConfigurationTest",
            "feature.integration.ExternalDocumentFileFlowTest",
            "feature.integration.ExternalDocumentationFlowTest",
            "feature.integration.M8ContractCodecTest",
            "feature.integration.M8BusinessFlowTest",
            "feature.auth.AuthFlowTest",
            "feature.auth.CookieAuthFlowTest",
            "feature.auth.CitizenModelTest",
            "security.JwtServiceTest",
            "security.LoginRateLimitFilterTest",
            "OpenApiDocsGenerationTest",
            "AsyncApiDocsGenerationTest",
        ).forEach { includeTestsMatching("com.uade.dda2.server.$it") }
    }
    // Evitar que flags heredados del despliegue habiliten transportes. Los tests
    // que requieren HTTP optan explícitamente por un servidor efímero loopback.
    listOf("CORE_ENABLED", "CORE_BUSINESS_ENABLED", "CORE_AMQP_CHECK_ON_STARTUP", "CORE_PUBLISHER_ENABLED",
        "CORE_CONSUMER_ENABLED", "CORE_SMOKE_ENABLED", "M1_AUTH_ENABLED", "M1_DOCUMENTS_ENABLED")
        .forEach { environment(it, "false") }
    inputs.files(
        "docs/external/m1-auth/login-success.json",
        "docs/external/m1-auth/login-invalid-credentials.json",
        "docs/postman/m1-session.postman_collection.json",
        "docs/postman/m1-documents.postman_collection.json",
    )
    outputs.dir(layout.buildDirectory.dir("openapi"))
    outputs.dir(layout.buildDirectory.dir("asyncapi"))
}

tasks.register("verifyM1Integration") {
    group = "verification"
    description = "Cierre local de M1: pruebas seleccionadas, contratos OpenAPI/AsyncAPI, colecciones Postman y bootJar."
    dependsOn(testM1Integration, tasks.named("bootJar"))
}

tasks.register<Test>("testContainers") {
    group = "verification"
    description = "Integration tests against disposable PostgreSQL and RabbitMQ containers."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("testcontainers") }
    maxHeapSize = "1g"
}

tasks.register<Test>("testCoreConnectivity") {
    group = "verification"
    description = "Prueba opt-in contra Core; requiere accesos e IP habilitada y una base técnica desechable."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("core-smoke") }
    doNotTrackState("La prueba usa estado remoto y UUIDs de una ejecución deliberada; no puede reutilizar resultados en caché.")
    doFirst {
        require(System.getenv("CORE_SMOKE_ENABLED") == "true") {
            "La prueba remota requiere CORE_SMOKE_ENABLED=true y configuración privada explícita."
        }
    }
}

tasks.register<Test>("generateOpenApiDocs") {
    group = "documentation"
    description =
        "Levanta el contexto de Spring con el perfil 'docs' (H2) y vuelca el spec de OpenAPI en build/openapi/openapi.json"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter {
        includeTestsMatching("com.uade.dda2.server.OpenApiDocsGenerationTest")
    }
    outputs.dir(layout.buildDirectory.dir("openapi"))
}

tasks.register<Test>("testApplicationsPostgres") {
    group = "verification"
    description =
        "Prueba solicitudes contra un PostgreSQL desechable en 127.0.0.1:55439/application_test (crea y borra ese esquema)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("com.uade.dda2.server.feature.application.ApplicationFlowTest") }
    systemProperty("application.test.postgres-url", "jdbc:postgresql://127.0.0.1:55439/application_test")
    systemProperty(
        "application.test.postgres-password",
        providers.environmentVariable("APPLICATION_TEST_POSTGRES_PASSWORD").orElse("").get()
    )
}

tasks.register<Test>("testApplicationResolutionsPostgres") {
    group = "verification"
    description = "Prueba resoluciones y concurrencia contra PostgreSQL desechable en 127.0.0.1:55439/application_resolution_test (create-drop)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("com.uade.dda2.server.feature.application.ApplicationResolutionFlowTest") }
    systemProperty("resolution.test.postgres-url", "jdbc:postgresql://127.0.0.1:55439/application_resolution_test")
    systemProperty("resolution.test.postgres-user", "application_test")
    systemProperty("resolution.test.postgres-password", providers.environmentVariable("APPLICATION_TEST_POSTGRES_PASSWORD").orElse("").get())
}

tasks.register<Test>("testBenefitGrantsPostgres") {
    group = "verification"
    description = "Prueba otorgamientos contra PostgreSQL desechable en 127.0.0.1:55439/benefit_grant_test (create-drop)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("com.uade.dda2.server.feature.benefit.BenefitGrantFlowTest") }
    systemProperty("benefit-grant.test.postgres-url", "jdbc:postgresql://127.0.0.1:55439/benefit_grant_test")
    systemProperty("benefit-grant.test.postgres-user", "application_test")
    systemProperty("benefit-grant.test.postgres-password", providers.environmentVariable("APPLICATION_TEST_POSTGRES_PASSWORD").orElse("").get())
}

tasks.register<Test>("testInterventionPlansPostgres") {
    group = "verification"
    description = "Prueba planes y seguimientos contra PostgreSQL desechable en 127.0.0.1:55439/intervention_plan_test (create-drop)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("com.uade.dda2.server.feature.intervention.InterventionPlanFlowTest") }
    systemProperty("intervention.test.postgres-url", "jdbc:postgresql://127.0.0.1:55439/intervention_plan_test")
    systemProperty("intervention.test.postgres-user", "application_test")
    systemProperty("intervention.test.postgres-password", providers.environmentVariable("APPLICATION_TEST_POSTGRES_PASSWORD").orElse("").get())
}

tasks.register<Test>("testCentersPostgres") {
    group = "verification"
    description =
        "Prueba concurrencia de centros contra PostgreSQL local en 127.0.0.1:5433/center_test_real (clon de backup, usa update, no borra el backup)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("com.uade.dda2.server.feature.center.CenterConcurrencyFlowTest") }
    systemProperty(
        "center.test.postgres-url",
        providers.environmentVariable("CENTER_TEST_POSTGRES_URL").orElse("jdbc:postgresql://127.0.0.1:5433/center_test_real").get()
    )
    systemProperty(
        "center.test.postgres-user",
        providers.environmentVariable("CENTER_TEST_POSTGRES_USER").orElse("postgres").get()
    )
    systemProperty(
        "center.test.postgres-password",
        providers.environmentVariable("CENTER_TEST_POSTGRES_PASSWORD").orElse("postgres").get()
    )
    systemProperty(
        "center.test.ddl-auto",
        providers.environmentVariable("CENTER_TEST_DDL_AUTO").orElse("update").get()
    )
}

tasks.register<Test>("testAppointmentsPostgres") {
    group = "verification"
    description =
        "Prueba concurrencia de turnos contra un PostgreSQL desechable en 127.0.0.1:55439/appointment_test (create-drop)."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("com.uade.dda2.server.feature.appointment.AppointmentConcurrencyFlowTest") }
    systemProperty("appointment.test.postgres-url", "jdbc:postgresql://127.0.0.1:55439/appointment_test")
    systemProperty(
        "appointment.test.postgres-user",
        providers.environmentVariable("APPOINTMENT_TEST_POSTGRES_USER").orElse("appointment_test").get()
    )
    systemProperty(
        "appointment.test.postgres-password",
        providers.environmentVariable("APPOINTMENT_TEST_POSTGRES_PASSWORD").orElse("").get()
    )
}

tasks.register<Test>("generateAsyncApiDocs") {
    group = "documentation"
    description = "Genera AsyncAPI desde los adaptadores y data classes M8 en build/asyncapi/asyncapi.json, sin Core remoto."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching("com.uade.dda2.server.AsyncApiDocsGenerationTest") }
    outputs.dir(layout.buildDirectory.dir("asyncapi"))
}
