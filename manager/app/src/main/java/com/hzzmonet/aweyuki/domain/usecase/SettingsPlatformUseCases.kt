package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.settings.SettingsPlatformRepository
import com.hzzmonet.aweyuki.domain.model.AppearanceSetting
import com.hzzmonet.aweyuki.domain.model.PlatformSetting

class LoadSettingsPlatformUseCase(private val repository: SettingsPlatformRepository) {
    operator fun invoke() = repository.load()
}

class UpdateAppearanceUseCase(private val repository: SettingsPlatformRepository) {
    suspend operator fun invoke(setting: AppearanceSetting) = repository.updateAppearance(setting)
}

class UpdatePlatformSettingUseCase(private val repository: SettingsPlatformRepository) {
    operator fun invoke(setting: PlatformSetting) = repository.updatePlatform(setting)
}

class GetPlatformFeatureStatusUseCase(private val repository: SettingsPlatformRepository) {
    suspend operator fun invoke() = repository.getFeatureStatus()
}

class IsSoftRebootPreferredUseCase(private val repository: SettingsPlatformRepository) {
    operator fun invoke() = repository.isSoftRebootPreferred()
}
