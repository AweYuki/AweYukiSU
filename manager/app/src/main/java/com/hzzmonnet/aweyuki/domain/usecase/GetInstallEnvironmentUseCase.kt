package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.flash.FlashRepository
import com.hzzmonnet.aweyuki.domain.model.InstallEnvironment

class GetInstallEnvironmentUseCase(
    private val repository: FlashRepository,
) {
    fun cached(): InstallEnvironment? = repository.installEnvironment.value

    suspend operator fun invoke(forceRefresh: Boolean = false) = repository.getInstallEnvironment(forceRefresh)
}
