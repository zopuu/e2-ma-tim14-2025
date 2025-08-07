plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.habibitar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.habibitar"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
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

    // Desugaring (moderne Java API-je na starijim Androidima)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // --- Firebase (Auth + Firestore) preko BOM-a ---
    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // --- Room (SQLite) – Java varianta koristi annotationProcessor ---
    implementation("androidx.room:room-runtime:2.7.2")
    annotationProcessor("androidx.room:room-compiler:2.7.2")

    // --- ZXing (QR skener) ---
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // --- MPAndroidChart ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- Navigation komponenta (Fragments + NavUI) ---
    implementation("androidx.navigation:navigation-fragment:2.9.3")
    implementation("androidx.navigation:navigation-ui:2.9.3")
}
