plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.gms.google-services")

}

android {
    namespace = "com.xero.machinetesttask"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.xero.machinetesttask"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx:22.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.8.1")
    implementation ("com.google.firebase:firebase-database-ktx")
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation ("com.google.android.gms:play-services-auth:19.2.0")
    implementation ("com.google.android.gms:play-services-auth-api-phone")
    implementation ("pl.droidsonroids.gif:android-gif-drawable:1.2.24")
}