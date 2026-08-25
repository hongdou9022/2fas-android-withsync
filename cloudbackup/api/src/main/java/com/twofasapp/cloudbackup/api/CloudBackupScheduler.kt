package com.twofasapp.cloudbackup.api

interface CloudBackupScheduler {
    fun scheduleBackup(trigger: CloudBackupTrigger = CloudBackupTrigger.ServicesChanged)
}
