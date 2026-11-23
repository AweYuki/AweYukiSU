package com.hzzmonet.aweyuki.data.application

import android.app.Application
import android.content.pm.PackageManager
import com.hzzmonet.aweyuki.data.packageinfo.InstalledPackageRepository
import com.hzzmonet.aweyuki.data.shell.KsuCliRepository
import com.hzzmonet.aweyuki.domain.model.CompanionAppRecord
import com.hzzmonet.aweyuki.domain.model.CompanionAppsState
import com.hzzmonet.aweyuki.domain.model.CompanionInstalledApp
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class CompanionAppRepository(
    private val application: Application,
    private val installedPackageRepository: InstalledPackageRepository,
    private val ksuCliRepository: KsuCliRepository,
) {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(CompanionAppsState())
    val state: StateFlow<CompanionAppsState> = mutableState.asStateFlow()

    suspend fun refresh(): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            mutableState.update { it.copy(isRefreshing = !it.isLoading) }
            runCatching {
                installedPackageRepository.refresh().getOrThrow()
                val trusted = ksuCliRepository.listCompanions().map { entry ->
                    CompanionAppRecord(
                        packageName = entry.packageName,
                        certSha256 = entry.certSha256,
                        uid = entry.uid,
                        enabled = entry.enabled,
                        label = entry.label,
                    )
                }
                val trustedByPackage = trusted.associateBy { it.packageName }
                val installed = installedPackageRepository.packages.value
                    .asSequence()
                    .filter { !it.isSystem && it.packageName != application.packageName }
                    .distinctBy { it.packageName }
                    .mapNotNull { pkg ->
                        val certSha256 = resolveSigningCertSha256(pkg.packageName, pkg.apkPath)
                            ?: return@mapNotNull null
                        val trustedEntry = trustedByPackage[pkg.packageName]
                        val matchesCert = trustedEntry != null &&
                            trustedEntry.certSha256.equals(certSha256, ignoreCase = true)
                        CompanionInstalledApp(
                            label = pkg.appLabel.ifBlank { pkg.packageName },
                            packageName = pkg.packageName,
                            uid = pkg.uid,
                            certSha256 = certSha256,
                            isTrusted = matchesCert,
                            isEnabled = matchesCert && trustedEntry.enabled,
                        )
                    }
                    .sortedWith(appComparator)
                    .toList()
                mutableState.value = CompanionAppsState(
                    trustedApps = trusted,
                    installedApps = installed,
                    isLoading = false,
                    isRefreshing = false,
                    isSubmitting = false,
                )
            }.onFailure {
                mutableState.update { current ->
                    current.copy(isLoading = false, isRefreshing = false)
                }
            }
        }
    }

    suspend fun authorize(
        packageName: String,
        certSha256: String,
        uid: Int? = null,
        label: String? = null,
    ): Result<Unit> = submit {
        ksuCliRepository.addCompanion(
            packageName = packageName,
            certSha256 = certSha256,
            uid = uid,
            label = label,
            disabled = false,
        )
    }

    suspend fun revoke(
        packageName: String,
        certSha256: String? = null,
    ): Result<Unit> = submit {
        ksuCliRepository.removeCompanion(packageName = packageName, certSha256 = certSha256)
    }

    suspend fun setEnabled(entry: CompanionAppRecord, enabled: Boolean): Result<Unit> = submit {
        ksuCliRepository.addCompanion(
            packageName = entry.packageName,
            certSha256 = entry.certSha256,
            uid = entry.uid.takeIf { it >= 0 },
            label = entry.label.takeIf { it.isNotBlank() },
            disabled = !enabled,
        )
    }

    private suspend fun submit(command: suspend () -> Boolean): Result<Unit> = mutex.withLock {
        withContext(Dispatchers.IO) {
            mutableState.update { it.copy(isSubmitting = true) }
            runCatching {
                check(command())
                val trusted = ksuCliRepository.listCompanions().map { entry ->
                    CompanionAppRecord(
                        packageName = entry.packageName,
                        certSha256 = entry.certSha256,
                        uid = entry.uid,
                        enabled = entry.enabled,
                        label = entry.label,
                    )
                }
                val trustedByPackage = trusted.associateBy { it.packageName }
                mutableState.update { current ->
                    current.copy(
                        trustedApps = trusted,
                        installedApps = current.installedApps.map { app ->
                            val trustedEntry = trustedByPackage[app.packageName]
                            val matchesCert = trustedEntry != null &&
                                trustedEntry.certSha256.equals(app.certSha256, ignoreCase = true)
                            app.copy(
                                isTrusted = matchesCert,
                                isEnabled = matchesCert && trustedEntry.enabled,
                            )
                        }.sortedWith(appComparator),
                        isSubmitting = false,
                    )
                }
            }.onFailure {
                mutableState.update { current -> current.copy(isSubmitting = false) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveSigningCertSha256(packageName: String, apkPath: String): String? {
        val pm = application.packageManager
        val pkgInfo = runCatching {
            pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        }.getOrNull() ?: apkPath.takeIf { it.isNotBlank() }?.let { path ->
            runCatching {
                pm.getPackageArchiveInfo(path, PackageManager.GET_SIGNING_CERTIFICATES)
            }.getOrNull()
        } ?: return null

        val signatures = pkgInfo.signingInfo?.let { info ->
            if (info.hasMultipleSigners()) info.apkContentsSigners else info.signingCertificateHistory
        } ?: pkgInfo.signatures
        val firstCertBytes = signatures?.firstOrNull()?.toByteArray() ?: return null
        val digest = MessageDigest.getInstance("SHA-256").digest(firstCertBytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        val appComparator =
            compareByDescending<CompanionInstalledApp> { it.isTrusted }
                .thenByDescending { it.packageName.contains("kaorios", ignoreCase = true) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
    }
}
