package com.twofasapp.cloudbackup.webdav.di

import com.twofasapp.cloudbackup.api.CloudBackupProvider
import com.twofasapp.cloudbackup.webdav.WebDavClient
import com.twofasapp.cloudbackup.webdav.WebDavCloudBackupProvider
import com.twofasapp.cloudbackup.webdav.WebDavConfigStore
import com.twofasapp.cloudbackup.webdav.WebDavRepository
import com.twofasapp.cloudbackup.webdav.WebDavRepositoryImpl
import com.twofasapp.cloudbackup.webdav.ui.WebDavSettingsViewModel
import com.twofasapp.common.di.KoinModule
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

class WebDavCloudBackupModule : KoinModule {
    override fun provide() = module {
        single { WebDavConfigStore(get()) }
        single { WebDavClient() }
        singleOf(::WebDavRepositoryImpl) { bind<WebDavRepository>() }
        singleOf(::WebDavCloudBackupProvider) { bind<CloudBackupProvider>() }
        viewModelOf(::WebDavSettingsViewModel)
    }
}
