plugins {
    kotlin("jvm") version "2.4.20"
    id("io.ktor.plugin") version "3.6.0"
}

group = "com.github.nizienko"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-websockets")
}

kotlin {
    jvmToolchain(18)
}

application {
    mainClass.set("com.github.nizienko.roboarm.MainKt")
}

tasks.test {
    useJUnitPlatform()
}