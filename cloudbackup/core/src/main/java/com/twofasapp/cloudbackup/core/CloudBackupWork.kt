package com.twofasapp.cloudbackup.core

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.twofasapp.cloudbackup.api.CloudBackupManager
import com.twofasapp.cloudbackup.api.CloudBackupTrigger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class CloudBackupWork(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    private val manager: CloudBackupManager by inject()

    override suspend fun doWork(): Result {
        val result = manager.backupNow(CloudBackupTrigger.ServicesChanged)
        return if (result.failedProviders.isNotEmpty() && runAttemptCount < 3) Result.retry() else Result.success()
    }
}
