import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion = "1.8.22" // keep in sync with plugin version
val ktorVersion = "2.2.4"
val logbackVersion = "1.2.5"
val jdbiVersion = "3.14.4"

plugins {
    kotlin("jvm") version "1.8.22"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    application
}

group = "sh.zachwal"

repositories {
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(platform("org.jdbi:jdbi3-bom:$jdbiVersion"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-server-sessions:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-css-jvm:1.0.0-pre.265-kotlin-1.5.31")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    // Observability
    implementation("io.sentry:sentry:6.17.0")

    // Reflection (for @Controller annotation inspection)
    implementation("org.reflections:reflections:0.10.2")

    // database
    implementation("com.zaxxer:HikariCP:3.4.5")
    implementation("org.postgresql:postgresql:42.1.4")
    implementation("org.jdbi:jdbi3-core")
    implementation("org.jdbi:jdbi3-kotlin")
    implementation("org.jdbi:jdbi3-postgres")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject")

    // passwords
    implementation("org.mindrot:jbcrypt:0.4")

    // DI
    implementation("com.google.inject:guice:6.0.0")

    // Twilio
    implementation("com.twilio.sdk:twilio:8.32.0")

    // CSV
    implementation("com.opencsv:opencsv:5.9")

    // Guava cache
    implementation("com.google.guava:guava:33.4.0-jre")

    // Use the Kotlin test library.
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")

    // For testing Ktor & websockets
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-websockets:$ktorVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("com.google.truth:truth:1.1.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.7")
    testImplementation("org.testcontainers:postgresql:1.19.7")
    testImplementation("org.liquibase:liquibase-core:4.27.0")
    testImplementation("org.jdbi:jdbi3-jackson2:$jdbiVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.13.5")
}

application {
    // Define the main class for the application.
    mainClass.set("sh.zachwal.button.AppKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED"
    )
}

// Frontend integration tasks: copy frontend assets
tasks.register<Sync>("copyFrontend") {
    from("frontend/src/main")
    into("src/main/resources/static/src")
}

// Ensure processResources task picks up latest frontend files.
// assemble, run, etc. all depend on processResources
tasks.named("processResources") {
    dependsOn("copyFrontend")
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
