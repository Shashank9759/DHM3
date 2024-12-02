// Top-level build file where you can add configuration options common to all sub-projects/modules.
 plugins {
   // val room_version = "2.6.1"
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
 //   id("com.google.devtools.ksp") version "1.9.10-1.0.13" apply false


}
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.2") // Version may vary
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.44")
//        classpath("com.android.tools.build:gradle:8.6.1")
    }
    repositories{
        mavenCentral()
    }
}