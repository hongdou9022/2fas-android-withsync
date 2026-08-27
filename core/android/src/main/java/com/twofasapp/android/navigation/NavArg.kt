package com.twofasapp.android.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

object NavArg {
    val AddServiceInitRoute = navArgument("AddServiceInitRoute") { type = NavType.StringType; nullable = true; defaultValue = null }
    val Guide = navArgument("Guide") { type = NavType.StringType; }
    val GuideVariantIndex = navArgument("GuideVariantIndex") { type = NavType.IntType; }
    val ServiceId = navArgument("ServiceId") { type = NavType.LongType; defaultValue = 0L }
    val GroupId = navArgument("GroupId") { type = NavType.StringType }
    val EditServiceAction = navArgument("EditServiceAction") {
        type = NavType.StringType
        defaultValue = "Details"
    }
    val ExtensionId = navArgument("ExtensionId") { type = NavType.StringType }
    val TurnOnBackup = navArgument("TurnOnBackup") { type = NavType.BoolType; defaultValue = false; }
    val ImportType = navArgument("ImportType") { type = NavType.StringType }
    val ImportFileUri = navArgument("ImportFileUri") { type = NavType.StringType; nullable = true; defaultValue = null }
    val ImportFileContent = navArgument("ImportFileContent") { type = NavType.StringType; nullable = true; defaultValue = null }
    val CloudBackupProviderId = navArgument("CloudBackupProviderId") { type = NavType.StringType }
}
