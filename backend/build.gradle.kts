plugins {
    java
    id("org.springframework.boot") version "3.4.0"
    id("io.spring.dependency-management") version "1.1.6"
    jacoco
}

group = "com.gametrend"
version = "0.0.1-SNAPSHOT"
description = "GameTrend-Insight"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

val springAiVersion = "1.0.0-M5"
val resilience4jVersion = "2.2.0"
val springdocVersion = "2.7.0"
val testcontainersVersion = "1.20.3"
val wiremockVersion = "3.9.2"

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux") // WebClient only
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-batch")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Spring AI (Anthropic Claude)
    implementation("org.springframework.ai:spring-ai-anthropic-spring-boot-starter")

    // JWT (W4 D4 — P4 워치리스트 + 인증)
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // 통계 (W5 D3 — 시차 상관, 회귀 분석 등)
    implementation("org.apache.commons:commons-math3:3.6.1")

    // .env 자동 로드 (Spring Boot 는 기본적으로 .env 미지원)
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Resilience4j (Circuit Breaker)
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-reactor:$resilience4jVersion")

    // Metrics (Prometheus)
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.batch:spring-batch-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.wiremock:wiremock-standalone:$wiremockVersion")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        html.required = true
        xml.required = true
    }
}
