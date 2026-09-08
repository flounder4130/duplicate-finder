plugins {
    base
    kotlin("jvm") apply false
    id("org.jetbrains.compose") apply false
    id("org.jetbrains.kotlin.plugin.compose") apply false
}

allprojects {
    group = "dev.flounder"
    version = "1.0"
    repositories {
        mavenCentral()
        google()
    }
}

tasks.register("test") { dependsOn(":core:test", ":app:test", ":example-client:test") }
tasks.named("check") { dependsOn("test") }
tasks.named("assemble") { dependsOn(":core:assemble", ":app:assemble", ":example-client:assemble") }
tasks.register<Copy>("fatJar") {
    dependsOn(":app:fatJar")
    from(project(":app").layout.buildDirectory.file("libs/duplicate-finder.jar"))
    into(layout.buildDirectory.dir("libs"))
}
tasks.register("downloadWordsList") { dependsOn(":app:downloadWordsList") }
