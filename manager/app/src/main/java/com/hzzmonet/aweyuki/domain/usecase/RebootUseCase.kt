package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.application.ApplicationControlRepository

class RebootUseCase(
    private val repository: ApplicationControlRepository,
) {
    suspend operator fun invoke(reason: String = "") = repository.reboot(reason)
}
