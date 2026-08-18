plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.7"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com.uade.dda2"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// ─────────────────────────────────────────────
	// Spring Boot - Web / API REST
	// ─────────────────────────────────────────────

	// Spring MVC: controllers REST, HTTP, JSON, Tomcat, etc.
	implementation("org.springframework.boot:spring-boot-starter-webmvc")

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
	testRuntimeOnly("com.h2database:h2")
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
}

tasks.register<Test>("generateOpenApiDocs") {
	group = "documentation"
	description = "Levanta el contexto de Spring con el perfil 'docs' (H2) y vuelca el spec de OpenAPI en build/openapi/openapi.json"
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	filter {
		includeTestsMatching("com.uade.dda2.server.OpenApiDocsGenerationTest")
	}
	outputs.dir(layout.buildDirectory.dir("openapi"))
}
