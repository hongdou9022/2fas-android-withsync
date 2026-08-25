@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.twofasAndroidLibrary)
    alias(libs.plugins.twofasLint)
}

android {
    namespace = "com.twofasapp.cloudbackup.api"
}

dependencies {
    implementation(libs.kotlinCoroutines)
}
