import java.util.Properties

// Слой данных: реализации репозиториев, сервер, локальный кэш, местоположение.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

fun serverProperty(name: String): String {
    val local = Properties()
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { local.load(it) }
    return local.getProperty(name) ?: providers.gradleProperty(name).get()
}

android {
    namespace = "com.coursework.nearby.data"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 26
        // Адрес и публичный ключ сервера: значения по умолчанию в gradle.properties указывают
        // на локальный Supabase, для облачного проекта их переопределяют в local.properties.
        buildConfigField("String", "SUPABASE_URL", "\"${serverProperty("supabase.url")}\"")
        buildConfigField("String", "SUPABASE_KEY", "\"${serverProperty("supabase.key")}\"")
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.coroutines.android)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.serialization.json)
    implementation(libs.play.services.location)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
}
