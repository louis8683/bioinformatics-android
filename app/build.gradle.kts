plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)

    // KSP
    id("com.google.devtools.ksp")

    // Hilt
    id("com.google.dagger.hilt.android")

    // Compose compiler
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.louislu.pennbioinformatics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.louislu.pennbioinformatics"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // AppAuth
        manifestPlaceholders["appAuthRedirectScheme"] = "com.louislu.pennbioinformatics"
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
//        resources {
//            excludes += "/META-INF/{AL2.0,LGPL2.1}"
//        }

        resources.excludes.addAll(
            listOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "/META-INF/{AL2.0,LGPL2.1}"
        )
        )
    }
    composeCompiler {
        reportsDestination = layout.buildDirectory.dir("compose_compiler")
        stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Material 3 compose
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.4.0-alpha10")

    // Compose navigation
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.rules)
    implementation(libs.play.services.location)
    ksp(libs.hilt.android.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // GSON
    implementation(libs.gson)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // BLE
    implementation("no.nordicsemi.android:ble:2.9.0")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // OAuth 2.0 + OIDC
    implementation("net.openid:appauth:0.11.1")

    // Browser
    implementation("androidx.browser:browser:1.5.0")

    // OKIO to read config from JSON
    implementation("com.squareup.okio:okio:3.10.2")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Datastore
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation("org.robolectric:robolectric:4.8")

    androidTestImplementation("io.mockk:mockk-android:1.13.17")
    androidTestImplementation("io.mockk:mockk-agent:1.13.17")



    // Turbine
    androidTestImplementation("app.cash.turbine:turbine:1.2.0")
}
