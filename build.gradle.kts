import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * 获取当前的 git commit id
 */
fun getGitCommitId(): String {
    return try {
        Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short", "HEAD")).inputStream.reader().readText().trim()
        // Runtime.getRuntime().exec("git rev-parse --short HEAD").inputStream.reader().readText().trim()
    } catch (ex: Exception) {
        "unknown"
    }
}

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.lightless.burp"
version = "1.2.0-SNAPSHOT"

val ktorServerVersion = "3.0.3"
val exposedVersion = "0.57.0"
val graalvmVersion = "24.1.1"

repositories {
    // maven("https://maven.aliyun.com/repository/public/")
    maven("https://repo.huaweicloud.com/repository/maven/")
    mavenCentral()
}

dependencies {
    implementation("net.portswigger.burp.extender:burp-extender-api:1.7.22")

    // Ktor
    implementation("io.ktor:ktor-server-core:$ktorServerVersion")
    implementation("io.ktor:ktor-server-netty:$ktorServerVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorServerVersion")
    implementation("io.ktor:ktor-server-compression:$ktorServerVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorServerVersion")
    implementation("io.ktor:ktor-server-cors:$ktorServerVersion")
    // implementation("io.ktor:ktor-server-websockets:$ktorServerVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorServerVersion")

    // logging for ktor-call-logging
    implementation("ch.qos.logback:logback-classic:1.5.15")

    // ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jodatime:$exposedVersion")
    implementation("org.xerial:sqlite-jdbc:3.42.0.0")

    // js
    implementation("org.graalvm.js:js-scriptengine:$graalvmVersion")
    implementation("org.graalvm.js:js:$graalvmVersion")
//    implementation("org.graalvm.truffle:truffle-api:$graalvmVersion")

    // OkHTTP 给JS脚本使用的
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
}

// 构建前端资源
val feBuild = task<Exec>("feBuild") {
    workingDir("fe")
    if (System.getProperty("os.name").lowercase().contains("win")) {
        commandLine(listOf("pnpm.cmd", "run", "generate"))
    } else {
        commandLine(listOf("pnpm", "run", "generate"))
    }
}

// 复制前端资源文件到 resource 目录
val copyFeDist = task<Copy>("copyFeDist") {
    dependsOn(feBuild)
    delete(file("src/main/resources/dist"))
    from(file("fe/.output/public"))
    into(file("src/main/resources/dist"))
}

tasks {
    withType<KotlinCompile> {
        // kotlinOptions.jvmTarget = "1.8"
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    withType<JavaCompile> {
        targetCompatibility = "1.8"
    }

    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "MainKt"))
        }
        archiveBaseName.set("SignMe2")
        archiveVersion.set("${project.version}-${getGitCommitId()}")
        archiveClassifier.set("")
    }

    processResources {
        if (project.hasProperty("RELEASE")) {
            println("Release mode. Try build frontend file...")
            dependsOn(copyFeDist)
        }
    }
}