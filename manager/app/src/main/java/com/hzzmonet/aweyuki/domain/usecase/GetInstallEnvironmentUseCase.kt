package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.flash.FlashRepository
import com.hzzmonet.aweyuki.domain.model.InstallEnvironment

class GetInstallEnvironmentUseCase(
    private val repository: FlashRepository,
) {
    fun cached(): InstallEnvironment? = repository.installEnvironment.value

    suspend operator fun invoke(forceRefresh: Boolean = false) = repository.getInstallEnvironment(forceRefresh)
}
