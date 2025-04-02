plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.doanlmao"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.doanlmao"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Thêm block packaging để xử lý xung đột META-INF
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.swiperefreshlayout)
    implementation(libs.cardview)
    implementation(libs.media)
    implementation(libs.viewpager2)
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.documentfile)
    implementation(libs.palette)
    implementation(libs.viewpager2.v100)

    implementation(libs.firebase.auth) // Firebase Auth
    implementation(libs.play.services.auth) // Google Sign-In
    implementation(libs.firebase.database)

    implementation(libs.google.api.services.drive)
    implementation(libs.google.http.client.android)
    implementation(libs.google.api.client.android)

    implementation(libs.blurry) // Để blur ảnh sau này
    implementation(libs.glide)

    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
}