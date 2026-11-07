import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun loadProperties(fileName: String) = Properties().apply {
    val file = rootProject.file(fileName)
    if (file.exists()) file.inputStream().use(::load)
}

// Backend address per build type. Override without editing this file by adding
// to local.properties (or passing -P on the command line):
//   lumine.apiBaseUrl.debug=https://your-tunnel.ngrok-free.dev
//   lumine.apiBaseUrl.release=https://api.your-domain.com
val localProperties = loadProperties("local.properties")
fun lumineProperty(name: String): String? =
    (findProperty(name) as String?) ?: localProperties.getProperty(name)

val debugApiBaseUrl = lumineProperty("lumine.apiBaseUrl.debug")
    ?: "https://neaped-lanell-momentous.ngrok-free.dev"
val releaseApiBaseUrl = lumineProperty("lumine.apiBaseUrl.release") ?: debugApiBaseUrl

// Supabase address and anon key, used only to set an admin's password, which the server
// can't do. Without them, Forgot Password and Change Password say they aren't set up.
//   lumine.supabaseUrl=https://abcd.supabase.co
//   lumine.supabaseAnonKey=...
// The key ends up inside the APK, so add it only after database/update.sql has been
// run. Until the tables are locked down, that key can read them.
val supabaseUrl = lumineProperty("lumine.supabaseUrl").orEmpty()
val supabaseAnonKey = lumineProperty("lumine.supabaseAnonKey").orEmpty()

// Release signing key, read from keystore.properties (git-ignored):
//   storeFile=lumine-release.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
val keystoreProperties = loadProperties("keystore.properties")

android {
    namespace = "com.thesis.lumine"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thesis.lumine"
        minSdk = 26
        targetSdk = 36
        versionCode = 8
        versionName = "1.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    signingConfigs {
        if (!keystoreProperties.isEmpty) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes.add("META-INF/*.kotlin_module")
        }
    }
}

dependencies {
    // Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // CameraX
    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // MediaPipe
    implementation("com.google.mediapipe:tasks-vision:0.10.26.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Unity Integration
    implementation(project(":unityLibrary"))
    implementation(fileTree(mapOf("dir" to "../unityLibrary/libs", "include" to listOf("*.jar"))))
}