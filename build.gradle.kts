apply(plugin = "com.github.ben-manes.versions")

buildscript {
    repositories {
        google()
        jcenter()

    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.10")
        classpath("com.android.tools.build:gradle:4.2.0-alpha14")
        classpath("com.google.gms:google-services:4.3.4")
        classpath("com.github.ben-manes:gradle-versions-plugin:0.33.0")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}