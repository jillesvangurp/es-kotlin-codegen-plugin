import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    // update to 1.4.0 is blocked on code poet
    id("org.jetbrains.kotlin.jvm") version "1.4.20"
    id("org.jetbrains.dokka") version "1.4.20"

    id("com.github.ben-manes.versions") version "0.36.0" // gradle dependencyUpdates -Drevision=release
    java
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    `maven-publish`
    `project-report` // gradle dependencyReport
}

repositories {
    mavenCentral()
}

group = "com.github.jillesvangurp"
version = "1.0"

// compile bytecode to java 8 (default is java 6)
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val elasticsearchVersion = "7.13.0"
val kotlinVersion = "1.4.20"

dependencies {
    api("org.elasticsearch.client:elasticsearch-rest-high-level-client:$elasticsearchVersion")
    api("org.elasticsearch.client:elasticsearch-rest-client:$elasticsearchVersion")

    api("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.2")
    implementation("org.reflections:reflections:0.9.12")
    implementation("com.squareup:kotlinpoet:1.7.2")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    this.sourceFilesExtensions
}

gradlePlugin {
    plugins {
        create("codegen") {
            id = "com.github.jillesvangurp.codegen"
            implementationClass = "com.jillesvangurp.escodegen.EsKotlinCodeGenPlugin"
        }
    }
}

configure<PublishingExtension> {
    // for local testing
    repositories {
        maven("build/repository")
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

configurations.getByName("functionalTestImplementation").extendsFrom(configurations.getByName("testImplementation"))

// Add a task to run the functional tests
val functionalTest by tasks.creating(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

val check by tasks.getting(Task::class) {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}
