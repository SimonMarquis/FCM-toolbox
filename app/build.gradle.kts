plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}

val versionMajor = 1
val versionMinor = 8
val versionPatch = 2
val versionBuild = 0

android {
    compileSdkVersion(30)
    defaultConfig {
        applicationId = "fr.smarquis.fcm"
        minSdkVersion(16)
        targetSdkVersion(30)
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
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.4.10")

    /* AndroidX */
    implementation("androidx.appcompat:appcompat:1.3.0-alpha02")
    implementation("androidx.constraintlayout:constraintlayout:2.0.2")
    implementation("androidx.core:core-ktx:1.5.0-alpha04")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.2.0")
    implementation("androidx.preference:preference-ktx:1.1.1")
    implementation("androidx.recyclerview:recyclerview:1.2.0-alpha06")
    implementation("androidx.transition:transition:1.3.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")

    /* Material Design */
    implementation("com.google.android.material:material:1.3.0-alpha03")

    /* Firebase */
    implementation("com.google.firebase:firebase-core:17.5.1")
    implementation("com.google.firebase:firebase-messaging:20.3.0")
    implementation("com.google.firebase:firebase-database:19.5.1")

    /* Koin: Dependency Injection */
    val koin = "2.1.6"
    implementation("org.koin:koin-android:$koin")
    implementation("org.koin:koin-android-scope:$koin")
    implementation("org.koin:koin-android-viewmodel:$koin")
    testImplementation("org.koin:koin-test:$koin")
    androidTestImplementation("org.koin:koin-test:$koin")

    /* Moshi: JSON parsing */
    val moshi = "1.11.0"
    implementation("com.squareup.moshi:moshi-adapters:$moshi")
    implementation("com.squareup.moshi:moshi-kotlin:$moshi")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshi")

    /* Room: SQLite persistence */
    val room = "2.2.5"
    implementation("androidx.room:room-runtime:$room")
    kapt("androidx.room:room-compiler:$room")
    implementation("androidx.room:room-ktx:$room")
    testImplementation("androidx.room:room-testing:$room")

    /* Kotlin Coroutines */
    val coroutines = "1.4.0-M1"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutines")

    /* JUnit */
    testImplementation("junit:junit:4.13.1")

}

apply(plugin = "com.google.gms.google-services")
