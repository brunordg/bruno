plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
}

group = "com.codeteam"
version = "1.2.0"

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
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Plugin.Java)

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
                <li>Request bodies now support nested objects, lists/arrays, and multipart file uploads (@RequestPart / MultipartFile), rendered as real JSON/multipart content instead of flat placeholders</li>
                <li>Generating now opens a single screen to select which endpoints to include, with a live preview of which files will be added, removed, or left unchanged before anything is written to disk</li>
                <li>Show a progress indicator while scanning controllers and while writing the collection</li>
            </ul>
            <p>1.1.4:</p>
            <ul>
                <li>New projects now start with a single default environment (dev) instead of three</li>
                <li>Add an optional output directory setting (Tools &gt; Bruno Generator) to generate the collection outside the project root</li>
                <li>The generated collection folder is now named after the project instead of always being called "bruno"</li>
            </ul>
            <p>1.1.2:</p>
            <ul>
                <li>Replace the Settings environments table implementation (ListTableWithButtons, which extends the deprecated java.util.Observable) with TableModelEditor</li>
            </ul>
            <p>1.1.1:</p>
            <ul>
                <li>Fix plugin logo not displaying in Marketplace search results (switched pluginIcon.png to the required pluginIcon.svg format)</li>
                <li>Replace a deprecated ReadAction.compute call with a non-deprecated equivalent</li>
            </ul>
            <p>1.1.0:</p>
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
