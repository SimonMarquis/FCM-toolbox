import de.fayard.refreshVersions.core.versionFor
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}

val versionMajor = 1
val versionMinor = 8
val versionPatch = 3
val versionBuild = 0

android {
    compileSdk = 30
    defaultConfig {
        applicationId = "fr.smarquis.fcm"
        minSdk = 16
        targetSdk = 30
        versionCode = versionMajor * 1000000 + versionMinor * 10000 + versionPatch * 100 + versionBuild
        versionName = "$versionMajor.$versionMinor.$versionPatch"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true

        javaCompileOptions {
            annotationProcessorOptions {
                argument("room.schemaLocation", "$projectDir/schemas")
                argument("room.incremental", "true")
                argument("room.expandProjection", "true")
            }
        }
    }
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    /* AndroidX */
    implementation(AndroidX.appCompat)
    implementation(AndroidX.constraintLayout)
    implementation(AndroidX.core.ktx)
    implementation(AndroidX.lifecycle.liveDataKtx)
    implementation(AndroidX.lifecycle.viewModelKtx)
    implementation(AndroidX.preferenceKtx)
    implementation(AndroidX.recyclerView)
    implementation(AndroidX.transition)
    androidTestImplementation(AndroidX.test.espresso.core)
    androidTestImplementation(AndroidX.test.ext.junit)

    /* Material Design */
    implementation(Google.Android.material)

    /* Firebase */
    implementation(platform(Firebase.bom))
    implementation(Firebase.cloudMessaging)
    implementation(Firebase.realtimeDatabase)

    /* Koin: Dependency Injection */
    val koin = "3.0.1-beta-1"
    implementation(Koin.android)
    testImplementation(Koin.test)
    androidTestImplementation(Koin.test)

    /* Moshi: JSON parsing */
    implementation(Square.moshi)
    implementation(Square.moshi.kotlinReflect)
    kapt(Square.moshi.kotlinCodegen)
    versionFor(Square.moshi)
    implementation("com.squareup.moshi:moshi-adapters:${versionFor(Square.moshi)}")

    /* Room: SQLite persistence */
    implementation(AndroidX.room.runtime)
    kapt(AndroidX.room.compiler)
    implementation(AndroidX.room.ktx)
    testImplementation(AndroidX.room.testing)

    /* Kotlin Coroutines */
    implementation(KotlinX.coroutines.core)
    implementation(KotlinX.coroutines.android)
    implementation(KotlinX.coroutines.playServices)

    /* JUnit */
    testImplementation(Testing.junit4)

}

apply(plugin = "com.google.gms.google-services")
