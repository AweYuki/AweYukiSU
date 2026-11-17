package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.AppSettingsRepository
import com.hzzmonet.aweyuki.data.startup.ApplicationInitializationRepository
import com.hzzmonet.aweyuki.data.startup.StartupRepository

class InitializeApplicationUseCase(
    private val settingsRepository: AppSettingsRepository,
    private val startupRepository: StartupRepository,
    private val initializationRepository: ApplicationInitializationRepository,
) {
    suspend operator fun invoke() {
        runCatching {
            settingsRepository.preload()
            initializationRepository.initialize()
        }.onSuccess {
            startupRepository.markReady()
        }.onFailure { error ->
            startupRepository.markFailed(error)
        }
    }
}
