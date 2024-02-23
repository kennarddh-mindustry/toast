plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

project.version = project.file("version.txt").readLines()[0]

dependencies {
    implementation(project(":toast-common"))

    implementation("com.xpdustry:kotlin-runtime:3.1.1-k.1.9.22")

    implementation("com.github.Anuken.Mindustry:core:v146")
    implementation("com.github.Anuken.Mindustry:server:v146")
    implementation("com.github.Anuken.Arc:arc-core:v146")

    implementation("net.dv8tion:JDA:5.0.0-beta.20") {
        exclude(module = "opus-java")
    }

    implementation("org.slf4j:slf4j-api:2.0.11")
    implementation("org.slf4j:slf4j-simple:2.0.11")
}

kotlin {
    jvmToolchain(17)
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
    environment("DISCOVERY_REDIS_HOST", "localhost")
    environment("DISCOVERY_REDIS_PORT", "6379")
    environment("ENABLE_DEV_SERVER_LIST", "false")

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