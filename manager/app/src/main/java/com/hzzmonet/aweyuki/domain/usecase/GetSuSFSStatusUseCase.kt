package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.susfs.SuSFSRepository

class GetSuSFSStatusUseCase(private val repository: SuSFSRepository) {
    suspend operator fun invoke() = repository.getStatus()
}
