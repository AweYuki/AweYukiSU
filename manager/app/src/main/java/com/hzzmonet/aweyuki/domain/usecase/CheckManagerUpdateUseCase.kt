package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.update.ManagerUpdateRepository
import com.hzzmonet.aweyuki.domain.model.ManagerUpdateChannel
import com.hzzmonet.aweyuki.domain.model.ManagerUpdateInfo

class CheckManagerUpdateUseCase(
    private val repository: ManagerUpdateRepository,
) {
    suspend operator fun invoke(channel: ManagerUpdateChannel): ManagerUpdateInfo? = when (channel) {
        ManagerUpdateChannel.STABLE -> repository.checkStableUpdate()
        ManagerUpdateChannel.BETA -> repository.checkBetaUpdate()
    }
}
