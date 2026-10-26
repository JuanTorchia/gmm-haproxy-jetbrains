import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import java.net.URI
import java.security.MessageDigest
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.kotlin.jvm") version "2.3.21"
}

group = "net.gmmhub"
version = project.findProperty("pluginVersion")?.toString() ?: "0.2.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("263.7307.44")
        bundledPlugin("org.jetbrains.plugins.textmate")
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// The language server and grammar come from a released gmm-haproxy-vscode VSIX (Open VSX),
// pinned by version and SHA-256 in gradle.properties. See README "Bundled language server" to bump it.
val haproxyServerVersion = providers.gradleProperty("haproxyServerVersion").get()
val haproxyServerSha256 = providers.gradleProperty("haproxyServerSha256").get()
val haproxyServerVsix = layout.buildDirectory.file("haproxy-server/gmm-haproxy-vscode-$haproxyServerVersion.vsix")
val haproxyServerDir = layout.buildDirectory.dir("haproxy-server/extension")

val downloadLanguageServer = tasks.register("downloadLanguageServer") {
    group = "build"
    description = "Downloads the pinned gmm-haproxy-vscode VSIX and checks its SHA-256."
    val version = haproxyServerVersion
    val expectedSha256 = haproxyServerSha256
    val vsix = haproxyServerVsix
    inputs.property("version", version)
    inputs.property("sha256", expectedSha256)
    outputs.file(vsix)

    doLast {
        val target = vsix.get().asFile
        val url = "https://open-vsx.org/api/gmm/gmm-haproxy-vscode/$version/file/gmm.gmm-haproxy-vscode-$version.vsix"
        target.parentFile.mkdirs()
        URI(url).toURL().openStream().use { input -> target.outputStream().use { input.copyTo(it) } }
        val actual = MessageDigest.getInstance("SHA-256").digest(target.readBytes()).joinToString("") { "%02x".format(it) }
        if (actual != expectedSha256) {
            target.delete()
            throw GradleException("SHA-256 mismatch for $url: expected $expectedSha256, got $actual")
        }
    }
}

val extractLanguageServer = tasks.register<Sync>("extractLanguageServer") {
    group = "build"
    description = "Extracts server.js, the TextMate grammar and the language configuration from the pinned VSIX."
    from(zipTree(downloadLanguageServer.map { haproxyServerVsix.get() })) {
        include(
            "extension/server/out/server.js",
            "extension/syntaxes/haproxy.tmLanguage.json",
            "extension/language-configuration.json",
        )
        eachFile { path = path.removePrefix("extension/") }
        includeEmptyDirs = false
    }
    into(haproxyServerDir)
}

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    withType<PrepareSandboxTask>().configureEach {
        dependsOn(extractLanguageServer)
        val pluginDir = intellijPlatform.projectName.get()
        from(haproxyServerDir.map { it.file("server/out/server.js") }) {
            into("$pluginDir/language-server")
        }
        from(haproxyServerDir.map { it.file("syntaxes/haproxy.tmLanguage.json") }) {
            into("$pluginDir/textmate/haproxy/syntaxes")
        }
        from(haproxyServerDir.map { it.file("language-configuration.json") }) {
            into("$pluginDir/textmate/haproxy")
        }
        from(layout.projectDirectory.file("src/main/textmate/package.json")) {
            into("$pluginDir/textmate/haproxy")
        }
    }

    runIde {
        argumentProviders += CommandLineArgumentProvider {
            listOf(layout.projectDirectory.dir("demo").asFile.absolutePath)
        }
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Didea.trust.all.projects=true",
                "-Djb.consents.confirmation.enabled=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Dide.show.tips.on.startup.default.value=false",
            )
        }
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "263"
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        ides {
            // -PverifierLocalIdes=/path/a,/path/b reuses already extracted IDEs (saves several GB per IDE).
            val localIdes = providers.gradleProperty("verifierLocalIdes").orNull
            if (localIdes != null) {
                localIdes.split(",").forEach { local(file(it.trim())) }
            } else {
                create(IntelliJPlatformType.IntellijIdea, "263.7307.44")
                create(IntelliJPlatformType.PyCharm, "263.7307.50")
            }
        }
    }

    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }
}

intellijPlatformTesting.runIde.register("runPyCharm") {
    type = IntelliJPlatformType.PyCharm
    version = "263.7307.50"
    task {
        argumentProviders += CommandLineArgumentProvider {
            listOf(layout.projectDirectory.dir("demo").asFile.absolutePath)
        }
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Didea.trust.all.projects=true",
                "-Djb.consents.confirmation.enabled=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
            )
        }
    }
}
