import com.github.megatronking.stringfog.plugin.StringFogExtension
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("stringfog")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
}
apply(plugin = "stringfog")

val appPackageName = "com.firefly.oshe.lunli"

var localProperty: Properties? = null
if (file("${rootDir}/local.properties").exists()) {
    localProperty = Properties()
    file("${rootDir}/local.properties").inputStream().use { localProperty?.load(it) }
}
val pwd = System.getenv("KEYSTORE_PASSWORD") ?: localProperty?.getProperty("pwd")

val defaultApi = System.getenv("DEFAULT_API") ?: localProperty?.getProperty("defaultApi")
val supabaseApi = System.getenv("SUPABASE_API") ?: localProperty?.getProperty("supabaseApi")
val supabaseToken1 = System.getenv("SUPABASE_TOKEN_1") ?: localProperty?.getProperty("supabaseToken1")
val supabaseToken2 = System.getenv("SUPABASE_TOKEN_2") ?: localProperty?.getProperty("supabaseToken2")
val supabaseToken3 = System.getenv("SUPABASE_TOKEN_3") ?: localProperty?.getProperty("supabaseToken3")
val token1 = System.getenv("TOKEN_1") ?: localProperty?.getProperty("token1")
val token2 = System.getenv("TOKEN_2") ?: localProperty?.getProperty("token2")
val token3 = System.getenv("TOKEN_3") ?: localProperty?.getProperty("token3")

val generatedAppDir = file("$buildDir/generated/source/oshelunli/java")

configure<StringFogExtension> {
    implementation = "com.github.megatronking.stringfog.xor.StringFogImpl"
    fogPackages = arrayOf("$appPackageName")
    kg = com.github.megatronking.stringfog.plugin.kg.RandomKeyGenerator()
    mode = com.github.megatronking.stringfog.plugin.StringFogMode.bytes
}

android {
    namespace = appPackageName
    compileSdk = 35

    signingConfigs {
        create("releaseBuild") {
            storeFile = file("release.key")
            storePassword = pwd
            keyAlias = "Firefly"
            keyPassword = pwd
        }

        create("debugBuild") {
            storeFile = file("debug.key")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = appPackageName
        minSdk = 29
        versionCode = 10910
        versionName = "1.0.9.1.0-devel"

        resValue("string", "version_code", "${versionCode}")
        vectorDrawables { 
            useSupportLibrary = true
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

    sourceSets["main"].java.srcDirs(generatedAppDir)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
    
}

fun generateJavaClass(
    sourceOutputDir: File,
    packageName: String,
    className: String,
    constantList: List<String>
) {
    val outputDir = File(sourceOutputDir, packageName.replace(".", "/"))
    outputDir.mkdirs()
    val javaFile = File(outputDir, "$className.java")
    javaFile.writeText(
        """
        |/**
        | * Automatically generated file. DO NOT MODIFY
        | */
        |package $packageName;
        |
        |public class $className {
        |${constantList.joinToString("\n") { "\t$it" }}
        |}
        """.trimMargin()
    )
    println("Generated Java file: ${javaFile.absolutePath}")
}

tasks.register("generateInfoDistributor") {
    doLast {
        fun String.toStatement(type: String = "String", variable: String) = "public static final $type $variable = $this;"

        val constantList = listOf(
            "\"$defaultApi\"".toStatement(variable = "DEFAULT_API"),
            "\"$supabaseApi\"".toStatement(variable = "SUPABASE_API"),
            "\"$supabaseToken1\"".toStatement(variable = "SUPABASE_TOKEN_1"),
            "\"$supabaseToken2\"".toStatement(variable = "SUPABASE_TOKEN_2"),
            "\"$supabaseToken3\"".toStatement(variable = "SUPABASE_TOKEN_3"),
            "\"$token1\"".toStatement(variable = "TOKEN_1"),
            "\"$token2\"".toStatement(variable = "TOKEN_2"),
            "\"$token3\"".toStatement(variable = "TOKEN_3")
        )
        generateJavaClass(generatedAppDir, "$appPackageName.info", "InfoDistributor", constantList)
    }
}

tasks.named("preBuild") {
    dependsOn("generateInfoDistributor")
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.exifinterface:exifinterface:1.3.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.room:room-ktx:2.8.3")
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.transition:transition:1.4.1")

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
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-websockets:2.3.12")
    // 看到下面的疯狂排除模型了吗? 请移步app/src/main/assets/markwon.txt
    implementation("io.noties.markwon:core:4.6.2") {
        exclude(group = "com.atlassian.commonmark", module = "commonmark")
    }
    implementation("io.noties.markwon:image:4.6.2") {
        exclude(group = "com.atlassian.commonmark", module = "commonmark")
    }
    implementation("io.noties.markwon:image-glide:4.6.2") {
        exclude(group = "com.atlassian.commonmark", module = "commonmark")
    }
    implementation("org.commonmark:commonmark:0.18.2")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.18.2")
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.json:json:20231013")

    implementation(fileTree(mapOf(
        "dir" to "libs",
        "include" to listOf("*.aar")
    )))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    kapt("com.github.bumptech.glide:compiler:4.13.2")
    kapt("androidx.room:room-compiler:2.8.3")
}
