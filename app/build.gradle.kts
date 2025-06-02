import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}

val apiProperties = Properties().apply {
    val apiPropertiesFile = rootProject.file("api.properties")
    if (apiPropertiesFile.exists()) {
        load(FileInputStream(apiPropertiesFile))
    } else {
        rootProject.file("api.properties").writeText(
            "OPENROUTER_API_KEY=your_api_key_here"
        )
        throw GradleException(
            "api.properties not found. Please create api.properties file based on api.properties.template"
        )
    }
}


android {
    namespace = "com.example.pennykeeper"
    compileSdk = 34



    defaultConfig {

        applicationId = "com.example.pennykeeper"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        //load the api key
        buildConfigField(
            type = "String",
            name = "OPENROUTER_API_KEY",
            value = "\"${apiProperties.getProperty("OPENROUTER_API_KEY") ?: ""}\""
        )

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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation(libs.androidx.appcompat)

    //HTTP client for Chatbot
    implementation(libs.okhttp)

    val nav_version = "2.8.3"
    implementation("androidx.navigation:navigation-compose:$nav_version")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    implementation("androidx.compose.runtime:runtime-livedata:1.5.0")

    //Room
    implementation("androidx.room:room-runtime:${rootProject.extra["room_version"]}")
    ksp("androidx.room:room-compiler:${rootProject.extra["room_version"]}")
    implementation("androidx.room:room-ktx:${rootProject.extra["room_version"]}")


    //testing
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

}