plugins {
    kotlin("jvm")
    application
}

dependencies { implementation(project(":core")) }
kotlin { jvmToolchain(21) }
application { mainClass.set("example.MainKt") }