import com.github.megatronking.stringfog.plugin.StringFogExtension

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("stringfog")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    fogPackages = arrayOf("com.firefly.oshe.lunli")
    kg = com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator()
    mode = com.github.megatronking.stringfog.plugin.StringFogMode.bytes
}

android {
    namespace = "com.firefly.oshe.lunli"
    compileSdk = 34

    signingConfigs {
        create("releaseBuild") {
            storeFile = file("release.key")
            storePassword = "Firefly"
            keyAlias = "Firefly"
            keyPassword = "Firefly"
        }

        create("debugBuild") {
            storeFile = file("debug.key")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.firefly.oshe.lunli"
        minSdk = 28
        versionCode = 100
        versionName = "1.0-devel"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            targetSdk = 28
        }
        create("prod") {
            dimension = "environment"
            targetSdk = 34
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("releaseBuild")
        }
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debugBuild")
        }
    }

    buildFeatures {
        viewBinding = true
    }
    
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.caverock:androidsvg:1.4")
    implementation("com.github.bumptech.glide:glide:4.13.2")
    implementation("com.github.megatronking.stringfog:xor:5.0.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.4.2")
    implementation("io.github.jan-tennert.supabase:realtime-kt:2.4.2")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.4.2")
    implementation("io.github.jan-tennert.supabase:storage-kt:2.4.2")
    implementation("io.ktor:ktor-client-android:2.3.0")
    // 看到下面的疯狂排除模型了吗? 请移步app/src/main/assets/markwon.txt
    implementation("io.noties.markwon:core:4.6.2") {
        exclude(group = "com.atlassian.commonmark", module = "commonmark")
    }
    // implementation("io.noties:markwon:ext-tables:4.6.2")
    // implementation("io.noties:markwon:html:4.6.2")
    implementation("io.noties.markwon:image:4.6.2") {
        exclude(group = "com.atlassian.commonmark", module = "commonmark")
    }
    implementation("io.noties.markwon:image-glide:4.6.2") {
        exclude(group = "com.atlassian.commonmark", module = "commonmark")
    }
    // implementation("io.noties:markwon:linkify:4.6.2")

    implementation("org.commonmark:commonmark:0.18.2")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.18.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.json:json:20231013")

    implementation(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.aar")
    )))

    kapt("com.github.bumptech.glide:compiler:4.13.2")
}
