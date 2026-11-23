package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.application.CompanionAppRepository
import com.hzzmonet.aweyuki.domain.model.CompanionAppRecord

class ObserveCompanionAppsStateUseCase(private val repository: CompanionAppRepository) {
    operator fun invoke() = repository.state
}

class RefreshCompanionAppsUseCase(private val repository: CompanionAppRepository) {
    suspend operator fun invoke() = repository.refresh()
}

class AuthorizeCompanionAppUseCase(private val repository: CompanionAppRepository) {
    suspend operator fun invoke(
        packageName: String,
        certSha256: String,
        uid: Int? = null,
        label: String? = null,
    ) = repository.authorize(packageName, certSha256, uid, label)
}

class RevokeCompanionAppUseCase(private val repository: CompanionAppRepository) {
    suspend operator fun invoke(packageName: String, certSha256: String? = null) = repository.revoke(packageName, certSha256)
}

class SetCompanionAppEnabledUseCase(private val repository: CompanionAppRepository) {
    suspend operator fun invoke(entry: CompanionAppRecord, enabled: Boolean) = repository.setEnabled(entry, enabled)
}
