package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.flash.FlashRepository
import com.hzzmonet.aweyuki.domain.model.FlashOperation

class ExecuteFlashOperationUseCase(private val repository: FlashRepository) {
    operator fun invoke(operation: FlashOperation) = repository.execute(operation)
}
