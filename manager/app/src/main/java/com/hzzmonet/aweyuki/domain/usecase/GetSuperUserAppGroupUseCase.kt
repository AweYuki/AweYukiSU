package com.hzzmonet.aweyuki.domain.usecase

import com.hzzmonet.aweyuki.data.packageinfo.SuperUserRepository

class GetSuperUserAppGroupUseCase(private val repository: SuperUserRepository) {
    suspend operator fun invoke(uid: Int, primaryPackageName: String) = repository.getAppGroup(uid, primaryPackageName)
}
