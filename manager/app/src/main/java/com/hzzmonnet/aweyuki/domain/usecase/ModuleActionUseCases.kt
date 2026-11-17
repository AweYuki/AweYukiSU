package com.hzzmonnet.aweyuki.domain.usecase

import com.hzzmonnet.aweyuki.data.module.ModuleActionRepository

class ExecuteModuleActionUseCase(
    private val repository: ModuleActionRepository,
) {
    operator fun invoke(moduleId: String) = repository.execute(moduleId)
}

class SaveModuleActionLogUseCase(
    private val repository: ModuleActionRepository,
) {
    suspend operator fun invoke(content: String) = repository.saveLog(content)
}
