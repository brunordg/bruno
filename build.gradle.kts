plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
}

group = "com.codeteam"
version = "1.5.0"

// Build with the JDK installed on this machine (currently JDK 25), but keep the compiled
// bytecode at Java 21: IntelliJ Platform 2025.3.1's bundled JetBrains Runtime is JDK 21,
// so a plugin whose classes target a newer class file version fails to load at runtime
// (UnsupportedClassVersionError) even though it compiles and packages fine.
kotlin {
    jvmToolchain(25)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
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
    testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.18.2")
    testImplementation("io.swagger.core.v3:swagger-annotations:2.2.28")
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }

        changeNotes = """
            <p><b>1.4.0:</b></p>
            <ul>
                <li>Configurable folder strategy for generated requests: "By URL path" (default) nests folders mirroring the endpoint path, "By controller" groups one folder per controller class, and "Flat" puts every request directly under requests/</li>
                <li>Pick the folder strategy per generation in the selection dialog (with a live diff preview) or set the persisted default in Settings &gt; Tools &gt; Bruno Generator</li>
                <li>Request file names stay stable per strategy (expect a one-time folder reshuffle on the first regeneration after changing the strategy)</li>
            </ul>
            <p><b>1.3.0:</b></p>
            <ul>
                <li>Request bodies now respect Jackson annotations: @JsonProperty renames keys and @JsonIgnore / access = READ_ONLY fields are omitted (class-level @JsonNaming and @JsonInclude are not yet supported)</li>
                <li>Enrich requests from springdoc / OpenAPI 3 annotations: @Operation(summary) names the request, @Operation/@Parameter/@Schema descriptions fill the docs block, and @Schema(example) sets the field value (taking precedence over validation and heuristic fakes)</li>
                <li>Stable request file names derived from the endpoint method and path, so changing the selection no longer renumbers files (expect a one-time rename on the first regeneration after upgrading)</li>
                <li>New "Add-only" regeneration mode that only creates missing request files, preserving manual edits; "Overwrite" remains the default</li>
                <li>Configurable authentication (Bearer, Basic, API Key): sets collection-level auth that every request inherits, with credential variables provisioned per environment</li>
            </ul>
            <p><b>1.2.1:</b></p>
            <ul>
                <li>Customize the generated collection folder name with configurable prefix and suffix (Settings &gt; Tools &gt; Bruno Generator)</li>
                <li>Collection name field now empty by default — uses project folder name as base</li>
                <li>Default prefix is "bruno-" (e.g., project "users-api" → folder "bruno-users-api")</li>
            </ul>
            <p><b>1.2.0:</b></p>
            <ul>
                <li>Request bodies now support nested objects, lists/arrays, and multipart file uploads (@RequestPart / MultipartFile), rendered as real JSON/multipart content instead of flat placeholders</li>
                <li>Generating now opens a single screen to select which endpoints to include, with a live preview of which files will be added, removed, or left unchanged before anything is written to disk</li>
                <li>Show a progress indicator while scanning controllers and while writing the collection</li>
            </ul>
            <p><b>1.1.4:</b></p>
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

    register<Copy>("copyPluginToRoot") {
        from("build/distributions")
        include("*.zip")
        into(rootDir)
        doLast {
            println("✓ Plugin copied to project root")
        }
    }

    buildPlugin {
        finalizedBy("copyPluginToRoot")
    }
}
