package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.startup.StartupRepository

class ObserveStartupStateUseCase(
    private val repository: StartupRepository,
) {
    operator fun invoke() = repository.state
}
