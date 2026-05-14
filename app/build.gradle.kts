plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.diskettefox.MadridLibreTransport"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "dev.diskettefox.madridbus"
        minSdk = 26
        targetSdk = 36
        versionCode = 120
        versionName = "v1.2.0"

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
    buildFeatures {
        compose = true
    }
}


dependencies {
    implementation("com.squareup.retrofit2:retrofit:3.0.0") // Apache 2.0
    implementation("com.squareup.okhttp3:okhttp:5.3.2") // Apache 2.0
    implementation("com.squareup.okhttp3:logging-interceptor:5.3.2") // Apache 2.0
    implementation("com.google.code.gson:gson:2.13.2") // Apache 2.0
    implementation("com.google.android.material:material:1.13.0")  // Apache 2.0
    implementation("com.squareup.retrofit2:converter-gson:3.0.0") // Apache 2.0
    implementation("com.github.chrisbanes:PhotoView:2.3.0") // Apache 2.0
    implementation ("com.github.bumptech.glide:glide:5.0.5") // MIT and Apache 2.0
    annotationProcessor ("com.github.bumptech.glide:compiler:5.0.5") // MIT and Apache 2.0
    
    // MapsForge Map (GPLv3)
    implementation("com.github.mapsforge.mapsforge:mapsforge-core:0.27.0")
    implementation("com.github.mapsforge.mapsforge:mapsforge-map:0.27.0")
    implementation("com.github.mapsforge.mapsforge:mapsforge-map-reader:0.27.0")
    implementation("com.github.mapsforge.mapsforge:mapsforge-themes:0.27.0")
    implementation("com.github.mapsforge.mapsforge:mapsforge-map-android:0.27.0")
    implementation("com.caverock:androidsvg:1.4")

    // MapsForge POI (GPLv3)
    implementation("com.github.mapsforge.mapsforge:mapsforge-core:0.27.0")
    implementation("com.github.mapsforge.mapsforge:mapsforge-poi:0.27.0")
    implementation("com.github.mapsforge.mapsforge:mapsforge-poi-android:0.27.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material3.adaptive.navigation.suite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}