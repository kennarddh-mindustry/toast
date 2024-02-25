plugins {
    kotlin("jvm") version "1.9.22"
}

group = "com.github.kennarddh.mindustry"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://maven.xpdustry.com/releases")
    maven("https://maven.xpdustry.com/anuken")

    maven {
        url = uri("http://23.95.107.12:9999/releases")
        isAllowInsecureProtocol = true
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "maven-publish")

    project.group = "com.github.kennarddh.mindustry"

    repositories {
        mavenCentral()
        maven("https://maven.xpdustry.com/releases")
        maven("https://maven.xpdustry.com/anuken")

        maven {
            url = uri("http://23.95.107.12:9999/releases")
            isAllowInsecureProtocol = true
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    sourceSets {
        main {
            java.srcDir("src/main/kotlin")
        }
    }

    dependencies {
        implementation(platform("org.jetbrains.exposed:exposed-bom:0.47.0"))
        implementation("org.jetbrains.exposed:exposed-kotlin-datetime")
        implementation("org.jetbrains.exposed:exposed-core")
        implementation("org.jetbrains.exposed:exposed-jdbc")

        implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")
        implementation("com.zaxxer:HikariCP:5.1.0")

        implementation("com.password4j:password4j:1.7.3")

        implementation("com.rabbitmq:amqp-client:5.20.0")

        implementation("io.github.domgew:kedis-jvm:0.0.2")

        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
    }
}