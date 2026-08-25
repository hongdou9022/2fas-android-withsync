@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.twofasAndroidLibrary)
    alias(libs.plugins.twofasCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.twofasLint)
}

android {
    namespace = "com.twofasapp.cloudbackup.webdav"
}

dependencies {
    implementation(project(":cloudbackup:api"))
    implementation(project(":core:common"))
    implementation(project(":core:storage"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:locale"))

    implementation(libs.bundles.compose)
    implementation(libs.bundles.viewModel)
    implementation(libs.kotlinCoroutines)
    implementation(libs.kotlinSerialization)
    implementation(libs.ktorCore)
    implementation(libs.ktorOkhttp)
    implementation(libs.ktorAuth)
}
