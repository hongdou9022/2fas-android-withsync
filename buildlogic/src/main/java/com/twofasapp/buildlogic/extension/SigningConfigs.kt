package com.twofasapp.buildlogic.extension

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Project
import java.io.File
import java.io.FileInputStream
import java.util.Properties

internal fun Project.applySigningConfigs(
    applicationExtension: ApplicationExtension,
) {
    applicationExtension.apply {

        val localConfig = Properties().apply {
            load(FileInputStream(File(rootProject.rootDir, "config/config.properties")))
        }
        val releaseLocalConfig = (providers.gradleProperty("releaseLocalSigningProperties").orNull
            ?: localConfig.getProperty("releaseLocal.signingProperties"))
            ?.let(::file)
            ?.also { check(it.isFile) { "Release signing properties not found: $it" } }
            ?.let { propertiesFile ->
                Properties().apply {
                    propertiesFile.inputStream().use(::load)
                }
            }

        signingConfigs {
            getByName("debug") {
                storeFile = file("../config/debug_signing.jks")
                storePassword = localConfig.getProperty("debug.storePassword")
                keyAlias = localConfig.getProperty("debug.keyAlias")
                keyPassword = localConfig.getProperty("debug.keyPassword")
            }
            create("release") {
                storeFile = file("../config/release_upload.jks")
                storePassword = localConfig.getProperty("releaseUpload.storePassword")
                keyAlias = localConfig.getProperty("releaseUpload.keyAlias")
                keyPassword = localConfig.getProperty("releaseUpload.keyPassword")
            }
            create("releaseLocal") {
                if (releaseLocalConfig == null) {
                    storeFile = file("../config/release_signing.jks")
                    storePassword = localConfig.getProperty("release.storePassword")
                    keyAlias = localConfig.getProperty("release.keyAlias")
                    keyPassword = localConfig.getProperty("release.keyPassword")
                } else {
                    storeFile = file(releaseLocalConfig.getProperty("storeFile"))
                    storePassword = releaseLocalConfig.getProperty("storePassword")
                    keyAlias = releaseLocalConfig.getProperty("keyAlias")
                    keyPassword = releaseLocalConfig.getProperty("keyPassword")
                    storeType = releaseLocalConfig.getProperty("storeType", "JKS")
                }
            }
        }
    }
}
