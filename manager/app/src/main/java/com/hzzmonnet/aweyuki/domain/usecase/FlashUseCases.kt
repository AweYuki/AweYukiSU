package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.flash.FlashRepository

class ObserveKernelFlashUseCase(private val repository: FlashRepository) {
    operator fun invoke() = repository.kernelFlashSession
}

class StartKernelFlashUseCase(private val repository: FlashRepository) {
    operator fun invoke(uri: String, selectedSlot: String?, skipKsud: Boolean = false) = repository.startKernelFlash(uri, selectedSlot, skipKsud)
}
