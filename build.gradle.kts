import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.32"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.1"
    application
}

group = "sh.zachwal"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    // Ktor
    val ktorVersion = "1.6.7"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-html-builder:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.2.5")

    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")

    // for testing many simultaneous websockets
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-client-websockets:$ktorVersion")

    testImplementation("io.mockk:mockk:1.12.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("sh.zachwal.button.ServerKt")
}
