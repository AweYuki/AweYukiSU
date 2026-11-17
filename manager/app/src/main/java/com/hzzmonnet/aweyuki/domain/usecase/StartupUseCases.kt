package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.startup.StartupRepository

class ObserveStartupStateUseCase(
    private val repository: StartupRepository,
) {
    operator fun invoke() = repository.state
}
