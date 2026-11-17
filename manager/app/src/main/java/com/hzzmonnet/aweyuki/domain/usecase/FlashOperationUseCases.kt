package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.flash.FlashRepository
import com.hzzmonnet.aweyuki.domain.model.FlashOperation

class ExecuteFlashOperationUseCase(private val repository: FlashRepository) {
    operator fun invoke(operation: FlashOperation) = repository.execute(operation)
}
