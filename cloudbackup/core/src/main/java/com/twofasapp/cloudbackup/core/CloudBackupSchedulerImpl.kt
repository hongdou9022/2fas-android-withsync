package com.twofasapp.cloudbackup.core

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.twofasapp.cloudbackup.api.CloudBackupProvider
import com.twofasapp.cloudbackup.api.CloudBackupScheduler
import com.twofasapp.cloudbackup.api.CloudBackupTrigger
import java.util.concurrent.TimeUnit

internal class CloudBackupSchedulerImpl(
    private val context: Context,
    private val providers: List<CloudBackupProvider>,
) : CloudBackupScheduler {

    override fun scheduleBackup(trigger: CloudBackupTrigger) {
        if (providers.none { it.state().configured && it.state().enabled }) return

        val request = OneTimeWorkRequestBuilder<CloudBackupWork>()
            .setInitialDelay(2, TimeUnit.SECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "CloudBackupAutomatic",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
