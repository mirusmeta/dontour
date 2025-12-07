plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    id("ru.ok.tracer") version "0.2.7"
}

android {
    namespace = "ru.dontour"
    compileSdk = 36

    defaultConfig {
        applicationId = "ru.dontour"
        minSdk = 27
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        addManifestPlaceholders(
            mapOf(
                "VKIDRedirectHost" to "vk.com",
                "VKIDRedirectScheme" to "vk54268162",
                "VKIDClientID" to "54268162",
                "VKIDClientSecret" to "P49ngb1OPkT6snaeEzVP"
            )
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

tracer {
    create("defaultConfig") {
        // См. в разделе _«Настройки»_
        pluginToken = "KiMOVLtCGterOAV1iuOt9DOWlzTLJvHxF27xOxPtElq"
        appToken = "REJvV9xFdfbxJklQmN2ePgfhcOep8dH2SgQBEm0s0bk"

        uploadMapping = true
    }

    create("debug") {
    }
    create("demoDebug") {
    }
}

dependencies {
    implementation("com.vk.id:onetap-xml:2.5.0")
    implementation("com.vk.id:vkid:2.5.0")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation(libs.firebase.firestore)
    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("com.yandex.android:maps.mobile:4.19.0-full")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.ai)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.android.gif.drawable)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.squareup.picasso:picasso:2.8")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // 🔹 Tracer SDK — сбор ошибок, крэшей, профайлинг и т.д.
    implementation("ru.ok.tracer:tracer-crash-report:0.2.7")
// Сбор и анализ нативных крешей
    implementation("ru.ok.tracer:tracer-crash-report-native:0.2.7")
// Сбор и анализ хипдапмов при OOM
    implementation("ru.ok.tracer:tracer-heap-dumps:0.2.7")
// Анализ потребления дискового места на устройстве
    implementation("ru.ok.tracer:tracer-disk-usage:0.2.7")
// Семплирующий профайлер
    implementation("ru.ok.tracer:tracer-profiler-sampling:0.2.7")
// Систрейс
    implementation("ru.ok.tracer:tracer-profiler-systrace:0.2.7")
}

