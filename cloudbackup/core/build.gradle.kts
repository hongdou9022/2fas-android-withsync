@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.twofasAndroidLibrary)
    alias(libs.plugins.twofasLint)
}

android {
    namespace = "com.twofasapp.cloudbackup.core"
}

dependencies {
    implementation(project(":cloudbackup:api"))
    implementation(project(":core:common"))
    implementation(project(":core:storage"))
    implementation(project(":data:services"))
    implementation(project(":prefs"))

    implementation(libs.kotlinCoroutines)
    implementation(libs.kotlinSerialization)
    implementation(libs.workManager)
    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinCoroutinesTest)
}
