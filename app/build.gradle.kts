plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.driversafe2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.driversafe2"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

buildFeatures{
    viewBinding = true
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.mlkit:face-detection:16.1.5")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment)
    testImplementation(libs.junit)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("nl.joery.animatedbottombar:library:1.1.0")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.firebase:firebase-storage-ktx")
    androidTestImplementation(libs.androidx.junit)
    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.navigation:navigation-fragment-ktx")
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.navigation:navigation-ui-ktx")
    implementation("com.github.denzcoskun:ImageSlideshow:0.1.2")
    implementation("androidx.core:core-splashscreen:1.0.1")
    androidTestImplementation(libs.androidx.espresso.core)
}