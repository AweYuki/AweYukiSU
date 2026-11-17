package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.flash.FlashRepository

class CheckFlashModuleMountUseCase(private val repository: FlashRepository) {
    suspend operator fun invoke(uri: String) = repository.moduleNeedsMount(uri)
}
