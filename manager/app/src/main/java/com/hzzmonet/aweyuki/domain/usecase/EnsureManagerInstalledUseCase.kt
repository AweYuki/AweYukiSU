package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.application.ApplicationControlRepository

class EnsureManagerInstalledUseCase(
    private val repository: ApplicationControlRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.ensureManagerInstalled()
}
