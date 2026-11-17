package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.flash.FlashRepository

class CheckFlashModuleMountUseCase(private val repository: FlashRepository) {
    suspend operator fun invoke(uri: String) = repository.moduleNeedsMount(uri)
}
