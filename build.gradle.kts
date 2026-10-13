import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
    id("org.jetbrains.kotlin.jvm") version "2.3.21"
}

group = "net.gmmhub"
version = project.findProperty("pluginVersion")?.toString() ?: "0.0.1-spike"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea("2026.1.3")
        bundledPlugin("org.jetbrains.plugins.textmate")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val languageServerRepo = layout.projectDirectory.dir(
    providers.gradleProperty("haproxyServerRepo").orElse("../gmm-haproxy-vscode").get()
)

val buildLanguageServer = tasks.register<Exec>("buildLanguageServer") {
    group = "build"
    description = "Builds the bundled HAProxy language server in the sibling VS Code repository when missing."
    workingDir = languageServerRepo.asFile
    commandLine("npm", "run", "build:server")
    onlyIf { !languageServerRepo.file("server/out/server.js").asFile.exists() }
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
        dependsOn(buildLanguageServer)
        val pluginDir = intellijPlatform.projectName.get()
        from(languageServerRepo.file("server/out/server.js")) {
            into("$pluginDir/language-server")
        }
        from(languageServerRepo.file("syntaxes/haproxy.tmLanguage.json")) {
            into("$pluginDir/textmate/haproxy/syntaxes")
        }
        from(languageServerRepo.file("language-configuration.json")) {
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
            listOfNotNull(
                "-Didea.trust.all.projects=true",
                "-Djb.consents.confirmation.enabled=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Dide.show.tips.on.startup.default.value=false",
                providers.gradleProperty("haproxyVersion").orNull?.let { "-Dhaproxy.version=$it" },
            )
        }
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
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
                create(IntelliJPlatformType.IntellijIdea, "2026.1.4")
                create(IntelliJPlatformType.IntellijIdea, "2026.2.2")
                create(IntelliJPlatformType.PyCharm, "2026.1.3")
            }
        }
    }
}

intellijPlatformTesting.runIde.register("runPyCharm") {
    type = IntelliJPlatformType.PyCharm
    version = "2026.1.3"
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
