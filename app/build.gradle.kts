plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ru.ruvideohub.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.ruvideohub.app"
        // minSdk поднят с 23 до 26: нужен для adaptive icon и части Media3/Keystore API.
        // Для личного использования на современных ТВ/телефонах это не ограничение.
        minSdk = 26
        targetSdk = 35
        versionCode = 140
        versionName = "14.0"
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Встроенный видеоплеер с поддержкой HLS (RUTUBE и другие источники)
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Зашифрованное хранилище для API-ключей и токенов источников
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
