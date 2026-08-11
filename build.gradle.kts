plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
}

group = "com.codeteam"
version = "1.1.0-SNAPSHOT"

// Build with JDK 26 (the only JDK installed on this machine), but keep the compiled
// bytecode at Java 21: IntelliJ Platform 2025.3.1's bundled JetBrains Runtime is JDK 21,
// so a plugin whose classes target a newer class file version fails to load at runtime
// (UnsupportedClassVersionError) even though it compiles and packages fine.
kotlin {
    jvmToolchain(26)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

// Applied in afterEvaluate: the IntelliJ Platform Gradle plugin sets its own Kotlin
// jvmTarget convention (matching the toolchain, 26) during project evaluation, which
// would otherwise win over a plain top-level `compilerOptions` block.
afterEvaluate {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("platformVersion"))
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

        // Add plugin dependencies for compilation here:
        bundledPlugin("com.intellij.java")
    }

    testImplementation("org.springframework:spring-web:6.2.1")
    testImplementation("jakarta.validation:jakarta.validation-api:3.1.1")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }

        changeNotes = """
            <ul>
                <li>Extract @RequestParam, @PathVariable, @RequestHeader and @CookieValue into generated requests</li>
                <li>Use Bean Validation annotations (@NotNull, @Size, @Email, @Min/@Max, @Pattern) and enum fields to generate more realistic example payloads</li>
                <li>Add a Settings screen (Tools &gt; Bruno Generator) to configure multiple named environments, generated under bruno/environments/</li>
                <li>Fix async error handling and a PSI performance anti-pattern in the controller scanner</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }
}
