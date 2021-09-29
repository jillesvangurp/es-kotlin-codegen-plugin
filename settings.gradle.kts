rootProject.name = "es-kotlin-codegen-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("de.fayard.refreshVersions") version "0.23.0"
}

refreshVersions {
//    extraArtifactVersionKeyRules(file("version_key_rules.txt"))
}
