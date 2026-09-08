import java.net.URI

plugins {
    kotlin("jvm")
    application
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(project(":core"))
    implementation("commons-cli:commons-cli:1.5.0")
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(21) }
application { mainClass.set("finder.cli.MainKt") }
tasks.test { useJUnitPlatform() }

tasks.register<Jar>("fatJar") {
    group = "build"
    description = "Assembles the CLI and desktop application with its dependencies"
    archiveFileName.set("duplicate-finder.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = application.mainClass.get() }
    from(sourceSets.main.get().output)
    from({ configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) } })
    dependsOn(configurations.runtimeClasspath)
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
}

tasks.register("downloadWordsList") {
    group = "verification"
    doLast {
        URI("https://www.mit.edu/~ecprice/wordlist.10000").toURL().openStream().use { input ->
            file("src/test/resources/words").outputStream().use { input.copyTo(it) }
        }
    }
}