package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.profile.ProfileRepository
import com.hzzmonet.aweyuki.domain.model.AppProfile

class GetAppProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(packageName: String, uid: Int) = repository.getProfile(packageName, uid)
}

class SetAppProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(profile: AppProfile) = repository.setProfile(profile)
}

class GetDefaultUmountModulesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke() = repository.isDefaultUmountModules()
}
