plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

dependencies {
    implementation("org.commonmark:commonmark:0.22.0")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    testImplementation(kotlin("test"))
}

kotlin { jvmToolchain(21) }
java { withSourcesJar() }
base { archivesName.set("duplicate-finder-core") }
tasks.test { useJUnitPlatform() }

publishing {
    publications {
        create<MavenPublication>("library") {
            from(components["java"])
            artifactId = "duplicate-finder-core"
        }
    }
}