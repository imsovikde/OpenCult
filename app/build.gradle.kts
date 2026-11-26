import java.io.File
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

fun loadDotEnvPublicOnly(envFile: File): Map<String, String> {
    if (!envFile.exists()) return emptyMap()
    val allowedKeys = setOf("OPENCULT_SUPABASE_URL", "OPENCULT_SUPABASE_ANON_KEY")
    val result = mutableMapOf<String, String>()
    envFile.forEachLine { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEachLine
        val eqIdx = line.indexOf('=')
        if (eqIdx <= 0) return@forEachLine
        val key = line.substring(0, eqIdx).trim()
        if (key !in allowedKeys) return@forEachLine
        var value = line.substring(eqIdx + 1).trim()
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))
        ) {
            value = value.substring(1, value.length - 1)
        }
        result[key] = value
    }
    return result
}

fun escapeBuildConfig(raw: String): String =
    raw.replace("\\", "\\\\").replace("\"", "\\\"")

val dotEnv = loadDotEnvPublicOnly(rootProject.file(".env.local"))
val supabaseUrl = (dotEnv["OPENCULT_SUPABASE_URL"] ?: System.getenv("OPENCULT_SUPABASE_URL") ?: "").trim()
val supabaseAnonKey = (dotEnv["OPENCULT_SUPABASE_ANON_KEY"] ?: System.getenv("OPENCULT_SUPABASE_ANON_KEY") ?: "").trim()

if (supabaseUrl.isNotEmpty() && !supabaseUrl.startsWith("https://")) {
    throw GradleException("OPENCULT_SUPABASE_URL must use https:// (found non-HTTPS scheme)")
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}

val releaseStorePath = keystoreProps.getProperty("storeFile")?.trim()
    ?: System.getenv("OPENCULT_KEYSTORE_PATH")?.trim().orEmpty()
val releaseStorePassword = keystoreProps.getProperty("storePassword")?.trim()
    ?: System.getenv("OPENCULT_KEYSTORE_PASSWORD")?.trim().orEmpty()
val releaseKeyAlias = keystoreProps.getProperty("keyAlias")?.trim()
    ?: System.getenv("OPENCULT_KEY_ALIAS")?.trim().orEmpty()
val releaseKeyPassword = keystoreProps.getProperty("keyPassword")?.trim()
    ?: System.getenv("OPENCULT_KEY_PASSWORD")?.trim().orEmpty()

val resolvedKeystoreFile = if (releaseStorePath.isNotEmpty()) {
    listOf(project.file(releaseStorePath), rootProject.file(releaseStorePath)).firstOrNull { it.exists() }
} else {
    null
}

val hasReleaseSigning = resolvedKeystoreFile != null &&
    releaseStorePassword.isNotEmpty() &&
    releaseKeyAlias.isNotEmpty() &&
    releaseKeyPassword.isNotEmpty()

val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

if (releaseTaskRequested && !hasReleaseSigning) {
    throw GradleException(
        "Missing release keystore configuration. Provide gitignored keystore.properties + keystore/opencult-release.jks " +
            "or OPENCULT_KEYSTORE_PATH/PASSWORD/ALIAS/KEY_PASSWORD env vars. Release signing never falls back to debug."
    )
}

android {
    namespace = "org.opencult.scale"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.opencult.scale"
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"${escapeBuildConfig(supabaseUrl)}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${escapeBuildConfig(supabaseAnonKey)}\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = resolvedKeystoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:audio"))
    implementation(project(":core:bia"))
    implementation(project(":core:ble"))
    implementation(project(":core:database"))
    implementation(project(":core:healthconnect"))
    implementation(project(":core:sync-supabase"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:home"))
    implementation(project(":feature:weighin"))
    implementation(project(":feature:bodymap"))
    implementation(project(":feature:reports"))
    implementation(project(":feature:profiles"))
    implementation(project(":feature:inspector"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
