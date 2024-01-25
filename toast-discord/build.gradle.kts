plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.10"
    java
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()

    maven {
        url = uri("http://23.95.107.12:9999/releases")
        isAllowInsecureProtocol = true
    }
}

project.group = "com.github.kennarddh.mindustry"
project.version = project.file("version.txt").readLines()[0]

val exposedVersion: String = "0.46.0"

dependencies {
    implementation(project(":toast-common"))

    implementation(platform("org.jetbrains.exposed:exposed-bom:0.46.0"))
    implementation("org.jetbrains.exposed:exposed-java-time")
    implementation("org.jetbrains.exposed:exposed-core")
    implementation("org.jetbrains.exposed:exposed-jdbc")

    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")
    implementation("com.zaxxer:HikariCP:5.1.0")

    implementation("com.password4j:password4j:1.7.3")

    implementation("com.rabbitmq:amqp-client:5.20.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")

    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude(module = "opus-java")
    }
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
    }
}

tasks {
    jar {
        manifest {
            attributes(
                mapOf(
                    "Multi-Release" to "true",
                    "Main-Class" to "com.github.kennarddh.mindustry.toast.discord.ToastBotKt"
                )
            )
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(sourceSets.main.get().output)
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}

publishing {
    repositories {
        maven {
            name = "reposilite"
            url = uri("http://23.95.107.12:9999/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
            isAllowInsecureProtocol = true
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = groupId
            artifactId = "toast-discord"
            version = version
            from(components["java"])
        }
    }
}

tasks.register("getArtifactPath") {
    doLast { println(tasks.jar.get().archiveFile.get().toString()) }
}

tasks.register<JavaExec>("runBot") {
    environment("DB_HOST", "jdbc:mariadb://localhost:3307/toast")
    environment("DB_USERNAME", "root")
    environment("DB_PASSWORD", "root")
    environment("RABBITMQ_URI", "amqp://root:root@localhost:5672")

    doFirst {
        file(project.file(".env")).readLines().forEach {
            val (key, value) = it.split('=')
            environment(key, value)
        }
    }

    workingDir = temporaryDir

    dependsOn(tasks.jar)
    classpath(tasks.jar)

    description = "Starts a local discord bot"
    standardInput = System.`in`
}