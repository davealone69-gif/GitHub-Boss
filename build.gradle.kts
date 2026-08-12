import org.gradle.kotlin.dsl.kotlin

buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

plugins {
    // Version catalogs or other plugins can go here if needed
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}