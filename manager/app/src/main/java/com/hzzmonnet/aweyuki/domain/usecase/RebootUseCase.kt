package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.application.ApplicationControlRepository

class RebootUseCase(
    private val repository: ApplicationControlRepository,
) {
    suspend operator fun invoke(reason: String = "") = repository.reboot(reason)
}
