plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.example"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.mongodb.bson)
    implementation(libs.mongodb.driverCore)
    implementation(libs.mongodb.driverSync)
    implementation(ktorLibs.server.di)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}

tasks.test {
    exclude("**/UserApiEndToEndTest.class")
}

tasks.register<Test>("e2eTest") {
    group = "verification"
    description = "Verifies the user API over HTTP against a running MongoDB instance."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    include("**/UserApiEndToEndTest.class")
    outputs.upToDateWhen { false }
}
