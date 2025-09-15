plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.9.0"
}

group = "com.toucan_software.autotabgrouper"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure the JVM toolchain for consistency
kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        create("RD", "2025.1")
    }
}

intellijPlatform {
    pluginConfiguration {
        changeNotes.set("""
            Initial release of the Smart Tab Grouper plugin.
        """)
    }
}
