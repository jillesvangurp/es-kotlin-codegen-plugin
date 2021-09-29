import org.gradle.api.publish.PublishingExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    // update to 1.4.0 is blocked on code poet
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")

    id("com.github.ben-manes.versions") // gradle dependencyUpdates -Drevision=release
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
val kotlinVersion = "1.5.10"

dependencies {
    api("org.elasticsearch.client:elasticsearch-rest-high-level-client:_")
    api("org.elasticsearch.client:elasticsearch-rest-client:_")

    api(KotlinX.coroutines.jdk8)
    implementation("org.reflections:reflections:_")
    implementation(Square.kotlinPoet)

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:_"))

    // Use the Kotlin JDK 8 standard library.
    implementation(Kotlin.stdlib)
    implementation("org.jetbrains.kotlin:kotlin-reflect:_")

    // Use the Kotlin test library.
    testImplementation(Kotlin.test)

    // Use the Kotlin JUnit integration.
    testImplementation(Kotlin.test.junit)
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
