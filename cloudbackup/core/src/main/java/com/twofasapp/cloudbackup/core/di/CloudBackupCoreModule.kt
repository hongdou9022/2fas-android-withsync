package com.twofasapp.cloudbackup.core.di

import com.twofasapp.cloudbackup.api.CloudBackupManager
import com.twofasapp.cloudbackup.api.CloudBackupProvider
import com.twofasapp.cloudbackup.api.CloudBackupScheduler
import com.twofasapp.cloudbackup.core.CloudBackupManagerImpl
import com.twofasapp.cloudbackup.core.CloudBackupSchedulerImpl
import com.twofasapp.cloudbackup.core.CloudBackupSettingsStore
import com.twofasapp.common.di.KoinModule
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

class CloudBackupCoreModule : KoinModule {
    override fun provide() = module {
        single { CloudBackupSettingsStore(get()) }
        single {
            CloudBackupManagerImpl(
                backupRepository = get(),
                remoteBackupKeyPreference = get(),
                settingsStore = get(),
                providers = getAll<CloudBackupProvider>(),
            )
        }
        single<CloudBackupManager> { get<CloudBackupManagerImpl>() }
        single<CloudBackupScheduler> {
            CloudBackupSchedulerImpl(
                context = androidContext(),
                providers = getAll<CloudBackupProvider>(),
            )
        }
    }
}
