package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.update.ManagerUpdateRepository
import com.hzzmonnet.aweyuki.domain.model.ManagerUpdateChannel
import com.hzzmonnet.aweyuki.domain.model.ManagerUpdateInfo

class CheckManagerUpdateUseCase(
    private val repository: ManagerUpdateRepository,
) {
    suspend operator fun invoke(channel: ManagerUpdateChannel): ManagerUpdateInfo? = when (channel) {
        ManagerUpdateChannel.STABLE -> repository.checkStableUpdate()
        ManagerUpdateChannel.BETA -> repository.checkBetaUpdate()
    }
}
