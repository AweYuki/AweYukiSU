package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.susfs.SuSFSRepository

class GetSuSFSStatusUseCase(private val repository: SuSFSRepository) {
    suspend operator fun invoke() = repository.getStatus()
}
