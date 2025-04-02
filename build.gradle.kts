// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Sử dụng uri() để chuyển đổi String thành URI
    }
}
buildscript {
    dependencies {
        classpath (libs.google.services) // Cập nhật phiên bản mới nhất
    }
}