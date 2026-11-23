package com.hzzmonet.aweyuki.domain.model

data class CompanionAppRecord(
    val packageName: String,
    val certSha256: String,
    val uid: Int = -1,
    val enabled: Boolean = true,
    val label: String = "",
)

data class CompanionInstalledApp(
    val label: String,
    val packageName: String,
    val uid: Int,
    val certSha256: String,
    val isTrusted: Boolean,
    val isEnabled: Boolean,
)

data class CompanionAppsState(
    val trustedApps: List<CompanionAppRecord> = emptyList(),
    val installedApps: List<CompanionInstalledApp> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
)

data class CompanionMetadata(
    val managerPackage: String,
    val serviceUid: Int,
    val ipcSocketPath: String,
    val ksuVersion: Int?,
    val kernelUapiVersion: Int?,
    val hookType: String,
    val isLkmMode: Boolean?,
    val isSelinuxHideEnabled: Boolean,
)
