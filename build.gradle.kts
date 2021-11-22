// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.31")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.5.0")

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}



plugins {
    id("org.jetbrains.dokka") version "1.5.0"
}

tasks.dokkaHtmlMultiModule.configure {
    outputDirectory.set(rootProject.file("docs/api"))
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}