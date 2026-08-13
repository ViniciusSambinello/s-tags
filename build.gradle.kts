plugins {
    java
    alias(libs.plugins.shadow)
}

group = "io.github.viniciussambinello"
version = "0.1.0"
description = "A Paper 26.2 plugin providing a permission-owned tag and title cosmetic system."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)

    implementation(libs.hikaricp)
    implementation(libs.mysql.connector.j)

    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile> {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val expandedProperties = mapOf("version" to project.version)
    inputs.properties(expandedProperties)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(expandedProperties)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.zaxxer.hikari", "io.github.viniciussambinello.stags.libs.hikari")
    relocate("com.mysql", "io.github.viniciussambinello.stags.libs.mysql")
    minimize {
        exclude(dependency("com.mysql:mysql-connector-j:.*"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier.set("plain")
}
